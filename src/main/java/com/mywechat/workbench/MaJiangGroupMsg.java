package com.mywechat.workbench;

import com.frame.util.PropertiesTools;
import com.frame.util.RedisTools;
import com.frame.util.SpringGetBean;
import com.frame.util.SystemUtil;
import com.google.gson.*;
import com.mywechat.constant.DialogueConstant;
import com.mywechat.spring.iface.BaseInfoIface;
import com.mywechat.spring.service.BaseInfoService;
import com.mywechat.workbench.base.BaseWorkbench;
import com.mywechat.workbench.base.BaseWorkbenchIface;
import com.mywechat.api.WechatApi;
import com.mywechat.workbench.model.GroupInfo;
import io.github.biezhi.wechat.model.GroupMessage;
import io.github.biezhi.wechat.model.UserMessage;
import redis.clients.jedis.Jedis;

import java.text.SimpleDateFormat;
import java.util.*;

public class MaJiangGroupMsg extends BaseWorkbench implements BaseWorkbenchIface {
    BaseInfoIface baseInfo;

    private MaJiangGroupMsg(WechatApi api) {
        this.api = api;
        baseInfo = SpringGetBean.getBean(BaseInfoService.class, BaseInfoIface.class);
    }

    private static volatile MaJiangGroupMsg instance;

    public static MaJiangGroupMsg getIstance(WechatApi api) {
        if (instance == null) {
            synchronized (MaJiangGroupMsg.class) {
                if (instance == null) {
                    instance = new MaJiangGroupMsg(api);
                }
            }
        }
        return instance;

    }

    private Map<String, GroupInfo> sessionActiveGroup = new HashMap();

    public void wxSync(JsonObject dic) {
        JsonArray msgs = dic.getAsJsonArray("AddMsgList");

        for (JsonElement element : msgs) {
            JsonObject msg = element.getAsJsonObject();
            if (msg.get("MsgType").getAsString().equals("1")) {
                log.debug("wxSync => {}", msg.toString());
                if (msg.get("FromUserName").getAsString().contains("@@")) {
                    if (sessionActiveGroup.containsKey(msg.get("FromUserName").getAsString())) {

                    } else {
                        GroupInfo gi = new GroupInfo();
                        gi.setUserName(msg.get("FromUserName").getAsString());
                        JsonArray groupList = api.batchGetContact(msg.get("FromUserName").getAsString()).get(0).getAsJsonObject().get("MemberList").getAsJsonArray();
                        log.debug(" ===> {}", groupList.toString());
                        gi.setMemberList(groupList);
                        sessionActiveGroup.put(msg.get("FromUserName").getAsString(), gi);
                    }
                }
            }

        }
    }

    @Override
    public void listener() {

        try {
            Thread.sleep(1000 * 5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        JsonArray groupJA = api.getGroupList();
        for (JsonElement element : groupJA) {
            JsonObject msg = element.getAsJsonObject();
            if (msg.get("UserName").getAsString().contains("@@")) {
                if (sessionActiveGroup.containsKey(msg.get("UserName").getAsString())) {

                } else {
                    GroupInfo gi = new GroupInfo();
                    gi.setUserName(msg.get("UserName").getAsString());
                    JsonArray groupList = api.getGroupMemeberList().get(msg.get("UserName").getAsString());
                    log.debug(" ===> {}", groupList.toString());
                    gi.setMemberList(groupList);
                    sessionActiveGroup.put(msg.get("UserName").getAsString(), gi);
                }
            }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        log.debug("sessionActiveGroup keySet size => {}", sessionActiveGroup.keySet().toString());
                        for (String s : sessionActiveGroup.keySet()) {
                            GroupInfo gi = sessionActiveGroup.get(s);
                            log.debug("gi userName => {}", gi.getUserName());
                            //两次说话之间大于10分钟
                            if (
                                    gi.getLastMsgDate() == null ||
                                            ((new Date().getTime() - gi.getLastMsgDate().getTime()) / 1000 / 60) > 10
                                    ) {


                                Date postDage = (Date) gi.getPostMsgDate().get("MSG201");
                                if (
                                        postDage == null ||
                                                ((new Date().getTime() - postDage.getTime()) / 1000 / 60 / 60) > 1
                                        ) {
                                    SimpleDateFormat sdf = new SimpleDateFormat("mm");
                                    int mm = Integer.valueOf(sdf.format(new Date()));
                                    if (mm > 7 || mm < 3) {
                                        api.wxSendMessage(
                                                String.format(DialogueConstant.MSG201, "URL"), gi.getUserName());
                                        log.info("对用户 {} 发送了文案消息 {}", gi.getUserName(), "MSG201");
                                        gi.getPostMsgDate().put("MSG201", new Date());
                                        gi.setLastMsgDate(new Date());
                                    }
                                    continue;
                                }


                                postDage = (Date) gi.getPostMsgDate().get("MSG202");
                                if (
                                        postDage == null ||
                                                ((new Date().getTime() - postDage.getTime()) / 1000 / 60 / 60) > 2) {
                                    SimpleDateFormat sdf = new SimpleDateFormat("mm");
                                    int mm = Integer.valueOf(sdf.format(new Date()));
                                    if (mm > 7 || mm < 3) {
                                        api.wxSendMessage(
                                                String.format(DialogueConstant.MSG202), gi.getUserName());
                                        log.info("对用户 {} 发送了文案消息 {}", gi.getUserName(), "MSG202");
                                        gi.getPostMsgDate().put("MSG202", new Date());
                                        gi.setLastMsgDate(new Date());
                                    }
                                    continue;
                                }


                            }
                        }

                        Thread.sleep(1000 * 60);
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

        String gId = msg.get("FromUserName").getAsString();

        String text = null;
        if (content.contains(":<br/>")) {
            text = content.split(":<br/>")[1];
        } else {
            text = content;
        }
        log.info("群消息[{}] {}", gId, content);
        if (gId.startsWith("@@")) {
            List<String> list = new ArrayList();
            list.add(gId);

            //JsonArray groupInfo = api.batchGetContact(list);
            //JsonArray groupUser = api.batchGetContact(list);


            //api.wxSendMessage("自动回复群消息：" + text, gId);
        }

    }

    /**
     * 位置消息
     *
     * @param msg
     */
    public void msgtype_text_location(JsonObject msg) {

    }

    /**
     * 系统信息
     *
     * @param msg
     */
    public void msgtype_sys(JsonObject msg) {
        String toGid = msg.get("FromUserName").getAsString();

        //邀请入群信息
        if (msg.get("Status").getAsString().equals("4")) {
            String content = msg.get("Content").getAsString();
            if (content.contains("邀请你")) {//群主邀请机器人

                log.debug("msgtype_sys 邀请你 => {}", content);
                content = content.split("\"邀请你")[0];
                JsonObject ukObj = new JsonParser().parse(content.split("#")[1]).getAsJsonObject();
                saveGroupUser(msg.get("FromUserName").getAsString(), ukObj.get("u").getAsString());

                Map userMap = baseInfo.getUserInfo(ukObj.get("u").getAsString());
                String game_id = PropertiesTools.adminGameId;
                if (userMap.get("game_id") != null && !userMap.get("game_id").equals("")) {
                    game_id = userMap.get("game_id").toString();
                }
                api.wxSendMessage(
                        String.format(DialogueConstant.MSG101, userMap.get("nick_name"), userMap.get("nick_name"), game_id),
                        toGid);
                baseInfo.saveDialogueLog("MSG101", ukObj.get("u").getAsString());
                log.info("对用户 {} 发送了文案消息 {}", ukObj.get("u").getAsString(), "MSG101");

            } else if (content.contains("邀请")) {//群主邀请其他人

                content = content.replace("加入了群聊", "");
                log.debug("msgtype_sys 群主邀请其他人 => {}", content);
                content = content.split("\"邀请")[0];
                JsonObject ukObj = new JsonParser().parse(content.split("#")[1]).getAsJsonObject();
                saveGroupAddUser(msg.get("FromUserName").getAsString(), ukObj.get("u").getAsString());


                Map userMap = baseInfo.getUserInfo(ukObj.get("u").getAsString());
                if (userMap != null) {
                    api.wxSendMessage(
                            String.format(DialogueConstant.MSG101, userMap.get("nick_name"), userMap.get("nick_name"), userMap.get("game_id")),
                            toGid);
                    baseInfo.saveDialogueLog("MSG101", ukObj.get("u").getAsString());
                    log.info("对用户 {} 发送了文案消息 {}", ukObj.get("u").getAsString(), "MSG101");
                }


            } else if (content.contains("移出群聊")) {
                log.debug("msgtype_sys 移出群聊 => {}", content);
                content = content.split("移出群聊")[0];
            }

        }
        log.debug("msgtype_sys => {}", msg.toString());
    }


    private void saveGroupUser(String FromUserName, String father_key) {

        JsonArray groupList = api.batchGetContact(FromUserName).get(0).getAsJsonObject().get("MemberList").getAsJsonArray();
        //sessionActiveGroup.get(FromUserName).setMemberList(groupList);
        //sessionActiveGroup.get(FromUserName).setAdminUserKey(father_key);

        log.debug("saveGroupUser groupList => {}", groupList.toString());
        //获取群成员列表
        //JsonArray jp = new JsonParser().parse(jo.get("MemberList").toString()).getAsJsonArray();

        //分页拉取群成员
        int page = groupList.size() / 50;
        for (int i = 0; i < page + 1; i++) {
            int no = groupList.size() - (i * 50);
            if (no > 50) {
                no = 50;
            }

            List userNameList = new ArrayList();
            for (int j = (i * 50); j < ((i * 50) + no); j++) {
                JsonObject ujo = groupList.get(j).getAsJsonObject();
                userNameList.add(ujo.get("UserName").getAsString());
            }

            JsonArray jaUser = api.batchGetContact(userNameList, FromUserName);
            log.debug("jaUser[{}] => {}", i, jaUser.toString());

            //保存群成员
            for (JsonElement element : jaUser) {
                JsonObject joo = element.getAsJsonObject();

                if (joo.get("UserName").getAsString().equals(api.getUser().get("UserName"))) {//是自己则跳过
                    continue;
                }

                String user_key = SystemUtil.randomUUID().substring(26, 32);

                Map map = new HashMap();

                map.put("robot_uin", api.getSession().getUin());
                map.put("user_key", user_key);
                map.put("nick_name", SystemUtil.removeEmoji(joo.get("NickName").getAsString()));
                //map.put("remark_name", joo.get("RemarkName").getAsString());
                map.put("key_word", joo.get("KeyWord").getAsString());
                map.put("province", joo.get("Province").getAsString());
                map.put("city", joo.get("City").getAsString());
                map.put("head_img_url", joo.get("HeadImgUrl").getAsString());
                map.put("sex", joo.get("Sex").getAsString());
                map.put("signature", SystemUtil.removeEmoji(joo.get("Signature").getAsString()));
                map.put("father_key", father_key);
                try {
                    baseInfo.saveUserInfo(joo.get("UserName").getAsString(), map);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        }

    }

    /**
     * 群主邀请新人入群
     *
     * @param FromUserName
     * @param father_key
     */
    private void saveGroupAddUser(String FromUserName, String father_key) {
        JsonArray groupList = api.batchGetContact(FromUserName).get(0).getAsJsonObject().get("MemberList").getAsJsonArray();
        sessionActiveGroup.get(FromUserName).setMemberList(groupList);
        sessionActiveGroup.get(FromUserName).setAdminUserKey(father_key);

        log.debug("saveGroupAddUser groupList => {}", groupList.toString());

        //分页拉取群成员
        int page = groupList.size() / 50;
        for (int i = 0; i < page + 1; i++) {
            int no = groupList.size() - (i * 50);
            if (no > 50) {
                no = 50;
            }

            List userNameList = new ArrayList();
            for (int j = (i * 50); j < ((i * 50) + no); j++) {
                JsonObject ujo = groupList.get(j).getAsJsonObject();
                userNameList.add(ujo.get("UserName").getAsString());
            }

            JsonArray jaUser = api.batchGetContact(userNameList, FromUserName);
            log.debug("jaUser[{}] => {}", i, jaUser.toString());

            List atName = new ArrayList();

            //保存群成员
            for (JsonElement element : jaUser) {
                JsonObject joo = element.getAsJsonObject();

                if (joo.get("UserName").getAsString().equals(api.getUser().get("UserName"))) {//是自己则跳过
                    continue;
                }

                String user_key = SystemUtil.randomUUID().substring(26, 32);

                Map map = new HashMap();

                map.put("robot_uin", api.getSession().getUin());
                map.put("user_key", user_key);
                map.put("nick_name", SystemUtil.removeEmoji(joo.get("NickName").getAsString()));
                map.put("remark_name", joo.get("RemarkName").getAsString());
                map.put("key_word", joo.get("KeyWord").getAsString());
                map.put("province", joo.get("Province").getAsString());
                map.put("city", joo.get("City").getAsString());
                map.put("head_img_url", joo.get("HeadImgUrl").getAsString());
                map.put("sex", joo.get("Sex").getAsString());
                map.put("signature", SystemUtil.removeEmoji(joo.get("Signature").getAsString()));
                map.put("father_key", father_key);

                try {
                    baseInfo.saveUserInfo(joo.get("UserName").getAsString(), map);

                    atName.add(map.get("nick_name"));
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }


            /*String msgText = "";
            for (Object o : atName) {
                msgText = msgText + "@" + o + " ";
            }
            msgText = msgText + "欢迎你加入";

            api.wxSendMessage(msgText, FromUserName);*/
        }
    }

    @Override
    public void msgtype_image(JsonObject msg) {
        log.debug("msgtype_image => {}", msg.toString());
    }

    @Override
    public void msgtype_verifymsg(JsonObject msg) {
        //拉人入群
    }

    @Override
    public void msgtype_app(JsonObject msg) {
        log.debug("msgtype_app => {}" + msg.toString());
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
