package service;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLvl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTNumLvl;

import java.io.FileInputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestNumbering {

    MultiKeyMap<BigInteger, Integer> numMap = new MultiKeyMap();

    public String getNumLevel(XWPFParagraph paragraph,XWPFNumbering numbering){

        if (paragraph.getNumID() != null && numbering != null) {
            //存在自動編碼
            BigInteger numID = paragraph.getNumID();
            BigInteger ilvl = paragraph.getNumIlvl();

            // 取得對應的 AbstractNum
            XWPFNum num = numbering.getNum(numID);
            if (num != null) {
                BigInteger abstractNumID = num.getCTNum().getAbstractNumId().getVal();
                XWPFAbstractNum abstractNum = numbering.getAbstractNum(abstractNumID);
                CTLvl lvl = abstractNum.getCTAbstractNum().getLvlArray(ilvl.intValue());

                // 編號格式字串，例如 "第%1章"
                String lvlText = lvl.getLvlText().getVal();
                String format = lvl.getNumFmt().getVal().toString();

                // 取得當前的計數器值，並加一
                Integer numVal = numMap.get(numID, ilvl);
                numVal = (numVal == null ? 0 : numVal) + 1;
                numMap.put(numID, ilvl, numVal);

                //確認階層
                Pattern pattern = Pattern.compile("%[1-9]");
                Matcher matcher = pattern.matcher(lvlText);

                String matched = "";
                if(matcher.find()) {
                    matched = matcher.group();
                }


                return lvlText.replace(matched, formatNumber(numVal, format));

            }

        }
        return "";
    }

    public static String formatNumber(int value, String numFmt) {
        return switch (numFmt) {
            case "decimal" -> String.valueOf(value);
            case "upperLetter" -> toAlphabet(value).toUpperCase();
            case "lowerLetter" -> toAlphabet(value).toLowerCase();
            case "upperRoman" -> toRoman(value).toUpperCase();
            case "lowerRoman" -> toRoman(value).toLowerCase();
            case "taiwaneseCountingThousand" -> toChineseNumeral(value, false);
            case "chineseLegalSimplified" -> "第" + toChineseNumeral(value, true) + "條";
            default -> String.valueOf(value); // fallback
        };
    }

    // 英文字母（支援超過26，例如 AA, AB）
    private static String toAlphabet(int num) {
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            num--; // make it 0-indexed
            sb.insert(0, (char) ('A' + (num % 26)));
            num /= 26;
        }
        return sb.toString();
    }

    // 羅馬數字
    private static String toRoman(int number) {
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] symbols = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

        StringBuilder roman = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (number >= values[i]) {
                number -= values[i];
                roman.append(symbols[i]);
            }
        }
        return roman.toString();
    }

    // 中文數字（簡單處理 1~99）
    private static String toChineseNumeral(int num, boolean useFormal) {
        String[] digits = useFormal
                ? new String[]{"〇", "壹", "貳", "參", "肆", "伍", "陸", "柒", "捌", "玖"}
                : new String[]{"零", "一", "二", "三", "四", "五", "六", "七", "八", "九"};

        String[] units = useFormal
                ? new String[]{"", "拾", "佰", "仟"}
                : new String[]{"", "十", "百", "千"};

        if (num == 0) return digits[0];
        StringBuilder sb = new StringBuilder();
        String numStr = String.valueOf(num);
        int len = numStr.length();
        for (int i = 0; i < len; i++) {
            int digit = numStr.charAt(i) - '0';
            if (digit != 0) {
                sb.append(digits[digit]).append(units[len - 1 - i]);
            } else {
                if (!sb.toString().endsWith(digits[0])) {
                    sb.append(digits[0]);
                }
            }
        }

        String result = sb.toString();
        // 修正中文十~十九開頭不要「一十」
        if (!useFormal && result.startsWith("一十")) {
            result = result.replaceFirst("一十", "十");
        }
        return result.replaceAll("零+$", ""); // 去除尾部多餘的零
    }

}
