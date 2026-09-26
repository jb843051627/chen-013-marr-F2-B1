package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TMarrDueTaskMapper;
import com.fc.v2.model.auto.TMarrDueTask;
import com.fc.v2.model.custom.MarrDueRound;
import com.fc.v2.service.ITMarrDueTaskService;

/**
 * 冷静期与领证期到期催办条目 Service业务层处理（scheduling-job 形状：周期执行）
 *
 * 口径只有一条：从 due_at（届满那天的最后一刻）往前数 amount 个自然日，压到当天就算该催；
 * 挑（listDue）与回写（runOnce）共用同一把尺。要改口径只能改 amount，不许把 due_at 往后拖。
 * 出手只认每天 09:00-11:30、15:00-17:30 两格，格外压到脸上也按下不动。
 * 一宗两位当事人各送一条，只送一位不算办完；送不出去的挂起并当场写明缘由，
 * 后面的挨个轮得上，不许叫一件卡住全场。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TMarrDueTaskServiceImpl implements ITMarrDueTaskService {

    private static final Logger log = LoggerFactory.getLogger(TMarrDueTaskServiceImpl.class);

    /** 条目情形：待发 */
    private static final int STATUS_WAIT = 0;
    /** 条目情形：已发（两位当事人都送到才算） */
    private static final int STATUS_DONE = 1;
    /** 条目情形：挂起（发不出去，已当场写明缘由） */
    private static final int STATUS_FAIL = 2;

    /** 出手时段：09:00-11:30、15:00-17:30（含两端，按 HHmm 整数比） */
    private static final int AM_FROM = 900;
    private static final int AM_TO = 1130;
    private static final int PM_FROM = 1500;
    private static final int PM_TO = 1730;

    /** 单条结局：仍压待发 */
    private static final int OUTCOME_PENDING = 0;
    /** 单条结局：已发 */
    private static final int OUTCOME_SENT = 1;
    /** 单条结局：挂起 */
    private static final int OUTCOME_SUSPENDED = 2;

    @javax.annotation.Resource
    private TMarrDueTaskMapper marrDueTaskMapper;

    @Override
    public TMarrDueTask selectTMarrDueTaskById(Long id) {
        return this.marrDueTaskMapper.selectById(id);
    }

    @Override
    public List<TMarrDueTask> selectTMarrDueTaskList(QueryWrapper<TMarrDueTask> queryWrapper) {
        return this.marrDueTaskMapper.selectList(queryWrapper);
    }

    /**
     * 时段比较统一走**墙钟字符串**（yyyy-MM-dd HH:mm:ss）：JDBC 的 serverTimezone 与本机
     * 时区不对称，直接把 java.util.Date 作参数会整体偏移，使"恰好到期"这类边界用例错判。
     */
    private static String ts(Date d) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
    }

    /**
     * 启口时刻：从届满那一刻（那天的最后一刻）往前数 amount 个自然日，落到那天的开口。
     * 压到当天就算该催——全处只许这一种说法。
     */
    private static Date openAt(Date dueAt, BigDecimal amount) {
        long days = amount == null ? 0L : amount.longValue();
        Calendar c = Calendar.getInstance();
        c.setTime(dueAt);
        c.add(Calendar.DATE, -(int) Math.min(days, Integer.MAX_VALUE));
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /** 该时刻是否已压到启口（due_at 没记的永远压不到） */
    private static boolean isDue(TMarrDueTask r, Date at) {
        if (r.getDueAt() == null) {
            return false;
        }
        return ts(openAt(r.getDueAt(), r.getAmount())).compareTo(ts(at)) <= 0;
    }

    /** 该时刻是否在出手时段内（只认每天两格，含两端） */
    private static boolean inSendWindow(Date at) {
        int hm = Integer.parseInt(new SimpleDateFormat("HHmm").format(at));
        return (hm >= AM_FROM && hm <= AM_TO) || (hm >= PM_FROM && hm <= PM_TO);
    }

    /** 未删全量：待发的是候选，已发/挂起的留作"重复提交只认头一遍"的账底 */
    private List<TMarrDueTask> selectAlive() {
        List<TMarrDueTask> all = this.marrDueTaskMapper.selectList(
                new QueryWrapper<TMarrDueTask>().eq("del_flag", 0));
        return all == null ? Collections.<TMarrDueTask>emptyList() : all;
    }

    /** 已压到启口、尚未处理的条目（不看时段；按落账先后排，先落的先轮） */
    private static List<TMarrDueTask> filterOpen(List<TMarrDueTask> alive, Date at) {
        List<TMarrDueTask> open = new ArrayList<TMarrDueTask>();
        for (TMarrDueTask r : alive) {
            if (r.getStatus() != null && r.getStatus() == STATUS_WAIT && isDue(r, at)) {
                open.add(r);
            }
        }
        open.sort(Comparator.comparing(TMarrDueTask::getId, Comparator.nullsLast(Long::compareTo)));
        return open;
    }

    @Override
    public List<TMarrDueTask> listDue(Date at) {
        // 不在格内一条也不捞：即便已经压到脸上也按下不动，等下一轮
        if (!inSendWindow(at)) {
            return Collections.emptyList();
        }
        return filterOpen(selectAlive(), at);
    }

    @Override
    public int runOnce(Date at) {
        return doRun(at, "scan").getSent();
    }

    @Override
    public MarrDueRound runOnce(Date at, String scene) {
        return doRun(at, scene);
    }

    /**
     * 一轮执行：捞出哪些、送出几条、挂起几条都在这里算完再回写，跑没跑成看给回的局面。
     */
    private MarrDueRound doRun(Date at, String scene) {
        MarrDueRound round = new MarrDueRound();
        round.setScene(scene);
        boolean inWindow = inSendWindow(at);
        round.setInWindow(inWindow);

        // 候选与账底同源（同一批对象引用）：本轮送出的送达标记当场就长在账底上
        List<TMarrDueTask> alive = selectAlive();
        List<TMarrDueTask> open = filterOpen(alive, at);
        if (open.isEmpty()) {
            // 一条都不到期的日子：答一声本轮无事就算跑完，不是故障
            round.setMessage("本轮无事");
            return round;
        }
        if (!inWindow) {
            round.setPending(open.size());
            round.setMessage("未到出手时段，当日待发" + open.size() + "条按下不动，等下一轮");
            return round;
        }

        // 同事项账底按单号归堆，供"同一事项同一收信人重复提交只认头一遍"核对
        Map<String, List<TMarrDueTask>> byItemNo = new HashMap<String, List<TMarrDueTask>>();
        for (TMarrDueTask r : alive) {
            if (r.getItemNo() == null) {
                continue;
            }
            byItemNo.computeIfAbsent(r.getItemNo(), k -> new ArrayList<TMarrDueTask>()).add(r);
        }

        int sent = 0;
        int suspended = 0;
        int pending = 0;
        for (TMarrDueTask r : open) {
            int outcome = OUTCOME_PENDING;
            try {
                // 落库成功才认结局：屏上摆的数须与账上回写一致
                outcome = processOne(r, byItemNo.get(r.getItemNo()));
            } catch (Exception e) {
                // 单条出岔跳过继续，不许叫一件卡住全场；没落上账的仍按待发算
                log.warn("到期催办单条处理出岔，跳过继续：id={} itemNo={}", r.getId(), r.getItemNo(), e);
            }
            if (outcome == OUTCOME_SENT) {
                sent++;
            } else if (outcome == OUTCOME_SUSPENDED) {
                suspended++;
            } else {
                pending++;
            }
        }
        round.setPending(pending);
        round.setSent(sent);
        round.setSuspended(suspended);
        round.setMessage("当日待发" + pending + "条、已发" + sent + "条、挂起" + suspended + "条");
        return round;
    }

    /**
     * 处理一条：落定不催、电话为空挂起、重复提交只认头一遍、两位各送一条。
     * 返回的结局以落库为准（updateById 成了才算数），单条怎么出岔都不许卡住全场。
     */
    private int processOne(TMarrDueTask r, List<TMarrDueTask> sameItem) {
        // 本宗月初已按撤回落定：不再催，挂起写明缘由
        if (r.getSettledFlag() != null && r.getSettledFlag() == 1) {
            return suspend(r, "本宗已按撤回落定，不再催办") ? OUTCOME_SUSPENDED : OUTCOME_PENDING;
        }
        // 联系电话为空：送不出去，挂起写明缘由
        boolean aBlank = isBlank(r.getPhoneA());
        boolean bBlank = isBlank(r.getPhoneB());
        if (aBlank || bBlank) {
            return suspend(r, aBlank && bBlank ? "双方联系电话均为空，送不出去"
                    : aBlank ? "甲方联系电话为空，送不出去" : "乙方联系电话为空，送不出去")
                    ? OUTCOME_SUSPENDED : OUTCOME_PENDING;
        }
        // 同一事项对同一收信人重复提交：账上只认头一遍，后到的不补第二句、也不改写先落的那条
        boolean coveredA = coveredByLedger(sameItem, r, r.getPhoneA());
        boolean coveredB = coveredByLedger(sameItem, r, r.getPhoneB());
        if (coveredA && coveredB && !isSent(r.getSentA()) && !isSent(r.getSentB())) {
            return suspend(r, "同一事项同一收信人当日已有催办在账，重复提交不补第二句")
                    ? OUTCOME_SUSPENDED : OUTCOME_PENDING;
        }
        // 两位各送一条；已送的那一格不重送，只补没收到的那一位
        if (!isSent(r.getSentA())) {
            if (coveredA) {
                r.setSentA(1);
            } else {
                try {
                    deliver(r, r.getPhoneA());
                    r.setSentA(1);
                } catch (Exception e) {
                    log.warn("到期催办甲方送出未妥，留下轮单独补：id={} itemNo={}", r.getId(), r.getItemNo(), e);
                }
            }
        }
        if (!isSent(r.getSentB())) {
            if (coveredB) {
                r.setSentB(1);
            } else {
                try {
                    deliver(r, r.getPhoneB());
                    r.setSentB(1);
                } catch (Exception e) {
                    log.warn("到期催办乙方送出未妥，留下轮单独补：id={} itemNo={}", r.getId(), r.getItemNo(), e);
                }
            }
        }
        // 两位都送到才算已发；只送到一位仍压待发，下轮只补没收到的那一位
        boolean done = isSent(r.getSentA()) && isSent(r.getSentB());
        r.setStatus(done ? STATUS_DONE : STATUS_WAIT);
        if (this.marrDueTaskMapper.updateById(r) <= 0) {
            return OUTCOME_PENDING;
        }
        return done ? OUTCOME_SENT : OUTCOME_PENDING;
    }

    /**
     * 出手送一条。默认落日志留痕（对接短信网关另起一处，不在这里展开）；
     * 送不出去抛异常，这一格留下轮单独补，不耽误另一位。
     */
    protected void deliver(TMarrDueTask row, String phone) {
        log.info("到期催办送出：单号[{}] 收信[{}] 事由[{}]", row.getItemNo(), phone, row.getContent());
    }

    /** 挂起：当场写明为什么挂，后头的挨个轮得上；落库成了才算挂上 */
    private boolean suspend(TMarrDueTask r, String reason) {
        r.setStatus(STATUS_FAIL);
        r.setHangReason(reason);
        return this.marrDueTaskMapper.updateById(r) > 0;
    }

    /** 同一事项同一收信人是否已有催办在账（认别的条目送出的那一格，不认自己） */
    private static boolean coveredByLedger(List<TMarrDueTask> sameItem, TMarrDueTask self, String phone) {
        if (sameItem == null || phone == null) {
            return false;
        }
        for (TMarrDueTask o : sameItem) {
            if (o == self) {
                continue;
            }
            if (self.getId() != null && self.getId().equals(o.getId())) {
                continue;
            }
            if (isSent(o.getSentA()) && phone.equals(o.getPhoneA())) {
                return true;
            }
            if (isSent(o.getSentB()) && phone.equals(o.getPhoneB())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSent(Integer sent) {
        return sent != null && sent == 1;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
