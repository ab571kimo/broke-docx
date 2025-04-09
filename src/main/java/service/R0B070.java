package service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import utils.VirtualVo;
import utils.VirtualVo.VoSymbol;

import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class R0B070 {

    Timestamp now = new Timestamp(System.currentTimeMillis());

    public void run() throws Exception {

        String ORI_FILE_PATH = "D:/testRKB/File/"; //extMap.get("FILE_PATH") + "File/"; TODO

        String FILE_PATH = "D:/testRKB/File2/"; //XR_Z10600.parseFILE_PATH("XRR0B071"); TODO

        //每一份檔案代表一部法規，內含多版本
        File fileJ = new File("D:\\FL000565api.json");

        JsonReader reader = new JsonReader(new FileReader(fileJ));

        List<Map<String, Object>> ori = new Gson().fromJson(reader, new TypeToken<List<Map<String, Object>>>() {
        }.getType());

        OriLaw theOriLaw = new OriLaw();

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


        Map Data = MapUtils.getMap(ori.get(0), "Data");
        String RKB_EXT_NO = MapUtils.getString(Data, "LawID");
        String FLOW_NO = MapUtils.getString(Data, "AmendDate") + "000" + MapUtils.getString(Data, "SerialNo");

        //整理出D700資料
        Map D700 = theOriLaw.dataToD700(RKB_EXT_NO, FLOW_NO, Data);

        D700List.add(D700);
        D800List.add(theOriLaw.mapToD800("00", RKB_EXT_NO, FLOW_NO, "XRR0_B070"));
        //產一筆D810
        D810List.add(theOriLaw.mapToD810("00", RKB_EXT_NO, FLOW_NO));


        //這筆資料待關聯
        D700.put("STATUS", "02");

        //歷史法規
        D701List.add(D700);

        int i = 1;

        //處理法規附件
        List<Map> ConversionFile = MapUtils.getObject(Data, "ConversionFile", new ArrayList<>());
        List<Map> AttachmentFiles = MapUtils.getObject(Data, "AttachmentFiles", new ArrayList<>());

        Map<String, List<Map>> rtnMap = theOriLaw.downloadFileList(RKB_EXT_NO, FLOW_NO, FILE_PATH, ConversionFile, "00");

        //現行法規
        D710List.addAll(rtnMap.get("D710List"));

        //歷史法規
        D711List.addAll(rtnMap.get("D710List"));
        Z600List.addAll(rtnMap.get("Z600List"));

        rtnMap = theOriLaw.downloadFileList(RKB_EXT_NO, FLOW_NO, FILE_PATH, AttachmentFiles, "01");

        //現行法規
        D710List.addAll(rtnMap.get("D710List"));

        //歷史法規
        D711List.addAll(rtnMap.get("D710List"));
        Z600List.addAll(rtnMap.get("Z600List"));


        List<Map> LawArticles = MapUtils.getObject(Data, "LawArticles", new ArrayList<>());

        //先排序，用No再用Level
        theOriLaw.sortLawArticles(LawArticles);

        //將LawArticles整理為D720清單
        Map<String, List<Map>> lawArticlesMap = theOriLaw.lawArticlesToD720List(RKB_EXT_NO, FLOW_NO, LawArticles);
        List<Map> theD720List = lawArticlesMap.get("D720List");

        //將前一版條文移置舊版List
        VirtualVo D620 = new D06100().getVo("DBXR", "DTXRD799");
        D620.setCondition("COMP_ID", "00");
        D620.setCondition("RKB_EXT_NO", RKB_EXT_NO);
        D620.setColumn("*");
        oriLawList = D620.select(false);

        D720List.addAll(theD720List);

        //歷史法規
        D721List.addAll(theD720List);


        //比對新舊法規，newLawList新增的法規加上UUID，既有的法規繼承UUID
        //新增的法規回傳為D730
        List<Map> theD730List = theOriLaw.compareLawArticles(RKB_EXT_NO, FLOW_NO, oriLawList, theD720List);

        D730List.addAll(theD730List);

        //處理附件
        List<Map> theAttachmentFiles = lawArticlesMap.get("AttachmentFiles");
        rtnMap = theOriLaw.downloadFileList(RKB_EXT_NO, FLOW_NO, FILE_PATH, theAttachmentFiles, "02");
        for (Map map : D710List) {
            map.put("SER_NO", i);
            i++;
        }
        //現行法規
        D710List.addAll(rtnMap.get("D710List"));

        //歷史法規
        D711List.addAll(rtnMap.get("D710List"));
        Z600List.addAll(rtnMap.get("Z600List"));


        Connection conn = getConnection();
        conn.setAutoCommit(false);

        try {

            for (Map map : D701List) {
                VirtualVo theD701 = new VirtualVo(conn, "DBXR", "DTXRD701");
                theD701.setColumn("*");
                theD701.setCondition("COMP_ID", map.get("COMP_ID"));
                theD701.setCondition("RKB_EXT_NO", map.get("RKB_EXT_NO"));
                theD701.setCondition("STATUS", VoSymbol.NOT_IN, new String[]{"10", "99"});
                List<Map> otherD701List = theD701.select(true);

                for (Map otherMap : otherD701List) {

                    VirtualVo theD601 = new VirtualVo(conn, "DBXR", "DTXRD701");
                    theD601.setCondition("COMP_ID", otherMap.get("COMP_ID"));
                    theD601.setCondition("RKB_LAW_NO", otherMap.get("RKB_EXT_NO"));
                    theD601.setCondition("FLOW_NO", otherMap.get("FLOW_NO"));
                    theD601.setUpdate("STATUS", "10");
                    theD601.setUpdate("ACTIVE", "1");
                    theD601.updateByBatch(false);

                    VirtualVo theD800 = new VirtualVo(conn, "DBXR", "DTXRD800");
                    theD800.setCondition("COMP_ID", otherMap.get("COMP_ID"));
                    theD800.setCondition("RKB_LAW_NO", otherMap.get("RKB_EXT_NO"));
                    theD800.setCondition("FLOW_NO", otherMap.get("FLOW_NO"));
                    theD800.setUpdate("FLOW_STATUS", "10");
                    theD800.updateByBatch(false);

                    Map D810Map = theOriLaw.mapToD810("00", RKB_EXT_NO, FLOW_NO);
                    D810Map.put("SER_NO", "99");
                    D810Map.put("FLOW_STATUS", "10");
                    VirtualVo theD810 = new VirtualVo(conn, "DBXR", "DTXRD810");
                    theD810.setInsert(D810Map);
                    theD810.insert(true);

                }
            }

            VirtualVo theD700 = new VirtualVo(conn, "DBXR", "DTXRD700");
            theD700.setCondition("COMP_ID", D700.get("COMP_ID"));
            theD700.setCondition("RKB_EXT_NO", D700.get("RKB_EXT_NO"));
            theD700.deleteByBatch(false);
            theD700.insertByBatch(D700List, false);

            VirtualVo theD701 = new VirtualVo(conn, "DBXR", "DTXRD701");
            theD701.insertByBatch(D701List, false);

            VirtualVo D720 = new VirtualVo(conn, "DBXR", "DTXRD720");
            D720.setCondition("COMP_ID", D700.get("COMP_ID"));
            D720.setCondition("RKB_EXT_NO", D700.get("RKB_EXT_NO"));
            D720.deleteByBatch(false);
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

    public List<Map> queryRegHistory(String COMP_ID, String RKB_EXT_NO, String FLOW_NO, String CONTECT_UUID) throws Exception {
        Connection conn = getConnection();
        VirtualVo theD701 = new VirtualVo(conn, "DBXR", "DTXRD701");
        theD701.setColumn(new String[]{"FLOW_NO", "UPDATE_DATE"});
        theD701.setCondition("COMP_ID", COMP_ID);
        theD701.setCondition("RKB_EXT_NO", RKB_EXT_NO);
        theD701.setCondition("FLOW_NO", VoSymbol.LESS_OR_EQUAL, FLOW_NO);
        theD701.setOrder("FLOW_NO",-1);
        List<Map> D701List = theD701.select(true);

        List<Map> rtnList = new ArrayList();

        for (Map D701Map : D701List) {
            String theFLOW_NO = MapUtils.getString(D701Map, "FLOW_NO");
            String UPDATE_DATE = MapUtils.getString(D701Map, "UPDATE_DATE");
            VirtualVo theD799 = new VirtualVo(conn, "DBXR", "DTXRD798");
            theD799.setColumn(new String[]{"CONTENT_UUID", "ORI_CONTENT_UUID","CONTENT"});
            theD799.setCondition("COMP_ID", COMP_ID);
            theD799.setCondition("RKB_EXT_NO", RKB_EXT_NO);
            theD799.setCondition("FLOW_NO", theFLOW_NO);
            theD799.setCondition("CONTENT_UUID", CONTECT_UUID);
            Map D799Map = theD799.select(true).get(0);

            String theORI_CONTENT_UUID = MapUtils.getString(D799Map, "ORI_CONTENT_UUID");
            String theCONTENT_UUID = MapUtils.getString(D799Map, "CONTENT_UUID");

            if (!theCONTENT_UUID.equals(theORI_CONTENT_UUID)) {
                //UUID有異動，更新沿革
                Map map = new HashMap();
                map.put("UPDATE_DATE", UPDATE_DATE);
                map.put("CONTENT", D799Map.get("CONTENT"));
                rtnList.add(map);
            }


            if (StringUtils.isBlank(theORI_CONTENT_UUID)) {
                //空值，表示已無版本
                return rtnList;
            }
            //有值，替換目前UUID，繼續迴圈
            CONTECT_UUID = theORI_CONTENT_UUID;

        }
        return rtnList;

    }
}