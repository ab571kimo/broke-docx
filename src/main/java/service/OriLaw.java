package service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.apache.commons.collections4.MapUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Timestamp;
import java.util.*;

public class OriLaw {

    public void run() throws Exception {

        //每一份檔案代表一部法規，內含多版本
        File fileJ = new File("D:\\FL000565.json");

        JsonReader reader = new JsonReader(new FileReader(fileJ));

        List<Map<String, Object>> ori = new Gson().fromJson(reader, new TypeToken<List<Map<String, Object>>>() {
        }.getType());

        //從舊到新排序
        Collections.sort(ori, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> m1, Map<String, Object> m2) {
                Map map1 = MapUtils.getMap(m1, "Data");
                Map map2 = MapUtils.getMap(m2, "Data");

                String AmendDate1 = MapUtils.getString(map1, "AmendDate", "");
                String AmendDate2 = MapUtils.getString(map2, "AmendDate", "");
                // 先按 AmendDate 升序排列
                int result = AmendDate1.compareTo(AmendDate2);

                // 如果 AmendDate 相同，則按 SerialNo 升序排列
                if (result == 0) {
                    String SerialNo1 = MapUtils.getString(map1, "SerialNo", "");
                    String SerialNo2 = MapUtils.getString(map2, "SerialNo", "");
                    return SerialNo1.compareTo(SerialNo2);
                }
                return result;
            }
        });

        Timestamp now = new Timestamp(System.currentTimeMillis());

        List<Map> D700List = new ArrayList();
        List<Map> D701List = new ArrayList();
        List<Map> D710List = new ArrayList();
        List<Map> D711List = new ArrayList();
        List<Map> D720List = new ArrayList();
        List<Map> D721List = new ArrayList();
        List<Map> D730List = new ArrayList();
        List<Map> Z600List = new ArrayList();

        List<Map> oriLawList = new ArrayList();
        List<Map> newLawList = new ArrayList();


        for (Map<String, Object> oriMap : ori) {
            Map Data = MapUtils.getMap(oriMap, "Data");
            String RKB_EXT_NO = MapUtils.getString(Data, "LawID");

            //處理D700
            Map D700 = new LinkedHashMap();
            D700.put("COMP_ID", "00");
            D700.put("RKB_EXT_NO", RKB_EXT_NO);
            D700.put("EXT_NAME", Data.get("LawName"));

            Map<String, String> Level = MapUtils.getMap(Data, "Level");
            D700.put("LEVEL_ID", Level.get("ID"));
            D700.put("LEVEL_NAME", Level.get("Name"));

            D700.put("ANN_DATE", Data.get("AnnounceDate"));
            D700.put("UPDATE_DATE", Data.get("AmendDate"));

            Map<String, String> Valid = MapUtils.getMap(Data, "Valid");
            D700.put("VALID_DATE", Valid.get("ID"));
            D700.put("VALID_MEMO", Valid.get("Name"));

            Map<String, String> AmendTag = MapUtils.getMap(Data, "AmendTag");
            D700.put("AMENT_ID", AmendTag.get("ID"));
            D700.put("AMENT_NAME", AmendTag.get("Name"));
            D700.put("REPEAL_DATE", Data.get("RepealDate"));
            D700.put("REG_HISTORY", Data.get("Source"));
            D700.put("FOREWORD", Data.get("Foreword"));

            List<Map<String, String>> Unit = MapUtils.getObject(Data, "Unit", new ArrayList<>());
            D700.put("COMP_AUTH_ID", Unit.get(0).get("ID"));
            D700.put("COMP_AUTH_NAME", Unit.get(0).get("Name"));

            D700.put("REASONS", Data.get("LegislativeReasons"));

            Map<String, String> Category = MapUtils.getMap(Data, "Category");
            D700.put("CATEGORY_ID", Category.get("ID"));
            D700.put("CATEGORY_NAME", Category.get("Name"));
            D700.put("EDIT_TIME", Data.get("EditTime"));

            //LatestVersion 現行法規
            boolean LatestVersion = MapUtils.getBoolean(Data, "LatestVersion");
            String ACTIVE = LatestVersion ? "0" : "1";

            String AmendDate = MapUtils.getString(Data, "AmendDate");
            String SerialNo = MapUtils.getString(Data, "SerialNo");

            D700.put("ACTIVE", ACTIVE);
            D700.put("LST_PROC_ID", "XRR0_B071");
            D700.put("LST_PROC_NAME", "XRR0_B071");
            D700.put("LST_PROC_DIV_NO", "XRR0_B071");
            D700.put("LST_PROC_DIV_NAME", "XRR0_B071");
            D700.put("LST_PROC_TIME", now);

            String FLOW_NO = AmendDate + "000" + SerialNo;

            if (LatestVersion) {
                //現行法規
                D700.put("LST_FLOW_NO", FLOW_NO);
                D700List.add(D700);
            } else {
                //歷史法規
                D700.put("FLOW_NO", FLOW_NO);
                D701List.add(D700);
            }

            //處理法規附件
            List<Map> ConversionFile = MapUtils.getObject(Data, "ConversionFile", new ArrayList<>());
            List<Map> AttachmentFiles = MapUtils.getObject(Data, "AttachmentFiles", new ArrayList<>());

            int i = 0;
            for (Map map : ConversionFile) {
                Map D710 = new LinkedHashMap();
                D710.put("COMP_ID", "00");

                String FileID = MapUtils.getString(map, "FileID");
                String FileName = MapUtils.getString(map, "FileName");
                String FileExtension = MapUtils.getString(map, "FileExtension");

                String FullName = FileName + '.' + FileExtension;

                //取得FILE_ID
                //String FILE_ID = XR_Z0Z002().getFILE_ID(DATE.today());

                //將舊檔案移到指定位置 TODO
                //舊檔案位置 = ORI_FILE_PATH + FileName + '.' + FileExtension
                //新檔案位置 = FILE_PATH + FileID + '.' + FileExtension

                D710.put("RKB_EXT_NO", RKB_EXT_NO);
                D710.put("FILE_NAME", FullName);
                D710.put("FILE_TYPE", "00");
                D710.put("IS_CONTENT", map.get("isContentFile"));
                D710.put("FILE_URL", map.get("FileUrl"));
                D710.put("SER_NO", i);
                D710.put("SOURCE_ID", map.get("FileID"));
                D710.put("EDIT_TIME", map.get("EditTime"));
                D710.put("LST_PROC_ID", "XRR0_B071");
                D710.put("LST_PROC_NAME", "XRR0_B071");
                D710.put("LST_PROC_DIV_NO", "XRR0_B071");
                D710.put("LST_PROC_DIV_NAME", "XRR0_B071");
                D710.put("LST_PROC_TIME", now);

                if (LatestVersion) {
                    //現行法規
                    D710.put("CREATE_FLOW_NO", FLOW_NO);
                    D710List.add(D700);
                } else {
                    //歷史法規
                    D710.put("FLOW_NO", FLOW_NO);
                    D711List.add(D700);
                }

                Map Z600 = new LinkedHashMap();
                Z600.put("RKB_LAW_NO", RKB_EXT_NO);
                //Z600.put("FILE_ID",FILE_ID);
                //Z600.put("FILE_PATH",FILE_PATH);
                //Z600.put("FILE_EXT",FILE_EXT);
                Z600.put("CREATE_TIME", now);
                Z600List.add(Z600);

                i++;
            }

            List<Map> LawArticles = MapUtils.getObject(Data, "LawArticles", new ArrayList<>());


            Map levelMap = new HashMap();

            //將前一版條文移置舊版List
            oriLawList.clear();
            oriLawList.addAll(newLawList);
            newLawList.clear();

            //先排序，用No再用Level
            Collections.sort(LawArticles, new Comparator<Map>() {
                @Override
                public int compare(Map m1, Map m2) {
                    // 取得 No 值並轉換為數字
                    Integer AmendDate1 = MapUtils.getInteger(m1, "No", 0);
                    Integer AmendDate2 = MapUtils.getInteger(m2, "No", 0);

                    // 先按 AmendDate 升序排列
                    int result = AmendDate1.compareTo(AmendDate2);

                    // 如果 AmendDate 相同，則按 Level 升序排列
                    if (result == 0) {
                        Integer SerialNo1 = MapUtils.getInteger(m1, "Level", 0);
                        Integer SerialNo2 = MapUtils.getInteger(m2, "Level", 0);
                        return SerialNo1.compareTo(SerialNo2);
                    }
                    return result;
                }
            });

            for (Map map : LawArticles) {
                Map D720 = new LinkedHashMap();

                //確認層級，若非條文則僅記錄標題
                int LawLevel = MapUtils.getInteger(map, "Level");
                switch (LawLevel) {
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                        levelMap.put(LawLevel + "_Title", map.get("Title"));
                        levelMap.put(LawLevel + "_Data", map.get("Data"));
                        continue;
                    case 9:
                        break;
                    default:
                        throw new Exception("出現未知層級");
                }

                D720.put("COMP_ID", "00");
                D720.put("RKB_EXT_NO", RKB_EXT_NO);
                D720.put("SER_NO", map.get("No"));

                D720.put("PART_NAME", levelMap.get("1_Title"));
                D720.put("PART_MEMO", levelMap.get("1_Data"));
                D720.put("CHAPTER_NAME", levelMap.get("2_Title"));
                D720.put("CHAPTER_MEMO", levelMap.get("2_Data"));
                D720.put("SECTION_NAME", levelMap.get("3_Title"));
                D720.put("SECTION_MEMO", levelMap.get("3_Data"));
                D720.put("CLAUSE_NAME", levelMap.get("4_Title"));
                D720.put("CLAUSE_MEMO", levelMap.get("4_Data"));
                D720.put("ITEM_NAME", levelMap.get("5_Title"));
                D720.put("ITEM_MEMO", levelMap.get("5_Data"));


                //條號和註解法源沒拆分
                D720.put("ARTICLE_NAME", map.get("Title"));
                D720.put("ARTICLE_MEMO", map.get("Title"));

                //條文內容
                D720.put("CONTENT", map.get("Data"));

                D720.put("IS_AMEND", map.get("IsAmend"));

                List<String> HisList = (List<String>) map.get("HisNo");
                D720.put("HIS_SER_NO", HisList.size() == 0 ? "" : HisList.get(0));
                D720.put("EDIT_TIME", map.get("EditTime"));

                D720.put("LST_PROC_ID", "XRR0_B071");
                D720.put("LST_PROC_NAME", "XRR0_B071");
                D720.put("LST_PROC_DIV_NO", "XRR0_B071");
                D720.put("LST_PROC_DIV_NAME", "XRR0_B071");
                D720.put("LST_PROC_TIME", now);

                D720.put("FLOW_NO", FLOW_NO);

                newLawList.add(D720);

                if (LatestVersion) {
                    //現行法規
                    D720List.add(D720);
                } else {
                    //歷史法規
                    D721List.add(D720);
                }

            }

            Map oriLawMap = new HashMap();
            for (Map map : oriLawList) {
                String SER_NO = MapUtils.getString(map, "SER_NO");
                oriLawMap.put(SER_NO, map);
            }

            for (Map map : newLawList) {
                String HIS_SER_NO = MapUtils.getString(map, "HIS_SER_NO");
                Map theMap = MapUtils.getMap(oriLawMap, HIS_SER_NO);
                boolean IsAmend = MapUtils.getBoolean(map, "IS_AMEND"); //是否異動註記
                if (IsAmend || theMap == null) {
                    //條文有異動
                    //法源資料的第一筆IsAmend = FALSE，因此無法只用IsAmend判斷是否為新條文
                    map.put("ORI_CONTENT_UUID", theMap == null ? "" : theMap.get("CONTENT_UUID"));
                    map.put("HIS_CONTENT", theMap == null ? "" : theMap.get("CONTENT"));

                    //生一個新的UUID
                    String CONTENT_UUID = UUID.randomUUID().toString();
                    map.put("CONTENT_UUID", CONTENT_UUID);

                    //將新的UUID放入D730
                    Map D730 = new HashMap();
                    D730.put("COMP_ID", "00");
                    D730.put("RKB_EXT_NO", RKB_EXT_NO);
                    D730.put("CONTENT_UUID", CONTENT_UUID);
                    D730.put("CONTENT", map.get("CONTENT"));
                    D730.put("CREATE_FLOW_NO", FLOW_NO);
                    D730.put("LST_PROC_ID", "XRR0_B071");
                    D730.put("LST_PROC_NAME", "XRR0_B071");
                    D730.put("LST_PROC_DIV_NO", "XRR0_B071");
                    D730.put("LST_PROC_DIV_NAME", "XRR0_B071");
                    D730.put("LST_PROC_TIME", now);
                    D730List.add(D730);

                } else {
                    map.put("HIS_CONTENT", theMap.get("CONTENT"));
                    map.put("ORI_CONTENT_UUID", theMap.get("CONTENT_UUID"));
                    //內文一樣，繼承UUID
                    map.put("CONTENT_UUID", theMap.get("CONTENT_UUID"));
                }
            }


        }
        int x = 0;
    }


}
