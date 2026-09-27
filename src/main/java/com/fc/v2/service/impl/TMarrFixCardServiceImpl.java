package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TMarrFixCardMapper;
import com.fc.v2.model.auto.TMarrFixCard;
import com.fc.v2.service.ITMarrFixCardService;

/**
 * 婚姻登记差错更正事务卡 Service业务层处理（state-machine 形状：单据流转）
 *
 * 关口（stage 0..5）与落定（status 0在办 1已转出 2已结案）同一刻算完、同一次落库，
 * 台账上摆的环节与状态必须同出这一处，不许推了关口忘改落定。
 * 推进一次只过一道口：0..5 逐档走，压在第 5 档再推才转出（status=1）；
 * 回退一次只收一道口，转出的可收回成在办；已结案一笔定死，推、退、改、删一律不收。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TMarrFixCardServiceImpl implements ITMarrFixCardService {

    /** 关口共 0..5 六道，第 5 道是末道，过了末道才转出 */
    private static final int MAX_STAGE = 5;

    /** 卡的落定：0在办 1已转出 2已结案（与表上注释同一套说法） */
    private static final int STATUS_ACTIVE = 0;
    private static final int STATUS_TRANSFERRED = 1;
    private static final int STATUS_CLOSED = 2;

    @javax.annotation.Resource
    private TMarrFixCardMapper marrFixCardMapper;

    @Override
    public TMarrFixCard selectTMarrFixCardById(Long id) {
        TMarrFixCard r = this.marrFixCardMapper.selectById(id);
        if (r != null && Integer.valueOf(1).equals(r.getDelFlag())) {
            return null;
        }
        return r;
    }

    @Override
    public List<TMarrFixCard> selectTMarrFixCardList(QueryWrapper<TMarrFixCard> queryWrapper) {
        QueryWrapper<TMarrFixCard> qw = queryWrapper == null ? new QueryWrapper<TMarrFixCard>() : queryWrapper;
        // 台账只摆在册的卡，删过的不许混进来
        return this.marrFixCardMapper.selectList(qw.eq("del_flag", 0));
    }

    /**
     * 推进一道口：在办的卡逐档走；压在末道（stage=5）再推则转出。
     * 已转出的不再往前推（要收回收口走回退）；已结案的一律不收。被拒返回 null。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMarrFixCard advance(Long id, String remark) {
        TMarrFixCard r = this.marrFixCardMapper.selectById(id);
        if (r == null || Integer.valueOf(1).equals(r.getDelFlag())) {
            return null;
        }
        Integer status = r.getStatus() == null ? STATUS_ACTIVE : r.getStatus();
        if (status == STATUS_CLOSED || status == STATUS_TRANSFERRED) {
            return null;
        }
        int stage = r.getStage() == null ? 0 : Math.max(0, r.getStage());
        String action;
        if (stage >= MAX_STAGE) {
            // 末道已过：落"已转出"，环节钉在末道不悬空
            r.setStage(MAX_STAGE);
            r.setStatus(STATUS_TRANSFERRED);
            action = "转出";
        } else {
            r.setStage(stage + 1);
            r.setStatus(STATUS_ACTIVE);
            action = "推进至第" + (stage + 1) + "道";
        }
        r.setLastAction(action);
        r.setRemark(remark);
        this.marrFixCardMapper.updateById(r);
        return r;
    }

    /**
     * 回退一道口：在办的卡退回上一档（已在头档无头可退返 null）；
     * 已转出的收回成在办（环节钉在末道）；已结案的一笔定死不许翻。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMarrFixCard rollback(Long id, String remark) {
        TMarrFixCard r = this.marrFixCardMapper.selectById(id);
        if (r == null || Integer.valueOf(1).equals(r.getDelFlag())) {
            return null;
        }
        Integer status = r.getStatus() == null ? STATUS_ACTIVE : r.getStatus();
        if (status == STATUS_CLOSED) {
            return null;
        }
        int stage = r.getStage() == null ? 0 : Math.max(0, r.getStage());
        String action;
        if (status == STATUS_TRANSFERRED) {
            // 收回转出：回到末道在办，接下去可继续逐道回退
            r.setStage(MAX_STAGE);
            r.setStatus(STATUS_ACTIVE);
            action = "收回转出";
        } else if (stage <= 0) {
            // 已在头一道，无头可退
            return null;
        } else {
            r.setStage(stage - 1);
            r.setStatus(STATUS_ACTIVE);
            action = "回退至第" + (stage - 1) + "道";
        }
        r.setLastAction(action);
        r.setRemark(remark);
        this.marrFixCardMapper.updateById(r);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateContent(Long id, String remark) {
        TMarrFixCard r = this.marrFixCardMapper.selectById(id);
        if (r == null || Integer.valueOf(1).equals(r.getDelFlag())) {
            return false;
        }
        // 已结案一笔定死，纠错记事也不许再动
        if (Integer.valueOf(STATUS_CLOSED).equals(r.getStatus())) {
            return false;
        }
        r.setContent(remark);
        r.setLastAction("更正记事");
        return this.marrFixCardMapper.updateById(r) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean remove(Long id) {
        TMarrFixCard r = this.marrFixCardMapper.selectById(id);
        if (r == null || Integer.valueOf(1).equals(r.getDelFlag())) {
            return false;
        }
        // 已结案的卡留档，不许删
        if (Integer.valueOf(STATUS_CLOSED).equals(r.getStatus())) {
            return false;
        }
        // 与全处台账同一口径：只做逻辑删除，账底还在
        r.setDelFlag(1);
        r.setLastAction("删除");
        return this.marrFixCardMapper.updateById(r) > 0;
    }

}
