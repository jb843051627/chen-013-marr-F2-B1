package com.fc.v2.service.impl;

import com.fc.v2.mapper.auto.TMarrCertBillMapper;
import com.fc.v2.model.auto.TMarrCertBill;
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
 * 逐级核准单单测：环节与情形同次落库；任一人/两名点齐口径；末层点齐核讫；
 * 已讫/已打回重复动作只认头一遍；退回上一层清签字且无头不可退。
 *
 * @author fuce
 * @date 2026-09-26
 */
@ExtendWith(MockitoExtension.class)
class TMarrCertBillServiceImplTest {

    @Mock
    private TMarrCertBillMapper marrCertBillMapper;

    @InjectMocks
    private TMarrCertBillServiceImpl service;

    private static final int RUNNING = 0;
    private static final int PASS = 1;
    private static final int VETO = 2;

    private TMarrCertBill bill(long id, Integer node, Integer mode, Integer need, Integer signed, Integer status) {
        TMarrCertBill r = new TMarrCertBill();
        r.setId(id);
        r.setNodeNo(node);
        r.setSignMode(mode);
        r.setNeedCount(need);
        r.setSignCount(signed);
        r.setStatus(status);
        r.setDelFlag(0);
        when(marrCertBillMapper.selectById(id)).thenReturn(r);
        return r;
    }

    @Test
    @DisplayName("任一人:一票即过层,进下一环节,新层签字清零")
    void orModeOneVoteAdvancesNode() {
        TMarrCertBill r = bill(1L, 0, 0, null, 0, RUNNING);
        TMarrCertBill out = service.approve(1L, "甲", null);
        assertEquals(1, out.getNodeNo());
        assertEquals(0, out.getSignCount());
        assertEquals(RUNNING, out.getStatus());
        verify(marrCertBillMapper).updateById(r);
    }

    @Test
    @DisplayName("两名点齐:头一票压在本层在核,第二票才进下一环节")
    void andModeRequiresBoth() {
        TMarrCertBill r = bill(1L, 0, 1, null, 0, RUNNING);

        TMarrCertBill first = service.approve(1L, "甲", null);
        assertEquals(0, first.getNodeNo(), "头一票不外挪环节");
        assertEquals(1, first.getSignCount());
        assertEquals(RUNNING, first.getStatus());

        TMarrCertBill second = service.approve(1L, "乙", null);
        assertEquals(1, second.getNodeNo(), "点齐才进一层");
        assertEquals(0, second.getSignCount());
    }

    @Test
    @DisplayName("应签人数:need_count 说了算,3 名点齐才过层")
    void needCountDrivesGate() {
        TMarrCertBill r = bill(1L, 0, 1, 3, 1, RUNNING);

        TMarrCertBill second = service.approve(1L, "乙", null);
        assertEquals(0, second.getNodeNo());
        assertEquals(2, second.getSignCount());

        TMarrCertBill third = service.approve(1L, "丙", null);
        assertEquals(1, third.getNodeNo());
        assertEquals(0, third.getSignCount());
    }

    @Test
    @DisplayName("末层点齐:环节钉在第3层,情形落已核讫")
    void lastNodeCompletes() {
        TMarrCertBill r = bill(1L, 3, 0, null, 0, RUNNING);
        TMarrCertBill out = service.approve(1L, "终审", null);
        assertEquals(3, out.getNodeNo());
        assertEquals(PASS, out.getStatus());
        assertEquals(1, out.getSignCount());
    }

    @Test
    @DisplayName("幂等:已核讫再签只认头一遍,不回写;已打回不受理签批")
    void terminalStatesRejectNewVotes() {
        TMarrCertBill done = bill(1L, 3, 0, null, 1, PASS);
        assertSame(done, service.approve(1L, "多事者", null));
        verify(marrCertBillMapper, never()).updateById(any());

        TMarrCertBill vetoed = bill(2L, 1, 0, null, 0, VETO);
        assertNull(service.approve(2L, "多事者", null));
        verify(marrCertBillMapper, never()).updateById(vetoed);
    }

    @Test
    @DisplayName("否决:在核中一票打回;已讫不许翻案;已打回重复否决只认头一遍")
    void rejectLifecycle() {
        TMarrCertBill r = bill(1L, 1, 0, null, 0, RUNNING);
        TMarrCertBill out = service.reject(1L, "审者", "材料不全");
        assertEquals(VETO, out.getStatus());
        assertEquals(1, out.getNodeNo(), "环节就地不动,看得出停在哪层");

        assertSame(out, service.reject(1L, "再审者", null), "重复否决不回写");

        TMarrCertBill passed = bill(2L, 3, 0, null, 1, PASS);
        assertNull(service.reject(2L, "审者", null), "已讫不许翻案");
    }

    @Test
    @DisplayName("退回:退一层并清本层签字;头一层无头可退返null;已讫不退")
    void rollbackOneNodeOnly() {
        TMarrCertBill r = bill(1L, 2, 1, 2, 2, RUNNING);
        TMarrCertBill out = service.rollback(1L, "补材料");
        assertEquals(1, out.getNodeNo());
        assertEquals(0, out.getSignCount(), "退回的层要重点");
        assertEquals(RUNNING, out.getStatus());

        TMarrCertBill head = bill(2L, 0, 0, null, 0, RUNNING);
        assertNull(service.rollback(2L, null), "已在头一层,无头可退");

        TMarrCertBill done = bill(3L, 3, 0, null, 1, PASS);
        assertNull(service.rollback(3L, null), "已讫不办退回");
    }

    @Test
    @DisplayName("缺签批人:签批与否决都不收,返null不落库")
    void blankApproverRejected() {
        bill(1L, 0, 0, null, 0, RUNNING);
        assertNull(service.approve(1L, "  ", null));
        assertNull(service.reject(1L, null, null));
        verify(marrCertBillMapper, never()).updateById(any());
    }
}
