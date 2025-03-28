package service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import utils.VirtualVo;

import java.sql.*;
import java.util.*;

@Slf4j
public class D06100 {
    private static final String JDBC_URL = "jdbc:db2://localhost:50000/testdb";
    private static final String USERNAME = "db2inst1";
    private static final String PASSWORD = "INSTPW";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
    }

    public VirtualVo getVo(String DB,String TABLE) throws Exception {
        Connection conn = getConnection();
        return new VirtualVo(conn, DB,TABLE);
    }

    public void save(Map D600Map, List<Map> D610List, List<Map> D620List) throws SQLException {
        //壓上使用者資訊
        putUserData(D600Map);
        Connection conn = getConnection();
        try{
            conn.setAutoCommit(false);

//寫入內規條文暫存檔DTXRD621，D620List為空值則略過
            if (D620List != null & !D620List.isEmpty()) {

                List<Map> D630List = new ArrayList();

//逐筆處理D621List
                for (Map map : D620List) {
                    String uuid = MapUtils.getString(map, "CONTENT_UUID");
                    if (StringUtils.isBlank(uuid)) {
                        String newUUID = UUID.randomUUID().toString();
                        map.put("CONTENT_UUID", newUUID);
                        //新產生的UUID資料待寫入D630
                        D630List.add(map);
                    }
                }
//處理完畢後，壓上使用者資訊
                for (Map map : D620List) {
                    putUserData(map);

                    map.put("COMP_ID", D600Map.get("COMP_ID"));
                    map.put("RKB_INR_NO", D600Map.get("RKB_INR_NO"));
                    map.put("FLOW_NO", D600Map.get("FLOW_NO"));

                    //轉型
                    //map.put("SER_NO",MapUtils.getInteger(map,"SER_NO"));
                    // map.put("PART_NO",MapUtils.getInteger(map,"PART_NO"));
                    //map.put("CHAPTER_NO",MapUtils.getInteger(map,"CHAPTER_NO"));
                    //map.put("ARTICLE_NO",MapUtils.getInteger(map,"ARTICLE_NO"));
                    // map.put("SECTION_NO",MapUtils.getInteger(map,"SECTION_NO"));

                }
                for (Map map : D630List) {
                    putUserData(map);
                    map.put("COMP_ID", D600Map.get("COMP_ID"));
                    map.put("RKB_INR_NO", D600Map.get("RKB_INR_NO"));
                    map.put("CREATE_FLOW_NO", D600Map.get("FLOW_NO"));
                }


                if(D600Map.get("FLOW_NO").equals("20250320000099")){
                    VirtualVo D600 = new VirtualVo(conn, "DBXR", "DTXRD600");
                    D600.setInsert(D600Map);
                    D600.insert(false);

                    VirtualVo D620 = new VirtualVo(conn, "DBXR", "DTXRD620");

                    D620.insertByBatch(D620List, false);
                }


                VirtualVo D601 = new VirtualVo(conn, "DBXR", "DTXRD601");
                D601.setInsert(D600Map);
                D601.insert(false);

                VirtualVo D621 = new VirtualVo(conn, "DBXR", "DTXRD621");

                D621.insertByBatch(D620List, false);


                VirtualVo D630 = new VirtualVo(conn, "DBXR", "DTXRD630");
                D630.insertByBatch(D630List, false);

//寫入DTXRD630
                // XR_D630T1.insertByBatch(D630List)

//寫入DTXRD621
                // 先刪除XR_D621T1.deleteByBatch(D621List)，查無資料視為正常
                // 再寫入XR_D621T1.insertByBatch(D621List)

            }

            conn.commit();

        }catch (BatchUpdateException e) {
            log.debug(e.getNextException().getMessage());

            conn.rollback();
        } catch (Exception e) {
            log.debug(e.getMessage(),e);

            conn.rollback();
        }finally {
            conn.close();
        }



//寫入主檔DTXRD601


    }

    Timestamp now = new Timestamp(System.currentTimeMillis());

    public void putUserData(Map map) {
        map.put("LST_PROC_ID", "XRR0_B071");
        map.put("LST_PROC_NAME", "XRR0_B071");
        map.put("LST_PROC_DIV_NO", "XRR0_B071");
        map.put("LST_PROC_DIV_NAME", "XRR0_B071");
        map.put("LST_PROC_TIME", now);
    }

    public List<Map> compareRKB_INR(List<Map> ORI_D620List, List<Map> NEW_D620List) {
        if (ORI_D620List == null || ORI_D620List.isEmpty()) {
            return NEW_D620List;
        }

//將ORI_D620List[i].CONTENT逐筆取出，組出新Map
        Map<String, Map<String, String>> oriMap = new LinkedHashMap<>();
        for (Map<String, String> map : ORI_D620List) {
            oriMap.put(map.get("CONTENT"), map);
        }

//逐筆讀NEW_D620List，確認NEW_D620List[i].CONTENT是否出現在oriMap
        for (Map map : NEW_D620List) {
            String key = MapUtils.getString(map,"CONTENT");
            if (oriMap.containsKey(key)) {
                //有舊條文關聯
                map.put("ORI_PART_NAME", oriMap.get(key).get("PART_NAME"));
                map.put("ORI_CHAPTER_NAME", oriMap.get(key).get("CHAPTER_NAME"));
                map.put("ORI_SECTION_NAME", oriMap.get(key).get("SECTION_NAME"));
                map.put("ORI_ARTICLE_NAME", oriMap.get(key).get("ARTICLE_NAME"));
                map.put("ORI_CONTENT ", oriMap.get(key).get("CONTENT"));
                map.put("ORI_CONTENT_UUID", oriMap.get(key).get("CONTENT_UUID"));

                map.put("ORI_PART_MEMO", oriMap.get(key).get("PART_MEMO"));
                map.put("ORI_CHAPTER_MEMO", oriMap.get(key).get("CHAPTER_MEMO"));
                map.put("ORI_SECTION_MEMO", oriMap.get(key).get("SECTION_MEMO"));
                map.put("ORI_ARTICLE_MEMO", oriMap.get(key).get("ARTICLE_MEMO"));

                map.put("ORI_PART_NO",MapUtils.getString(oriMap.get(key),"PART_NO"));
                map.put("ORI_CHAPTER_NO",MapUtils.getString(oriMap.get(key),"CHAPTER_NO"));
                map.put("ORI_SECTION_NO",MapUtils.getString(oriMap.get(key),"SECTION_NO"));
                map.put("ORI_ARTICLE_NO",MapUtils.getString(oriMap.get(key),"ARTICLE_NO"));


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

            map.put("ORI_PART_MEMO",map.get("PART_MEMO"));
            map.put("ORI_CHAPTER_MEMO",map.get("CHAPTER_MEMO"));
            map.put("ORI_SECTION_MEMO",map.get("SECTION_MEMO"));
            map.put("ORI_ARTICLE_MEMO",map.get("ARTICLE_MEMO"));

            map.put("ORI_PART_NO",MapUtils.getString(map,"PART_NO"));
            map.put("ORI_CHAPTER_NO",MapUtils.getString(map,"CHAPTER_NO"));
            map.put("ORI_SECTION_NO",MapUtils.getString(map,"SECTION_NO"));
            map.put("ORI_ARTICLE_NO",MapUtils.getString(map,"ARTICLE_NO"));


            map.remove("PART_NAME");
            map.remove("CHAPTER_NAME");
            map.remove("SECTION_NAME");
            map.remove("ARTICLE_NAME");

            map.remove("PART_MEMO");
            map.remove("CHAPTER_MEMO");
            map.remove("SECTION_MEMO");
            map.remove("ARTICLE_MEMO");

            map.remove("PART_NO");
            map.remove("CHAPTER_NO");
            map.remove("SECTION_NO");
            map.remove("ARTICLE_NO");

            map.remove("CONTENT_UUID");
            map.remove("CONTENT");

            //將map插入NEW_D620List
            int serNo = Integer.parseInt(MapUtils.getString(map,"SER_NO"));

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
