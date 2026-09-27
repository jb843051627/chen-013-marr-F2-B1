package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TMarrReportRowMapper;
import com.fc.v2.model.auto.TMarrReportRow;
import com.fc.v2.service.ITMarrReportRowService;

/**
 * 登记统计报送册核收转存行 Service业务层处理（batch-process 形状：整批提交）
 *
 * 一册子逐行核收，各行互不相干：合法的汇纳入账（status=1），非法的挂驳回另记（status=2）
 * 并留住原册行次，一条填错不许连坐整册、也不许漏掉后面的行。
 * 同一批次重复提交只认头一遍：账上已有该册号即原样答回已汇纳行数，不补第二遍。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TMarrReportRowServiceImpl implements ITMarrReportRowService {

    /** 单批行数上限，越过整批不收 */
    private static final int MAX_ROWS = 500;

    /** 行进展：0待核 1已汇纳 2挂驳回（与表上注释同一套说法） */
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_OK = 1;
    private static final int STATUS_FAIL = 2;

    @javax.annotation.Resource
    private TMarrReportRowMapper marrReportRowMapper;

    @Override
    public TMarrReportRow selectTMarrReportRowById(Long id) {
        return this.marrReportRowMapper.selectById(id);
    }

    /**
     * 整批提交，逐行隔离：
     * 空批答 0（一轮无事，不算故障）；越过单批上限答 -1，整批一笔都不入；
     * 账上已有同册号的，重复提交只认头一遍，原样答回已汇纳行数；
     * 正常逐行核收，合法入已汇纳、非法入挂驳回（留住原册行次），返回已汇纳行数。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int submitBatch(String batchNo, List<TMarrReportRow> rows) {
        // 空批：没有可处理的条目，答 0 收摊，不许在 rows.get(0) 上空着身子撞异常退出
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        if (rows.size() > MAX_ROWS) {
            return -1;
        }

        String no = resolveBatchNo(batchNo, rows);
        List<TMarrReportRow> existed = rowsOfBatch(no);
        if (!existed.isEmpty()) {
            // 同册号头一遍已在账（含汇纳与挂驳回）：不重复入库，原样答回已汇纳行数
            int ok = 0;
            for (TMarrReportRow r : existed) {
                if (Integer.valueOf(STATUS_OK).equals(r.getStatus())) {
                    ok++;
                }
            }
            return ok;
        }

        int ok = 0;
        int seq = 0;
        for (TMarrReportRow r : rows) {
            seq++;
            r.setBatchNo(no);
            // 原册行次没记的按提交次序补，留住行号才好指给人看哪行挂了
            if (r.getRowNo() == null) {
                r.setRowNo(seq);
            }
            r.setDelFlag(0);
            if (isValid(r)) {
                r.setStatus(STATUS_OK);
                this.marrReportRowMapper.insert(r);
                ok++;
            } else {
                // 单条挂驳回只隔离这一条：当场落账写明，后面的行挨个轮得上
                r.setStatus(STATUS_FAIL);
                this.marrReportRowMapper.insert(r);
            }
        }
        return ok;
    }

    @Override
    public List<TMarrReportRow> listErrors(String batchNo) {
        if (batchNo == null || batchNo.trim().isEmpty()) {
            return new ArrayList<TMarrReportRow>();
        }
        return this.marrReportRowMapper.selectList(new QueryWrapper<TMarrReportRow>()
                .eq("batch_no", batchNo)
                .eq("del_flag", 0)
                .eq("status", STATUS_FAIL)
                .orderByAsc("row_no", "id"));
    }

    /** 册号以入口参数为准；入口没给就认头一行带的，都没有按空串落（仍逐行核收，不抛异常） */
    private static String resolveBatchNo(String batchNo, List<TMarrReportRow> rows) {
        if (batchNo != null && !batchNo.trim().isEmpty()) {
            return batchNo;
        }
        for (TMarrReportRow r : rows) {
            if (r != null && r.getBatchNo() != null && !r.getBatchNo().trim().isEmpty()) {
                return r.getBatchNo();
            }
        }
        return "";
    }

    /** 该册账上已有的行（不分进展）：判重用，头一遍落过账就不再补第二遍 */
    private List<TMarrReportRow> rowsOfBatch(String batchNo) {
        return this.marrReportRowMapper.selectList(new QueryWrapper<TMarrReportRow>()
                .eq("batch_no", batchNo)
                .eq("del_flag", 0));
    }

    /** 行成立条件：报表项代号非空、登记对数为正；任一格不成立这一条挂驳回，不连坐 */
    private static boolean isValid(TMarrReportRow r) {
        return r != null
                && r.getItemCode() != null && !r.getItemCode().trim().isEmpty()
                && r.getQty() != null && r.getQty().compareTo(BigDecimal.ZERO) > 0;
    }
}
