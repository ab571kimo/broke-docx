package service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import utils.VirtualVo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class D08000 {
    Timestamp now = new Timestamp(System.currentTimeMillis());
    private static final String JDBC_URL = "jdbc:db2://localhost:50000/testdb";
    private static final String USERNAME = "db2inst1";
    private static final String PASSWORD = "INSTPW";

    private static Connection conn;

    public static Connection getConnection() throws SQLException {
        if (conn == null) {
            Connection theconn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
            conn = theconn;
            conn.setAutoCommit(false);
            return conn;
        } else {
            return conn;
        }
    }

    public VirtualVo getVo(String DB, String TABLE) throws Exception {
        Connection conn = getConnection();

        return new VirtualVo(conn, DB, TABLE);
    }

    public Connection getConn() throws SQLException {
        return conn;
    }

    public void submitFlow(String COMP_ID, String RKB_LAW_NO, String FLOW_NO, String NEXT_FLOW_STATUS, String CAUSE, String DESCRIP) throws Exception {

//取得本案案件主檔
        VirtualVo D800 = getVo("DBXR", "DTXRD800");
        D800.setColumn("*");
        D800.setCondition("COMP_ID", COMP_ID);
        D800.setCondition("RKB_LAW_NO", RKB_LAW_NO);
        D800.setCondition("FLOW_NO", FLOW_NO);
        List<Map> D800List = D800.select(false);

        if (D800List.isEmpty()) {
            submitFlowFirst(COMP_ID, RKB_LAW_NO, FLOW_NO, NEXT_FLOW_STATUS, CAUSE, DESCRIP);
            return;
        }
        Map D800Map = D800List.get(0);

        //取得案件下一筆SER_NO
        String SER_NO = getNEXT_SER_NO(COMP_ID, RKB_LAW_NO, FLOW_NO).toString();

        Map D810Map = new HashMap();
        D810Map.put("COMP_ID", COMP_ID);
        D810Map.put("RKB_LAW_NO", RKB_LAW_NO);
        D810Map.put("FLOW_NO", FLOW_NO);
        D810Map.put("SER_NO", SER_NO);
        D810Map.put("FLOW_STATUS", D800Map.get("FLOW_STATUS"));
        D810Map.put("SER_TYPE", "01");
        D810Map.put("CAUSE", CAUSE);
        D810Map.put("DESCRIP", DESCRIP);

        VirtualVo D810 = getVo("DBXR", "DTXRD810");
        D810.setInsert(D810Map);
        D810.insert(false);

        D800.setUpdate("FLOW_STATUS", NEXT_FLOW_STATUS);
        D800.setConditionByPk(D800Map);
        D800.updateByBatch(false);


    }

    public void submitFlowFirst(String COMP_ID, String RKB_LAW_NO, String FLOW_NO, String NEXT_FLOW_STATUS, String CAUSE, String DESCRIP) throws Exception {

        //產生案件主檔
        Map D800Map = new HashMap();
        D800Map.put("COMP_ID", COMP_ID);
        D800Map.put("RKB_LAW_NO", RKB_LAW_NO);
        D800Map.put("FLOW_NO", FLOW_NO);
        D800Map.put("FLOW_STATUS", NEXT_FLOW_STATUS);
        D800Map.put("CREATE_TIME", now);
        D800Map.put("CREATE_EMP_ID", "SYSTEM");
        VirtualVo D800 = getVo("DBXR", "DTXRD800");
        D800.setInsert(D800Map);
        D800.insert(false);

        //產生第一筆
        Map D810Map = new HashMap();
        D810Map.put("COMP_ID", COMP_ID);
        D810Map.put("RKB_LAW_NO", RKB_LAW_NO);
        D810Map.put("FLOW_NO", FLOW_NO);
        D810Map.put("SER_NO", "1");
        D810Map.put("FLOW_STATUS", "00");
        D810Map.put("SER_TYPE", "01");
        D810Map.put("CAUSE", "新件輸入");
        D810Map.put("DESCRIP", "新件輸入");
        VirtualVo D810 = getVo("DBXR", "DTXRD810");
        D810.setInsert(D810Map);
        D810.insert(false);

    }

    public Integer getNEXT_SER_NO(String COMP_ID, String RKB_LAW_NO, String FLOW_NO) throws Exception {
        VirtualVo D810 = getVo("DBXR", "DTXRD810");
        D810.setColumn("MAX(SER_NO) AS SER_NO");
        D810.setCondition("COMP_ID", COMP_ID);
        D810.setCondition("RKB_LAW_NO", RKB_LAW_NO);
        D810.setCondition("FLOW_NO", FLOW_NO);
        List<Map> D800List = D810.select(true);
        Map D800Map = D800List.get(D800List.size() - 1);

        int ser_no = MapUtils.getInteger(D800Map, "SER_NO");

//加一號
        ser_no = ser_no + 1;

        return ser_no;
    }

}
