package service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.apache.commons.collections4.MapUtils;

import java.io.File;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpertation {

    public void run() throws Exception {

        String ORI_FILE_PATH = "D:/testRKB/File/"; //extMap.get("FILE_PATH") + "File/"; TODO

        String FILE_PATH = "D:/testRKB/File2/"; //XR_Z10600.parseFILE_PATH("XRR0B071"); TODO

        //每一份檔案代表一部法規，內含多版本
        File fileJ = new File("D:\\Interpertation.json");

        JsonReader reader = new JsonReader(new FileReader(fileJ));

        List<Map<String, Object>> ori = new Gson().fromJson(reader, new TypeToken<List<Map<String, Object>>>() {
        }.getType());

        //只有新增的函釋需要送比對
        List<Map> D750List = new ArrayList();
        List<Map> D751List = new ArrayList();
        List<Map> D752List = new ArrayList();
        List<Map> D760List = new ArrayList();
        List<Map> D761List = new ArrayList();
        List<Map> D800List = new ArrayList();
        List<Map> Z600List = new ArrayList();

        for (Map<String, Object> oriMap : ori) {

            Map Data = MapUtils.getMap(oriMap, "Data");
            Map D750 = new HashMap();

            String RKB_RUL_NO = MapUtils.getString(Data, "ID");
            String EDIT_TIME = MapUtils.getString(Data, "EditTime");

            LocalDateTime dateTime = LocalDateTime.parse(EDIT_TIME);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            String FLOW_NO = dateTime.format(formatter);

            D750.put("COMP_ID", "00");
            D750.put("RKB_RUL_NO", RKB_RUL_NO);
            D750.put("FLOW_NO", FLOW_NO);
            D750.put("CASE", Data.get("Case"));
            D750.put("NO", Data.get("No"));
            D750.put("UPDATE_DATE", Data.get("Date"));

            D750.put("ISSUE", Data.get("Issue"));
            D750.put("CONTENT", Data.get("Data"));
            D750.put("NOTE", Data.get("Note"));

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
            D750.put("UNIT_ID", UNIT_ID.toString());
            D750.put("UNIT_NAME", UNIT_NAME.toString());

            Map<String, String> Category = MapUtils.getMap(Data, "Category");
            D750.put("CATEGORY_ID", Category.get("ID"));
            D750.put("CATEGORY_NAME", Category.get("Name"));
            D750.put("EDIT_TIME", EDIT_TIME);

            Map<String, String> AmendTag = MapUtils.getMap(Data, "AmendTag");
            D750.put("AMENT_ID", AmendTag.get("ID"));
            D750.put("AMENT_NAME", AmendTag.get("Name"));

            //都把前一版覆蓋
            D750List.add(D750);
            //保留歷程
            D751List.add(D750);
            Map D800Map = new OriLaw().mapToD800(D750);
            D800Map.put("RKB_LAW_NO", RKB_RUL_NO);
            D800List.add(D800Map);



            List<Map> AttachmentFiles = MapUtils.getObject(Data, "AttachmentFiles", new ArrayList<>());

            int i = 1;
            Map<String, List<Map>> rtnMap = new OriLaw().processFileList(RKB_RUL_NO, FLOW_NO, FILE_PATH, ORI_FILE_PATH, AttachmentFiles, "00");
            for (Map map : rtnMap.get("D710List")) {
                map.put("RKB_RUL_NO ", RKB_RUL_NO);
                map.put("SER_NO", i);
                i++;
            }

            List<Map> theD750List = rtnMap.get("D710List");
            //都把前一版覆蓋
            D760List.addAll(theD750List);
            //保留歷程
            D761List.addAll(theD750List);

            //檔案放入Z600List
            List<Map> theZ600List = rtnMap.get("Z600List");
            Z600List.addAll(theZ600List);

            List<Map> RelaLawNos = MapUtils.getObject(Data, "RelaLawNos", new ArrayList<>());

            List<Map> theD752List = new OriLaw().processRelaLawNos(RKB_RUL_NO, FLOW_NO, RelaLawNos);
            D752List.addAll(theD752List);
        }


        int x = 0;
    }


}
