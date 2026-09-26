package com.fc.v2.service.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fc.v2.common.exception.marr.MarrRuleException;
import com.fc.v2.mapper.auto.TMarrArchiveGrantMapper;
import com.fc.v2.mapper.auto.TMarrVolDocMapper;
import com.fc.v2.model.auto.TMarrArchiveGrant;
import com.fc.v2.model.auto.TMarrImgCard;
import com.fc.v2.model.auto.TMarrVolDoc;
import com.fc.v2.service.ITMarrImgCardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 查档可见域单测：本人只见名下卷、律师只见受托卷、机关见辖内；
 * 域外直拼卡号不认；两个入口共用一条取数路径。
 *
 * @author fuce
 * @date 2026-09-26
 */
@ExtendWith(MockitoExtension.class)
class TMarrArchiveViewServiceImplTest {

    @Mock
    private TMarrArchiveGrantMapper grantMapper;
    @Mock
    private TMarrVolDocMapper volDocMapper;
    @Mock
    private ITMarrImgCardService imgCardService;

    @InjectMocks
    private TMarrArchiveViewServiceImpl service;

    private TMarrArchiveGrant grant(int type, String siteNo, String scopeRoad) {
        TMarrArchiveGrant g = new TMarrArchiveGrant();
        g.setId(1L);
        g.setViewerType(type);
        g.setSiteNo(siteNo);
        g.setScopeRoad(scopeRoad);
        g.setStatus(0);
        g.setDelFlag(0);
        return g;
    }

    private TMarrVolDoc arch(String siteNo, String road) {
        TMarrVolDoc a = new TMarrVolDoc();
        a.setSiteNo(siteNo);
        a.setRoadName(road);
        a.setStatus(0);
        a.setDelFlag(0);
        return a;
    }

    @Test
    @DisplayName("本人:选卷页只列名下那一卷,别人的卷不列出")
    void selfSeesOnlyOwnArchive() {
        when(grantMapper.selectById(1L)).thenReturn(grant(TMarrArchiveGrant.VIEWER_SELF, "JH0001", null));
        when(volDocMapper.selectList(any())).thenReturn(Collections.singletonList(arch("JH0001", "江原省—江原市—临川县")));

        List<TMarrVolDoc> list = service.listVisibleArchives(1L);
        assertEquals(1, list.size());
        assertEquals("JH0001", list.get(0).getSiteNo());
    }

    @Test
    @DisplayName("律师:受托哪一宗才看得见哪一宗")
    void lawyerSeesOnlyEntrustedArchive() {
        when(grantMapper.selectById(1L)).thenReturn(grant(TMarrArchiveGrant.VIEWER_LAWYER, "LH0001", null));
        when(volDocMapper.selectList(any())).thenReturn(Collections.singletonList(arch("LH0001", "江原省—江原市—临川县")));
        when(imgCardService.selectCurrentList("LH0001")).thenReturn(Collections.emptyList());

        List<TMarrImgCard> cards = service.listByGrant(1L);
        verify(imgCardService).selectCurrentList("LH0001");
        verify(imgCardService, never()).selectCurrentList("JH0001");
        assertNotNull(cards);
    }

    @Test
    @DisplayName("机关:凭公函看本辖内全部卷宗,域外的不进")
    void orgSeesAllWithinJurisdiction() {
        when(grantMapper.selectById(1L))
                .thenReturn(grant(TMarrArchiveGrant.VIEWER_ORG, null, "江原省—江原市—临川县"));
        when(volDocMapper.selectList(any())).thenReturn(Arrays.asList(
                arch("JH0001", "江原省—江原市—临川县"),
                arch("LH0001", "江原省—江原市—临川县")));
        when(imgCardService.selectCurrentList(anyString())).thenReturn(Collections.emptyList());

        service.listByGrant(1L);
        verify(imgCardService).selectCurrentList("JH0001");
        verify(imgCardService).selectCurrentList("LH0001");
    }

    @Test
    @DisplayName("域外直拼卡号:本人/律师取不到别人卷的卡,报查无此范围")
    void directCardIdOutsideScopeDenied() {
        when(grantMapper.selectById(1L)).thenReturn(grant(TMarrArchiveGrant.VIEWER_LAWYER, "LH0001", null));
        TMarrImgCard otherCard = new TMarrImgCard();
        otherCard.setId(50L);
        otherCard.setSiteNo("JH0001");
        when(imgCardService.selectCurrent(50L)).thenReturn(otherCard);

        MarrRuleException ex = assertThrows(MarrRuleException.class, () -> service.getCardByGrant(1L, 50L));
        assertEquals("site_no", ex.getField());
        verify(imgCardService, never()).issueWatermark(any());
    }

    @Test
    @DisplayName("域外直拼卡号:机关辖前缀外的卷同样挡住")
    void orgDirectCardOutsideRoadDenied() {
        when(grantMapper.selectById(1L))
                .thenReturn(grant(TMarrArchiveGrant.VIEWER_ORG, null, "江原省—江原市—临川县"));
        TMarrImgCard foreign = new TMarrImgCard();
        foreign.setId(60L);
        foreign.setSiteNo("WH9999");
        when(imgCardService.selectCurrent(60L)).thenReturn(foreign);
        when(volDocMapper.selectOne(any())).thenReturn(arch("WH9999", "别省—别市—别县"));

        MarrRuleException ex = assertThrows(MarrRuleException.class, () -> service.getCardByGrant(1L, 60L));
        assertEquals("site_no", ex.getField());
    }

    @Test
    @DisplayName("域内取卡放行,并可出证取已压死水印")
    void inScopeCardAndIssueAllowed() {
        when(grantMapper.selectById(1L)).thenReturn(grant(TMarrArchiveGrant.VIEWER_SELF, "JH0001", null));
        TMarrImgCard mine = new TMarrImgCard();
        mine.setId(70L);
        mine.setSiteNo("JH0001");
        mine.setStatus(2);
        when(imgCardService.selectCurrent(70L)).thenReturn(mine);
        when(imgCardService.issueWatermark(70L)).thenReturn("承办处：江原省—江原市—临川县｜查档事由：本人调档｜日期：2026-09-26");

        TMarrImgCard got = service.getCardByGrant(1L, 70L);
        assertEquals(70L, got.getId());
        String line = service.issueByGrant(1L, 70L);
        assertTrue(line.contains("本人调档"));
    }

    @Test
    @DisplayName("授权已失效:任何卷都不放")
    void expiredGrantDenied() {
        TMarrArchiveGrant dead = grant(TMarrArchiveGrant.VIEWER_SELF, "JH0001", null);
        dead.setStatus(1);
        when(grantMapper.selectById(1L)).thenReturn(dead);
        MarrRuleException ex = assertThrows(MarrRuleException.class, () -> service.listVisibleArchives(1L));
        assertEquals("grant_id", ex.getField());
    }
}
