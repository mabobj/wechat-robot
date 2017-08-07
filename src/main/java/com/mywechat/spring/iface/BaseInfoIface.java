package com.mywechat.spring.iface;

import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;

public interface BaseInfoIface {


    public void lastLogin(Map user);

    /**
     * 判断是否存在
     *
     * @param key
     * @return
     */
    public boolean existsSessionObject(String key);

    /**
     * 保存群信息
     *
     * @param map
     * @return
     */
    public String saveGroupInfo(Map map);


    /**
     * 保存用户信息
     *
     * @param userName
     * @param map
     * @return
     */
    public Map saveUserInfo(String userName, Map map);

    public Map getUserInfo(String user_key);
    /**
     * 获得群信息
     *
     * @param id
     */
    public JsonObject getSessionObject(String id);

    /**
     * 获得群列表
     *
     * @return
     */
    public List sessionGroupList();




    /**
     * 保存对话日志
     * @param id
     * @param user_key
     */
    public void saveDialogueLog(String id, String user_key);


    /**
     * 添加完好友，单未绑定游戏ID的用户，补发信息
     *
     * @return
     */
    public List<java.util.Map<String, Object>> getAgainMsgUserList1();

    /**
     * 更新游戏ID
     * @param user_key
     * @param game_id
     */
    public String updateGameId(String user_key, String game_id);
}
