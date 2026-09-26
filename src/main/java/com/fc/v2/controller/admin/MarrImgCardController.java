package com.fc.v2.controller.admin;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TMarrImgCard;
import com.fc.v2.service.ITMarrImgCardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 卷宗影像件建档卡 Controller（上件—版本—核对—压死 流转入口）
 *
 * @author fuce
 * @date 2026-09-12
 */
@Api(value = "卷宗影像件建档卡")
@Controller
@RequestMapping("/MarrImgCardController")
public class MarrImgCardController extends BaseController {

    private final String prefix = "admin/marrImgCard";

    @Autowired
    private ITMarrImgCardService marrImgCardService;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    @RequiresPermissions("marr:marrImgCard:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "卷宗影像件现行账查询", action = "list")
    @ApiOperation(value = "现行账列表(只露最新版)", notes = "现行账列表(只露最新版)")
    @GetMapping("/list")
    @RequiresPermissions("marr:marrImgCard:list")
    @ResponseBody
    public ResultTable list(String siteNo) {
        startPage();
        java.util.List<TMarrImgCard> rows = marrImgCardService.selectCurrentList(siteNo);
        com.github.pagehelper.PageInfo<TMarrImgCard> page = new com.github.pagehelper.PageInfo<>(rows);
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "卷宗影像件翻查", action = "detail")
    @ApiOperation(value = "现行卡翻查", notes = "现行卡翻查")
    @GetMapping("/detail")
    @RequiresPermissions("marr:marrImgCard:list")
    @ResponseBody
    public AjaxResult detail(Long id) {
        return AjaxResult.successData(200, marrImgCardService.selectCurrent(id));
    }

    @Log(title = "卷宗影像件上件建档", action = "register")
    @ApiOperation(value = "上件建档(系统发号,凭编号挂档案)", notes = "上件建档")
    @PostMapping("/register")
    @RequiresPermissions("marr:marrImgCard:add")
    @ResponseBody
    public AjaxResult register(TMarrImgCard record) {
        return AjaxResult.successData(200, marrImgCardService.register(record));
    }

    @Log(title = "卷宗影像件重扫出版", action = "reupload")
    @ApiOperation(value = "重扫出版(旧版退历史层,版本号进一)", notes = "重扫出版")
    @PostMapping("/reupload")
    @RequiresPermissions("marr:marrImgCard:edit")
    @ResponseBody
    public AjaxResult reupload(Long id, BigDecimal qty, BigDecimal fineAmt, String handlerName) {
        return AjaxResult.successData(200,
                marrImgCardService.reupload(id, qty, fineAmt, handlerName));
    }

    @Log(title = "卷宗影像件改挂", action = "rehang")
    @ApiOperation(value = "改挂/页次增减(对着新卷宗重折)", notes = "改挂")
    @PostMapping("/rehang")
    @RequiresPermissions("marr:marrImgCard:edit")
    @ResponseBody
    public AjaxResult rehang(Long id, String targetSiteNo, Integer segStart, Integer segEnd, String handlerName) {
        return AjaxResult.successData(200,
                marrImgCardService.rehang(id, targetSiteNo, segStart, segEnd, handlerName));
    }

    @Log(title = "卷宗影像件核对", action = "verify")
    @ApiOperation(value = "核对(待核对→已核对)", notes = "核对")
    @PostMapping("/verify")
    @RequiresPermissions("marr:marrImgCard:edit")
    @ResponseBody
    public AjaxResult verify(Long id, String handlerName) {
        return AjaxResult.successData(200, marrImgCardService.verify(id, handlerName));
    }

    @Log(title = "卷宗影像件水印压死", action = "press")
    @ApiOperation(value = "压死(承办处|事由|日期定格,此后不可改)", notes = "压死")
    @PostMapping("/press")
    @RequiresPermissions("marr:marrImgCard:edit")
    @ResponseBody
    public AjaxResult press(Long id, String matter,
                            @DateTimeFormat(pattern = "yyyy-MM-dd") Date wmDate, String handlerName) {
        return AjaxResult.successData(200, marrImgCardService.press(id, matter, wmDate, handlerName));
    }

    @Log(title = "卷宗影像件历史版本翻检", action = "history")
    @ApiOperation(value = "历史版本层翻检(旧版原样留存)", notes = "历史版本翻检")
    @GetMapping("/history")
    @RequiresPermissions("marr:marrImgCard:list")
    @ResponseBody
    public AjaxResult history(Long id) {
        return AjaxResult.successData(200, marrImgCardService.selectHistory(id));
    }

    @Log(title = "卷宗影像件承办续笔查询", action = "handlerLogs")
    @ApiOperation(value = "承办续笔(创建人不挪,承办人每趟一笔)", notes = "承办续笔")
    @GetMapping("/handlerLogs")
    @RequiresPermissions("marr:marrImgCard:list")
    @ResponseBody
    public AjaxResult handlerLogs(Long id) {
        return AjaxResult.successData(200, marrImgCardService.selectHandlerLogs(id));
    }
}
