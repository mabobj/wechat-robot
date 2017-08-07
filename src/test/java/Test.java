import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {

    public static void main(String[] args) {

// 表达式对象
        Pattern p = Pattern.compile("<span class=\"(?:(?!>).|\\n)*?\"></span>");

// 创建 Matcher 对象
        Matcher m = p.matcher("<span class=\"emoji emoji1f33a\"></span>薛佳佳·");

// 替换
        String newstring = m.replaceAll("");
        System.out.println(newstring);
    }
}
