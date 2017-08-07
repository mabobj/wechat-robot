package com.mywechat.workbench;

import com.frame.util.PropertiesTools;
import com.frame.util.SpringGetBean;
import com.frame.util.SystemUtil;
import com.google.gson.*;
import com.mywechat.api.WechatApi;
import com.mywechat.constant.DialogueConstant;
import com.mywechat.spring.iface.BaseInfoIface;
import com.mywechat.spring.service.BaseInfoService;
import com.mywechat.workbench.base.BaseWorkbench;
import com.mywechat.workbench.base.BaseWorkbenchIface;
import io.github.biezhi.wechat.model.GroupMessage;
import io.github.biezhi.wechat.model.UserMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MaJiangUserMsg extends BaseWorkbench implements BaseWorkbenchIface {

    private MaJiangUserMsg(WechatApi api) {
        this.api = api;
    }

    private static volatile MaJiangUserMsg instance;

    public static MaJiangUserMsg getIstance(WechatApi api) {
        if (instance == null) {
            synchronized (MaJiangUserMsg.class) {
                if (instance == null) {
                    instance = new MaJiangUserMsg(api);
                }
            }
        }
        return instance;
    }

    BaseInfoIface baseInfo = SpringGetBean.getBean(BaseInfoService.class, BaseInfoIface.class);



    public void listener() {


        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        //添加完好友，单未绑定游戏ID的用户，补发信息
                        List<Map<String, Object>> list = baseInfo.getAgainMsgUserList1();
                        JsonArray cJa = api.getContactList();

                        for (Map<String, Object> map : list) {
                            for (JsonElement element : cJa) {
                                if (map.get("remark_name").toString().equals(element.getAsJsonObject().get("RemarkName").getAsString())) {

                                    if (map.get("dialogue_key").equals("MSG002") || map.get("dialogue_key").equals("MSG002_2")) {//发了002后续发003
                                        api.wxSendMessage(
                                                DialogueConstant.MSG003, element.getAsJsonObject().get("UserName").getAsString());
                                        baseInfo.saveDialogueLog("MSG003", map.get("user_key").toString());
                                        log.info("对用户 {} 发送了文案消息 {}", map.get("user_key").toString(), "MSG003");
                                    } else if (map.get("dialogue_key").equals("MSG003")) {//如果发了003，则后续发002
                                        api.wxSendMessage(
                                                DialogueConstant.MSG002, element.getAsJsonObject().get("UserName").getAsString());
                                        baseInfo.saveDialogueLog("MSG002", map.get("user_key").toString());
                                        log.info("对用户 {} 发送了文案消息 {}", map.get("user_key").toString(), "MSG002");
                                    }

                                }
                            }
                        }


                        try {
                            Thread.sleep(60 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } catch (Exception e) {
                        log.error(e.toString());
                    }
                }
            }

        }).start();
    }

    /**
     * 文本消息
     *
     * @param msg
     */
    public void msgtype_text(JsonObject msg) {

        String msgType = msg.get("MsgType").getAsString();
        String msgId = msg.get("MsgId").getAsString();
        String content = msg.get("Content").getAsString().replace("&lt;", "<").replace("&gt;", ">");

        String toUid = msg.get("FromUserName").getAsString();

        /*String text = null;
        if (content.contains(":<br/>")) {
            text = content.split(":<br/>")[1];
        } else {
            text = content;
        }*/

        //绑定游戏ID
        Pattern r = Pattern.compile("\\d{7}");
        Matcher m = r.matcher(content);
        if ((content.contains("游戏") || content.toUpperCase().contains("ID")) && m.find()) {
            log.debug("游戏ID => {}", m.group(0));

            JsonObject jo = new JsonParser().parse(api.getContactByUserMame(toUid).get("RemarkName").getAsString().split("#")[1]).getAsJsonObject();
            String game_id = baseInfo.updateGameId(jo.get("u").getAsString(), m.group(0));
            if (game_id == null) {
                api.wxSendMessage(
                        String.format(DialogueConstant.MSG004, m.group(0)),
                        toUid);
                baseInfo.saveDialogueLog("MSG004", jo.get("u").getAsString());
                log.info("对用户 {} 发送了文案消息 {}", jo.get("u").getAsString(), "MSG004");
            } else {
                api.wxSendMessage(
                        String.format(DialogueConstant.MSG004_2, game_id),
                        toUid);
                baseInfo.saveDialogueLog("MSG004_2", jo.get("u").getAsString());
                log.info("对用户 {} 发送了文案消息 {}", jo.get("u").getAsString(), "MSG004_2");
            }

            api.wxSendMessage(
                    String.format(DialogueConstant.MSG005),
                    toUid);
            baseInfo.saveDialogueLog("MSG005", jo.get("u").getAsString());
            log.info("对用户 {} 发送了文案消息 {}", jo.get("u").getAsString(), "MSG005");


        }

        log.info("私信[{}] {}", toUid, content);
        //api.wxSendMessage("自动回复私信：" + text, toUid);

    }

    /**
     * 位置消息
     *
     * @param msg
     */
    public void msgtype_text_location(JsonObject msg) {

    }

    @Override
    public void msgtype_image(JsonObject msg) {
        log.debug("msgtype_image => {}", msg.toString());
    }

    /**
     * 好友验证消息
     *
     * @param msg
     */
    public void msgtype_verifymsg(JsonObject msg) {
        log.debug("好友验证消息");

        String userMame = msg.getAsJsonObject("RecommendInfo").get("UserName").getAsString().replaceAll("\"", "");
        String Content = msg.getAsJsonObject("RecommendInfo").get("Content").getAsString().replaceAll("\"", "");
        String ticket = msg.getAsJsonObject("RecommendInfo").get("Ticket").getAsString().replaceAll("\"", "");

        if (userMame.equals("")) {
            userMame = msg.get("FromUserName").getAsString();
            Content = msg.get("Content").getAsString();
        }

        JsonArray ja = api.batchGetContact(userMame);
        JsonObject jo = ja.get(0).getAsJsonObject();
        log.debug("msgtype_verifymsg msg => {}", msg.toString());
        log.debug("msgtype_verifymsg => {}", jo.toString());


        //if (!baseInfo.existsSessionObject(jo.get("UserName").getAsString())) {
        //}
        String user_key = SystemUtil.randomUUID().substring(26, 32);

        Map map = new HashMap();

        map.put("robot_uin", api.getSession().getUin());
        map.put("user_key", user_key);
        map.put("nick_name", SystemUtil.removeEmoji(jo.get("NickName").getAsString()));

        String rName = SystemUtil.removeEmoji(jo.get("NickName").getAsString());
        rName = rName.replace("#", "");
        Map tmap = new HashMap();
        tmap.put("u", user_key);
        rName = rName + "#" + new Gson().toJson(tmap).toString();

        map.put("remark_name", rName);
        map.put("key_word", jo.get("KeyWord").getAsString());
        map.put("province", jo.get("Province").getAsString());
        map.put("city", jo.get("City").getAsString());
        map.put("head_img_url", jo.get("HeadImgUrl").getAsString());
        map.put("sex", jo.get("Sex").getAsString());
        map.put("signature", SystemUtil.removeEmoji(jo.get("Signature").getAsString().trim()));

        /*try {
            map.put("add_content", URLEncoder.encode(new String(Content.getBytes()), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/

        map.put("add_content", SystemUtil.removeEmoji(Content));

        map = baseInfo.saveUserInfo(jo.get("UserName").getAsString(), map);

        //通过好友请求
        if (!ticket.equals("")) {
            api.addFriend(userMame, ticket);
        }

        //修改昵称
        api.editRemarks(jo.get("UserName").getAsString(), map.get("remark_name").toString());

        //添加好友欢迎语
        String game_id = PropertiesTools.adminGameId;
        if (map.get("game_id") != null && !map.get("game_id").toString().equals("")) {
            game_id = map.get("game_id").toString();
        }
        api.wxSendMessage(
                String.format(DialogueConstant.MSG001, game_id)
                , userMame);
        baseInfo.saveDialogueLog("MSG001", user_key);
        log.info("对用户 {} 发送了文案消息 {}", userMame, "MSG001");

        //更新通讯录信息
        api.getContact();
    }

    /**
     * 处理主动添加好友被通过后的信息
     * @param userName
     */
    public void addFriends(String userName) {

    }
    @Override
    public void msgtype_app(JsonObject msg) {
        log.debug("msgtype_app => {}" + msg.toString());

    }

    /**
     * 系统信息
     *
     * @param msg
     */
    public void msgtype_sys(JsonObject msg) {
        log.debug("msgtype_sys => {}", msg.toString());
    }

    public void userMessage(UserMessage userMessage) {

    }

    public void groupMessage(GroupMessage groupMessage) {

    }

    public void groupMemberChange(String groupId, JsonArray memberList) {

    }

    public void groupListChange(String groupId, JsonArray memberList) {

    }

}
