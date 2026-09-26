package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 卷宗影像件建档卡对象 t_marr_img_card（现行账：同卷宗同段页次只露最新版）
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_marr_img_card")
@ApiModel(value = "TMarrImgCard", description = "卷宗影像件建档卡")
public class TMarrImgCard implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 影像件建档卡号(系统发号,不许自编) */
    @TableField("bill_no")
    @ApiModelProperty(value = "影像件建档卡号")
    private String billNo;

    /** 婚姻卷宗档案编号(凭编号挂档案,不凭姓名) */
    @TableField("site_no")
    @ApiModelProperty(value = "婚姻卷宗档案编号")
    private String siteNo;

    /** 本段管起页次(页) */
    @TableField("seg_start")
    @ApiModelProperty(value = "本段管起页次(页)")
    private Integer segStart;

    /** 本段管止页次(页) */
    @TableField("seg_end")
    @ApiModelProperty(value = "本段管止页次(页)")
    private Integer segEnd;

    /** 本件影像版本号(重扫一版进一版) */
    @TableField("node_no")
    @ApiModelProperty(value = "本件影像版本号")
    private Integer nodeNo;

    /** 本卡合并后在册影像页数(页) */
    @TableField("qty")
    @ApiModelProperty(value = "本卡合并后在册影像页数(页)")
    private BigDecimal qty;

    /** 本年度新上件页数(页) */
    @TableField("fine_amt")
    @ApiModelProperty(value = "本年度新上件页数(页)")
    private BigDecimal fineAmt;

    /** 影像完整度档(系统折算写回,手工不得改) */
    @TableField("grade_level")
    @ApiModelProperty(value = "影像完整度档")
    private Integer gradeLevel;

    /** 本趟承办人(办一次续一笔,与创建人分列) */
    @TableField("handler_name")
    @ApiModelProperty(value = "本趟承办人")
    private String handlerName;

    /** 出证水印字样(压死那一刻定格,此后不可改) */
    @TableField("content")
    @ApiModelProperty(value = "出证水印字样")
    private String content;

    /** 水印当日日期(随影像版本走,新旧版各用各的) */
    @TableField("wm_date")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "水印当日日期")
    private Date wmDate;

    /** 进展 0待核对 1已核对 2已压死 */
    @TableField("status")
    @ApiModelProperty(value = "进展 0待核对 1已核对 2已压死")
    private Integer status;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者(立卡人,任人重录也不挪) */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }

    public Integer getSegStart() {
        return segStart;
    }

    public void setSegStart(Integer segStart) {
        this.segStart = segStart;
    }

    public Integer getSegEnd() {
        return segEnd;
    }

    public void setSegEnd(Integer segEnd) {
        this.segEnd = segEnd;
    }

    public Integer getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(Integer nodeNo) {
        this.nodeNo = nodeNo;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public BigDecimal getFineAmt() {
        return fineAmt;
    }

    public void setFineAmt(BigDecimal fineAmt) {
        this.fineAmt = fineAmt;
    }

    public Integer getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(Integer gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getWmDate() {
        return wmDate;
    }

    public void setWmDate(Date wmDate) {
        this.wmDate = wmDate;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
