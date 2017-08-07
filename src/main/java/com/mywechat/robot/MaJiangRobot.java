package com.mywechat.robot;

import com.mywechat.api.WechatApi;
import com.mywechat.workbench.MaJiangGroupMsg;

public class MaJiangRobot {


    private static volatile MaJiangRobot instance;

    public static MaJiangRobot getIstance() {
        if (instance == null) {
            synchronized (MaJiangGroupMsg.class) {
                if (instance == null) {
                    instance = new MaJiangRobot();
                }
            }
        }
        return instance;
    }





}
