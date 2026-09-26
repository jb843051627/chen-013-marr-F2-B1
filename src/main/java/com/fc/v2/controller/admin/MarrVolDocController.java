package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.log.Log;
import com.fc.v2.common.exception.marr.MarrRuleException;
import com.fc.v2.mapper.auto.TMarrVolDocMapper;
import com.fc.v2.model.auto.TMarrVolDoc;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 婚姻卷宗基础档案 Controller。
 * 档案编号系统自动发号，处里不许有人自编一套号；一封卷当场收口，新上件不收，旧件照旧翻得着。
 *
 * @author fuce
 * @date 2026-09-26
 */
@Api(value = "婚姻卷宗基础档案")
@Controller
@RequestMapping("/MarrVolDocController")
public class MarrVolDocController extends BaseController {

    private final String prefix = "admin/marrVolDoc";

    @Autowired
    private TMarrVolDocMapper marrVolDocMapper;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    @RequiresPermissions("marr:marrVolDoc:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "卷宗基础档案查询", action = "list")
    @ApiOperation(value = "档案列表", notes = "档案列表")
    @GetMapping("/list")
    @RequiresPermissions("marr:marrVolDoc:list")
    @ResponseBody
    public List<TMarrVolDoc> list(String siteType, String siteNo) {
        QueryWrapper<TMarrVolDoc> qw = new QueryWrapper<TMarrVolDoc>().eq("del_flag", 0);
        if (siteType != null && !siteType.isEmpty()) {
            qw.eq("site_type", siteType);
        }
        if (siteNo != null && !siteNo.isEmpty()) {
            qw.eq("site_no", siteNo);
        }
        return marrVolDocMapper.selectList(qw.orderByAsc("site_no"));
    }

    /** 卷宗四类各归各排，编号前缀各自连号；处里不许有人自己另编一套号。 */
    private static final List<String> TYPES = Arrays.asList("结婚", "离婚", "补领", "撤销");
    private static final java.util.Map<String, String> PREFIX = new java.util.HashMap<String, String>() {{
        put("结婚", "JH");
        put("离婚", "LH");
        put("补领", "BL");
        put("撤销", "CX");
    }};

    @Log(title = "卷宗基础档案建档(系统发号)", action = "create")
    @ApiOperation(value = "建档(编号系统按类别自动连号,不许自编)", notes = "建档")
    @PostMapping("/create")
    @RequiresPermissions("marr:marrVolDoc:add")
    @ResponseBody
    public AjaxResult create(TMarrVolDoc arch) {
        if (arch.getSiteType() == null || !TYPES.contains(arch.getSiteType())) {
            throw new MarrRuleException("site_type", "卷宗类别须是结婚/离婚/补领/撤销四类之一，各归各排");
        }
        if (arch.getSiteName() == null || arch.getSiteName().trim().isEmpty()) {
            throw new MarrRuleException("site_name", "卷宗全称不得为空");
        }
        if (arch.getRoadName() == null || arch.getRoadName().trim().isEmpty()) {
            throw new MarrRuleException("road_name", "承办处须写到省—市—区县");
        }
        if (arch.getImgMax() == null || arch.getTh1Max() == null
                || arch.getTh2Max() == null || arch.getTh3Max() == null) {
            throw new MarrRuleException("th1_max", "承接上限与一/二/三档封顶都须给齐");
        }
        if (!(arch.getTh1Max().compareTo(arch.getTh2Max()) < 0
                && arch.getTh2Max().compareTo(arch.getTh3Max()) < 0)) {
            throw new MarrRuleException("th1_max", "三道封顶须一档<二档<三档");
        }
        if (arch.getTh3Max().compareTo(arch.getImgMax()) > 0) {
            throw new MarrRuleException("img_max", "三档封顶不得高出本档案承接上限");
        }

        String prefix = PREFIX.get(arch.getSiteType());
        int nextSeq = marrVolDocMapper.selectList(new QueryWrapper<TMarrVolDoc>()
                        .likeRight("site_no", prefix).orderByDesc("site_no"))
                .stream()
                .map(TMarrVolDoc::getSiteNo)
                .filter(no -> no != null && no.length() > prefix.length())
                .mapToInt(no -> {
                    try {
                        return Integer.parseInt(no.substring(prefix.length()));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max().orElse(0) + 1;
        arch.setSiteNo(String.format("%s%04d", prefix, nextSeq));
        arch.setStatus(0);
        arch.setDelFlag(0);
        marrVolDocMapper.insert(arch);
        return AjaxResult.successData(200, arch);
    }

    @Log(title = "卷宗封卷收口", action = "seal")
    @ApiOperation(value = "封卷(当场收口,新上件不收,旧件可翻查)", notes = "封卷")
    @PostMapping("/seal")
    @RequiresPermissions("marr:marrVolDoc:seal")
    @ResponseBody
    public AjaxResult seal(Long id) {
        TMarrVolDoc arch = marrVolDocMapper.selectById(id);
        if (arch == null || Integer.valueOf(1).equals(arch.getDelFlag())) {
            throw new MarrRuleException("id", "档案对不上，无法封卷");
        }
        arch.setStatus(1);
        marrVolDocMapper.updateById(arch);
        return AjaxResult.success("卷宗" + arch.getSiteNo() + "已封卷收口");
    }
}
