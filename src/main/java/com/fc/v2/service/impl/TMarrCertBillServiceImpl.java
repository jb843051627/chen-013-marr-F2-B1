package com.fc.v2.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fc.v2.mapper.auto.TMarrCertBillMapper;
import com.fc.v2.model.auto.TMarrCertBill;
import com.fc.v2.service.ITMarrCertBillService;

/**
 * 婚姻登记记录证明逐级核准单 Service业务层处理（approval-chain 形状：多阶段签批）
 *
 * 环节（node_no 0..3）与情形（status 0在核 1已核讫 2已打回）同一刻算完、同一次落库，
 * 不许环节挪了情形没挪（超时/回滚两边对不上的账就是这么来的）。
 * 同层核准方式：0任一人签一票即过层；1两名点齐才过层（need_count 说了算，缺省按方式补 1/2）。
 * 已核讫的重复签批只认头一遍、不再落一笔；已打回的一律不受理。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TMarrCertBillServiceImpl implements ITMarrCertBillService {

    /** 环节：0..3 共四层，末层（第 3 层）点齐即核讫 */
    private static final int MAX_NODE = 3;
    /** 同层核准方式：任一人 */
    private static final int MODE_OR = 0;
    /** 同层核准方式：两名点齐 */
    private static final int MODE_AND = 1;

    /** 报批情形 */
    private static final int STATUS_RUNNING = 0;
    private static final int STATUS_PASS = 1;
    private static final int STATUS_VETO = 2;

    @javax.annotation.Resource
    private TMarrCertBillMapper marrCertBillMapper;

    @Override
    public TMarrCertBill selectTMarrCertBillById(Long id) {
        return this.marrCertBillMapper.selectById(id);
    }

    /**
     * 签批一票：本层已签数进一；点齐本层应签才进下一环节并清签字数，末层点齐置已核讫。
     * 环节与情形在同一次 update 里落定。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMarrCertBill approve(Long id, String approver, String comment) {
        TMarrCertBill r = this.marrCertBillMapper.selectById(id);
        if (r == null || isBlank(approver)) {
            return null;
        }
        if (r.getStatus() != null && r.getStatus() == STATUS_PASS) {
            // 已核讫：重复签批只认头一遍，不补第二笔、不再回写
            return r;
        }
        if (r.getStatus() != null && r.getStatus() == STATUS_VETO) {
            // 已打回的单不再受理签批
            return null;
        }

        int node = r.getNodeNo() == null ? 0 : Math.max(0, r.getNodeNo());
        int need = needCountOf(r);
        int signed = (r.getSignCount() == null ? 0 : r.getSignCount()) + 1;

        r.setNodeNo(node);
        r.setStatus(STATUS_RUNNING);
        if (signed < need) {
            // 两名点齐的层，只到一票仍压在本层在核，等第二票
            r.setSignCount(signed);
            r.setRemark(memo("签批待齐：" + approver, comment));
        } else if (node >= MAX_NODE) {
            // 末层点齐：环节不再外挪，情形落已核讫，签字数定格在应签数
            r.setNodeNo(MAX_NODE);
            r.setSignCount(need);
            r.setStatus(STATUS_PASS);
            r.setRemark(memo("核讫：" + approver, comment));
        } else {
            // 本层点齐：进下一环节，新一层签字从零起算
            r.setNodeNo(node + 1);
            r.setSignCount(0);
            r.setStatus(STATUS_RUNNING);
            r.setRemark(memo("进至第" + (node + 1) + "层：" + approver, comment));
        }
        this.marrCertBillMapper.updateById(r);
        return r;
    }

    /**
     * 否决：在核中的单一票打回，情形落已打回（环节就地不动，账上看得出停在哪一层）。
     * 已打回的再否决只认头一遍；已核讫的不许翻案打回。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMarrCertBill reject(Long id, String approver, String comment) {
        TMarrCertBill r = this.marrCertBillMapper.selectById(id);
        if (r == null || isBlank(approver)) {
            return null;
        }
        if (r.getStatus() != null && r.getStatus() == STATUS_VETO) {
            return r;
        }
        if (r.getStatus() != null && r.getStatus() == STATUS_PASS) {
            return null;
        }
        r.setStatus(STATUS_VETO);
        r.setRemark(memo("打回：" + approver, comment));
        this.marrCertBillMapper.updateById(r);
        return r;
    }

    /**
     * 退回上一环节：只收在核中的单；无头可退（已在第 0 层）返 null。
     * 退一层并把本层已签数清零（上一层要重点），情形仍落在核，一次回写。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMarrCertBill rollback(Long id, String comment) {
        TMarrCertBill r = this.marrCertBillMapper.selectById(id);
        if (r == null) {
            return null;
        }
        if (r.getStatus() != null && r.getStatus() != STATUS_RUNNING) {
            // 已讫/已打回的不办退回
            return null;
        }
        int node = r.getNodeNo() == null ? 0 : r.getNodeNo();
        if (node <= 0) {
            // 已在头一层，无头可退
            return null;
        }
        r.setNodeNo(node - 1);
        r.setSignCount(0);
        r.setStatus(STATUS_RUNNING);
        r.setRemark(memo("退回第" + (node - 1) + "层重核", comment));
        this.marrCertBillMapper.updateById(r);
        return r;
    }

    /** 本层应签人数：need_count 记了就认账；没记按核准方式补，任一人 1、两名点齐 2 */
    private static int needCountOf(TMarrCertBill r) {
        if (r.getNeedCount() != null && r.getNeedCount() > 0) {
            return r.getNeedCount();
        }
        return r.getSignMode() != null && r.getSignMode() == MODE_AND ? 2 : 1;
    }

    private static String memo(String action, String comment) {
        return isBlank(comment) ? action : action + "｜" + comment.trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
