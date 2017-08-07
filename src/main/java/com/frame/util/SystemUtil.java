package com.frame.util;

import java.text.SimpleDateFormat;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystemUtil {


    public static int getRandom(int start, int end) {
        if (start > end || start < 0 || end < 0) {
            return -1;
        }
        return (int) (Math.random() * (end - start + 1)) + start;
    }

    public static String randomUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * yyyy-MM-dd HH:ss:mm
     *
     * @return
     */
    public static SimpleDateFormat sdfDateTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    public static SimpleDateFormat sdfDate() {
        return new SimpleDateFormat("yyyy-MM-dd");
    }


    public static String removeEmoji(String string) {

        Pattern p = Pattern.compile("<span class=\"(?:(?!>).|\\n)*?\"></span>");
        Matcher m = p.matcher(string);
        return m.replaceAll("");
    }

    public static void main(String[] args) {

        for (int i = 0; i < 6; i++) {
            System.out.println(SystemUtil.getRandom(100000, 999999));
        }
    }
}
