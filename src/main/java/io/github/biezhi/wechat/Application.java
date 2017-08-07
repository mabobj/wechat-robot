package io.github.biezhi.wechat;

import io.github.biezhi.wechat.model.Environment;
import io.github.biezhi.wechat.ui.StartUI;

/**
 * wechat启动程序
 */
public class Application {

    public static void main(String[] args) throws Exception {
        System.setProperty("https.protocols", "TLSv1");
        System.setProperty("jsse.enableSNIExtension", "false");

        Environment environment = Environment.of("classpath:config.properties.bak");

        StartUI startUI = new StartUI(environment);

//        startUI.setMsgHandle(new MoliRobot(environment));
        //startUI.setMsgHandle(new MaJiangCenter());

        startUI.start();
    }

}