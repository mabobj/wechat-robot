package com.mywechat.workbench.base;

import com.frame.util.SpringGetBean;
import com.google.gson.*;
import com.mywechat.api.WechatApi;
import com.mywechat.spring.iface.BaseInfoIface;
import com.mywechat.spring.service.BaseInfoService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseWorkbench {
    protected final Logger log = LogManager.getLogger(this.getClass().getName());

    protected WechatApi api;

    public void wxSync(JsonObject dic) {
        //增加多线程处理

        //保存使用者微信信息
        /*BaseInfoIface baseInfo = SpringGetBean.getBean(BaseInfoService.class, BaseInfoIface.class);

        JsonArray msgs = dic.getAsJsonArray("AddMsgList");


        for (JsonElement element : msgs) {
            JsonObject msg = element.getAsJsonObject();
            log.debug("wxSync => {}", msg.toString());

            if (!baseInfo.existsSessionObject(msg.get("FromUserName").getAsString())) {
                JsonArray ja = api.batchGetContact(msg.get("FromUserName").getAsString());
                log.debug("wxSync FromUserName batchGetContact => {}", ja.toString());
                setSessionObject(ja, msg.get("FromUserName").getAsString(), baseInfo);

            }

            if (!baseInfo.existsSessionObject(msg.get("ToUserName").getAsString())) {
                JsonArray ja = api.batchGetContact(msg.get("ToUserName").getAsString());
                log.debug("wxSync ToUserName batchGetContact => {}", ja.toString());
                setSessionObject(ja, msg.get("ToUserName").getAsString(), baseInfo);
            }


        }*/

    }

    private void setSessionObject(JsonArray ja, String UserName, BaseInfoIface baseInfo) {
        Map map;

        for (JsonElement jsonElement : ja) {
            JsonObject jo = jsonElement.getAsJsonObject();

            map = new HashMap();
            if (UserName.contains("@@")) {
                //群信息

                map.put("RobotUin", api.getSession().getUin());
                map.put("UserName", jo.get("UserName").getAsString());
                map.put("NickName", jo.get("NickName").getAsString());
                map.put("HeadImgUrl", jo.get("HeadImgUrl").getAsString());
                map.put("MemberCount", jo.get("MemberCount").getAsString());
                map.put("MemberList", jo.get("MemberList").toString());
                //保存群信息
                String groupKey = baseInfo.saveGroupInfo(map);

                //获取群成员列表
                JsonArray jp = new JsonParser().parse(jo.get("MemberList").toString()).getAsJsonArray();


                //分页拉取群成员
                int page = jp.size() / 50;
                for (int i = 0; i < page + 1; i++) {
                    int no = jp.size() - (i * 50);
                    if (no > 50) {
                        no = 50;
                    }

                    List userNameList = new ArrayList();
                    for (int j = (i * 50); j < ((i * 50) + no); j++) {
                        JsonObject ujo = jp.get(j).getAsJsonObject();
                        userNameList.add(ujo.get("UserName").getAsString());
                    }

                    JsonArray jaUser = api.batchGetContact(userNameList, UserName);
                    log.debug("jaUser[{}] => {}", i, jaUser.toString());

                    //保存群成员
                    for (JsonElement element : jaUser) {
                        JsonObject joo = element.getAsJsonObject();

                        map.put("UserName", joo.get("UserName").getAsString());
                        map.put("RemarkName", jo.get("RemarkName").getAsString());
                        map.put("NickName", joo.get("NickName").getAsString());
                        map.put("HeadImgUrl", joo.get("HeadImgUrl").getAsString());
                        map.put("Sex", joo.get("Sex").getAsString());
                        map.put("Signature", joo.get("Signature").getAsString());
                        map.put("Province", joo.get("Province").getAsString());
                        map.put("City", joo.get("City").getAsString());
                        map.put("KeyWord", joo.get("KeyWord").getAsString());

                        String userKey = null;
                        //String userKey = baseInfo.saveUserInfo(groupKey, map);

                        //修改群成员昵称
                        if (jo.get("NickName").getAsString().contains("一元亮")) {

                            String rName = jo.get("RemarkName").getAsString();

                            if (rName.equals("") || rName.length() == 0) {//昵称为空
                                rName = joo.get("NickName").getAsString();
                                rName = rName.replace("#", "");

                                Map tmap = new HashMap();
                                tmap.put("u", userKey);
                                List tlist = new ArrayList();
                                tlist.add(groupKey);
                                tmap.put("g", tlist);

                                rName = rName + "#" + new Gson().toJson(tmap).toString();
                            } else {
                                JsonObject rJo = new JsonParser().parse(rName.split("#")[1]).getAsJsonObject();
                                JsonArray rGroupList = rJo.get("g").getAsJsonArray();
                                boolean b = false;
                                for (JsonElement ele : rGroupList) {
                                    if (ele.getAsString().equals(groupKey)) {
                                        b = true;
                                        break;
                                    }
                                }
                                if (!b) {
                                    rGroupList.add(groupKey);
                                }
                                rJo.add("g", rGroupList);

                                rName = rName.split("#")[0] + "#" + rJo.toString();
                            }

                            api.editRemarks(joo.get("UserName").getAsString(), rName);
                        }
                    }
                }

            } else {
                map.put("UserName", jo.get("UserName").getAsString());
                map.put("RemarkName", jo.get("RemarkName").getAsString());
                map.put("NickName", jo.get("NickName").getAsString());
                map.put("HeadImgUrl", jo.get("HeadImgUrl").getAsString());
                map.put("Sex", jo.get("Sex").getAsString());
                map.put("Signature", jo.get("Signature").getAsString());
                map.put("Province", jo.get("Province").getAsString());
                map.put("City", jo.get("City").getAsString());
                map.put("KeyWord", jo.get("KeyWord").getAsString());

                //baseInfo.saveUserInfo("", map);
            }

        }
    }
}
