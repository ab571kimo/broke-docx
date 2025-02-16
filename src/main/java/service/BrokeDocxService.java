package service;

import java.io.IOException;
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


    public List<Map<String, String>> brokeDocx(InputStream stream) throws IOException {
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
        List<Map<String, String>> rtnList = new ArrayList<>();

        XWPFDocument document = new XWPFDocument(stream);

        String partName = "";
        String partMemo = "";

        String chapterName = "";
        String chapterMemo = "";

        String sectionName = "";
        String sectionMemo = "";

        String articleName = "";
        String articleMemo = "";

        StringBuilder sb = new StringBuilder();

        List<XWPFParagraph> paragraphList = document.getParagraphs();
        for (XWPFParagraph paragraph : paragraphList) {
            String text = paragraph.getText();

            if (partReg.matcher(text).find()) {
                putMap(rtnList, partName, partMemo, chapterName, chapterMemo, sectionName, sectionMemo, articleName, articleMemo, sb, on);
                on = false;
                // 第O編
                partMemo = text.replaceAll(partStr, "");
                partName = text.replace(partMemo, "");
            } else if (chapterReg.matcher(text).find()) {
                putMap(rtnList, partName, partMemo, chapterName, chapterMemo, sectionName, sectionMemo, articleName, articleMemo, sb, on);
                on = false;
                // 第O章
                chapterMemo = text.replaceAll(chapterStr, "");
                chapterName = text.replace(chapterMemo, "");
            } else if (sectionReg.matcher(text).find()) {
                putMap(rtnList, partName, partMemo, chapterName, chapterMemo, sectionName, sectionMemo, articleName, articleMemo, sb, on);
                on = false;
                // 第O節
                sectionMemo = text.replaceAll(sectionStr, "");
                sectionName = text.replace(sectionMemo, "");
            } else if (articleReg.matcher(text).find()) {
                putMap(rtnList, partName, partMemo, chapterName, chapterMemo, sectionName, sectionMemo, articleName, articleMemo, sb, on);
                // 第O條
                articleMemo = text.replaceAll(articleStr, "");
                articleName = text.replace(articleMemo, "");
                on = true;
            } else if (StringUtils.isNoneBlank(text) && on) {
                // 條文內容，條文開始計算才寫入
                sb.append(text).append("\n");
            }
        }
        putMap(rtnList, partName, partMemo, chapterName, chapterMemo, sectionName, sectionMemo, articleName, articleMemo, sb, on);

        int i = 1;
        for (Map<String, String> map : rtnList) {
            map.put("SER_NO", Integer.toString(i));
            i++;
            //System.out.println(map);
        }

        return rtnList;
    }

    private void putMap(List<Map<String, String>> rtnList,
                        String part_name,
                        String part_memo,
                        String chapter_name,
                        String chapter_memo,
                        String section_name,
                        String section_memo,
                        String article_name,
                        String article_memo,
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
        sb.setLength(0);
        rtnList.add(map);

    }

}
