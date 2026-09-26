package com.fc.v2.model.custom;

/**
 * 到期催办一轮局面：每轮跑完回一屏，只摆本轮的事。
 * 当日待发、已发、挂起各多少条都在业务服务层那个执行方法里算完再回写，
 * 屏上摆的数须与本对象给回的一致，不许另算一份。
 *
 * @author fuce
 * @date 2026-09-26
 */
public class MarrDueRound {

    /** 哪一头叫醒的这一轮（定时扫描=scan） */
    private String scene;

    /** 本轮是否在出手时段内（只认每天 09:00-11:30、15:00-17:30 两格） */
    private boolean inWindow;

    /** 当日待发：本轮压到脸上、跑完仍压着的条数（格外按下不动也算在内） */
    private int pending;

    /** 本轮已发条数（两位当事人都送到才算一条） */
    private int sent;

    /** 本轮挂起条数（送不出去，已当场写明缘由） */
    private int suspended;

    /** 一句话局面：一条都不到期的日子答"本轮无事"，不算故障 */
    private String message;

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public boolean isInWindow() {
        return inWindow;
    }

    public void setInWindow(boolean inWindow) {
        this.inWindow = inWindow;
    }

    public int getPending() {
        return pending;
    }

    public void setPending(int pending) {
        this.pending = pending;
    }

    public int getSent() {
        return sent;
    }

    public void setSent(int sent) {
        this.sent = sent;
    }

    public int getSuspended() {
        return suspended;
    }

    public void setSuspended(int suspended) {
        this.suspended = suspended;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }
}
