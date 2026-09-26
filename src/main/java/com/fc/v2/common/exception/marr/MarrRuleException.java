package com.fc.v2.common.exception.marr;

/**
 * 婚姻卷宗影像业务规则异常。
 * 回执必须指名是册上哪一格出了岔，不能只甩一句"数不像话"，故带列名字段。
 *
 * @author fuce
 * @date 2026-09-26
 */
public class MarrRuleException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** 出岔的那一格列名（对应表列/实体字段），如 qty、fine_amt、site_no */
    private final String field;

    public MarrRuleException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
