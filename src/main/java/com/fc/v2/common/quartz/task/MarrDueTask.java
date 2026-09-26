package com.fc.v2.common.quartz.task;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fc.v2.model.custom.MarrDueRound;
import com.fc.v2.service.ITMarrDueTaskService;

/**
 * 冷静期与领证期到期催办：定时扫描具体工作类。
 * 这一轮催办只由定时扫描那一头叫醒，页面上不留补一条、改一条的地方。
 * 该不该出手、捞出哪些、送出几条、挂起几条，全在服务层那个执行方法里算完再回写；
 * 这里只负责叫醒并把给回的本轮局面落日志（屏上摆的数须与给回的一致）。
 *
 * 调用目标：marrDueTask.scan()
 * 建议 cron：0 0,30 9,10,11,15,16,17 * * ?（格内每半小时一轮；时段口径以服务层为准）
 *
 * @author fuce
 * @date 2026-09-26
 */
@Component("marrDueTask")
public class MarrDueTask {

    private static final Logger log = LoggerFactory.getLogger(MarrDueTask.class);

    @Autowired
    private ITMarrDueTaskService marrDueTaskService;

    /**
     * 定时扫描叫醒一轮。无参（ quartz 按 invokeTarget 直接调 ），
     * 时刻以当前为准；一条都不到期的日子，局面答"本轮无事"，不算故障。
     */
    public void scan() {
        MarrDueRound round = marrDueTaskService.runOnce(new Date(), "scan");
        log.info("到期催办本轮局面：{}", round);
    }
}
