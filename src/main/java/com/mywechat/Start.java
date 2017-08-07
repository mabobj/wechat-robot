package com.mywechat;

import com.mywechat.center.MaJiangCenter;
import com.mywechat.constant.DialogueConstant;
import com.mywechat.workbench.MaJiangUserMsg;
import io.github.biezhi.wechat.model.Environment;

public class Start {
    public static void main(String[] args) {
        System.setProperty("https.protocols", "TLSv1");
        System.setProperty("jsse.enableSNIExtension", "false");

        Environment environment = Environment.of("classpath:Config.properties");

        MaJiangCenter majiang = new MaJiangCenter(environment);
        majiang.start();

        //System.out.println(DialogueConstant.MSG001.toString());
    }

}
