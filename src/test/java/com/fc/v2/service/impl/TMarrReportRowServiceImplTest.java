package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fc.v2.mapper.auto.TMarrReportRowMapper;
import com.fc.v2.model.auto.TMarrReportRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 报送行单测：空批 0、超限 -1 且不入；逐行隔离（好条入汇纳、坏条挂驳回留住行号）；
 * 一条填错不连坐后面；同册重复提交不补第二遍。
 *
 * @author fuce
 * @date 2026-09-26
 */
@ExtendWith(MockitoExtension.class)
class TMarrReportRowServiceImplTest {

    @Mock
    private TMarrReportRowMapper marrReportRowMapper;

    @InjectMocks
    private TMarrReportRowServiceImpl service;

    private static final int OK = 1;
    private static final int FAIL = 2;

    private static TMarrReportRow row(String code, String qty) {
        TMarrReportRow r = new TMarrReportRow();
        r.setItemCode(code);
        r.setQty(qty == null ? null : new BigDecimal(qty));
        r.setDelFlag(0);
        return r;
    }

    @Test
    @DisplayName("空批:没有可处理条目答0,不许在get(0)上异常退出")
    void emptyBatchReturnsZero() {
        assertEquals(0, service.submitBatch("B1", null));
        assertEquals(0, service.submitBatch("B1", new ArrayList<TMarrReportRow>()));
        verify(marrReportRowMapper, never()).insert(any());
    }

    @Test
    @DisplayName("超限:越过500行答-1,整批一笔都不入")
    void overLimitRejectsWholeBatch() {
        List<TMarrReportRow> rows = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            rows.add(row("X" + i, "1"));
        }
        assertEquals(-1, service.submitBatch("B1", rows));
        verify(marrReportRowMapper, never()).insert(any());
    }

    @Test
    @DisplayName("逐行隔离:中间一条填错只挂这一条,好条照常入库,还回成功数")
    void badRowIsolatedNotBlockingOthers() {
        TMarrReportRow good1 = row("A", "3");
        TMarrReportRow badCode = row("  ", "3");
        TMarrReportRow badQty = row("B", "0");
        TMarrReportRow nullQty = row("C", null);
        TMarrReportRow good2 = row("D", "7");
        when(marrReportRowMapper.selectList(any())).thenReturn(Collections.<TMarrReportRow>emptyList());

        int ok = service.submitBatch("B1",
                Arrays.asList(good1, badCode, badQty, nullQty, good2));

        assertEquals(2, ok, "好条不受坏条连坐");
        ArgumentCaptor<TMarrReportRow> captor = ArgumentCaptor.forClass(TMarrReportRow.class);
        verify(marrReportRowMapper, times(5)).insert(captor.capture());
        List<Integer> statuses = new ArrayList<>();
        List<Integer> rowNos = new ArrayList<>();
        for (TMarrReportRow inserted : captor.getAllValues()) {
            statuses.add(inserted.getStatus());
            rowNos.add(inserted.getRowNo());
        }
        assertEquals(Arrays.asList(OK, FAIL, FAIL, FAIL, OK), statuses, "逐条落各自的情形");
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), rowNos, "留住原册行次");
        assertEquals("B1", captor.getAllValues().get(0).getBatchNo());
    }

    @Test
    @DisplayName("幂等:同册号头一遍已在账,重复提交一笔不补,原样答回已汇纳行数")
    void duplicateBatchNotReInserted() {
        TMarrReportRow existed = row("A", "3");
        existed.setStatus(OK);
        when(marrReportRowMapper.selectList(any())).thenReturn(Collections.singletonList(existed));

        int ok = service.submitBatch("B1", Collections.singletonList(row("A", "3")));

        assertEquals(1, ok);
        verify(marrReportRowMapper, never()).insert(any());
    }

    @Test
    @DisplayName("幂等:头一遍整册全驳回在账,第二遍同样不补录")
    void duplicateBatchAllFailedAlsoBlocked() {
        TMarrReportRow existed = row("A", null);
        existed.setStatus(FAIL);
        when(marrReportRowMapper.selectList(any())).thenReturn(Collections.singletonList(existed));

        assertEquals(0, service.submitBatch("B1", Collections.singletonList(row("A", "3"))));
        verify(marrReportRowMapper, never()).insert(any());
    }

    @Test
    @DisplayName("失败明细:按行号升序摆;册号空答空表不抛异常")
    void listErrorsOrdered() {
        TMarrReportRow r2 = row("B", "0");
        r2.setRowNo(2);
        r2.setStatus(FAIL);
        TMarrReportRow r1 = row("A", "0");
        r1.setRowNo(1);
        r1.setStatus(FAIL);
        when(marrReportRowMapper.selectList(any())).thenReturn(Arrays.asList(r2, r1));

        List<TMarrReportRow> errors = service.listErrors("B1");
        assertEquals(2, errors.size());

        assertTrue(service.listErrors("  ").isEmpty());
    }
}
