package service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLvl;

public class BrokeDocxService {

    public List<Map> brokeDocx(InputStream stream) throws Exception {
        String partStr = "^第[零一二三四五六七八九十]{1,3}編";
        String chapterStr = "^第[零一二三四五六七八九十]{1,3}章";
        String sectionStr = "^第[零一二三四五六七八九十]{1,3}節";
        String articleStr = "^第[0-9|\\-|零一二三四五六七八九十]{1,6}條";

        Pattern partReg = Pattern.compile(partStr);
        Pattern chapterReg = Pattern.compile(chapterStr);
        Pattern sectionReg = Pattern.compile(sectionStr);
        Pattern articleReg = Pattern.compile(articleStr);

        //條文啟動開關
        boolean on = false;
        List<Map> rtnList = new ArrayList<>();

        XWPFDocument document = new XWPFDocument(stream);

        Integer part = 0;
        String partName = "";
        String partMemo = "";

        Integer chapter = 0;
        String chapterName = "";
        String chapterMemo = "";

        Integer section = 0;
        String sectionName = "";
        String sectionMemo = "";

        Integer article = 0;
        String articleName = "";
        String articleMemo = "";

        MultiKeyMap<BigInteger, Integer> numMap = new MultiKeyMap();

        StringBuilder sb = new StringBuilder();

        List<XWPFParagraph> paragraphList = document.getParagraphs();
        XWPFNumbering numbering = document.getNumbering();

        TestNumbering testNumbering = new TestNumbering();

        for (XWPFParagraph paragraph : paragraphList) {

            String numText = testNumbering.getNumLevel(paragraph,numbering);

            String text = paragraph.getText();
            if (StringUtils.isNotBlank(numText)) {
                text = numText + ' ' + text;
            }

            if (partReg.matcher(text).find()) {
                putMap(rtnList, partName, partMemo, chapterName, chapterMemo, sectionName, sectionMemo, articleName, articleMemo, part.toString(), chapter.toString(), section.toString(), article.toString(), sb, on);
                on = false;
                // 第O編
                partMemo = text.replaceAll(partStr, "");
                partName = text.replace(partMemo, "");
                part++;
            } else if (chapterReg.matcher(text).find()) {
                putMap(rtnList, partName, partMemo, chapterName, chapterMemo, sectionName, sectionMemo, articleName, articleMemo, part.toString(), chapter.toString(), section.toString(), article.toString(), sb, on);
                on = false;
                // 第O章
                chapterMemo = text.replaceAll(chapterStr, "");
                chapterName = text.replace(chapterMemo, "");
                chapter++;
            } else if (sectionReg.matcher(text).find()) {
                putMap(rtnList, partName, partMemo, chapterName, chapterMemo, sectionName, sectionMemo, articleName, articleMemo, part.toString(), chapter.toString(), section.toString(), article.toString(), sb, on);
                on = false;
                // 第O節
                sectionMemo = text.replaceAll(sectionStr, "");
                sectionName = text.replace(sectionMemo, "");
                section++;
            } else if (articleReg.matcher(text).find()) {
                putMap(rtnList, partName, partMemo, chapterName, chapterMemo, sectionName, sectionMemo, articleName, articleMemo, part.toString(), chapter.toString(), section.toString(), article.toString(), sb, on);
                // 第O條
                articleMemo = text.replaceAll(articleStr, "");
                articleName = text.replace(articleMemo, "");
                article++;
                on = true;
            } else if (StringUtils.isNoneBlank(text) && on) {
                // 條文內容，條文開始計算才寫入
                sb.append(text).append("\n");
            }
        }
        putMap(rtnList, partName, partMemo, chapterName, chapterMemo, sectionName, sectionMemo, articleName, articleMemo, part.toString(), chapter.toString(), section.toString(), article.toString(), sb, on);

        int i = 1;
        for (Map<String, String> map : rtnList) {
            map.put("SER_NO", Integer.toString(i));
            i++;
            //System.out.println(map);
        }

        return rtnList;
    }

    private void putMap(List<Map> rtnList,
                        String part_name,
                        String part_memo,
                        String chapter_name,
                        String chapter_memo,
                        String section_name,
                        String section_memo,
                        String article_name,
                        String article_memo,
                        String part,
                        String chapter,
                        String section,
                        String article,
                        StringBuilder sb, boolean on) {

        if (!on) {
            return;
        }


        // 空值 結算條文
        Map<String, String> map = new HashMap<>();

        map.put("PART_NAME", part_name.trim());
        map.put("CHAPTER_NAME", chapter_name.trim());
        map.put("SECTION_NAME", section_name.trim());
        map.put("ARTICLE_NAME", article_name.trim());
        map.put("PART_MEMO", part_memo.trim());
        map.put("CHAPTER_MEMO", chapter_memo.trim());
        map.put("SECTION_MEMO", section_memo.trim());
        map.put("ARTICLE_MEMO", article_memo.trim());
        map.put("CONTENT", sb.toString().trim());
        map.put("PART_NO", part);
        map.put("CHAPTER_NO", chapter);
        map.put("SECTION_NO", section);
        map.put("ARTICLE_NO", article);

        sb.setLength(0);
        rtnList.add(map);

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


    public List<Map<String, String>> compareRKB_INR(List<Map<String, String>> ORI_D620List, List<Map<String, String>> NEW_D620List) {

        //NEW_D620來源可從檔案或DTXRD621，ORI_D620為資料庫DTXRD620
//僅比對新舊文字，若NEW_D620來源為資料庫則有UUID，若為檔案拆條則無
// ORI_D620比對NEW_D620，找到相同的文字合併為同一列，不同文字則新增一列至NEW_D620
// ORI_D620內的ORI_CONTENT_UUID，合併時不放入

        if (ORI_D620List == null || ORI_D620List.isEmpty()) {
            return NEW_D620List;
        }

//將ORI_D620List[i].CONTENT逐筆取出，組出新Map
        Map<String, Map<String, String>> oriMap = new LinkedHashMap<>();
        for (Map<String, String> map : ORI_D620List) {
            oriMap.put(map.get("CONTENT"), map);
        }

//逐筆讀NEW_D620List，確認NEW_D620List[i].CONTENT是否出現在oriMap
        for (Map<String, String> map : NEW_D620List) {
            String key = map.get("CONTENT");
            if (oriMap.containsKey(key)) {
                //有舊條文關聯
                map.put("ORI_PART_NAME", oriMap.get(key).get("PART_NAME"));
                map.put("ORI_CHAPTER_NAME", oriMap.get(key).get("CHAPTER_NAME"));
                map.put("ORI_SECTION_NAME", oriMap.get(key).get("SECTION_NAME"));
                map.put("ORI_ARTICLE_NAME", oriMap.get(key).get("ARTICLE_NAME"));
                map.put("ORI_CONTENT ", oriMap.get(key).get("CONTENT"));
                map.put("ORI_CONTENT_UUID", oriMap.get(key).get("CONTENT_UUID"));
//繼承舊條文UUID
                map.put("CONTENT_UUID", oriMap.get(key).get("CONTENT_UUID"));
                oriMap.remove(key); //從oriMap移除處理過的key
            }
        }
//用oriMap.keySet讀出剩餘的value，並放入NEW_D620List
        int newListSize = NEW_D620List.size();
        int i = 0;

        for (String key : oriMap.keySet()) {
//把條文標記為舊條文
            Map<String, String> map = oriMap.get(key);
            map.put("ORI_PART_NAME", map.get("PART_NAME"));
            map.put("ORI_CHAPTER_NAME", map.get("CHAPTER_NAME"));
            map.put("ORI_SECTION_NAME", map.get("SECTION_NAME"));
            map.put("ORI_ARTICLE_NAME", map.get("ARTICLE_NAME"));
            map.put("ORI_CONTENT_UUID", map.get("CONTENT_UUID"));
            map.put("ORI_CONTENT", map.get("CONTENT"));
            map.remove("PART_NAME");
            map.remove("CHAPTER_NAME");
            map.remove("SECTION_NAME");
            map.remove("ARTICLE_NAME");
            map.remove("CONTENT_UUID");
            map.remove("CONTENT");

            //將map插入NEW_D620List
            int serNo = Integer.parseInt(map.get("SER_NO"));

            if (serNo > newListSize) {
                NEW_D620List.add(map);
            } else {
                NEW_D620List.add(serNo + i, map);
                i++;
            }
        }

        return NEW_D620List;
    }
}
