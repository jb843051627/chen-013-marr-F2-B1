package com.fc.v2.service;

import com.fc.v2.model.auto.TMarrImgCard;
import com.fc.v2.model.auto.TMarrImgCardHis;
import com.fc.v2.model.auto.TMarrImgHandlerLog;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 卷宗影像件建档卡 Service接口
 *
 * 两条硬规：
 * 1.同一份卷宗同一段页次现行账只留一行，认卷宗编号+页次，不认到达先后；旧版不删，退历史层。
 * 2.影像完整度档由系统折算写回，手工不得改；页次增减、改挂都重折一遍。
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITMarrImgCardService {

    /**
     * 上件建档：凭档案编号挂接（不许凭姓名相近对号），系统发卡号，折算完整度档。
     * 同卷宗同段页次第二次上件，更新现行那一行，不另立一行。
     */
    TMarrImgCard register(TMarrImgCard record);

    /**
     * 重扫出版：同段页次出新一版。旧版整行退历史层原样留存（含水印），
     * 新版版本号进一，水印日期随新版走，新旧两版不许共用同一行水印。
     */
    TMarrImgCard reupload(Long id, BigDecimal newQty, BigDecimal newFineAmt, String handlerName);

    /**
     * 改挂到别的卷宗底下（或段页次增减）：对着新档案编号重折完整度档，续一笔承办。
     */
    TMarrImgCard rehang(Long id, String targetSiteNo, Integer segStart, Integer segEnd, String handlerName);

    /** 核对：待核对(0) → 已核对(1) */
    TMarrImgCard verify(Long id, String handlerName);

    /**
     * 压死：已核对(1) → 已压死(2)。此刻把"承办处名｜查档事由｜当日日期"定格进水印字样，
     * 此后谁都改不动这行字。
     */
    TMarrImgCard press(Long id, String matter, Date wmDate, String handlerName);

    /**
     * 出证取字：出去的每一页都压这一行（承办处｜事由｜日期）。
     * 只有已压死的版才放得出这行字，保证带得出出处。
     */
    String issueWatermark(Long id);

    /** 翻查现行卡（查档台与市局抽查两边入口共用这一条取数路径） */
    TMarrImgCard selectCurrent(Long id);

    /** 现行账列表：只露最新版，不把历史层并列摆出来 */
    List<TMarrImgCard> selectCurrentList(String siteNo);

    /** 事后翻检：某张卡封存过的全部历史版本 */
    List<TMarrImgCardHis> selectHistory(Long cardId);

    /** 承办续笔：立卡人不挪，跑趟人每办一趟续一笔 */
    List<TMarrImgHandlerLog> selectHandlerLogs(Long cardId);
}
