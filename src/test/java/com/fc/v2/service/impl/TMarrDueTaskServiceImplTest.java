package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fc.v2.mapper.auto.TMarrDueTaskMapper;
import com.fc.v2.model.auto.TMarrDueTask;
import com.fc.v2.model.custom.MarrDueRound;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 到期催办单测：倒计时从最后一刻往前数、压到当天算该催；
 * 只认 09:00-11:30、15:00-17:30 两格；一宗两位各送一条、没收到的那位单独补；
 * 空电话与已撤回落定挂起写明缘由；重复提交只认头一遍；本轮催过的下轮不捞；
 * 一条都不到期答"本轮无事"不算故障；屏上摆的数须与给回的一致。
 *
 * @author fuce
 * @date 2026-09-26
 */
@ExtendWith(MockitoExtension.class)
class TMarrDueTaskServiceImplTest {

    @Mock
    private TMarrDueTaskMapper marrDueTaskMapper;

    @InjectMocks
    private TMarrDueTaskServiceImpl service;

    // ---------------- 造数 ----------------

    /** 时刻：ymd 那天的 hm（HHmm 整数，如 900=09:00、1130=11:30） */
    private static Date at(String ymd, int hm) {
        return java.sql.Timestamp.valueOf(String.format("%s %02d:%02d:00", ymd, hm / 100, hm % 100));
    }

    /** 届满日：记到那天的最后一刻 */
    private static Date due(String ymd) {
        return java.sql.Timestamp.valueOf(ymd + " 23:59:59");
    }

    private static TMarrDueTask row(long id, String itemNo, String dueYmd, String amount,
                                    String phoneA, String phoneB) {
        TMarrDueTask r = new TMarrDueTask();
        r.setId(id);
        r.setItemNo(itemNo);
        r.setDueAt(dueYmd == null ? null : due(dueYmd));
        r.setAmount(amount == null ? null : new BigDecimal(amount));
        r.setPhoneA(phoneA);
        r.setPhoneB(phoneB);
        r.setSentA(0);
        r.setSentB(0);
        r.setSettledFlag(0);
        r.setStatus(0);
        r.setDelFlag(0);
        r.setContent("离婚登记届满提醒");
        return r;
    }

    /** 录下每一次出手、可对指定号码装作出岔的催办服务（送未妥留下轮单独补的场景） */
    private static class RecordingService extends TMarrDueTaskServiceImpl {
        final List<String> delivered = new ArrayList<String>();
        final Set<String> failFor = new HashSet<String>();
        TMarrDueTaskMapper mapper;

        @Override
        protected void deliver(TMarrDueTask row, String phone) {
            if (failFor.contains(phone)) {
                throw new RuntimeException("网关未妥");
            }
            delivered.add(phone);
        }
    }

    private RecordingService recordingService(List<TMarrDueTask> rows) {
        TMarrDueTaskMapper mapper = mock(TMarrDueTaskMapper.class);
        when(mapper.selectList(any())).thenReturn(rows);
        when(mapper.updateById(any())).thenReturn(1);
        RecordingService svc = new RecordingService();
        ReflectionTestUtils.setField(svc, "marrDueTaskMapper", mapper);
        svc.mapper = mapper;
        return svc;
    }

    // ---------------- 倒计时口径：从最后一刻往前数，压到当天算该催 ----------------

    @Test
    @DisplayName("倒计时:提前3日,压到当天(03-28)算该催,前一天(03-27)不算")
    void dueBoundaryCountsTheDay() {
        TMarrDueTask r = row(1L, "D001", "2026-03-31", "3", "13100000001", "13100000002");
        when(marrDueTaskMapper.selectList(any())).thenReturn(Collections.singletonList(r));

        assertEquals(1, service.listDue(at("2026-03-28", 900)).size(), "压到启口当天算该催");
        assertEquals(1, service.listDue(at("2026-03-31", 1000)).size(), "届满当天仍该催");
        assertEquals(0, service.listDue(at("2026-03-27", 1700)).size(), "早一天还不到启口");
    }

    @Test
    @DisplayName("倒计时:提前0日(含未填)届满当天才算该催")
    void dueBoundaryZeroAmount() {
        TMarrDueTask zero = row(1L, "D001", "2026-03-31", "0", "13100000001", "13100000002");
        TMarrDueTask none = row(2L, "D002", "2026-03-31", null, "13100000003", "13100000004");
        when(marrDueTaskMapper.selectList(any())).thenReturn(Arrays.asList(zero, none));

        assertEquals(2, service.listDue(at("2026-03-31", 900)).size(), "压到届满当天算该催");
        assertEquals(0, service.listDue(at("2026-03-30", 900)).size(), "提前0日早一天不算");
    }

    // ---------------- 出手时段：只认每天两格 ----------------

    @Test
    @DisplayName("时段:只认09:00-11:30、15:00-17:30两格,含两端,格外一条不捞")
    void sendWindowBoundaries() {
        TMarrDueTask r = row(1L, "D001", "2026-03-31", "10", "13100000001", "13100000002");
        when(marrDueTaskMapper.selectList(any())).thenReturn(Collections.singletonList(r));

        String day = "2026-03-28";
        assertEquals(0, service.listDue(at(day, 859)).size(), "08:59 还没到格");
        assertEquals(1, service.listDue(at(day, 900)).size(), "09:00 开格");
        assertEquals(1, service.listDue(at(day, 1130)).size(), "11:30 格尾仍算");
        assertEquals(0, service.listDue(at(day, 1131)).size(), "11:31 出格");
        assertEquals(0, service.listDue(at(day, 1200)).size(), "午休不推");
        assertEquals(1, service.listDue(at(day, 1500)).size(), "15:00 开格");
        assertEquals(1, service.listDue(at(day, 1730)).size(), "17:30 格尾仍算");
        assertEquals(0, service.listDue(at(day, 1731)).size(), "17:31 出格");
        assertEquals(0, service.listDue(at(day, 2300)).size(), "夜里不推");
    }

    @Test
    @DisplayName("格外:条目压到脸上也按下不动,等下一轮,一条都不回写")
    void outsideWindowHoldsEverything() {
        TMarrDueTask r = row(1L, "D001", "2026-03-31", "10", "13100000001", "13100000002");
        when(marrDueTaskMapper.selectList(any())).thenReturn(Collections.singletonList(r));

        MarrDueRound lunch = service.runOnce(at("2026-03-28", 1200), "scan");
        assertFalse(lunch.isInWindow());
        assertEquals(1, lunch.getPending(), "压到脸上的压着等下轮");
        assertEquals(0, lunch.getSent());
        assertEquals(0, lunch.getSuspended());
        assertTrue(lunch.getMessage().contains("按下不动"));

        MarrDueRound night = service.runOnce(at("2026-03-28", 2200), "scan");
        assertEquals(1, night.getPending(), "夜里同样按下不动");

        verify(marrDueTaskMapper, never()).updateById(any());
    }

    // ---------------- 空轮：本轮无事不算故障 ----------------

    @Test
    @DisplayName("空轮:一条都不到期,答本轮无事算跑完,不许报成故障")
    void emptyRoundIsNotAFailure() {
        when(marrDueTaskMapper.selectList(any())).thenReturn(Collections.<TMarrDueTask>emptyList());

        assertEquals(0, service.runOnce(at("2026-03-28", 1000)), "空轮回 0，不许抛异常");

        MarrDueRound round = service.runOnce(at("2026-03-28", 1000), "scan");
        assertEquals("本轮无事", round.getMessage());
        assertEquals(0, round.getPending());
        assertEquals(0, round.getSent());
        assertEquals(0, round.getSuspended());
        verify(marrDueTaskMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("空轮:没记届满日的条目永远压不到,也算本轮无事")
    void nullDueAtNeverDue() {
        TMarrDueTask r = row(1L, "D001", null, "3", "13100000001", "13100000002");
        when(marrDueTaskMapper.selectList(any())).thenReturn(Collections.singletonList(r));

        MarrDueRound round = service.runOnce(at("2026-03-28", 1000), "scan");
        assertEquals("本轮无事", round.getMessage());
        verify(marrDueTaskMapper, never()).updateById(any());
    }

    // ---------------- 双人各送一条 ----------------

    @Test
    @DisplayName("双人:一宗两位各送一条,两位都送到才算已发")
    void bothPartiesEachGetOne() {
        TMarrDueTask r = row(1L, "D001", "2026-03-31", "3", "13100000001", "13100000002");
        RecordingService svc = recordingService(Collections.singletonList(r));

        MarrDueRound round = svc.runOnce(at("2026-03-28", 1000), "scan");

        assertEquals(2, svc.delivered.size(), "两位各送一条");
        assertTrue(svc.delivered.contains("13100000001"));
        assertTrue(svc.delivered.contains("13100000002"));
        assertEquals(1, r.getSentA());
        assertEquals(1, r.getSentB());
        assertEquals(1, r.getStatus(), "两位都送到才算已发");
        assertEquals(1, round.getSent());
        assertEquals(0, round.getPending());
        assertEquals("当日待发0条、已发1条、挂起0条", round.getMessage());
    }

    @Test
    @DisplayName("单独补:甲方当日已收到,下轮只补乙方那一条,甲方不重发")
    void makeUpOnlyTheMissingOne() {
        TMarrDueTask r = row(1L, "D001", "2026-03-31", "3", "13100000001", "13100000002");
        r.setSentA(1);
        r.setStatus(0);
        RecordingService svc = recordingService(Collections.singletonList(r));

        int sent = svc.runOnce(at("2026-03-28", 1000));

        assertEquals(1, sent);
        assertEquals(Collections.singletonList("13100000002"), svc.delivered, "只补没收到的那一位");
        assertEquals(1, r.getStatus());
    }

    @Test
    @DisplayName("送未妥:乙方这格留下轮,甲方已送不重发,下轮只补乙方")
    void deliverFailureRetriesOnlyMissingNextRound() {
        TMarrDueTask r = row(1L, "D001", "2026-03-31", "3", "13100000001", "13100000002");
        RecordingService first = recordingService(Collections.singletonList(r));
        first.failFor.add("13100000002");

        MarrDueRound round1 = first.runOnce(at("2026-03-28", 1000), "scan");
        assertEquals(Collections.singletonList("13100000001"), first.delivered, "乙方未妥不影响甲方");
        assertEquals(1, r.getSentA(), "甲方已送落账");
        assertEquals(0, r.getSentB());
        assertEquals(0, r.getStatus(), "只送到一位仍压待发");
        assertEquals(1, round1.getPending());
        assertEquals(0, round1.getSent());

        // 下一轮：同一本账（甲方已送），只补乙方
        RecordingService second = recordingService(Collections.singletonList(r));
        int sent = second.runOnce(at("2026-03-28", 1030));
        assertEquals(1, sent);
        assertEquals(Collections.singletonList("13100000002"), second.delivered, "甲方当日已收到,不重发");
        assertEquals(1, r.getStatus());
    }

    // ---------------- 挂起：送不出去的得有交代 ----------------

    @Test
    @DisplayName("挂起:乙方电话为空,这条挂起写明缘由,不卡住后面那条")
    void emptyPhoneSuspendsWithReason() {
        TMarrDueTask bad = row(1L, "D001", "2026-03-31", "3", "13100000001", "");
        TMarrDueTask good = row(2L, "D002", "2026-03-31", "3", "13100000003", "13100000004");
        RecordingService svc = recordingService(Arrays.asList(bad, good));

        MarrDueRound round = svc.runOnce(at("2026-03-28", 1000), "scan");

        assertEquals(2, bad.getStatus(), "送不出去的挂起");
        assertTrue(bad.getHangReason().contains("乙方联系电话为空"), "当场写明为什么挂");
        assertEquals(1, good.getStatus(), "后面的挨个轮得上,不许叫一件卡住全场");
        assertEquals(1, round.getSent());
        assertEquals(1, round.getSuspended());
        assertEquals(0, round.getPending());
    }

    @Test
    @DisplayName("挂起:本宗月初已按撤回落定,不再催,挂起写明缘由")
    void settledMatterSuspends() {
        TMarrDueTask r = row(1L, "D001", "2026-03-31", "3", "13100000001", "13100000002");
        r.setSettledFlag(1);
        RecordingService svc = recordingService(Collections.singletonList(r));

        MarrDueRound round = svc.runOnce(at("2026-03-28", 1000), "scan");

        assertEquals(2, r.getStatus());
        assertTrue(r.getHangReason().contains("撤回落定"));
        assertTrue(svc.delivered.isEmpty(), "落定的宗一条也不送");
        assertEquals(0, round.getSent());
        assertEquals(1, round.getSuspended());
    }

    // ---------------- 幂等：重复提交只认头一遍 ----------------

    @Test
    @DisplayName("重复提交:同事项同收信人当日已有催办在账,后到的不补第二句、不改写先落的那条")
    void duplicateSubmissionCountsOnlyFirst() {
        TMarrDueTask first = row(1L, "D001", "2026-03-31", "3", "13100000001", "13100000002");
        first.setSentA(1);
        first.setSentB(1);
        first.setStatus(1);
        TMarrDueTask dupe = row(2L, "D001", "2026-03-31", "3", "13100000001", "13100000002");
        RecordingService svc = recordingService(Arrays.asList(first, dupe));

        MarrDueRound round = svc.runOnce(at("2026-03-28", 1000), "scan");

        assertTrue(svc.delivered.isEmpty(), "后到的不补第二句");
        assertEquals(2, dupe.getStatus(), "重复提交的那条挂起交代");
        assertTrue(dupe.getHangReason().contains("重复提交"));
        assertEquals(0, round.getSent());
        assertEquals(1, round.getSuspended());
        // 先落的那一条不许被改写
        verify(svc.mapper, never()).updateById(first);
        verify(svc.mapper, times(1)).updateById(dupe);
    }

    @Test
    @DisplayName("重复提交:同轮捞出两条同事项,先落的送,后到的挂起,收信人只收一遍")
    void sameRoundDuplicateRows() {
        TMarrDueTask first = row(1L, "D001", "2026-03-31", "3", "13100000001", "13100000002");
        TMarrDueTask dupe = row(2L, "D001", "2026-03-31", "3", "13100000001", "13100000002");
        RecordingService svc = recordingService(Arrays.asList(first, dupe));

        MarrDueRound round = svc.runOnce(at("2026-03-28", 1000), "scan");

        assertEquals(2, svc.delivered.size(), "同一收信人只收一遍");
        assertEquals(1, first.getStatus(), "先落的那条已发");
        assertEquals(2, dupe.getStatus(), "后到的挂起");
        assertEquals(1, round.getSent());
        assertEquals(1, round.getSuspended());
    }

    @Test
    @DisplayName("幂等:本轮催过的条目下轮不再捞出来重催")
    void sentRowNeverRePicked() {
        TMarrDueTask r = row(1L, "D001", "2026-03-31", "3", "13100000001", "13100000002");
        r.setSentA(1);
        r.setSentB(1);
        r.setStatus(1);
        when(marrDueTaskMapper.selectList(any())).thenReturn(Collections.singletonList(r));

        MarrDueRound round = service.runOnce(at("2026-03-28", 1000), "scan");
        assertEquals("本轮无事", round.getMessage());
        assertEquals(0, service.listDue(at("2026-03-28", 1000)).size());
        verify(marrDueTaskMapper, never()).updateById(any());
    }

    // ---------------- 单条出岔不卡全场 ----------------

    @Test
    @DisplayName("单条出岔跳过继续:一条回写出岔,后面的挨个轮得上")
    void singleFailureDoesNotBlockOthers() {
        TMarrDueTask bad = row(1L, "D001", "2026-03-31", "3", "13100000001", "13100000002");
        TMarrDueTask good = row(2L, "D002", "2026-03-31", "3", "13100000003", "13100000004");
        when(marrDueTaskMapper.selectList(any())).thenReturn(Arrays.asList(bad, good));
        when(marrDueTaskMapper.updateById(bad)).thenThrow(new RuntimeException("库抖动"));
        when(marrDueTaskMapper.updateById(good)).thenReturn(1);

        MarrDueRound round = service.runOnce(at("2026-03-28", 1000), "scan");

        assertEquals(1, round.getSent(), "好那条照常送出");
        assertEquals(1, round.getPending(), "出岔那条仍压待发等下轮");
        assertEquals(1, good.getStatus());
    }

    // ---------------- 屏上摆的数须与给回的一致 ----------------

    @Test
    @DisplayName("局面:当日待发/已发/挂起只摆本轮,两个同名方法给回的数一致")
    void screenMatchesWhatExecuteReturns() {
        // 一 sent、一挂起、一留下轮，同一局面两种给法
        TMarrDueTask ok = row(1L, "D001", "2026-03-31", "3", "13100000001", "13100000002");
        TMarrDueTask hang = row(2L, "D002", "2026-03-31", "3", "13100000003", null);
        TMarrDueTask half = row(3L, "D003", "2026-03-31", "3", "13100000005", "13100000006");
        RecordingService svc = recordingService(Arrays.asList(ok, hang, half));
        svc.failFor.add("13100000006");

        MarrDueRound round = svc.runOnce(at("2026-03-28", 1000), "scan");
        assertEquals(1, round.getSent());
        assertEquals(1, round.getSuspended());
        assertEquals(1, round.getPending());
        assertEquals("当日待发1条、已发1条、挂起1条", round.getMessage());
        assertTrue(round.isInWindow());

        // 同一局面用已有执行方法再跑一遍（同样的账），给回的成功条数须一致
        TMarrDueTask ok2 = row(1L, "D001", "2026-03-31", "3", "13100000001", "13100000002");
        TMarrDueTask hang2 = row(2L, "D002", "2026-03-31", "3", "13100000003", null);
        TMarrDueTask half2 = row(3L, "D003", "2026-03-31", "3", "13100000005", "13100000006");
        RecordingService svc2 = recordingService(Arrays.asList(ok2, hang2, half2));
        svc2.failFor.add("13100000006");
        assertEquals(round.getSent(), svc2.runOnce(at("2026-03-28", 1000)), "已有执行方法与同名重载给回的数一致");
    }
}
