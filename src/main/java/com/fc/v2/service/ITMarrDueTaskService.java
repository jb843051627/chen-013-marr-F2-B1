package com.fc.v2.service;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TMarrDueTask;
import com.fc.v2.model.custom.MarrDueRound;

/**
 * 冷静期与领证期到期催办条目 Service接口（scheduling-job 形状：周期执行，无增删改查入口）
 *
 * 倒计时一律从届满那天的最后一刻往前数"提前几日"，压到当天就算该催；
 * 挑的时候这么数，回写的时候也这么数，全处只许这一种说法。
 * 出手只认每天 09:00-11:30、15:00-17:30 两格，格外压到脸上也按下不动。
 * 一宗两位当事人各送一条；送不出去的挂起并当场写明缘由，不许叫一件卡住全场。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITMarrDueTaskService {

    /** 按主键回查条目 */
    TMarrDueTask selectTMarrDueTaskById(Long id);

    /** 只读台账（发没发出去页面上看得见；不提供补一条、改一条的入口） */
    List<TMarrDueTask> selectTMarrDueTaskList(QueryWrapper<TMarrDueTask> queryWrapper);

    /** 该时刻可处理的条目（执行窗口内 + 已压到启口 + 尚未处理） */
    List<TMarrDueTask> listDue(Date at);

    /**
     * 执行一次，返回**成功条数**；单条失败跳过继续。
     * 一条都不到期的日子返回 0（本轮无事），不算故障。
     */
    int runOnce(Date at);

    /**
     * 同名重载：执行一轮并回本轮局面（当日待发/已发/挂起，只摆本轮的事）。
     * 屏上摆的数须与给回的一致。scene 记哪一头叫醒的这一轮（定时扫描=scan）。
     */
    MarrDueRound runOnce(Date at, String scene);
}
