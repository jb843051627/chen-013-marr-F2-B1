package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.fc.v2.mapper.auto.TMarrRiskLineMapper;
import com.fc.v2.model.auto.TMarrRiskLine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 风险判据线单测：时效口径五处同源（现行+已启用+未交棒，交棒日不含）；
 * 压线取上一档，与卷宗影像卡完整度折算同一把尺；抢档顺位生效；越界/无版本返 0。
 *
 * @author fuce
 * @date 2026-09-26
 */
@ExtendWith(MockitoExtension.class)
class TMarrRiskLineServiceImplTest {

    @Mock
    private TMarrRiskLineMapper marrRiskLineMapper;

    @InjectMocks
    private TMarrRiskLineServiceImpl service;

    private static Date t(String s) {
        return Timestamp.valueOf(s);
    }

    private static TMarrRiskLine rule(long id, String code, String start, String end,
                                      int status, int priority, String t1, String t2, String t3) {
        TMarrRiskLine r = new TMarrRiskLine();
        r.setId(id);
        r.setRuleCode(code);
        r.setEffStart(start == null ? null : t(start));
        r.setEffEnd(end == null ? null : t(end));
        r.setStatus(status);
        r.setPriority(priority);
        r.setDelFlag(0);
        r.setTh1Max(new BigDecimal(t1));
        r.setTh2Max(new BigDecimal(t2));
        r.setTh3Max(new BigDecimal(t3));
        return r;
    }

    @Test
    @DisplayName("时效:启用那一刻算生效;交棒那一刻(不含)算失效,正好到点不得延后一轮")
    void effectiveWindowBoundaries() {
        TMarrRiskLine r = rule(1L, "R1", "2026-03-01 00:00:00", "2026-04-01 00:00:00", 0, 1, "5", "12", "30");
        when(marrRiskLineMapper.selectById(1L)).thenReturn(r);

        assertTrue(service.usable(1L, t("2026-03-01 00:00:00")), "启用那一刻即生效");
        assertTrue(service.usable(1L, t("2026-03-31 23:59:59")), "交棒前一刻仍生效");
        assertFalse(service.usable(1L, t("2026-04-01 00:00:00")), "交棒之日不含,正好踩点即失效");
        assertFalse(service.usable(1L, t("2026-02-28 23:59:59")), "启用前不生效");
    }

    @Test
    @DisplayName("时效:已停用的判据五处口径都不认(列表/求值/抢档/单条/条数)")
    void stoppedRuleIgnoredEverywhere() {
        TMarrRiskLine stopped = rule(1L, "R1", "2026-03-01 00:00:00", null, 1, 1, "5", "12", "30");
        when(marrRiskLineMapper.selectList(any())).thenReturn(Collections.singletonList(stopped));
        when(marrRiskLineMapper.selectById(1L)).thenReturn(stopped);

        Date at = t("2026-03-10 00:00:00");
        assertTrue(service.listAvailable(at).isEmpty());
        assertEquals(0, service.countAvailable(at));
        assertEquals(0, service.evaluate("R1", new BigDecimal("3"), at));
        assertEquals(0, service.evaluateTop(new BigDecimal("3"), at));
        assertFalse(service.usable(1L, at));
    }

    @Test
    @DisplayName("分档:压线取上一档,与影像卡完整度折算同一把尺")
    void levelBoundaryTakesUpperTier() {
        TMarrRiskLine r = rule(1L, "R1", "2026-03-01 00:00:00", null, 0, 1, "5", "12", "30");
        when(marrRiskLineMapper.selectList(any())).thenReturn(Collections.singletonList(r));

        Date at = t("2026-03-10 00:00:00");
        assertEquals(1, service.evaluate("R1", new BigDecimal("4.99"), at));
        assertEquals(2, service.evaluate("R1", new BigDecimal("5"), at), "压一档线取二档");
        assertEquals(2, service.evaluate("R1", new BigDecimal("11.99"), at));
        assertEquals(3, service.evaluate("R1", new BigDecimal("12"), at), "压二档线取三档");
        assertEquals(3, service.evaluate("R1", new BigDecimal("29.99"), at));
        assertEquals(4, service.evaluate("R1", new BigDecimal("30"), at), "压三档线取四档");
    }

    @Test
    @DisplayName("求值:越界/缺参/无生效版本一律返0,不许拿旧判据顶包")
    void invalidInputsAndNoVersionReturnZero() {
        TMarrRiskLine old = rule(1L, "R1", "2026-01-01 00:00:00", "2026-02-01 00:00:00", 0, 1, "5", "12", "30");
        when(marrRiskLineMapper.selectList(any())).thenReturn(Collections.singletonList(old));

        Date at = t("2026-03-10 00:00:00");
        assertEquals(0, service.evaluate("R1", new BigDecimal("3"), at), "已交棒的版本不顶包");
        assertEquals(0, service.evaluate("R1", new BigDecimal("-1"), t("2026-01-10 00:00:00")));
        assertEquals(0, service.evaluate("R1", new BigDecimal("100.01"), t("2026-01-10 00:00:00")));
        assertEquals(0, service.evaluate("  ", new BigDecimal("3"), at));
        assertEquals(0, service.evaluateTop(null, at));
    }

    @Test
    @DisplayName("抢档:多条生效时优先级高者判;并列再按ruleCode降序")
    void evaluateTopPicksHighestPriority() {
        TMarrRiskLine low = rule(1L, "A", "2026-03-01 00:00:00", null, 0, 5, "50", "80", "90");
        TMarrRiskLine high = rule(2L, "B", "2026-03-01 00:00:00", null, 0, 9, "5", "12", "30");
        when(marrRiskLineMapper.selectList(any())).thenReturn(Arrays.asList(low, high));

        Date at = t("2026-03-10 00:00:00");
        List<TMarrRiskLine> avail = service.listAvailable(at);
        assertEquals("B", avail.get(0).getRuleCode(), "优先级9压过5");
        assertEquals(2, service.countAvailable(at));
        // 10 分在高优先判据下是二档、在低优先判据下是一档，取高者的口径
        assertEquals(2, service.evaluateTop(new BigDecimal("10"), at));
    }

    @Test
    @DisplayName("同代号多版本:都生效时启用晚的新版压过旧版")
    void sameCodeNewerVersionWins() {
        TMarrRiskLine older = rule(1L, "R1", "2026-03-01 00:00:00", null, 0, 1, "5", "12", "30");
        TMarrRiskLine newer = rule(2L, "R1", "2026-03-05 00:00:00", null, 0, 1, "10", "20", "40");
        when(marrRiskLineMapper.selectList(any())).thenReturn(Arrays.asList(older, newer));

        // 8 分：旧版二档（压线口径）、新版一档，认新版即一档
        assertEquals(1, service.evaluate("R1", new BigDecimal("8"), t("2026-03-10 00:00:00")));
    }
}
