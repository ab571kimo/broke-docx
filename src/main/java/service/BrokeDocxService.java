package service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

public class BrokeDocxService {
    static String partStr = "^第[零一二三四五六七八九十]{1,3}編";
    static String chapterStr = "^第[零一二三四五六七八九十]{1,3}章";
    static String sectionStr = "^第[零一二三四五六七八九十]{1,3}節";
    static String articleStr = "^第[0-9|-]{1,6}條";

    static Pattern partReg = Pattern.compile(partStr);
    static Pattern chapterReg = Pattern.compile(chapterStr);
    static Pattern sectionReg = Pattern.compile(sectionStr);
    static Pattern articleReg = Pattern.compile(articleStr);

    //條文啟動開關
    boolean on = false;

    public String breakDocx(InputStream stream) {

        List<Map<String, String>> rtnList = new ArrayList<>();

        try (XWPFDocument document = new XWPFDocument(stream)) {

            String part = null;
            String partName = null;

            String chapter = null;
            String chapterName = null;

            String section = null;
            String sectionName = null;

            String article = null;
            String articleName = null;

            StringBuilder sb = new StringBuilder();

            List<XWPFParagraph> paragraphList = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphList) {
                String text = paragraph.getText();

                if (partReg.matcher(text).find()) {
                    putMap(rtnList, part, chapter, section, article, sb);
                    // 第O編
                    partName = text.replaceAll(partStr, "");
                    part = text.replace(partName, "");
                } else if (chapterReg.matcher(text).find()) {
                    putMap(rtnList, part, chapter, section, article, sb);
                    // 第O章
                    chapterName = text.replaceAll(chapterStr, "");
                    chapter = text.replace(chapterName, "");
                } else if (sectionReg.matcher(text).find()) {
                    putMap(rtnList, part, chapter, section, article, sb);
                    // 第O節
                    sectionName = text.replaceAll(sectionStr, "");
                    section = text.replace(sectionName, "");
                } else if (articleReg.matcher(text).find()) {
                    putMap(rtnList, part, chapter, section, article, sb);
                    // 第O條
                    articleName = text.replaceAll(articleStr, "");
                    article = text.replace(articleName, "");
                    on = true;
                } else if (StringUtils.isNoneBlank(text) && on) {
                    // 條文內容，條文開始計算才寫入
                    sb.append(text).append("\n");
                }
            }
            putMap(rtnList, part, chapter, section, article, sb);

            return formatCsv(rtnList);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void putMap(List<Map<String, String>> rtnList, String part, String chapter, String section,
                        String article, StringBuilder sb) {
        if (on) {
            // 空值 結算條文
            Map<String, String> map = new HashMap<>();
            map.put("編", part);
            map.put("章", chapter);
            map.put("節", section);
            map.put("條", article);
            map.put("內文", sb.toString());
            sb.setLength(0);
            rtnList.add(map);
        }
        on = false;
    }

    private String formatCsv(List<Map<String, String>> rtnList) {

        StringBuilder sb = new StringBuilder();
        sb.append("編").append(",章").append(",節").append(",條").append(",內文").append("\n");
        for (Map<String, String> map : rtnList) {
            String part = map.get("編");
            String chapter = map.get("章");
            String section = map.get("節");
            String article = map.get("條");
            String context = map.get("內文");
            sb.append(part).append(',')
                    .append(chapter).append(',')
                    .append(section).append(',')
                    .append(article).append(',')
                    .append(context);
        }
        return sb.toString();
    }
}
