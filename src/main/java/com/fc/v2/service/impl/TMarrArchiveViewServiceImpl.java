package com.fc.v2.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.exception.marr.MarrRuleException;
import com.fc.v2.mapper.auto.TMarrArchiveGrantMapper;
import com.fc.v2.mapper.auto.TMarrVolDocMapper;
import com.fc.v2.model.auto.TMarrArchiveGrant;
import com.fc.v2.model.auto.TMarrImgCard;
import com.fc.v2.model.auto.TMarrVolDoc;
import com.fc.v2.service.ITMarrArchiveViewService;
import com.fc.v2.service.ITMarrImgCardService;
import com.fc.v2.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 查档可见域 Service 实现。
 * 查档台与市局抽查是两个入口、一条取数路径，都汇到这里，保证同一张卡两边看到同一份数。
 *
 * @author fuce
 * @date 2026-09-26
 */
@Service
public class TMarrArchiveViewServiceImpl implements ITMarrArchiveViewService {

    @Autowired
    private TMarrArchiveGrantMapper grantMapper;
    @Autowired
    private TMarrVolDocMapper volDocMapper;
    @Autowired
    private ITMarrImgCardService imgCardService;

    @Override
    public List<TMarrVolDoc> listVisibleArchives(Long grantId) {
        TMarrArchiveGrant grant = requireValidGrant(grantId);
        QueryWrapper<TMarrVolDoc> qw = new QueryWrapper<TMarrVolDoc>().eq("del_flag", 0);
        switch (grant.getViewerType()) {
            case TMarrArchiveGrant.VIEWER_SELF:
            case TMarrArchiveGrant.VIEWER_LAWYER:
                // 本人名下那一卷 / 律师受托那一宗：选卷页上就只给这一卷，别卷不列出。
                qw.eq("site_no", grant.getSiteNo());
                break;
            case TMarrArchiveGrant.VIEWER_ORG:
                // 机关凭公函看本辖内全部：承办处名按辖前缀辖（省—市 可辖下全部区县）。
                qw.likeRight("road_name", grant.getScopeRoad());
                break;
            default:
                throw new MarrRuleException("viewer_type", "查档人情形无法辨认");
        }
        return volDocMapper.selectList(qw.orderByAsc("site_no"));
    }

    @Override
    public List<TMarrImgCard> listByGrant(Long grantId) {
        List<TMarrVolDoc> archives = listVisibleArchives(grantId);
        List<TMarrImgCard> out = new ArrayList<>();
        // 逐卷走现行账查询，现行位置只露最新版；卷再多也不会带出授权域外的卷。
        for (TMarrVolDoc arch : archives) {
            out.addAll(imgCardService.selectCurrentList(arch.getSiteNo()));
        }
        return out;
    }

    @Override
    public TMarrImgCard getCardByGrant(Long grantId, Long cardId) {
        TMarrImgCard card = imgCardService.selectCurrent(cardId);
        assertVisible(grantId, card.getSiteNo());
        return card;
    }

    @Override
    public String issueByGrant(Long grantId, Long cardId) {
        TMarrImgCard card = imgCardService.selectCurrent(cardId);
        assertVisible(grantId, card.getSiteNo());
        return imgCardService.issueWatermark(cardId);
    }

    // ============================ 内底 ============================

    private TMarrArchiveGrant requireValidGrant(Long grantId) {
        if (grantId == null) {
            throw new MarrRuleException("grant_id", "须持有效查档授权（证件/公函/委托函）");
        }
        TMarrArchiveGrant grant = grantMapper.selectById(grantId);
        if (grant == null || Integer.valueOf(1).equals(grant.getDelFlag())) {
            throw new MarrRuleException("grant_id", "查档授权对不上登记");
        }
        if (!Integer.valueOf(0).equals(grant.getStatus())) {
            throw new MarrRuleException("grant_id", "该查档授权已失效");
        }
        if ((grant.getViewerType() == TMarrArchiveGrant.VIEWER_SELF
                || grant.getViewerType() == TMarrArchiveGrant.VIEWER_LAWYER)
                && StringUtils.isEmpty(grant.getSiteNo())) {
            throw new MarrRuleException("site_no", "该授权未挂到具体卷宗，不得放卷");
        }
        if (grant.getViewerType() == TMarrArchiveGrant.VIEWER_ORG
                && StringUtils.isEmpty(grant.getScopeRoad())) {
            throw new MarrRuleException("scope_road", "机关公函未载本辖承办处，不得放卷");
        }
        return grant;
    }

    /** 该卷宗是否落在授权可见域：本人/律师认编号死等，机关认辖前缀。 */
    private void assertVisible(Long grantId, String siteNo) {
        TMarrArchiveGrant grant = requireValidGrant(grantId);
        boolean ok;
        if (grant.getViewerType() == TMarrArchiveGrant.VIEWER_SELF
                || grant.getViewerType() == TMarrArchiveGrant.VIEWER_LAWYER) {
            ok = grant.getSiteNo() != null && grant.getSiteNo().equals(siteNo);
        } else {
            TMarrVolDoc arch = volDocMapper.selectOne(new QueryWrapper<TMarrVolDoc>()
                    .eq("site_no", siteNo).eq("del_flag", 0));
            ok = arch != null && arch.getRoadName() != null
                    && arch.getRoadName().startsWith(grant.getScopeRoad() == null ? "" : grant.getScopeRoad());
        }
        if (!ok) {
            // 域外一律报"查无"，不承认列表外直拼卡号，页面也不显示别人卷的存在。
            throw new MarrRuleException("site_no", "此卷不在所持授权的查档范围内");
        }
    }
}
