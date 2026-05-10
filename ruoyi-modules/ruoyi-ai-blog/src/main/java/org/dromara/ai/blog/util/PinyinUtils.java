package org.dromara.ai.blog.util;

import com.github.promeg.pinyinhelper.Pinyin;

/**
 * 拼音工具类
 *
 * @author KMatrix
 */
public class PinyinUtils {

    private PinyinUtils() {}

    public static String toPinyin(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Pinyin.isChinese(c)) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(Pinyin.toPinyin(c).toLowerCase());
            } else if (!Character.isWhitespace(c)) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }
}
