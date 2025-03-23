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


        List<Map> D750List = new ArrayList();
        List<Map> D751List = new ArrayList();
        List<Map> D752List = new ArrayList();
        List<Map> D760List = new ArrayList();
        List<Map> D761List = new ArrayList();
        List<Map> D800List = new ArrayList();
        List<Map> Z600List = new ArrayList();

        List<Map> oriLawList = new ArrayList();
        List<Map> newLawList = new ArrayList();


        for (Map<String, Object> oriMap : ori) {

            Map Data = MapUtils.getMap(oriMap, "Data");
            Map D750 = new HashMap();

            String RKB_RUL_NO = MapUtils.getString(Data,"ID");
            String EDIT_TIME = MapUtils.getString(Data,"EditTime");

            LocalDateTime dateTime = LocalDateTime.parse(EDIT_TIME);

            // Define formatter for the desired output format
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

            // Format the date-time
            String FLOW_NO = dateTime.format(formatter);


            D750.put("RKB_RUL_NO",RKB_RUL_NO);
            D750.put("FLOW_NO",FLOW_NO);
            D750.put("CASE",Data.get("Case"));
            D750.put("NO",Data.get("No"));
            D750.put("UPDATE_DATE",Data.get("Date"));

            D750.put("ISSUE",Data.get("Issue"));
            D750.put("CONTENT",Data.get("Data"));
            D750.put("NOTE",Data.get("Note"));

            List<Map<String, String>> Unit = MapUtils.getObject(Data, "Unit", new ArrayList<>());
            StringBuilder COMP_AUTH_ID = new StringBuilder();
            StringBuilder COMP_AUTH_NAME = new StringBuilder();
            for (Map map : Unit) {
                COMP_AUTH_ID.append(map.get("ID")).append(',');
                COMP_AUTH_NAME.append(map.get("Name")).append(',');
            }
            if (COMP_AUTH_ID.toString().endsWith(",")) {
                COMP_AUTH_ID.deleteCharAt(COMP_AUTH_ID.length() - 1);

            }
            if (COMP_AUTH_NAME.toString().endsWith(",")) {
                COMP_AUTH_NAME.deleteCharAt(COMP_AUTH_NAME.length() - 1);

            }
            D750.put("COMP_AUTH_ID", COMP_AUTH_ID.toString());
            D750.put("COMP_AUTH_NAME", COMP_AUTH_NAME.toString());

            Map<String, String> Category = MapUtils.getMap(Data, "Category");
            D750.put("CATEGORY_ID", Category.get("ID"));
            D750.put("CATEGORY_NAME", Category.get("Name"));
            D750.put("EDIT_TIME", EDIT_TIME);

            Map<String, String> AmendTag = MapUtils.getMap(Data, "AmendTag");
            D750.put("AMENT_ID", AmendTag.get("ID"));
            D750.put("AMENT_NAME", AmendTag.get("Name"));

            D750List.add(D750);
            D751List.add(D750);

            List<Map> AttachmentFiles = MapUtils.getObject(Data, "AttachmentFiles", new ArrayList<>());

            new OriLaw().processFileList(RKB_RUL_NO, FLOW_NO, FILE_PATH, ORI_FILE_PATH, AttachmentFiles, "00");

        }

        int x = 0;
    }


}
