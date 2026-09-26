package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.Date;

import com.fc.v2.common.exception.marr.MarrRuleException;
import com.fc.v2.mapper.auto.TMarrImgCardHisMapper;
import com.fc.v2.mapper.auto.TMarrImgCardMapper;
import com.fc.v2.mapper.auto.TMarrImgHandlerLogMapper;
import com.fc.v2.mapper.auto.TMarrVolDocMapper;
import com.fc.v2.model.auto.TMarrImgCard;
import com.fc.v2.model.auto.TMarrImgCardHis;
import com.fc.v2.model.auto.TMarrImgHandlerLog;
import com.fc.v2.model.auto.TMarrVolDoc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 卷宗影像件建档卡规则单测：折算压线、怪数指名拒收、封卷收口、
 * 同段页次只留一行、重扫版本旧行退历史、水印压死定格不可改、改挂重折。
 *
 * @author fuce
 * @date 2026-09-26
 */
@ExtendWith(MockitoExtension.class)
class TMarrImgCardServiceImplTest {

    @Mock
    private TMarrImgCardMapper baseMapper;
    @Mock
    private TMarrVolDocMapper marrVolDocMapper;
    @Mock
    private TMarrImgCardHisMapper imgCardHisMapper;
    @Mock
    private TMarrImgHandlerLogMapper handlerLogMapper;

    @InjectMocks
    private TMarrImgCardServiceImpl service;

    private static final String SITE = "JH0001";

    private TMarrVolDoc openArch() {
        TMarrVolDoc a = new TMarrVolDoc();
        a.setId(0L);
        a.setSiteNo(SITE);
        a.setSiteName("结婚卷宗");
        a.setRoadName("江原省—江原市—临川县");
        a.setImgMax(new BigDecimal("50"));
        a.setTh1Max(new BigDecimal("5"));
        a.setTh2Max(new BigDecimal("12"));
        a.setTh3Max(new BigDecimal("30"));
        a.setStatus(0);
        a.setDelFlag(0);
        return a;
    }

    @BeforeEach
    void setUp() {
        // baseMapper 是 ServiceImpl 继承来的字段，@InjectMocks 注不进，手工塞。
        ReflectionTestUtils.setField(service, "baseMapper", baseMapper);
    }

    private TMarrImgCard newCard(String qty, String fine) {
        TMarrImgCard c = new TMarrImgCard();
        c.setSiteNo(SITE);
        c.setSegStart(1);
        c.setSegEnd(4);
        c.setQty(new BigDecimal(qty));
        c.setFineAmt(new BigDecimal(fine));
        c.setHandlerName("王经办");
        return c;
    }

    // ---------------- 完整度折算：压线算上一格，越三档归三档 ----------------

    @Test
    @DisplayName("折算:不足一档=1档;正好压线=上一格;越过三档封顶仍归三档")
    void calcGradeBoundaries() {
        TMarrVolDoc a = openArch();
        assertEquals(1, TMarrImgCardServiceImpl.calcGrade(new BigDecimal("4"), a));
        assertEquals(2, TMarrImgCardServiceImpl.calcGrade(new BigDecimal("5"), a), "压线一档封顶按二档论");
        assertEquals(2, TMarrImgCardServiceImpl.calcGrade(new BigDecimal("11"), a));
        assertEquals(3, TMarrImgCardServiceImpl.calcGrade(new BigDecimal("12"), a), "压线二档封顶按三档论");
        assertEquals(3, TMarrImgCardServiceImpl.calcGrade(new BigDecimal("30"), a));
        assertEquals(3, TMarrImgCardServiceImpl.calcGrade(new BigDecimal("40"), a), "越过三档封顶一律按三档论");
    }

    // ---------------- 建档 ----------------

    @Test
    @DisplayName("建档:系统发号、首版1、系统写回完整度档、水印待压、续一笔承办")
    void registerOk() {
        when(marrVolDocMapper.selectOne(any())).thenReturn(openArch());
        when(baseMapper.selectOne(any())).thenReturn(null);

        TMarrImgCard got = service.register(newCard("5", "2"));

        assertNotNull(got.getBillNo());
        assertTrue(got.getBillNo().startsWith("YK"));
        assertEquals(1, got.getNodeNo());
        assertEquals(2, got.getGradeLevel(), "5页正好压一档线，按二档");
        assertEquals(0, got.getStatus());
        assertNull(got.getContent());
        assertNull(got.getWmDate());

        ArgumentCaptor<TMarrImgCard> cap = ArgumentCaptor.forClass(TMarrImgCard.class);
        verify(baseMapper).insert(cap.capture());
        verify(handlerLogMapper).insert(any(TMarrImgHandlerLog.class));
    }

    @Test
    @DisplayName("建档:卡号不许自编,递上来也退回")
    void registerRejectsSelfMadeBillNo() {
        TMarrImgCard c = newCard("5", "2");
        c.setBillNo("MY-OWN-001");
        MarrRuleException ex = assertThrows(MarrRuleException.class, () -> service.register(c));
        assertEquals("bill_no", ex.getField());
        verify(baseMapper, never()).insert(any());
    }

    @Test
    @DisplayName("建档:编号对不上档案,不许凭姓名相近挂接")
    void registerUnknownSiteNo() {
        when(marrVolDocMapper.selectOne(any())).thenReturn(null);
        MarrRuleException ex = assertThrows(MarrRuleException.class, () -> service.register(newCard("5", "2")));
        assertEquals("site_no", ex.getField());
    }

    @Test
    @DisplayName("建档:封卷卷宗当场收口,新上件不收")
    void registerSealedArch() {
        TMarrVolDoc sealed = openArch();
        sealed.setStatus(1);
        when(marrVolDocMapper.selectOne(any())).thenReturn(sealed);
        MarrRuleException ex = assertThrows(MarrRuleException.class, () -> service.register(newCard("5", "2")));
        assertEquals("site_no", ex.getField());
        verify(baseMapper, never()).insert(any());
    }

    @Test
    @DisplayName("怪数:在册页数为零/为负/超承接上限,回执都点 qty 这一格")
    void registerBadQty() {
        when(marrVolDocMapper.selectOne(any())).thenReturn(openArch());
        for (String bad : new String[]{"0", "-3"}) {
            MarrRuleException ex = assertThrows(MarrRuleException.class, () -> service.register(newCard(bad, "0")));
            assertEquals("qty", ex.getField());
        }
        MarrRuleException over = assertThrows(MarrRuleException.class,
                () -> service.register(newCard("51", "0")));
        assertEquals("qty", over.getField());
        verify(baseMapper, never()).insert(any());
    }

    @Test
    @DisplayName("怪数:本年度新上件为负/多于在册总数,回执点 fine_amt 这一格")
    void registerBadFineAmt() {
        when(marrVolDocMapper.selectOne(any())).thenReturn(openArch());
        MarrRuleException neg = assertThrows(MarrRuleException.class,
                () -> service.register(newCard("5", "-1")));
        assertEquals("fine_amt", neg.getField());
        MarrRuleException tooMany = assertThrows(MarrRuleException.class,
                () -> service.register(newCard("5", "6")));
        assertEquals("fine_amt", tooMany.getField());
    }

    @Test
    @DisplayName("同卷宗同段页次第二次上件:账上只留一行(更新不另插),并重折档")
    void registerSameSegmentUpdatesOneRow() {
        TMarrImgCard existing = newCard("4", "4");
        existing.setId(100L);
        existing.setBillNo("YK100");
        existing.setNodeNo(1);
        existing.setGradeLevel(1);
        existing.setStatus(0);
        when(marrVolDocMapper.selectOne(any())).thenReturn(openArch());
        when(baseMapper.selectOne(any())).thenReturn(existing);

        TMarrImgCard got = service.register(newCard("20", "2"));

        assertSame(existing, got);
        assertEquals(0, new BigDecimal("20").compareTo(got.getQty()));
        assertEquals(3, got.getGradeLevel());
        verify(baseMapper, never()).insert(any());
        verify(baseMapper).updateById(existing);
    }

    @Test
    @DisplayName("现行版已压死:不许回头动手改,须走重扫")
    void registerAgainstPressedRejected() {
        TMarrImgCard pressed = newCard("4", "4");
        pressed.setId(100L);
        pressed.setStatus(2);
        when(marrVolDocMapper.selectOne(any())).thenReturn(openArch());
        when(baseMapper.selectOne(any())).thenReturn(pressed);
        MarrRuleException ex = assertThrows(MarrRuleException.class, () -> service.register(newCard("6", "1")));
        assertEquals("status", ex.getField());
        verify(baseMapper, never()).updateById(any());
    }

    // ---------------- 重扫出版 ----------------

    private TMarrImgCard storedCard(int status) {
        TMarrImgCard c = newCard("4", "4");
        c.setId(7L);
        c.setBillNo("YK7");
        c.setNodeNo(1);
        c.setGradeLevel(1);
        c.setStatus(status);
        c.setDelFlag(0);
        return c;
    }

    @Test
    @DisplayName("重扫:旧版整行退历史层(水印原样带走),版本进一,新水印清空待重押")
    void reuploadArchivesOldVersion() {
        TMarrImgCard cur = storedCard(0);
        when(baseMapper.selectById(7L)).thenReturn(cur);
        when(marrVolDocMapper.selectOne(any())).thenReturn(openArch());

        TMarrImgCard got = service.reupload(7L, new BigDecimal("12"), new BigDecimal("8"), "李重扫");

        assertEquals(2, got.getNodeNo());
        assertEquals(3, got.getGradeLevel(), "12页压二档线按三档");
        assertEquals(0, got.getStatus());
        assertNull(got.getContent());
        assertNull(got.getWmDate());

        ArgumentCaptor<TMarrImgCardHis> his = ArgumentCaptor.forClass(TMarrImgCardHis.class);
        verify(imgCardHisMapper).insert(his.capture());
        assertEquals(1, his.getValue().getNodeNo(), "历史层封存的是旧版v1");
        assertEquals(7L, his.getValue().getCardId());
        verify(handlerLogMapper).insert(any(TMarrImgHandlerLog.class));
    }

    @Test
    @DisplayName("重扫:封卷后只可翻查不可动手")
    void reuploadSealedRejected() {
        TMarrVolDoc sealed = openArch();
        sealed.setStatus(1);
        when(baseMapper.selectById(7L)).thenReturn(storedCard(0));
        when(marrVolDocMapper.selectOne(any())).thenReturn(sealed);
        MarrRuleException ex = assertThrows(MarrRuleException.class,
                () -> service.reupload(7L, new BigDecimal("8"), new BigDecimal("1"), "李重扫"));
        assertEquals("site_no", ex.getField());
        verify(imgCardHisMapper, never()).insert(any());
    }

    // ---------------- 改挂 / 页次增减 ----------------

    @Test
    @DisplayName("改挂:旧行退历史,对着新卷宗重折完整度档,版本进一")
    void rehangRecalculatesAgainstTarget() {
        TMarrImgCard cur = storedCard(0);
        TMarrVolDoc target = openArch();
        target.setSiteNo("BL0009");
        target.setTh1Max(new BigDecimal("2"));
        target.setTh2Max(new BigDecimal("4"));
        target.setTh3Max(new BigDecimal("10"));
        when(baseMapper.selectById(7L)).thenReturn(cur);
        when(marrVolDocMapper.selectOne(any())).thenReturn(openArch(), target);
        when(baseMapper.selectOne(any())).thenReturn(null);

        TMarrImgCard got = service.rehang(7L, "BL0009", 1, 4, "赵改挂");

        assertEquals("BL0009", got.getSiteNo());
        assertEquals(2, got.getNodeNo());
        assertEquals(3, got.getGradeLevel(), "4页在新档案(2/4/10)下越过二档线,归三档");
        verify(imgCardHisMapper).insert(any(TMarrImgCardHis.class));
    }

    @Test
    @DisplayName("改挂:目标卷宗该段页次账上已有别人一行,不得并列")
    void rehangSlotClashRejected() {
        TMarrImgCard cur = storedCard(0);
        TMarrImgCard other = storedCard(0);
        other.setId(999L);
        other.setBillNo("YK999");
        when(baseMapper.selectById(7L)).thenReturn(cur);
        when(marrVolDocMapper.selectOne(any())).thenReturn(openArch(), openArch());
        when(baseMapper.selectOne(any())).thenReturn(other);

        MarrRuleException ex = assertThrows(MarrRuleException.class,
                () -> service.rehang(7L, SITE, 9, 12, "赵改挂"));
        assertEquals("seg_start", ex.getField());
        verify(baseMapper, never()).updateById(any());
    }

    // ---------------- 核对 / 压死 ----------------

    @Test
    @DisplayName("核对后压死:水印含承办处/事由/当日日期三项,定格后再压被拒")
    void verifyThenPressLocksWatermark() {
        TMarrImgCard cur = storedCard(0);
        when(baseMapper.selectById(7L)).thenReturn(cur);
        when(marrVolDocMapper.selectOne(any())).thenReturn(openArch());

        service.verify(7L, "王核对");
        assertEquals(1, cur.getStatus());

        Date day = java.sql.Date.valueOf("2026-09-26");
        TMarrImgCard pressed = service.press(7L, "律师持法院函调2016年案卷", day, "孙出证");
        assertEquals(2, pressed.getStatus());
        String line = pressed.getContent();
        assertTrue(line.contains("江原省—江原市—临川县"), "水印带承办处");
        assertTrue(line.contains("律师持法院函调2016年案卷"), "水印带查档事由");
        assertTrue(line.contains("2026-09-26"), "水印带当日日期");
        assertEquals(day, pressed.getWmDate());

        // 定了之后谁都改不动。
        MarrRuleException ex = assertThrows(MarrRuleException.class,
                () -> service.press(7L, "想改字样", day, "孙出证"));
        assertEquals("content", ex.getField());

        // 出证取字放的就是压死那一行。
        assertEquals(line, service.issueWatermark(7L));
    }

    @Test
    @DisplayName("未核对不得压死;未压死不得出证")
    void pressAndIssueGuards() {
        when(baseMapper.selectById(7L)).thenReturn(storedCard(0));
        MarrRuleException noPress = assertThrows(MarrRuleException.class,
                () -> service.press(7L, "事由", java.sql.Date.valueOf("2026-09-26"), "孙出证"));
        assertEquals("status", noPress.getField());
        MarrRuleException noIssue = assertThrows(MarrRuleException.class, () -> service.issueWatermark(7L));
        assertEquals("status", noIssue.getField());
    }

    @Test
    @DisplayName("跨版水印不共用:v1压死字样随旧版原样进历史,v2另行压新字样新日期")
    void watermarkTravelsWithVersion() {
        TMarrImgCard cur = storedCard(1);
        cur.setContent("承办处：江原省—江原市—临川县｜查档事由：旧案调档｜日期：2016-11-03");
        cur.setWmDate(java.sql.Date.valueOf("2016-11-03"));
        cur.setStatus(2);
        when(baseMapper.selectById(7L)).thenReturn(cur);
        when(marrVolDocMapper.selectOne(any())).thenReturn(openArch());

        // 已压死版重扫出新版本
        TMarrImgCard v2 = service.reupload(7L, new BigDecimal("6"), new BigDecimal("6"), "李重扫");
        assertNull(v2.getContent(), "新版不许沿用旧版那一行水印");

        ArgumentCaptor<TMarrImgCardHis> his = ArgumentCaptor.forClass(TMarrImgCardHis.class);
        verify(imgCardHisMapper).insert(his.capture());
        assertTrue(his.getValue().getContent().contains("2016-11-03"), "旧版水印原样留在历史层");

        // v2 核对、压新字样、新日期
        service.verify(7L, "王核对");
        Date day2 = java.sql.Date.valueOf("2026-09-26");
        TMarrImgCard pressed2 = service.press(7L, "去年十一月司法查档", day2, "孙出证");
        assertTrue(pressed2.getContent().contains("2026-09-26"));
        assertFalse(pressed2.getContent().contains("2016-11-03"));
    }
}
