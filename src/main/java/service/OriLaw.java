package service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import utils.VirtualVo;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
public class OriLaw {

    Timestamp now = new Timestamp(System.currentTimeMillis());

    public void run() throws Exception {

        String ORI_FILE_PATH = "D:/testRKB/File/"; //extMap.get("FILE_PATH") + "File/"; TODO

        String FILE_PATH = "D:/testRKB/File2/"; //XR_Z10600.parseFILE_PATH("XRR0B071"); TODO

        //每一份檔案代表一部法規，內含多版本
        File fileJ = new File("D:\\Law02.json"); //Law01 FL000565

        JsonReader reader = new JsonReader(new FileReader(fileJ));

        List<Map<String, Object>> ori = new Gson().fromJson(reader, new TypeToken<List<Map<String, Object>>>() {
        }.getType());

        //將法規依年份排序，從小到大
        this.sortOriJson(ori);

        List<Map> D700List = new ArrayList();
        List<Map> D701List = new ArrayList();
        List<Map> D710List = new ArrayList();
        List<Map> D711List = new ArrayList();
        List<Map> D720List = new ArrayList();
        List<Map> D721List = new ArrayList();
        List<Map> D730List = new ArrayList();
        List<Map> D800List = new ArrayList();
        List<Map> D810List = new ArrayList();
        List<Map> Z600List = new ArrayList();

        List<Map> oriLawList = new ArrayList();
        List<Map> newLawList = new ArrayList();


        for (Map<String, Object> oriMap : ori) {

            Map Data = MapUtils.getMap(oriMap, "Data");
            String RKB_EXT_NO = MapUtils.getString(Data, "LawID");
            String FLOW_NO = MapUtils.getString(Data, "AmendDate") + "000" + MapUtils.getString(Data, "SerialNo");

            //整理出D700資料
            Map D700 = this.dataToD700(RKB_EXT_NO, FLOW_NO, Data);

            //LatestVersion 現行法規
            boolean LatestVersion = MapUtils.getBoolean(Data, "LatestVersion");
            if (LatestVersion) {
                //現行法規額外處理
                D700List.add(D700);
                D800List.add(mapToD800("00",RKB_EXT_NO,FLOW_NO,"XRR0_B071"));
                D810List.add(mapToD810("00",RKB_EXT_NO,FLOW_NO));
                //這筆資料待關聯
                D700.put("STATUS", "02");
            }
            //歷史法規
            D701List.add(D700);

            int i = 1;

            //處理法規附件
            List<Map> ConversionFile = MapUtils.getObject(Data, "ConversionFile", new ArrayList<>());
            List<Map> AttachmentFiles = MapUtils.getObject(Data, "AttachmentFiles", new ArrayList<>());

            Map<String, List<Map>> rtnMap = this.processFileList(RKB_EXT_NO, FLOW_NO, FILE_PATH, ORI_FILE_PATH, ConversionFile, "00");
            //編號
            for (Map map : rtnMap.get("D710List")) {
                map.put("SER_NO", i);
                i++;
            }
            if (LatestVersion) {
                //現行法規
                D710List.addAll(rtnMap.get("D710List"));

            }
            //歷史法規
            D711List.addAll(rtnMap.get("D710List"));
            Z600List.addAll(rtnMap.get("Z600List"));

            rtnMap = this.processFileList(RKB_EXT_NO, FLOW_NO, FILE_PATH, ORI_FILE_PATH, AttachmentFiles, "01");
            for (Map map : rtnMap.get("D710List")) {
                map.put("SER_NO", i);
                i++;
            }
            if (LatestVersion) {
                //現行法規
                D710List.addAll(rtnMap.get("D710List"));
            }
            //歷史法規
            D711List.addAll(rtnMap.get("D710List"));
            Z600List.addAll(rtnMap.get("Z600List"));


            List<Map> LawArticles = MapUtils.getObject(Data, "LawArticles", new ArrayList<>());

            //先排序，用No再用Level
            this.sortLawArticles(LawArticles);

            //將LawArticles整理為D720清單
            Map<String, List<Map>> lawArticlesMap = this.lawArticlesToD720List(RKB_EXT_NO, FLOW_NO, LawArticles);
            List<Map> theD720List = lawArticlesMap.get("D720List");

            //將前一版條文移置舊版List
            oriLawList.clear();
            oriLawList.addAll(newLawList);
            //本次法規清單
            newLawList.clear();
            newLawList.addAll(theD720List);

            if (LatestVersion) {
                //現行法規
                D720List.addAll(theD720List);
            }
            //歷史法規
            D721List.addAll(theD720List);


            //比對新舊法規，newLawList新增的法規加上UUID，既有的法規繼承UUID
            //新增的法規回傳為D730
            List<Map> theD730List = this.compareLawArticles(RKB_EXT_NO, FLOW_NO, oriLawList, newLawList);

            D730List.addAll(theD730List);

            //處理附件
            List<Map> theAttachmentFiles = lawArticlesMap.get("AttachmentFiles");
            rtnMap = this.processFileList(RKB_EXT_NO, FLOW_NO, FILE_PATH, ORI_FILE_PATH, theAttachmentFiles, "02");
            for (Map map : rtnMap.get("D710List")) {
                map.put("SER_NO", i);
                i++;
            }
            if (LatestVersion) {
                //現行法規
                D710List.addAll(rtnMap.get("D710List"));
            }
            //歷史法規
            D711List.addAll(rtnMap.get("D710List"));
            Z600List.addAll(rtnMap.get("Z600List"));

        }

        Connection conn = getConnection();
        conn.setAutoCommit(false);

        try {


            VirtualVo D700 = new VirtualVo(conn, "DBXR", "DTXRD700");
            D700.insertByBatch(D700List, false);

            VirtualVo D701 = new VirtualVo(conn, "DBXR", "DTXRD701");
            D701.insertByBatch(D701List, false);
            VirtualVo D720 = new VirtualVo(conn, "DBXR", "DTXRD720");

            D720.insertByBatch(D720List, false);
            VirtualVo D721 = new VirtualVo(conn, "DBXR", "DTXRD721");
            D721.insertByBatch(D721List, false);

            VirtualVo D730 = new VirtualVo(conn, "DBXR", "DTXRD730");
            D730.insertByBatch(D730List, false);

            VirtualVo D800 = new VirtualVo(conn, "DBXR", "DTXRD800");
            D800.insertByBatch(D800List, false);

            VirtualVo D810 = new VirtualVo(conn, "DBXR", "DTXRD810");
            D810.insertByBatch(D810List, false);

            conn.commit();
        } catch (Exception e) {
            log.debug("", e);
            conn.rollback();
        }

        int x = 0;
    }

    private static final String JDBC_URL = "jdbc:db2://localhost:50000/testdb";
    private static final String USERNAME = "db2inst1";
    private static final String PASSWORD = "INSTPW";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
    }

    public Map dataToD700(String RKB_EXT_NO, String FLOW_NO, Map Data) {

        //處理D700
        Map D700 = new LinkedHashMap();
        D700.put("COMP_ID", "00");
        D700.put("RKB_EXT_NO", RKB_EXT_NO);
        D700.put("FLOW_NO", FLOW_NO);
        D700.put("EXT_NAME", Data.get("LawName"));

        Map<String, String> Level = MapUtils.getMap(Data, "Level");
        D700.put("LEVEL_ID", Level.get("ID"));
        D700.put("LEVEL_NAME", Level.get("Name"));

        D700.put("ANN_DATE", yyyyMMddAddDash(MapUtils.getString(Data, "AnnounceDate")));
        D700.put("UPDATE_DATE", yyyyMMddAddDash(MapUtils.getString(Data, "AmendDate")));

        Map<String, String> Valid = MapUtils.getMap(Data, "Valid");
        D700.put("VALID_DATE", yyyyMMddAddDash(MapUtils.getString(Valid, "Date")));
        D700.put("VALID_MEMO", Valid.get("Memo"));

        Map<String, String> AmendTag = MapUtils.getMap(Data, "AmendTag");
        D700.put("AMENT_ID", AmendTag.get("ID"));
        D700.put("AMENT_NAME", AmendTag.get("Name"));

        D700.put("REPEAL_DATE", yyyyMMddAddDash(MapUtils.getString(Data, "RepealDate")));

        D700.put("REG_HISTORY", Data.get("Source"));
        D700.put("FOREWORD", Data.get("Foreword"));
        D700.put("SOURCE_ID", Data.get("ID"));

        List<Map<String, String>> Unit = MapUtils.getObject(Data, "Unit", new ArrayList<>());
        StringBuilder UNIT_ID = new StringBuilder();
        StringBuilder UNIT_NAME = new StringBuilder();
        for (Map map : Unit) {
            UNIT_ID.append(map.get("ID")).append(',');
            UNIT_NAME.append(map.get("Name")).append(',');
        }
        if (UNIT_ID.toString().endsWith(",")) {
            UNIT_ID.deleteCharAt(UNIT_ID.length() - 1);

        }
        if (UNIT_NAME.toString().endsWith(",")) {
            UNIT_NAME.deleteCharAt(UNIT_NAME.length() - 1);

        }
        D700.put("UNIT_ID", UNIT_ID.toString());
        D700.put("UNIT_NAME", UNIT_NAME.toString());

        D700.put("REASONS", Data.get("LegislativeReasons"));

        Map<String, String> Category = MapUtils.getMap(Data, "Category");
        D700.put("CATEGORY_ID", Category.get("ID"));
        D700.put("CATEGORY_NAME", Category.get("Name"));
        D700.put("EDIT_TIME", Data.get("EditTime"));

        //LatestVersion 現行法規
        boolean LatestVersion = MapUtils.getBoolean(Data, "LatestVersion");
        String ACTIVE = LatestVersion ? "0" : "1";

        D700.put("STATUS", "10");
        D700.put("ACTIVE", ACTIVE);
        D700.put("LST_PROC_ID", "XRR0_B071");
        D700.put("LST_PROC_NAME", "XRR0_B071");
        D700.put("LST_PROC_DIV_NO", "XRR0_B071");
        D700.put("LST_PROC_DIV_NAME", "XRR0_B071");
        D700.put("LST_PROC_TIME", now);
        return D700;
    }

    public Map mapToD800(String COMP_ID, String RKB_LAW_NO, String FLOW_NO, String BATCH_NAME) {

        //處理D700
        Map D800 = new HashMap();
        D800.put("COMP_ID", COMP_ID);
        D800.put("RKB_LAW_NO", RKB_LAW_NO);
        D800.put("FLOW_NO", FLOW_NO);
        D800.put("FLOW_STATUS", "03");

        D800.put("TODO_EMP_ID", "SYSTEM");
        D800.put("TODO_EMP_NAME", "SYSTEM");
        D800.put("TODO_DIV_NO", "SYSTEM");
        D800.put("TODO_DIV_NAME", "SYSTEM");

        D800.put("CREATE_TIME", now);
        D800.put("CREATE_EMP_ID", BATCH_NAME);
        D800.put("CREATE_EMP_NAME", BATCH_NAME);
        D800.put("CREATE_DIV_NO", BATCH_NAME);
        D800.put("CREATE_DIV_NAME", BATCH_NAME);

        return D800;
    }

    public Map mapToD810(String COMP_ID, String RKB_LAW_NO, String FLOW_NO) {

        //處理D700
        Map D810 = new HashMap();
        D810.put("COMP_ID", COMP_ID);
        D810.put("RKB_LAW_NO", RKB_LAW_NO);
        D810.put("FLOW_NO", FLOW_NO);
        D810.put("FLOW_STATUS", "00");
        D810.put("SER_NO", "1");
        D810.put("SER_TYPE", "01");
        D810.put("CAUSE", "法源新增");
        D810.put("DESCRIP", "法源新增");

        return D810;
    }

    public Map<String, List<Map>> lawArticlesToD720List(String RKB_EXT_NO, String FLOW_NO, List<Map> LawArticles) throws Exception {

        Map levelMap = new HashMap();

        Map<String, List<Map>> rtnMap = new HashMap();
        List<Map> D720List = new ArrayList();
        List<Map> AttachmentFiles = new ArrayList();

        for (Map map : LawArticles) {

            //將附件資訊抽出另外處理
            List<Map> files = MapUtils.getObject(map, "AttachmentFiles", new ArrayList());
            for (Map fileMap : files) {
                //把每一筆資料押入法條內容
                fileMap.put("ARTICLE_SOURCE", map.get("Title"));
            }
            AttachmentFiles.addAll(files);

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
                    levelMap.put(LawLevel, MapUtils.getInteger(levelMap, LawLevel, 0) + 1);
                    continue;
                case 9:
                    levelMap.put(LawLevel, MapUtils.getInteger(levelMap, LawLevel, 0) + 1);
                    break;
                default:
                    throw new Exception("出現未知層級");
            }

            D720.put("COMP_ID", "00");
            D720.put("RKB_EXT_NO", RKB_EXT_NO);
            D720.put("SER_NO", map.get("No"));

            D720.put("PART_NO)", levelMap.get(1));
            D720.put("PART_NAME", levelMap.get("1_Title"));
            D720.put("PART_MEMO", levelMap.get("1_Data"));
            D720.put("CHAPTER_NO", levelMap.get(2));
            D720.put("CHAPTER_NAME", levelMap.get("2_Title"));
            D720.put("CHAPTER_MEMO", levelMap.get("2_Data"));
            D720.put("SECTION_NO", levelMap.get(3));
            D720.put("SECTION_NAME", levelMap.get("3_Title"));
            D720.put("SECTION_MEMO", levelMap.get("3_Data"));
            D720.put("CLAUSE_NO", levelMap.get(4));
            D720.put("CLAUSE_NAME", levelMap.get("4_Title"));
            D720.put("CLAUSE_MEMO", levelMap.get("4_Data"));
            D720.put("ITEM_NO", levelMap.get(5));
            D720.put("ITEM_NAME", levelMap.get("5_Title"));
            D720.put("ITEM_MEMO", levelMap.get("5_Data"));


            //條號和註解法源沒拆分
            D720.put("ARTICLE_NO", levelMap.get(9));
            D720.put("ARTICLE_NAME", map.get("Title"));
            D720.put("ARTICLE_MEMO", map.get("Title"));

            //條文內容
            D720.put("CONTENT", map.get("Data"));

            D720.put("IS_AMEND", MapUtils.getBoolean(map, "IsAmend"));

            List<String> HisList = (List<String>) map.get("HisNo");
            D720.put("HIS_SER_NO", HisList.size() == 0 ? "" : HisList.get(0));
            D720.put("EDIT_TIME", map.get("EditTime")); //TODO

            Map<String, String> Valid = MapUtils.getMap(map, "Valid");
            D720.put("VALID_DATE", yyyyMMddAddDash(MapUtils.getString(Valid, "Date")));
            D720.put("VALID_MEMO", Valid.get("Memo"));

            D720.put("LST_PROC_ID", "XRR0_B071");
            D720.put("LST_PROC_NAME", "XRR0_B071");
            D720.put("LST_PROC_DIV_NO", "XRR0_B071");
            D720.put("LST_PROC_DIV_NAME", "XRR0_B071");
            D720.put("LST_PROC_TIME", now);

            D720.put("FLOW_NO", FLOW_NO);
            D720List.add(D720);
        }

        rtnMap.put("D720List", D720List);
        rtnMap.put("AttachmentFiles", AttachmentFiles);

        return rtnMap;
    }

    void sortLawArticles(List<Map> LawArticles) {
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
    }

    public List<Map> compareLawArticles(String RKB_EXT_NO, String FLOW_NO, List<Map> oriLawList, List<Map> newLawList) {

        Map oriLawMap = new HashMap();
        for (Map map : oriLawList) {
            String SER_NO = MapUtils.getString(map, "SER_NO");
            oriLawMap.put(SER_NO, map);
        }

        List<Map> rtnList = new ArrayList();

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
                D730.put("FLOW_NO", FLOW_NO);
                D730.put("LST_PROC_ID", "XRR0_B071");
                D730.put("LST_PROC_NAME", "XRR0_B071");
                D730.put("LST_PROC_DIV_NO", "XRR0_B071");
                D730.put("LST_PROC_DIV_NAME", "XRR0_B071");
                D730.put("LST_PROC_TIME", now);
                rtnList.add(D730);

            } else {
                map.put("HIS_CONTENT", theMap.get("CONTENT"));
                map.put("ORI_CONTENT_UUID", theMap.get("CONTENT_UUID"));
                //內文一樣，繼承UUID
                map.put("CONTENT_UUID", theMap.get("CONTENT_UUID"));
            }
        }
        return rtnList;
    }

    private void sortOriJson(List<Map<String, Object>> ori) {
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
    }

    public Map<String, List<Map>> processFileList(String RKB_EXT_NO, String FLOW_NO, String FILE_PATH, String ORI_FILE_PATH, List<Map> fileList, String FILE_TYPE) throws IOException {

        Map<String, List<Map>> rtnMap = new HashMap();
        List<Map> D710List = new ArrayList<>();
        List<Map> Z600List = new ArrayList<>();

        for (Map map : fileList) {
            Map D710 = new LinkedHashMap();
            D710.put("COMP_ID", "00");

            String FileID = MapUtils.getString(map, "FileID");
            String FileName = MapUtils.getString(map, "FileName");
            String FileExtension = MapUtils.getString(map, "FileExtension");
            FileExtension = FileExtension.replaceFirst("^\\.+", "");

            String FullName = FileID + '.' + FileExtension;

            //取得FILE_ID
            String FILE_ID = "this_is_new";//XR_Z0Z002().getFILE_ID(DATE.today()); TODO

            //將舊檔案複製到指定位置
            String ORI_FULL_PATH = ORI_FILE_PATH + FullName;
            String NEW_FULL_PATH = FILE_PATH + FILE_ID + '.' + FileExtension;
            File orifile = new File(ORI_FULL_PATH);
            File newfile = new File(NEW_FULL_PATH);
            FileUtils.copyFile(orifile, newfile);

            D710.put("RKB_EXT_NO", RKB_EXT_NO);
            D710.put("FILE_ID", FILE_ID);
            D710.put("FILE_NAME", FileName + '.' + FileExtension);
            D710.put("FILE_TYPE", FILE_TYPE);
            D710.put("ARTICLE_SOURCE", map.get("ARTICLE_SOURCE"));
            D710.put("IS_CONTENT", map.get("isContentFile"));
            D710.put("FILE_URL", map.get("FileUrl"));
            //D710.put("SER_NO", i);
            D710.put("SOURCE_ID", map.get("FileID"));
            D710.put("EDIT_TIME", map.get("EditTime"));
            D710.put("LST_PROC_ID", "XRR0_B071");
            D710.put("LST_PROC_NAME", "XRR0_B071");
            D710.put("LST_PROC_DIV_NO", "XRR0_B071");
            D710.put("LST_PROC_DIV_NAME", "XRR0_B071");
            D710.put("LST_PROC_TIME", now);

            D710.put("FLOW_NO", FLOW_NO);
            D710List.add(D710);

            Map Z600 = new LinkedHashMap();
            Z600.put("RKB_LAW_NO", RKB_EXT_NO);
            Z600.put("FILE_ID", FILE_ID);
            Z600.put("FILE_PATH", FILE_PATH);
            Z600.put("FILE_EXT", FileExtension);
            Z600.put("CREATE_TIME", now);
            Z600List.add(Z600);

        }

        rtnMap.put("Z600List", Z600List);
        rtnMap.put("D710List", D710List);
        return rtnMap;
    }

    public List<Map> processRelaLawNos(String RKB_RUL_NO, String FLOW_NO, List<Map> RelaLawList) throws IOException {

        List<Map> D752List = new ArrayList<>();

        for (Map map : RelaLawList) {
            Map D752 = new HashMap();
            D752.put("COMP_ID", "00");
            D752.put("RKB_RUL_NO", RKB_RUL_NO);
            D752.put("FLOW_NO", FLOW_NO);

            D752.put("RKB_EXT_NO", map.get("LawID"));
            D752.put("SOURCE_ID", map.get("ID"));
            D752.put("AMENT_DATE", yyyyMMddAddDash(MapUtils.getString(map, "AmendDate")));
            D752.put("SER_NO", map.get("LawNo"));

            D752List.add(D752);

        }

        return D752List;
    }

    public Date yyyyMMddAddDash(String inputDate) {

        if (StringUtils.isBlank(inputDate)) {
            return null;
        }

        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String outputDate = LocalDate.parse((inputDate), inputFormatter).format(outputFormatter);

        Date.valueOf(outputDate);

        return Date.valueOf(outputDate);
    }

    public Map<String, List<Map>> downloadFileList(String RKB_EXT_NO, String FLOW_NO, String FILE_PATH, List<Map> fileList, String FILE_TYPE) throws IOException {

        Map<String, List<Map>> rtnMap = new HashMap();
        List<Map> D710List = new ArrayList<>();
        List<Map> Z600List = new ArrayList<>();

        for (Map map : fileList) {
            Map D710 = new LinkedHashMap();
            D710.put("COMP_ID", "00");

            String FileID = MapUtils.getString(map, "FileID");
            String FileName = MapUtils.getString(map, "FileName");
            String FileExtension = MapUtils.getString(map, "FileExtension");
            FileExtension = FileExtension.replaceFirst("^\\.+", "");

            String FullName = FileName + '.' + FileExtension;

            //取得FILE_ID
            String FILE_ID = "this_is_new";//XR_Z0Z002().getFILE_ID(DATE.today()); TODO

            //將舊檔案複製到指定位置
            String NEW_FULL_PATH = FILE_PATH + FILE_ID + '.' + FileExtension;

            String url = MapUtils.getString(map, "FileUrl");
            //File orifile = new File(url);
            File newfile = new File(NEW_FULL_PATH);
            FileUtils.copyURLToFile(new URL(url), newfile);
            //FileUtils.copyFile(orifile, newfile);

            D710.put("RKB_EXT_NO", RKB_EXT_NO);
            D710.put("FILE_ID", FILE_ID);
            D710.put("FILE_NAME", FullName);
            D710.put("FILE_TYPE", FILE_TYPE);
            D710.put("ARTICLE_SOURCE", map.get("ARTICLE_SOURCE"));
            D710.put("IS_CONTENT", map.get("isContentFile"));
            D710.put("FILE_URL", map.get("FileUrl"));
            //D710.put("SER_NO", i);
            D710.put("SOURCE_ID", map.get("FileID"));
            D710.put("EDIT_TIME", map.get("EditTime"));
            D710.put("LST_PROC_ID", "XRR0_B071");
            D710.put("LST_PROC_NAME", "XRR0_B071");
            D710.put("LST_PROC_DIV_NO", "XRR0_B071");
            D710.put("LST_PROC_DIV_NAME", "XRR0_B071");
            D710.put("LST_PROC_TIME", now);

            D710.put("FLOW_NO", FLOW_NO);
            D710List.add(D710);

            Map Z600 = new LinkedHashMap();
            Z600.put("RKB_LAW_NO", RKB_EXT_NO);
            Z600.put("FILE_ID", FILE_ID);
            Z600.put("FILE_PATH", FILE_PATH);
            Z600.put("FILE_EXT", FileExtension);
            Z600.put("CREATE_TIME", now);
            Z600List.add(Z600);

        }

        rtnMap.put("Z600List", Z600List);
        rtnMap.put("D710List", D710List);
        return rtnMap;
    }

    public  void putLST_PROC(String BATCH_NAME,Timestamp now,List<Map> reqList){
        for(Map map : reqList){
            map.put("LST_PROC_ID", BATCH_NAME);
            map.put("LST_PROC_NAME", BATCH_NAME);
            map.put("LST_PROC_DIV_NO", BATCH_NAME);
            map.put("LST_PROC_DIV_NAME", BATCH_NAME);
            map.put("LST_PROC_TIME", now);
        }
    }

    public  void putLST_PROC(String BATCH_NAME,Timestamp now,Map map){
            map.put("LST_PROC_ID", BATCH_NAME);
            map.put("LST_PROC_NAME", BATCH_NAME);
            map.put("LST_PROC_DIV_NO", BATCH_NAME);
            map.put("LST_PROC_DIV_NAME", BATCH_NAME);
            map.put("LST_PROC_TIME", now);
    }
}
