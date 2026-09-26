package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TMarrDueTask;
import com.fc.v2.service.ITMarrDueTaskService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 冷静期与领证期到期催办 Controller（只读台账）。
 * 发没发出去要在页面上看得见，故留台账查询；
 * 这一轮催办只由定时扫描那一头叫醒，页面上不留补一条、改一条的地方——此处只读，不写。
 *
 * @author fuce
 * @date 2026-09-26
 */
@Api(value = "冷静期与领证期到期催办台账")
@Controller
@RequestMapping("/marrDueTask")
public class MarrDueTaskController extends BaseController {

    private final String prefix = "admin/marrDueTask";

    @Autowired
    private ITMarrDueTaskService marrDueTaskService;

    @ApiOperation(value = "催办台账跳转", notes = "只读台账，页面不留补一条、改一条的入口")
    @GetMapping("/view")
    @RequiresPermissions("marr:marrDueTask:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "到期催办台账", action = "list")
    @ApiOperation(value = "催办台账(只读)", notes = "发没发出去页面上看得见")
    @GetMapping("/list")
    @RequiresPermissions("marr:marrDueTask:list")
    @ResponseBody
    public ResultTable list(TMarrDueTask record) {
        QueryWrapper<TMarrDueTask> queryWrapper = new QueryWrapper<TMarrDueTask>();
        queryWrapper.eq("del_flag", 0);
        if (record != null && record.getItemNo() != null && !record.getItemNo().isEmpty()) {
            queryWrapper.like("item_no", record.getItemNo());
        }
        if (record != null && record.getStatus() != null) {
            queryWrapper.eq("status", record.getStatus());
        }
        queryWrapper.orderByDesc("id");
        startPage();
        com.github.pagehelper.PageInfo<TMarrDueTask> page =
                new com.github.pagehelper.PageInfo<TMarrDueTask>(marrDueTaskService.selectTMarrDueTaskList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }
}
