package com.fc.v2.service.impl;

import com.fc.v2.mapper.auto.TMarrFixCardMapper;
import com.fc.v2.model.auto.TMarrFixCard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 差错更正事务卡单测：推进/回退一次只过一道口；末道再推才转出、转出可收回；
 * 关口与落定同次落库；已结案推退改删一律不收；删除走逻辑删。
 *
 * @author fuce
 * @date 2026-09-26
 */
@ExtendWith(MockitoExtension.class)
class TMarrFixCardServiceImplTest {

    @Mock
    private TMarrFixCardMapper marrFixCardMapper;

    @InjectMocks
    private TMarrFixCardServiceImpl service;

    private static final int ACTIVE = 0;
    private static final int TRANSFERRED = 1;
    private static final int CLOSED = 2;

    private TMarrFixCard card(long id, Integer stage, Integer status, Integer delFlag) {
        TMarrFixCard r = new TMarrFixCard();
        r.setId(id);
        r.setStage(stage);
        r.setStatus(status);
        r.setDelFlag(delFlag);
        when(marrFixCardMapper.selectById(id)).thenReturn(r);
        return r;
    }

    @Test
    @DisplayName("推进:一次只过一道口,在办落定不变")
    void advanceOneStageAtATime() {
        TMarrFixCard r = card(1L, 0, ACTIVE, 0);
        TMarrFixCard out = service.advance(1L, "受理");
        assertEquals(1, out.getStage(), "只进一档,不许跳两档");
        assertEquals(ACTIVE, out.getStatus(), "环节与落定同一口径");
        assertEquals("推进至第1道", out.getLastAction());
        verify(marrFixCardMapper).updateById(r);
    }

    @Test
    @DisplayName("转出:压在末道(第5道)再推才转出,环节钉在末道")
    void transferOnlyAfterLastStage() {
        TMarrFixCard r = card(1L, 5, ACTIVE, 0);
        TMarrFixCard out = service.advance(1L, null);
        assertEquals(5, out.getStage());
        assertEquals(TRANSFERRED, out.getStatus());
        assertEquals("转出", out.getLastAction());
    }

    @Test
    @DisplayName("转出后:不再往前推;回退可收回成在办")
    void transferredCanBeRevokedButNotAdvanced() {
        TMarrFixCard r = card(1L, 5, TRANSFERRED, 0);
        assertNull(service.advance(1L, null), "已转出不再推");

        TMarrFixCard back = service.rollback(1L, null);
        assertEquals(5, back.getStage());
        assertEquals(ACTIVE, back.getStatus());
        assertEquals("收回转出", back.getLastAction());
    }

    @Test
    @DisplayName("回退:在办的一次退一道;头一道无头可退返null")
    void rollbackOneStageAtATime() {
        TMarrFixCard r = card(1L, 3, ACTIVE, 0);
        TMarrFixCard out = service.rollback(1L, null);
        assertEquals(2, out.getStage());
        assertEquals(ACTIVE, out.getStatus());

        TMarrFixCard head = card(2L, 0, ACTIVE, 0);
        assertNull(service.rollback(2L, null), "已在头一道,无头可退");
    }

    @Test
    @DisplayName("已结案:推、退、改、删一律不收")
    void closedCardIsFrozen() {
        TMarrFixCard r = card(1L, 4, CLOSED, 0);
        assertNull(service.advance(1L, null));
        assertNull(service.rollback(1L, null));
        assertFalse(service.updateContent(1L, "改"));
        assertFalse(service.remove(1L));
        verify(marrFixCardMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("删除:在办卡只做逻辑删除,已删/查无返false")
    void removeIsLogical() {
        when(marrFixCardMapper.updateById(any())).thenReturn(1);
        TMarrFixCard r = card(1L, 2, ACTIVE, 0);
        assertTrue(service.remove(1L));
        assertEquals(1, r.getDelFlag());
        assertEquals("删除", r.getLastAction());

        card(2L, 2, ACTIVE, 1);
        assertFalse(service.remove(2L), "已删的不重复落");
    }

    @Test
    @DisplayName("记事:在办/已转出可改;改记事不挪关口")
    void updateContentRespectsStage() {
        when(marrFixCardMapper.updateById(any())).thenReturn(1);
        TMarrFixCard r = card(1L, 2, ACTIVE, 0);
        assertTrue(service.updateContent(1L, "补一句"));
        assertEquals("补一句", r.getContent());
        assertEquals(2, r.getStage());
    }
}
