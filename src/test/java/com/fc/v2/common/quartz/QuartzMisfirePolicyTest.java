package com.fc.v2.common.quartz;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.TriggerBuilder;
import org.quartz.Trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 超时（错失触发）补偿口径单测：账上 misfire_policy 怎么记，触发器就怎么补，
 * 全处只许 QuartzSchedulerUtil.applyMisfire 一处拿主意。
 * 守的是"两边对超时的判断不统一"这条：不许账上记"补一次"、触发器却静默"不补"。
 *
 * @author fuce
 * @date 2026-09-26
 */
class QuartzMisfirePolicyTest {

    private static final String CRON = "0 0,30 9,10,11,15,16,17 * * ?";

    /** 调私有静态 applyMisfire，再建成触发器，取它的超时补偿指令码 */
    private static int misfireInstruction(String policy) throws Exception {
        Method m = QuartzSchedulerUtil.class.getDeclaredMethod(
                "applyMisfire", CronScheduleBuilder.class, String.class);
        m.setAccessible(true);
        CronScheduleBuilder builder = CronScheduleBuilder.cronSchedule(CRON);
        CronScheduleBuilder applied = (CronScheduleBuilder) m.invoke(null, builder, policy);
        CronTrigger t = TriggerBuilder.newTrigger()
                .withIdentity("t", "g")
                .withSchedule(applied)
                .build();
        return t.getMisfireInstruction();
    }

    @Test
    @DisplayName("超时:账上记2(补一次),触发器就补触发一次——种子任务的口径")
    void fireAndProceedFollowsLedger() throws Exception {
        assertEquals(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW,
                misfireInstruction("2"), "misfire_policy=2 必须对应补触发一次");
    }

    @Test
    @DisplayName("超时:账上记1(立即补)对应IGNORE,3(不补)对应DO_NOTHING")
    void ignoreAndDoNothingFollowLedger() throws Exception {
        assertEquals(Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY,
                misfireInstruction("1"), "misfire_policy=1 必须对应立即补偿");
        assertEquals(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING,
                misfireInstruction("3"), "misfire_policy=3 必须对应不补");
    }

    @Test
    @DisplayName("超时:账上没记/空/认不出,走默认智能口径,不许静默落到不补")
    void blankOrUnknownFallsBackToSmartPolicy() throws Exception {
        assertEquals(CronTrigger.MISFIRE_INSTRUCTION_SMART_POLICY, misfireInstruction(null));
        assertEquals(CronTrigger.MISFIRE_INSTRUCTION_SMART_POLICY, misfireInstruction(""));
        assertEquals(CronTrigger.MISFIRE_INSTRUCTION_SMART_POLICY, misfireInstruction("9"));
    }
}
