package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.common.exception.marr.MarrRuleException;
import com.fc.v2.mapper.auto.TMarrImgCardHisMapper;
import com.fc.v2.mapper.auto.TMarrImgCardMapper;
import com.fc.v2.mapper.auto.TMarrImgHandlerLogMapper;
import com.fc.v2.mapper.auto.TMarrVolDocMapper;
import com.fc.v2.model.auto.TMarrImgCard;
import com.fc.v2.model.auto.TMarrImgCardHis;
import com.fc.v2.model.auto.TMarrImgHandlerLog;
import com.fc.v2.model.auto.TMarrVolDoc;
import com.fc.v2.service.ITMarrImgCardService;
import com.fc.v2.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

/**
 * 卷宗影像件建档卡Service业务层处理
 *
 * 规矩落在代码上：
 * - 上件凭档案编号对档案，姓名相近不算数；封卷卷宗当场收口，新上件不收。
 * - 同卷宗同段页次现行账只留一行；升版旧行不删，整行退历史层。
 * - 完整度档系统折算写回（压线算上一格，越三档封顶归三档），任何入口都改不了那一格。
 * - 水印字样在"压死"那一刻定格（承办处｜事由｜日期），随版本走，新旧版各用各的。
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TMarrImgCardServiceImpl extends ServiceImpl<TMarrImgCardMapper, TMarrImgCard> implements ITMarrImgCardService {

    /** 卡进展 */
    private static final int ST_TODO = 0;
    private static final int ST_CHECKED = 1;
    private static final int ST_PRESSED = 2;

    /** 档案情形 */
    private static final int ARCH_SEALED = 1;

    @Autowired
    private TMarrVolDocMapper marrVolDocMapper;
    @Autowired
    private TMarrImgCardHisMapper imgCardHisMapper;
    @Autowired
    private TMarrImgHandlerLogMapper handlerLogMapper;

    // ============================ 上件建档 ============================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMarrImgCard register(TMarrImgCard record) {
        if (record == null) {
            throw new MarrRuleException("record", "建档卡内容为空");
        }
        // 卡号一律系统发，谁递上来的都不认。
        if (StringUtils.isNotEmpty(record.getBillNo())) {
            throw new MarrRuleException("bill_no", "影像件建档卡号由系统发号，不许自编一套号");
        }
        TMarrVolDoc arch = loadArchByNo(record.getSiteNo());
        assertArchOpen(arch);
        validateSegment(record.getSegStart(), record.getSegEnd());
        validateQty(record.getQty(), arch);
        validateFineAmt(record.getFineAmt(), record.getQty());
        if (StringUtils.isEmpty(record.getHandlerName())) {
            throw new MarrRuleException("handler_name", "上件必须落本趟承办人姓名");
        }

        TMarrImgCard existing = findCurrent(arch.getSiteNo(), record.getSegStart(), record.getSegEnd());
        if (existing != null) {
            // 同卷宗同段页次第二次上件：账上只留一行，认编号和页次，不认谁先谁后到。
            if (Integer.valueOf(ST_PRESSED).equals(existing.getStatus())) {
                throw new MarrRuleException("status",
                        "该段页次现行版已压死，页次有增减须走重扫出版，不得回头动手改压死版");
            }
            existing.setQty(record.getQty());
            existing.setFineAmt(record.getFineAmt());
            existing.setGradeLevel(calcGrade(record.getQty(), arch));
            existing.setHandlerName(record.getHandlerName());
            this.baseMapper.updateById(existing);
            writeLog(existing, record.getHandlerName(), "register", "同段页次再次上件，并册后重折完整度档");
            return existing;
        }

        record.setBillNo(nextBillNo());
        record.setNodeNo(1);
        record.setGradeLevel(calcGrade(record.getQty(), arch));
        record.setContent(null);
        record.setWmDate(null);
        record.setStatus(ST_TODO);
        record.setDelFlag(0);
        this.baseMapper.insert(record);
        writeLog(record, record.getHandlerName(), "register", "建档上件");
        return record;
    }

    // ============================ 重扫出版 ============================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMarrImgCard reupload(Long id, BigDecimal newQty, BigDecimal newFineAmt, String handlerName) {
        TMarrImgCard cur = requireCurrent(id);
        TMarrVolDoc arch = loadArchByNo(cur.getSiteNo());
        // 封卷后先前上过的照旧翻得着，但翻查不等于能回头动手。
        assertArchOpen(arch);
        validateQty(newQty, arch);
        validateFineAmt(newFineAmt, newQty);
        if (StringUtils.isEmpty(handlerName)) {
            throw new MarrRuleException("handler_name", "重扫出版必须落本趟承办人姓名");
        }

        archiveToHistory(cur, handlerName);

        cur.setNodeNo(cur.getNodeNo() == null ? 1 : cur.getNodeNo() + 1);
        cur.setQty(newQty);
        cur.setFineAmt(newFineAmt);
        cur.setGradeLevel(calcGrade(newQty, arch));
        cur.setHandlerName(handlerName);
        // 字样随影像版本走：新版另行压字，旧版那一行原样留在历史层，两版不许共用同一行。
        cur.setContent(null);
        cur.setWmDate(null);
        cur.setStatus(ST_TODO);
        this.baseMapper.updateById(cur);
        writeLog(cur, handlerName, "reupload",
                "重扫出版第" + cur.getNodeNo() + "版，旧版退历史层，水印待重新压死");
        return cur;
    }

    // ============================ 改挂 / 页次增减 ============================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMarrImgCard rehang(Long id, String targetSiteNo, Integer segStart, Integer segEnd, String handlerName) {
        TMarrImgCard cur = requireCurrent(id);
        if (StringUtils.isEmpty(targetSiteNo)) {
            throw new MarrRuleException("site_no", "改挂须给目标卷宗档案编号");
        }
        if (StringUtils.isEmpty(handlerName)) {
            throw new MarrRuleException("handler_name", "改挂必须落本趟承办人姓名");
        }
        TMarrVolDoc srcArch = loadArchByNo(cur.getSiteNo());
        if (Integer.valueOf(ARCH_SEALED).equals(srcArch.getStatus())) {
            throw new MarrRuleException("site_no", "原卷宗已封卷，封卷后的件只可翻查，不得改挂");
        }
        TMarrVolDoc target = loadArchByNo(targetSiteNo);
        assertArchOpen(target);
        validateSegment(segStart, segEnd);
        validateQty(cur.getQty(), target);

        boolean sameSlot = target.getSiteNo().equals(cur.getSiteNo())
                && segStart.equals(cur.getSegStart()) && segEnd.equals(cur.getSegEnd());
        if (!sameSlot) {
            TMarrImgCard clash = findCurrent(target.getSiteNo(), segStart, segEnd);
            if (clash != null && !clash.getId().equals(cur.getId())) {
                throw new MarrRuleException("seg_start",
                        "目标卷宗该段页次账上已有一行（卡号" + clash.getBillNo() + "），不得并列两行");
            }
        }

        boolean slotChanged = !sameSlot;
        if (slotChanged) {
            // 改挂等于另出一版：旧行连同旧水印退历史层，新版对着新卷宗重折。
            archiveToHistory(cur, handlerName);
            cur.setNodeNo(cur.getNodeNo() == null ? 1 : cur.getNodeNo() + 1);
            cur.setContent(null);
            cur.setWmDate(null);
            cur.setStatus(ST_TODO);
        }
        cur.setSiteNo(target.getSiteNo());
        cur.setSegStart(segStart);
        cur.setSegEnd(segEnd);
        // 页次增减、改挂到别的卷宗底下，都要重折一遍。
        cur.setGradeLevel(calcGrade(cur.getQty(), target));
        cur.setHandlerName(handlerName);
        this.baseMapper.updateById(cur);
        writeLog(cur, handlerName, "rehang",
                "改挂至" + target.getSiteNo() + "第" + segStart + "-" + segEnd + "页，完整度档重新折算");
        return cur;
    }

    // ============================ 核对 / 压死 ============================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMarrImgCard verify(Long id, String handlerName) {
        TMarrImgCard cur = requireCurrent(id);
        if (!Integer.valueOf(ST_TODO).equals(cur.getStatus())) {
            throw new MarrRuleException("status", "只有待核对的卡能核对，当前进展=" + cur.getStatus());
        }
        if (StringUtils.isEmpty(handlerName)) {
            throw new MarrRuleException("handler_name", "核对必须落本趟承办人姓名");
        }
        cur.setStatus(ST_CHECKED);
        cur.setHandlerName(handlerName);
        this.baseMapper.updateById(cur);
        writeLog(cur, handlerName, "verify", "核对通过");
        return cur;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TMarrImgCard press(Long id, String matter, Date wmDate, String handlerName) {
        TMarrImgCard cur = requireCurrent(id);
        if (Integer.valueOf(ST_PRESSED).equals(cur.getStatus())) {
            // 定了之后谁都改不动。
            throw new MarrRuleException("content", "水印字样已压死定格，任何改动不收");
        }
        if (!Integer.valueOf(ST_CHECKED).equals(cur.getStatus())) {
            throw new MarrRuleException("status", "未核对的版不得压死水印，当前进展=" + cur.getStatus());
        }
        if (StringUtils.isEmpty(matter)) {
            throw new MarrRuleException("content", "压死水印须给查档事由");
        }
        if (wmDate == null) {
            throw new MarrRuleException("wm_date", "压死水印须给当日日期");
        }
        if (StringUtils.isEmpty(handlerName)) {
            throw new MarrRuleException("handler_name", "压死必须落本趟承办人姓名");
        }
        TMarrVolDoc arch = loadArchByNo(cur.getSiteNo());
        // 行里三项：承办处名、查档事由、当日日期——材料流出去也认得出从哪一屏出。
        String line = "承办处：" + arch.getRoadName() + "｜查档事由：" + matter + "｜日期：" + fmtDate(wmDate);
        cur.setContent(line);
        cur.setWmDate(wmDate);
        cur.setHandlerName(handlerName);
        cur.setStatus(ST_PRESSED);
        this.baseMapper.updateById(cur);
        writeLog(cur, handlerName, "press", "水印字样压死定格：" + line);
        return cur;
    }

    /**
     * 出证取字：出去的每一页都压这一行。只有已压死的版才放得出这行字，
     * 保证带得出出处（承办处、事由、日期）。
     */
    @Override
    public String issueWatermark(Long id) {
        TMarrImgCard cur = requireCurrent(id);
        if (!Integer.valueOf(ST_PRESSED).equals(cur.getStatus()) || StringUtils.isEmpty(cur.getContent())) {
            throw new MarrRuleException("status", "该版尚未压死水印，不得出证");
        }
        return cur.getContent();
    }

    // ============================ 翻查 ============================

    @Override
    public TMarrImgCard selectCurrent(Long id) {
        TMarrImgCard card = this.baseMapper.selectOne(new QueryWrapper<TMarrImgCard>()
                .eq("id", id).eq("del_flag", 0));
        if (card == null) {
            throw new MarrRuleException("id", "现行账上查无此卡");
        }
        return card;
    }

    @Override
    public List<TMarrImgCard> selectCurrentList(String siteNo) {
        QueryWrapper<TMarrImgCard> qw = new QueryWrapper<TMarrImgCard>().eq("del_flag", 0);
        if (StringUtils.isNotEmpty(siteNo)) {
            qw.eq("site_no", siteNo);
        }
        // 现行位置只露最新那一版，历史层不并列摆上来。
        qw.orderByAsc("site_no", "seg_start", "seg_end");
        return this.baseMapper.selectList(qw);
    }

    @Override
    public List<TMarrImgCardHis> selectHistory(Long cardId) {
        return imgCardHisMapper.selectList(new QueryWrapper<TMarrImgCardHis>()
                .eq("card_id", cardId).orderByAsc("node_no"));
    }

    @Override
    public List<TMarrImgHandlerLog> selectHandlerLogs(Long cardId) {
        return handlerLogMapper.selectList(new QueryWrapper<TMarrImgHandlerLog>()
                .eq("card_id", cardId).orderByAsc("create_time", "id"));
    }

    // ============================ 规则底 ============================

    /**
     * 完整度折算，压线算上一格：
     * 不足一档 → 1档；正好等于一档封顶按二档论；越过三档封顶一律按三档论。
     */
    static int calcGrade(BigDecimal qty, TMarrVolDoc arch) {
        if (qty.compareTo(arch.getTh1Max()) < 0) {
            return 1;
        }
        if (qty.compareTo(arch.getTh2Max()) < 0) {
            return 2;
        }
        return 3;
    }

    private TMarrVolDoc loadArchByNo(String siteNo) {
        if (StringUtils.isEmpty(siteNo)) {
            throw new MarrRuleException("site_no", "须凭卷宗档案编号挂接，编号不得为空");
        }
        TMarrVolDoc arch = marrVolDocMapper.selectOne(new QueryWrapper<TMarrVolDoc>()
                .eq("site_no", siteNo).eq("del_flag", 0));
        if (arch == null) {
            // 凭编号去对；光看当事人姓名相近就当对上了的不算。
            throw new MarrRuleException("site_no", "档案编号" + siteNo + "对不上任何在管卷宗，不得凭姓名相近挂接");
        }
        return arch;
    }

    private void assertArchOpen(TMarrVolDoc arch) {
        if (Integer.valueOf(ARCH_SEALED).equals(arch.getStatus())) {
            throw new MarrRuleException("site_no", "卷宗" + arch.getSiteNo() + "已封卷收口，新上件不收（旧件仍可翻查）");
        }
    }

    private void validateSegment(Integer segStart, Integer segEnd) {
        if (segStart == null || segStart <= 0) {
            throw new MarrRuleException("seg_start", "段起页次须为正整数");
        }
        if (segEnd == null || segEnd <= 0) {
            throw new MarrRuleException("seg_end", "段止页次须为正整数");
        }
        if (segEnd < segStart) {
            throw new MarrRuleException("seg_end", "段止页次不得小于段起页次");
        }
    }

    /** 零、负不成立；比所属档案承接上限多出一大截也不成立。回执点 qty 这一格。 */
    private void validateQty(BigDecimal qty, TMarrVolDoc arch) {
        if (qty == null) {
            throw new MarrRuleException("qty", "在册影像页数不得为空");
        }
        if (qty.signum() <= 0) {
            throw new MarrRuleException("qty", "在册影像页数填成零或负数，此卡不成立");
        }
        if (arch.getImgMax() != null && qty.compareTo(arch.getImgMax()) > 0) {
            throw new MarrRuleException("qty",
                    "在册影像页数" + qty.stripTrailingZeros().toPlainString()
                            + "已超出本档案承接上限" + arch.getImgMax().stripTrailingZeros().toPlainString()
                            + "页，此卡不成立");
        }
    }

    private void validateFineAmt(BigDecimal fineAmt, BigDecimal qty) {
        if (fineAmt == null) {
            throw new MarrRuleException("fine_amt", "本年度新上件页数不得为空（没有填0）");
        }
        if (fineAmt.signum() < 0) {
            throw new MarrRuleException("fine_amt", "本年度新上件页数填成负数，此卡不成立");
        }
        if (qty != null && fineAmt.compareTo(qty) > 0) {
            throw new MarrRuleException("fine_amt", "本年度新上件页数不得多于在册总页数");
        }
    }

    private TMarrImgCard findCurrent(String siteNo, Integer segStart, Integer segEnd) {
        return this.baseMapper.selectOne(new QueryWrapper<TMarrImgCard>()
                .eq("site_no", siteNo)
                .eq("seg_start", segStart)
                .eq("seg_end", segEnd)
                .eq("del_flag", 0));
    }

    private TMarrImgCard requireCurrent(Long id) {
        if (id == null) {
            throw new MarrRuleException("id", "卡号不得为空");
        }
        TMarrImgCard cur = this.baseMapper.selectById(id);
        if (cur == null || Integer.valueOf(1).equals(cur.getDelFlag())) {
            throw new MarrRuleException("id", "现行账上查无此卡");
        }
        return cur;
    }

    /** 旧版不删：整行快照退历史层，水印原样带走；封存人记本次动手的承办人。 */
    private void archiveToHistory(TMarrImgCard cur, String sealedBy) {
        TMarrImgCardHis his = new TMarrImgCardHis();
        his.setCardId(cur.getId());
        his.setBillNo(cur.getBillNo());
        his.setSiteNo(cur.getSiteNo());
        his.setSegStart(cur.getSegStart());
        his.setSegEnd(cur.getSegEnd());
        his.setNodeNo(cur.getNodeNo());
        his.setQty(cur.getQty());
        his.setFineAmt(cur.getFineAmt());
        his.setGradeLevel(cur.getGradeLevel());
        his.setHandlerName(cur.getHandlerName());
        his.setContent(cur.getContent());
        his.setWmDate(cur.getWmDate());
        his.setStatus(cur.getStatus());
        his.setCreateBy(sealedBy);
        his.setCreateTime(new Date());
        his.setRemark("升版退出现行位置，原样另存");
        imgCardHisMapper.insert(his);
    }

    private void writeLog(TMarrImgCard card, String handlerName, String action, String memo) {
        TMarrImgHandlerLog log = new TMarrImgHandlerLog();
        log.setCardId(card.getId());
        log.setBillNo(card.getBillNo());
        log.setSiteNo(card.getSiteNo());
        log.setNodeNo(card.getNodeNo());
        log.setHandlerName(handlerName);
        log.setAction(action);
        log.setMemo(memo);
        log.setCreateBy(handlerName);
        log.setCreateTime(new Date());
        handlerLogMapper.insert(log);
    }

    private String nextBillNo() {
        return "YK" + IdWorker.getId();
    }

    private static String fmtDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }
}
