package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TMarrRiskLineMapper;
import com.fc.v2.model.auto.TMarrRiskLine;
import com.fc.v2.service.ITMarrRiskLineService;

/**
 * 婚姻家庭辅导风险评估判据线 Service业务层处理（rule-eval 形状）
 *
 * 时效口径只有一处（{@link #effectiveAt}）：未删、现行（status=0）、已启用（eff_start≤at）、
 * 未交棒（at&lt;eff_end，交棒之日不含）。挑可用规则（listAvailable）、单规则求值（evaluate）、
 * 多规则抢档（evaluateTop）、单条可用（usable）、条数角标（countAvailable）全走这一处，
 * 谁也不许另算一份——超时/回滚两边口径对不上的岔子就出在各处各判。
 * 压线取高一档（等于上限归上一格），与影像卡完整度折算同一把尺，阈值都出自卷宗基础档案那一档说法。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TMarrRiskLineServiceImpl implements ITMarrRiskLineService {

    /** 判据情形：现行 */
    private static final int STATUS_ACTIVE = 0;

    /** 风险分值合法区间（含两端） */
    private static final BigDecimal SCORE_MIN = BigDecimal.ZERO;
    private static final BigDecimal SCORE_MAX = new BigDecimal("100");

    /** 优先级降序；并列按 ruleCode 降序，再并列按 id 降序（确定性） */
    private static final Comparator<TMarrRiskLine> PRIORITY_THEN_CODE_DESC = new Comparator<TMarrRiskLine>() {
        @Override
        public int compare(TMarrRiskLine a, TMarrRiskLine b) {
            int pa = a.getPriority() == null ? 0 : a.getPriority();
            int pb = b.getPriority() == null ? 0 : b.getPriority();
            if (pa != pb) {
                return pb - pa;
            }
            String ca = a.getRuleCode() == null ? "" : a.getRuleCode();
            String cb = b.getRuleCode() == null ? "" : b.getRuleCode();
            int byCode = cb.compareTo(ca);
            if (byCode != 0) {
                return byCode;
            }
            long ia = a.getId() == null ? 0L : a.getId();
            long ib = b.getId() == null ? 0L : b.getId();
            return Long.compare(ib, ia);
        }
    };

    /** 同代号多版本并存时，启用晚的新版压过启用早的旧版；并列 id 大者新 */
    private static final Comparator<TMarrRiskLine> NEW_VERSION_FIRST = new Comparator<TMarrRiskLine>() {
        @Override
        public int compare(TMarrRiskLine a, TMarrRiskLine b) {
            Date sa = a.getEffStart();
            Date sb = b.getEffStart();
            if (sa != null && sb != null && !sa.equals(sb)) {
                return sb.after(sa) ? 1 : -1;
            }
            if (sa != sb) {
                return sa == null ? 1 : -1;
            }
            long ia = a.getId() == null ? 0L : a.getId();
            long ib = b.getId() == null ? 0L : b.getId();
            return Long.compare(ib, ia);
        }
    };

    @javax.annotation.Resource
    private TMarrRiskLineMapper marrRiskLineMapper;

    /**
     * 时段比较统一走**墙钟字符串**（yyyy-MM-dd HH:mm:ss）：JDBC 的 serverTimezone 与本机
     * 时区不对称，直接把 java.util.Date 作参数会整体偏移，使"恰好交棒"这类边界用例错判。
     */
    private static String ts(Date d) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
    }

    @Override
    public TMarrRiskLine selectTMarrRiskLineById(Long id) {
        return this.marrRiskLineMapper.selectById(id);
    }

    @Override
    public List<TMarrRiskLine> listAvailable(Date at) {
        if (at == null) {
            return new ArrayList<TMarrRiskLine>();
        }
        List<TMarrRiskLine> all = this.marrRiskLineMapper.selectList(
                new QueryWrapper<TMarrRiskLine>().eq("del_flag", 0));
        List<TMarrRiskLine> avail = new ArrayList<TMarrRiskLine>();
        for (TMarrRiskLine r : all) {
            if (effectiveAt(r, at)) {
                avail.add(r);
            }
        }
        Collections.sort(avail, PRIORITY_THEN_CODE_DESC);
        return avail;
    }

    @Override
    public int evaluate(String ruleCode, BigDecimal input, Date at) {
        if (ruleCode == null || ruleCode.trim().isEmpty() || input == null || at == null) {
            return 0;
        }
        if (!scoreInRange(input)) {
            return 0;
        }
        TMarrRiskLine rule = findRule(ruleCode, at);
        if (rule == null) {
            // 该时刻没有生效版本：不允许拿已停用/未启用/已交棒的旧判据顶包
            return 0;
        }
        return levelOf(rule, input);
    }

    @Override
    public int evaluateTop(BigDecimal input, Date at) {
        if (input == null || at == null || !scoreInRange(input)) {
            return 0;
        }
        List<TMarrRiskLine> avail = listAvailable(at);
        if (avail.isEmpty()) {
            return 0;
        }
        // listAvailable 已按抢档顺位排好，取头一条即可
        return levelOf(avail.get(0), input);
    }

    @Override
    public boolean usable(Long id, Date at) {
        if (id == null || at == null) {
            return false;
        }
        TMarrRiskLine r = this.marrRiskLineMapper.selectById(id);
        return r != null && effectiveAt(r, at);
    }

    @Override
    public int countAvailable(Date at) {
        // 与列表同一处口径，角标数不许另查一份
        return listAvailable(at).size();
    }

    /** 该判据在 at 时刻是否作数：现行 + 已启用 + 未交棒。全处时效只许这一种说法。 */
    private static boolean effectiveAt(TMarrRiskLine r, Date at) {
        if (r == null || r.getStatus() == null || r.getStatus() != STATUS_ACTIVE) {
            return false;
        }
        String now = ts(at);
        if (r.getEffStart() != null && ts(r.getEffStart()).compareTo(now) > 0) {
            return false;
        }
        // 交棒之日不含：恰好踩点那一刻即视为已交棒，不得延后一轮再用
        if (r.getEffEnd() != null && now.compareTo(ts(r.getEffEnd())) >= 0) {
            return false;
        }
        return true;
    }

    private TMarrRiskLine findRule(String ruleCode, Date at) {
        List<TMarrRiskLine> sameCode = new ArrayList<TMarrRiskLine>();
        for (TMarrRiskLine r : listAvailable(at)) {
            if (ruleCode.equals(r.getRuleCode())) {
                sameCode.add(r);
            }
        }
        if (sameCode.isEmpty()) {
            return null;
        }
        Collections.sort(sameCode, NEW_VERSION_FIRST);
        return sameCode.get(0);
    }

    private static boolean scoreInRange(BigDecimal input) {
        return input.compareTo(SCORE_MIN) >= 0 && input.compareTo(SCORE_MAX) <= 0;
    }

    /**
     * 分档：压线算上一格（与影像卡完整度折算同一把尺）——
     * 不足一档归一档；正好压在一档封顶上按二档论；越过三档封顶归四档。
     * 某档封顶没维护时跳过该档，交下一档接着判。
     */
    static int levelOf(TMarrRiskLine rule, BigDecimal input) {
        if (rule.getTh1Max() != null && input.compareTo(rule.getTh1Max()) < 0) {
            return 1;
        }
        if (rule.getTh2Max() != null && input.compareTo(rule.getTh2Max()) < 0) {
            return 2;
        }
        if (rule.getTh3Max() != null && input.compareTo(rule.getTh3Max()) < 0) {
            return 3;
        }
        return 4;
    }
}
