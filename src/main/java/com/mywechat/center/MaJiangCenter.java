package com.mywechat.center;

import com.frame.util.SpringGetBean;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mywechat.spring.iface.BaseInfoIface;
import com.mywechat.spring.service.BaseInfoService;
import com.mywechat.workbench.MaJiangGroupMsg;
import com.mywechat.workbench.MaJiangUserMsg;
import com.mywechat.workbench.base.BaseWorkbenchIface;
import io.github.biezhi.wechat.Utils;
import com.mywechat.api.WechatApi;
import io.github.biezhi.wechat.model.Const;
import io.github.biezhi.wechat.model.Environment;
import io.github.biezhi.wechat.model.GroupMessage;
import io.github.biezhi.wechat.model.UserMessage;
import io.github.biezhi.wechat.ui.QRCodeFrame;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MaJiangCenter extends WechatApi {
    private static final Logger log = LogManager.getLogger(MaJiangCenter.class);

    private static final ExecutorService executorService = Executors.newFixedThreadPool(3);

    private BaseWorkbenchIface workbench = MaJiangUserMsg.getIstance(this);

    private QRCodeFrame qrCodeFrame;

    public MaJiangCenter(Environment environment) {
        super(environment);
    }


    /**
     * 启动机器人
     */
    public void start() {
        log.info(Const.LOG_MSG_START);
        log.info(Const.LOG_MSG_TRY_INIT);

        if (webwxinit()) {
            log.info(Const.LOG_MSG_SUCCESS);
        } else {
            waitForLogin();
            log.info(Const.LOG_MSG_LOGIN);
            if (!login()) {
                log.warn("登录失败");
            }
            log.info(Const.LOG_MSG_INIT);
            if (!webwxinit()) {
                log.warn("初始化失败");
            }
        }

        log.info(Const.LOG_MSG_STATUS_NOTIFY);
        if (!openStatusNotify()) {
            log.warn("状态通知打开失败");
        }
        log.info(Const.LOG_MSG_GET_CONTACT);
        if (!getContact()) {
            log.warn("获取联系人失败");
        }
        log.info(Const.LOG_MSG_CONTACT_COUNT, memberCount, memberList.size());
        log.info(Const.LOG_MSG_OTHER_CONTACT_COUNT, groupList.size(), contactList.size(), specialUsersList.size(), publicUsersList.size());

        if (groupList.size() > 0) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    log.info(Const.LOG_MSG_GET_GROUP_MEMBER);
                    MaJiangCenter.super.fetchGroupContacts();
                }
            });
        }
        log.info(Const.LOG_MSG_SNAPSHOT);

        //保存使用者微信信息
        BaseInfoIface baseInfo = SpringGetBean.getBean(BaseInfoService.class, BaseInfoIface.class);
        baseInfo.lastLogin(this.user);

        MaJiangUserMsg.getIstance(this).listener();
        MaJiangGroupMsg.getIstance(this).listener();

        super.snapshot();
        this.listen();
    }

    /**
     * 登录
     */
    private void waitForLogin() {

        while (true) {
            log.info(Const.LOG_MSG_GET_UUID);
            getUUID();
            log.info(Const.LOG_MSG_GET_QRCODE);
            final String qrCodePath = genqrcode();
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    try {
                        if (null != qrCodeFrame) qrCodeFrame.dispose();
                        UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
                        qrCodeFrame = new QRCodeFrame(qrCodePath);
                    } catch (Exception e) {
                        log.error("显示二维码失败", e);
                    }
                }
            });
            log.info(Const.LOG_MSG_SCAN_QRCODE);
            if (!waitforlogin(1)) {
                continue;
            }
            log.info(Const.LOG_MSG_CONFIRM_LOGIN);
            if (!waitforlogin(0)) {
                continue;
            }
            break;
        }
        qrCodeFrame.setVisible(false);
        qrCodeFrame.dispose();
    }


    private void listen() {
        while (true) {
            // retcode, selector
            try {
                int[] checkResponse = synccheck();
                int retcode = checkResponse[0];
                int selector = checkResponse[1];
                log.debug("retcode: {}, selector: {}", retcode, selector);
                switch (retcode) {
                    case 1100:
                        log.warn(Const.LOG_MSG_LOGOUT);
                        break;
                    case 1101:
                        log.warn(Const.LOG_MSG_LOGIN_OTHERWHERE);
                        break;
                    case 1102:
                        log.warn(Const.LOG_MSG_QUIT_ON_PHONE);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        break;
                    case 0:
                        this.handle(selector);
                        break;
                    default:
                        log.debug("wxSync: {}\n", wxSync().toString());
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    //状态处理
    private void handle(int selector) {
        JsonObject dic;
        switch (selector) {
            case 2:
                dic = wxSync();
                if (null != dic) {
                    handle_msg(dic);
                }
                break;
            case 3:
                wxSync();
                break;
            case 6:
                dic = wxSync();
                if (null != dic) {
                    handle_mod(dic);
                }
                break;
            case 7:
                wxSync();
                break;
            case 0:
                break;
            case 4:
                // 保存群聊到通讯录
                // 修改群名称
                // 新增或删除联系人
                // 群聊成员数目变化
                dic = wxSync();
                if (null != dic) {
                    handle_mod(dic);
                }
                break;
            default:
                break;
        }
    }

    //事件处理器
    private void handle_mod(JsonObject dic) {
        log.debug("事件：{}", dic.toString());
        handle_msg(dic);

        /*JsonArray modContactList = dic.getAsJsonArray("ModContactList");
        for (JsonElement element : modContactList) {
            JsonObject m = element.getAsJsonObject();

            if (m.get("UserName").getAsString().startsWith("@@")) {//群消息
                workbench = MaJiangGroupMsg.getIstance(this);
                workbench.msgtype_sys(m);

                boolean in_list = false;
                String g_id = m.get("UserName").getAsString();
                for (JsonElement ge : groupList) {
                    JsonObject group = ge.getAsJsonObject();
                    if (g_id.equals(group.get("UserName").getAsString())) {
                        in_list = true;
                        group.addProperty("MemberCount", m.get("MemberCount").getAsInt());
                        group.addProperty("NickName", m.get("NickName").getAsInt());
                        this.groupMemeberList.put(g_id, m.get("MemberList").getAsJsonArray());
                        if (null != workbench) {
                            workbench.groupMemberChange(g_id, m.get("MemberList").getAsJsonArray());
                        }
                        break;
                    }
                }
                if (!in_list) {
                    this.groupList.add(m);
                    this.groupMemeberList.put(g_id, m.get("MemberList").getAsJsonArray());
                    if (null != workbench) {
                        workbench.groupListChange(g_id, m.get("MemberList").getAsJsonArray());
                        workbench.groupMemberChange(g_id, m.get("MemberList").getAsJsonArray());
                    }
                }
            } else if (m.get("UserName").getAsString().equals("@")) {//用户消息
                workbench = MaJiangUserMsg.getIstance(this);
                workbench.msgtype_sys(m);

                boolean in_list = false;
                for (JsonElement ue : memberList) {
                    JsonObject u = ue.getAsJsonObject();
                    String u_id = m.get("UserName").getAsString();
                    if (u_id.equals(u.get("UserName").getAsString())) {
                        u = m;
                        in_list = true;
                        break;
                    }
                }
                if (!in_list) {
                    this.memberList.add(m);
                }
            }
        }*/
    }


    //消息处理器
    public void handle_msg(JsonObject dic) {

        if (null != workbench) {
            workbench.wxSync(dic);
        }

        //消息内容列队
        int n = dic.getAsJsonArray("AddMsgList").size();
        if (n == 0) {
            return;
        }

        log.debug(Const.LOG_MSG_NEW_MSG, n);

        JsonArray msgs = dic.getAsJsonArray("AddMsgList");
        for (JsonElement element : msgs) {
            JsonObject msg = element.getAsJsonObject();

            String msgType = msg.get("MsgType").getAsString();
            String content = msg.get("Content").getAsString().replace("&lt;", "<").replace("&gt;", ">");

            boolean isGroupMsg = (msg.get("FromUserName").getAsString() + msg.get("ToUserName").getAsString()).contains("@@");
            if (isGroupMsg) {
                workbench = MaJiangGroupMsg.getIstance(this);
            } else {
                workbench = MaJiangUserMsg.getIstance(this);
            }

            // 文本groupMessage
            log.info("msgType => " + msgType);
            if (conf.get("MSGTYPE_TEXT").equals(msgType)) {
                // 地理位置消息
                if (content.contains("pictype=location")) {
                    workbench.msgtype_text_location(msg);
                } else if (content.contains("我通过了你的朋友验证请求")) {
                    workbench.msgtype_verifymsg(msg);
                } else {
                    workbench.msgtype_text(msg);
                }
            }
            //图片信息
            else if (conf.get("MSGTYPE_IMAGE").equals(msgType)) {
                workbench.msgtype_image(msg);
            }
            //好友验证消息
            else if (conf.get("MSGTYPE_VERIFYMSG").equals(msgType)) {
                workbench.msgtype_verifymsg(msg);
            } else if (conf.get("MSGTYPE_APP").equals(msgType)) {
                workbench.msgtype_app(msg);
            }
            //系统信息
            else if (conf.get("MSGTYPE_SYS").equals(msgType)) {
                workbench.msgtype_sys(msg);
            } else if (conf.get("MSGTYPE_STATUSNOTIFY").equals(msgType)) {
                log.info(Const.LOG_MSG_NOTIFY_PHONE);
                return;
            }

        }
    }


    private void show_msg(UserMessage userMessage) {

        Map<String, Object> src = null;
        Map<String, Object> dst = null;
        Map<String, String> group = null;
        JsonObject msg = userMessage.getRawMsg();

        String content = msg.get("Content").getAsString();
        content = content.replace("&lt;", "<").replace("&gt;", ">");

        String msg_id = msg.get("MsgId").getAsString();

        // 接收到来自群的消息
        if (msg.get("FromUserName").getAsString().substring(2).equals("@@")) {
            String groupId = msg.get("FromUserName").getAsString();
            group = this.getGroupById(groupId);
            if (content.contains(":<br/>")) {
                String u_id = content.split(":<br/>")[0];
                src = new HashMap<String, Object>(this.getGroupUserById(u_id, groupId));
                dst = Utils.createMap("ShowName", "GROUP");
            } else {
                String u_id = msg.get("ToUserName").getAsString();
                src = new HashMap<String, Object>(Utils.createMap("ShowName", "SYSTEM"));
                dst = new HashMap<String, Object>(getGroupUserById(u_id, groupId));
            }
        } else {
            // 非群聊消息
            src = new HashMap<String, Object>(this.getUserById(msg.get("FromUserName").getAsString()));
            dst = new HashMap<String, Object>(this.getUserById(msg.get("ToUserName").getAsString()));
        }
        if (null != group) {
            log.info("{} |{}| {} -> {}: {}\n", msg.get("FromUserName").getAsString(), group.get("ShowName"),
                    dst.get("ShowName"), userMessage.getLog());
        } else {
            log.info("{} {} -> {}: {}\n", msg_id, src.get("ShowName"),
                    dst.get("ShowName"), userMessage.getLog());
        }
    }


    private GroupMessage make_group_msg(UserMessage userMessage) {
        log.debug("make group message");
        GroupMessage groupMessage = new GroupMessage(this);
        groupMessage.setRawMsg(userMessage.getRawMsg());
        groupMessage.setMsgId(userMessage.getRawMsg().get("MsgId").getAsString());
        groupMessage.setFromUserName(userMessage.getRawMsg().get("FromUserName").getAsString());
        groupMessage.setToUserName(userMessage.getRawMsg().get("ToUserName").getAsString());
        groupMessage.setMsgType(userMessage.getRawMsg().get("MsgType").getAsString());
        groupMessage.setText(userMessage.getText());

        String content = userMessage.getRawMsg().get("Content").getAsString().replace("&lt;", "<")
                .replace("&gt;", ">");

        Map<String, String> group = null, src = null;

        if (groupMessage.getFromUserName().startsWith("@@")) {
            //接收到来自群的消息
            String g_id = groupMessage.getFromUserName();
            groupMessage.setGroupId(g_id);
            group = this.getGroupById(g_id);
            if (content.contains(":<br/>")) {
                String u_id = content.split(":<br/>")[0];
                src = getGroupUserById(u_id, g_id);
            }
        } else if (groupMessage.getToUserName().startsWith("@@")) {
            // 自己发给群的消息
            String g_id = groupMessage.getToUserName();
            groupMessage.setGroupId(g_id);
            String u_id = groupMessage.getFromUserName();
            src = this.getGroupUserById(u_id, g_id);
            group = this.getGroupById(g_id);
        }

        if (null != src) {
            groupMessage.setUser_attrstatus(src.get("AttrStatus"));
            groupMessage.setUser_display_name(src.get("DisplayName"));
            groupMessage.setUser_nickname(src.get("NickName"));
        }
        if (null != group) {
            groupMessage.setGroup_count(group.get("MemberCount"));
            groupMessage.setGroup_owner_uin(group.get("OwnerUin"));
            groupMessage.setGroup_name(group.get("ShowName"));
        }
        groupMessage.setTimestamp(userMessage.getRawMsg().get("CreateTime").getAsString());

        return groupMessage;
    }
}
