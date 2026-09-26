package com.fc.v2.controller.admin;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.log.Log;
import com.fc.v2.service.ITMarrArchiveViewService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 查档入口 Controller。
 * 查档台(/desk/*)与市局抽查页(/city/*)两边入口点开同一张卡，背后同一条取数路径，
 * 看到的页次与完整度档必须是同一份数；本人/机关/律师各见各的域，别人的卷不列给他挑。
 *
 * @author fuce
 * @date 2026-09-26
 */
@Api(value = "查档台与市局抽查")
@Controller
@RequestMapping("/marrArchive")
public class MarrArchiveViewController extends BaseController {

    private final String prefix = "admin/marrArchive";

    @Autowired
    private ITMarrArchiveViewService archiveViewService;

    @ApiOperation(value = "查档台跳转", notes = "查档台跳转")
    @GetMapping("/desk/view")
    @RequiresPermissions("marr:archive:desk")
    public String deskView(ModelMap model) {
        return prefix + "/desk";
    }

    @ApiOperation(value = "市局抽查跳转", notes = "市局抽查跳转")
    @GetMapping("/city/view")
    @RequiresPermissions("marr:archive:city")
    public String cityView(ModelMap model) {
        return prefix + "/city";
    }

    // -------- 选卷：两个入口都汇到同一条可见域路径 --------

    @Log(title = "查档台可选卷宗", action = "deskArchives")
    @ApiOperation(value = "查档台可选卷宗(只见授权域内)", notes = "查档台可选卷宗")
    @GetMapping("/desk/archives")
    @RequiresPermissions("marr:archive:desk")
    @ResponseBody
    public AjaxResult deskArchives(Long grantId) {
        return AjaxResult.successData(200, archiveViewService.listVisibleArchives(grantId));
    }

    @Log(title = "市局抽查可选卷宗", action = "cityArchives")
    @ApiOperation(value = "市局抽查可选卷宗(与查档台同一路径同一结果)", notes = "市局抽查可选卷宗")
    @GetMapping("/city/archives")
    @RequiresPermissions("marr:archive:city")
    @ResponseBody
    public AjaxResult cityArchives(Long grantId) {
        return AjaxResult.successData(200, archiveViewService.listVisibleArchives(grantId));
    }

    // -------- 现行影像卡：两边点开必须同一份数 --------

    @Log(title = "查档台影像卡", action = "deskList")
    @ApiOperation(value = "查档台现行影像卡", notes = "查档台现行影像卡")
    @GetMapping("/desk/cards")
    @RequiresPermissions("marr:archive:desk")
    @ResponseBody
    public AjaxResult deskCards(Long grantId) {
        return AjaxResult.successData(200, archiveViewService.listByGrant(grantId));
    }

    @Log(title = "市局抽查影像卡", action = "cityList")
    @ApiOperation(value = "市局抽查现行影像卡(与查档台同一份数)", notes = "市局抽查现行影像卡")
    @GetMapping("/city/cards")
    @RequiresPermissions("marr:archive:city")
    @ResponseBody
    public AjaxResult cityCards(Long grantId) {
        return AjaxResult.successData(200, archiveViewService.listByGrant(grantId));
    }

    @Log(title = "查档台单卡翻查", action = "deskCard")
    @ApiOperation(value = "查档台点开单张卡", notes = "查档台单卡翻查")
    @GetMapping("/desk/card")
    @RequiresPermissions("marr:archive:desk")
    @ResponseBody
    public AjaxResult deskCard(Long grantId, Long cardId) {
        return AjaxResult.successData(200, archiveViewService.getCardByGrant(grantId, cardId));
    }

    @Log(title = "市局抽查单卡翻查", action = "cityCard")
    @ApiOperation(value = "市局抽查点开单张卡(同一份数)", notes = "市局抽查单卡翻查")
    @GetMapping("/city/card")
    @RequiresPermissions("marr:archive:city")
    @ResponseBody
    public AjaxResult cityCard(Long grantId, Long cardId) {
        return AjaxResult.successData(200, archiveViewService.getCardByGrant(grantId, cardId));
    }

    // -------- 出证：压一行带出处的水印 --------

    @Log(title = "查档台出证取水印", action = "deskIssue")
    @ApiOperation(value = "查档台出证(压承办处|事由|日期水印)", notes = "查档台出证")
    @PostMapping("/desk/issue")
    @RequiresPermissions("marr:archive:desk")
    @ResponseBody
    public AjaxResult deskIssue(Long grantId, Long cardId) {
        return AjaxResult.successData(200, archiveViewService.issueByGrant(grantId, cardId));
    }

    @Log(title = "市局抽查出证取水印", action = "cityIssue")
    @ApiOperation(value = "市局抽查出证(与查档台同一行水印)", notes = "市局抽查出证")
    @PostMapping("/city/issue")
    @RequiresPermissions("marr:archive:city")
    @ResponseBody
    public AjaxResult cityIssue(Long grantId, Long cardId) {
        return AjaxResult.successData(200, archiveViewService.issueByGrant(grantId, cardId));
    }
}
