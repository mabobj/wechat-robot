package com.mywechat.spring.service;

import com.frame.base.BaseService;
import com.frame.util.RedisTools;
import com.frame.util.SystemUtil;
import com.google.gson.*;
import com.mywechat.spring.iface.BaseInfoIface;
import redis.clients.jedis.Jedis;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Documented;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseInfoService extends BaseService implements BaseInfoIface {


    @Override
    public void lastLogin(Map user) {
        Map map = new HashMap();
        map.put("Uin", user.get("Uin"));
        map.put("NickName", user.get("NickName"));
        map.put("HeadImgUrl", user.get("HeadImgUrl"));
        map.put("Sex", user.get("Sex"));
        map.put("last_login_date", new Date());
        List<java.util.Map<String, Object>> list = queryForList("SELECT * FROM robot_user WHERE Uin = ?", user.get("Uin"));
        if (list != null && list.size() > 0) {
            map.put("id", list.get(0).get("id"));
        }

        saveOrUpdate(map, "robot_user");
    }

    @Override
    public boolean existsSessionObject(String key) {
        boolean b = false;
        Jedis r = RedisTools.getJedis();
        b = r.exists(key);
        RedisTools.returnResource(r);
        return b;
    }

    @Override
    public String saveGroupInfo(Map map) {
        /*Jedis r = RedisTools.getJedis();
        r.hmset(map.get("UserName").toString(), map);
        r.expire(map.get("UserName").toString(), 60 * 60 * 24);
        RedisTools.returnResource(r);*/

        String uuKey = SystemUtil.randomUUID().substring(26, 32);

        Map tmap = new HashMap();
        tmap.put("robot_uin", map.get("RobotUin").toString());
        tmap.put("group_key", uuKey);
        tmap.put("nick_name", map.get("NickName").toString());

        saveBackId(tmap, "group_info");

        return uuKey;
    }

    @Override
    public Map saveUserInfo(String userName, Map map) {
        /*Jedis r = RedisTools.getJedis();
        r.hmset(userName, map);
        r.expire(userName, 60 * 60 * 24);
        RedisTools.returnResource(r);*/

        try {
            saveBackId(map, "user_info");
        } catch (Exception e) {
            log.error(e.toString());
            Map tmap = queryForMap("SELECT * FROM user_info u WHERE u.nick_name = ? AND u.province = ? AND u.city = ?",
                    map.get("nick_name"),
                    map.get("province"),
                    map.get("city")
            );
            map.put("key_word", tmap.get("key_word"));

            String rName = map.get("nick_name").toString();
            Map joMap = new HashMap();
            joMap.put("u", tmap.get("user_key"));
            joMap.put("fu", tmap.get("father_key"));
            rName = rName + "#" + new Gson().toJson(joMap).toString();
            tmap.put("remark_name", rName);

            saveOrUpdate(tmap, "user_info");
            return tmap;
        }
        return map;
    }

    @Override
    public Map getUserInfo(String user_key) {
        List<Map<String, Object>> list = queryForList("SELECT * FROM user_info u WHERE u.user_key = ?", user_key);
        if (list.size() > 0) {
            /*if (list.get(0).get("game_id") != null && !list.get(0).get("game_id").toString().equals("")) {
                return list.get(0);
            }*/
            return list.get(0);
        }
        return null;
    }
    /*@Override
    public String saveUserInfo(String groupKey, Map map) {
        Jedis r = RedisTools.getJedis();
        r.hmset(map.get("UserName").toString(), map);
        r.expire(map.get("UserName").toString(), 60 * 60 * 24);
        RedisTools.returnResource(r);

        String uuKey = SystemUtil.randomUUID().substring(26, 32);

        Map tmap = new HashMap();
        tmap.put("user_key", uuKey);
        tmap.put("nick_name", map.get("NickName"));
        tmap.put("remark_name", map.get("RemarkName"));
        tmap.put("user_name", map.get("UserName"));
        tmap.put("user_uin", map.get("KeyWord"));
        tmap.put("head_img_url", map.get("HeadImgUrl"));
        tmap.put("sex", map.get("Sex"));
        tmap.put("province", map.get("Province"));
        tmap.put("city", map.get("City"));
        tmap.put("signature", map.get("Signature"));
        tmap.put("robot_user_id", "0");
        saveBackId(map, "user_info");

        if (groupKey != null && !groupKey.equals("")) {
            tmap = new HashMap();
            tmap.put("group_key", groupKey);
            tmap.put("user_key", uuKey);
            saveBackId(tmap, "group_user");
        }

        return uuKey;
    }*/

    @Override
    public JsonObject getSessionObject(String id) {
        return null;
    }

    @Override
    public List sessionGroupList() {
        return null;
    }


    @Override
    public void saveDialogueLog(String dialogue_key, String user_key) {
        Map map = new HashMap();
        map.put("dialogue_key", dialogue_key);
        map.put("user_key", user_key);
        map.put("create_time", new Date());

        saveBackId(map, "dialogue_log");

    }

    @Override
    public List<java.util.Map<String, Object>> getAgainMsgUserList1() {
        String sql = "SELECT u.*,dl.dialogue_key,dl.create_time FROM (\n" +
                "SELECT * FROM dialogue_log d WHERE d.id in (\n" +
                "\tSELECT MAX(id) FROM dialogue_log GROUP BY user_key\n" +
                ") \n" +
                "AND (d.dialogue_key = 'MSG001'\n" +
                "\tOR (\n" +
                "\t\t(\n" +
                "\t\t\td.dialogue_key = 'MSG002' \n" +
                "\t\t\tOR d.dialogue_key = 'MSG002_2' \n" +
                "\t\t\tOR d.dialogue_key = 'MSG003'\n" +
                "\t\t) \n" +
                "\t\tAND TIMESTAMPDIFF(HOUR ,d.create_time,NOW()) >0\n" +
                "\t)\n" +
                ")) dl INNER JOIN user_info u\n" +
                "ON u.user_key = dl.user_key";
        return queryForList(sql);
    }

    @Override
    public String updateGameId(String user_key, String game_id) {
        List<Map<String, Object>> list = queryForList("SELECT * FROM user_info u WHERE u.user_key = ?", user_key);
        if (list.size() > 0) {
            if (list.get(0).get("game_id") != null && !list.get(0).get("game_id").toString().equals("")) {
                return list.get(0).get("game_id").toString();
            }
        }
        update("UPDATE user_info SET game_id = ? WHERE user_key = ?", game_id, user_key);
        return null;
    }


}
