package com.fc.v2.service;

import com.fc.v2.model.auto.TMarrImgCard;
import com.fc.v2.model.auto.TMarrVolDoc;

import java.util.List;

/**
 * 查档可见域 Service。
 * 本人凭证件只翻自己名下那一卷；机关凭公函看本辖内全部；
 * 律师持委托函，受托哪一宗才看得见哪一宗——页面上不许把别人的卷也列出来给他挑。
 * 查档台与市局抽查两边入口点开同一张卡，走的是这里同一条取数路径，份数必须一致。
 *
 * @author fuce
 * @date 2026-09-26
 */
public interface ITMarrArchiveViewService {

    /** 按所持授权列出看得见的卷宗档案（供选卷页，别人的卷不许列出来给他挑） */
    List<TMarrVolDoc> listVisibleArchives(Long grantId);

    /** 按所持授权（证件/公函/委托函登记号对应的授权id）列出看得见的现行影像卡，别人的卷不进列表 */
    List<TMarrImgCard> listByGrant(Long grantId);

    /** 取单张卡：不在授权可见域内一律查无，不承认"列表外直拼卡号" */
    TMarrImgCard getCardByGrant(Long grantId, Long cardId);

    /** 出证取字：先验这张卡在本授权可见域内，再放已压死的那一行水印 */
    String issueByGrant(Long grantId, Long cardId);
}
