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
import java.util.Date;

/**
 * 查档授权登记对象 t_marr_archive_grant
 * viewer_type=1本人：id_no+site_no；2机关：scope_road辖内全看；3律师：id_no+受托site_no
 *
 * @author fuce
 * @date 2026-09-26
 */
@TableName("t_marr_archive_grant")
@ApiModel(value = "TMarrArchiveGrant", description = "查档授权登记")
public class TMarrArchiveGrant implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 查档人情形 1本人 2机关 3律师 */
    public static final int VIEWER_SELF = 1;
    public static final int VIEWER_ORG = 2;
    public static final int VIEWER_LAWYER = 3;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 查档授权登记号 */
    @TableField("grant_no")
    @ApiModelProperty(value = "查档授权登记号")
    private String grantNo;

    /** 查档人情形 1本人 2机关 3律师 */
    @TableField("viewer_type")
    @ApiModelProperty(value = "查档人情形 1本人 2机关 3律师")
    private Integer viewerType;

    /** 查档人/机关名 */
    @TableField("viewer_name")
    @ApiModelProperty(value = "查档人/机关名")
    private String viewerName;

    /** 本人证件号或律师执业证号 */
    @TableField("id_no")
    @ApiModelProperty(value = "本人证件号或律师执业证号")
    private String idNo;

    /** 机关本辖内承办处名(省—市—区县) */
    @TableField("scope_road")
    @ApiModelProperty(value = "机关本辖内承办处名")
    private String scopeRoad;

    /** 本人名下卷或律师受托卷编号(机关留空) */
    @TableField("site_no")
    @ApiModelProperty(value = "本人名下卷或律师受托卷编号")
    private String siteNo;

    /** 查档事由(进水印那一行) */
    @TableField("matter")
    @ApiModelProperty(value = "查档事由")
    private String matter;

    /** 公函/法院函/委托函号 */
    @TableField("letter_no")
    @ApiModelProperty(value = "公函/法院函/委托函号")
    private String letterNo;

    /** 授权情形 0有效 1已失效 */
    @TableField("status")
    @ApiModelProperty(value = "授权情形 0有效 1已失效")
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

    public String getGrantNo() {
        return grantNo;
    }

    public void setGrantNo(String grantNo) {
        this.grantNo = grantNo;
    }

    public Integer getViewerType() {
        return viewerType;
    }

    public void setViewerType(Integer viewerType) {
        this.viewerType = viewerType;
    }

    public String getViewerName() {
        return viewerName;
    }

    public void setViewerName(String viewerName) {
        this.viewerName = viewerName;
    }

    public String getIdNo() {
        return idNo;
    }

    public void setIdNo(String idNo) {
        this.idNo = idNo;
    }

    public String getScopeRoad() {
        return scopeRoad;
    }

    public void setScopeRoad(String scopeRoad) {
        this.scopeRoad = scopeRoad;
    }

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }

    public String getMatter() {
        return matter;
    }

    public void setMatter(String matter) {
        this.matter = matter;
    }

    public String getLetterNo() {
        return letterNo;
    }

    public void setLetterNo(String letterNo) {
        this.letterNo = letterNo;
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
