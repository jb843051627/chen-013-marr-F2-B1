package com.fc.v2.model.auto;

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
 * 卷宗影像件历史版本层对象 t_marr_img_card_his（只进不改不删，升版追加）
 *
 * @author fuce
 * @date 2026-09-26
 */
@TableName("t_marr_img_card_his")
@ApiModel(value = "TMarrImgCardHis", description = "卷宗影像件历史版本层")
public class TMarrImgCardHis implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 来源现行卡id */
    @TableField("card_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "来源现行卡id")
    private Long cardId;

    /** 影像件建档卡号 */
    @TableField("bill_no")
    @ApiModelProperty(value = "影像件建档卡号")
    private String billNo;

    /** 婚姻卷宗档案编号 */
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

    /** 封存的影像版本号 */
    @TableField("node_no")
    @ApiModelProperty(value = "封存的影像版本号")
    private Integer nodeNo;

    /** 该版在册封影像页数(页) */
    @TableField("qty")
    @ApiModelProperty(value = "该版在册封影像页数(页)")
    private BigDecimal qty;

    /** 该版本年度新上件页数(页) */
    @TableField("fine_amt")
    @ApiModelProperty(value = "该版本年度新上件页数(页)")
    private BigDecimal fineAmt;

    /** 该版影像完整度档 */
    @TableField("grade_level")
    @ApiModelProperty(value = "该版影像完整度档")
    private Integer gradeLevel;

    /** 该版承办人 */
    @TableField("handler_name")
    @ApiModelProperty(value = "该版承办人")
    private String handlerName;

    /** 该版压死定格的水印字样(原样留存) */
    @TableField("content")
    @ApiModelProperty(value = "该版压死定格的水印字样")
    private String content;

    /** 该版水印当日日期 */
    @TableField("wm_date")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "该版水印当日日期")
    private Date wmDate;

    /** 退现行时卡进展 0待核对 1已核对 2已压死 */
    @TableField("status")
    @ApiModelProperty(value = "退现行时卡进展")
    private Integer status;

    /** 版本封存人 */
    @TableField("create_by")
    @ApiModelProperty(value = "版本封存人")
    private String createBy;

    /** 版本封存时间 */
    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "版本封存时间")
    private Date createTime;

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

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
