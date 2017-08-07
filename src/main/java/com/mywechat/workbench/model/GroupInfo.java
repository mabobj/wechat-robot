package com.mywechat.workbench.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class GroupInfo {

    String userName;
    JsonArray MemberList;
    Date lastMsgDate;
    Map postMsgDate = new HashMap();
    String adminUserKey;
    String adminUserName;

    public String getAdminUserName() {
        return adminUserName;
    }

    public void setAdminUserName(String adminUserName) {
        this.adminUserName = adminUserName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public JsonArray getMemberList() {
        return MemberList;
    }

    public void setMemberList(JsonArray memberList) {
        MemberList = memberList;

        try {
            List tList = new ArrayList();

            for (JsonElement element : memberList) {
                JsonObject jo = element.getAsJsonObject();
                String[] nn = jo.get("NickName").getAsString().split("#");
                if (nn.length > 1) {
                    JsonObject ukObj = new JsonParser().parse(nn[1]).getAsJsonObject();
                    if (ukObj.get("fu") != null) {
                        tList.add(ukObj.get("fu").getAsString());
                    }
                }
            }

            Set<String> uniqueSet = new HashSet(tList);
            String[] sst = new String[2];
            sst[1] = "0";
            for (String temp : uniqueSet) {
                System.out.println(temp + ": " + Collections.frequency(tList, temp));
                if (Integer.valueOf(sst[1]) < Collections.frequency(tList, temp)) {
                    sst[0] = temp;
                    sst[1] = Collections.frequency(tList, temp) + "";
                }
            }

            setAdminUserKey(sst[0]);

            for (JsonElement element : memberList) {
                JsonObject jo = element.getAsJsonObject();
                String[] nn = jo.get("NickName").getAsString().split("#");
                if (nn.length > 1) {
                    JsonObject ukObj = new JsonParser().parse(nn[1]).getAsJsonObject();
                    if (ukObj.get("u") != null) {
                        if (ukObj.get("u").getAsString().equals(getAdminUserKey())) {
                            setAdminUserName(jo.get("UserName").getAsString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Date getLastMsgDate() {
        return lastMsgDate;
    }

    public void setLastMsgDate(Date lastMsgDate) {
        this.lastMsgDate = lastMsgDate;
    }

    public String getAdminUserKey() {
        return adminUserKey;
    }

    public void setAdminUserKey(String adminUserKey) {
        this.adminUserKey = adminUserKey;
    }


    public Map getPostMsgDate() {
        return postMsgDate;
    }

    public void setPostMsgDate(Map postMsgDate) {
        this.postMsgDate = postMsgDate;
    }
}
