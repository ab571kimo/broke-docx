package service;

import utils.VirtualVo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class D07200 {

    private static final String JDBC_URL = "jdbc:db2://localhost:50000/testdb";
    private static final String USERNAME = "db2inst1";
    private static final String PASSWORD = "INSTPW";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
    }

    public List<Map> queryExt() throws Exception {

        Connection conn = getConnection();
        VirtualVo theD701 = new VirtualVo(conn, "DBXR", "DTXRD7201");
        theD701.setColumn(new String[]{"*"});
        theD701.setCondition("COMP_ID", "00");
        List<Map> D701List = theD701.select(true);

        return D701List;
    }

    public List<Map> queryRul() throws Exception {

        Connection conn = getConnection();
        VirtualVo theD701 = new VirtualVo(conn, "DBXR", "DTXRD7202");
        theD701.setColumn(new String[]{"*"});
        theD701.setCondition("COMP_ID", "00");
        List<Map> D701List = theD701.select(true);

        return D701List;
    }

}
