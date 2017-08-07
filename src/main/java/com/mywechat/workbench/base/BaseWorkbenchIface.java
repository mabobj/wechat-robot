package com.mywechat.workbench.base;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.biezhi.wechat.model.GroupMessage;
import io.github.biezhi.wechat.model.UserMessage;

public interface BaseWorkbenchIface {

    public void listener();

    public void wxSync(JsonObject msg);

    public void userMessage(UserMessage userMessage);

    public void groupMessage(GroupMessage groupMessage);

    public void groupMemberChange(String groupId, JsonArray memberList);

    public void groupListChange(String groupId, JsonArray memberList);


    /**
     * 文本消息
     *
     * @param msg
     */
    public void msgtype_text(JsonObject msg);

    /**
     * 位置消息
     *
     * @param msg
     */
    public void msgtype_text_location(JsonObject msg);


    /**
     * 图片信息
     * @param msg
     */
    public void msgtype_image(JsonObject msg);

    /**
     * 好友验证消息
     * @param msg
     */
    public void msgtype_verifymsg(JsonObject msg);

    /**
     * 系统消息
     * @param msg
     */
    public void msgtype_app(JsonObject msg);

    /**
     * 系统信息
     * @param msg
     */
    public void msgtype_sys(JsonObject msg);
}
