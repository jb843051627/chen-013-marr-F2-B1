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
import java.util.Date;

/**
 * 影像件承办续笔对象 t_marr_img_handler_log（办一次续一笔，与创建人分列）
 *
 * @author fuce
 * @date 2026-09-26
 */
@TableName("t_marr_img_handler_log")
@ApiModel(value = "TMarrImgHandlerLog", description = "影像件承办续笔")
public class TMarrImgHandlerLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 影像件建档卡id */
    @TableField("card_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "影像件建档卡id")
    private Long cardId;

    /** 影像件建档卡号 */
    @TableField("bill_no")
    @ApiModelProperty(value = "影像件建档卡号")
    private String billNo;

    /** 婚姻卷宗档案编号 */
    @TableField("site_no")
    @ApiModelProperty(value = "婚姻卷宗档案编号")
    private String siteNo;

    /** 承办时所在影像版本号 */
    @TableField("node_no")
    @ApiModelProperty(value = "承办时所在影像版本号")
    private Integer nodeNo;

    /** 本趟承办人 */
    @TableField("handler_name")
    @ApiModelProperty(value = "本趟承办人")
    private String handlerName;

    /** 过口动作 register建档 reupload重扫 rehang改挂 segment页次增减 verify核对 press压死 */
    @TableField("action")
    @ApiModelProperty(value = "过口动作")
    private String action;

    /** 承办记要 */
    @TableField("memo")
    @ApiModelProperty(value = "承办记要")
    private String memo;

    /** 记录人 */
    @TableField("create_by")
    @ApiModelProperty(value = "记录人")
    private String createBy;

    /** 记录时间 */
    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "记录时间")
    private Date createTime;

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

    public Integer getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(Integer nodeNo) {
        this.nodeNo = nodeNo;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
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
}
