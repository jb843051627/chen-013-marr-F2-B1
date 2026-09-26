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
 * 冷静期与领证期到期催办条目对象 t_marr_due_task
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_marr_due_task")
@ApiModel(value = "TMarrDueTask", description = "冷静期与领证期到期催办条目")
public class TMarrDueTask implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 到期催办事项单号 */
    @TableField("item_no")
    @ApiModelProperty(value = "到期催办事项单号")
    private String itemNo;

    /** 本项届满之日那天的最后一刻 */
    @TableField("due_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "本项届满之日那天的最后一刻")
    private Date dueAt;

    /** 单条提前催办的自然日数 */
    @TableField("amount")
    @ApiModelProperty(value = "单条提前催办的自然日数")
    private BigDecimal amount;

    /** 催办事由与收信人记要 */
    @TableField("content")
    @ApiModelProperty(value = "催办事由与收信人记要")
    private String content;

    /** 甲方联系电话(两位各存各的) */
    @TableField("phone_a")
    @ApiModelProperty(value = "甲方联系电话(两位各存各的)")
    private String phoneA;

    /** 乙方联系电话(两位各存各的) */
    @TableField("phone_b")
    @ApiModelProperty(value = "乙方联系电话(两位各存各的)")
    private String phoneB;

    /** 甲方催办送达 0未送 1已送 */
    @TableField("sent_a")
    @ApiModelProperty(value = "甲方催办送达 0未送 1已送")
    private Integer sentA;

    /** 乙方催办送达 0未送 1已送 */
    @TableField("sent_b")
    @ApiModelProperty(value = "乙方催办送达 0未送 1已送")
    private Integer sentB;

    /** 本宗已按撤回落定 0否 1是(落定后不再催) */
    @TableField("settled_flag")
    @ApiModelProperty(value = "本宗已按撤回落定 0否 1是(落定后不再催)")
    private Integer settledFlag;

    /** 挂起缘由(当场写明为什么挂) */
    @TableField("hang_reason")
    @ApiModelProperty(value = "挂起缘由(当场写明为什么挂)")
    private String hangReason;

    /** 条目情形 0待发 1已发 2挂起(发不出去) */
    @TableField("status")
    @ApiModelProperty(value = "条目情形 0待发 1已发 2挂起(发不出去)")
    private Integer status;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
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

    public String getItemNo() {
        return itemNo;
    }

    public void setItemNo(String itemNo) {
        this.itemNo = itemNo;
    }

    public Date getDueAt() {
        return dueAt;
    }

    public void setDueAt(Date dueAt) {
        this.dueAt = dueAt;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPhoneA() {
        return phoneA;
    }

    public void setPhoneA(String phoneA) {
        this.phoneA = phoneA;
    }

    public String getPhoneB() {
        return phoneB;
    }

    public void setPhoneB(String phoneB) {
        this.phoneB = phoneB;
    }

    public Integer getSentA() {
        return sentA;
    }

    public void setSentA(Integer sentA) {
        this.sentA = sentA;
    }

    public Integer getSentB() {
        return sentB;
    }

    public void setSentB(Integer sentB) {
        this.sentB = sentB;
    }

    public Integer getSettledFlag() {
        return settledFlag;
    }

    public void setSettledFlag(Integer settledFlag) {
        this.settledFlag = settledFlag;
    }

    public String getHangReason() {
        return hangReason;
    }

    public void setHangReason(String hangReason) {
        this.hangReason = hangReason;
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
