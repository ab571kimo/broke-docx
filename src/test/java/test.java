import java.io.File;
import java.io.FileInputStream;
import java.util.*;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import service.BrokeDocxService;

public class test {

    public static void main(String[] args) {
        // TODO Auto-generated method stub

        //國泰世華商業銀行_民法準則.docx
        //D:\人壽供測試用內規\供測試用內規\紅利分配辦法.docx
        try {
            File file = new File("D:\\國泰世華商業銀行_民法準則2.docx");
            byte[] encodedBytes = Base64.encodeBase64(FileUtils.readFileToByteArray(file));
            FileInputStream inputStream = new FileInputStream(file);
            BrokeDocxService BreakDocxService = new BrokeDocxService();


            List<Map<String, String>> ORI_D620List = BreakDocxService.brokeDocx(inputStream);

            int x = 0;
            for (Map<String, String> map : ORI_D620List) {
                map.put("CONTENT_UUID", String.valueOf(x));
                x++;
            }


            Map<String, Map<String, String>> oriMap = new LinkedHashMap<>();
            for (Map<String, String> map : ORI_D620List) {
                oriMap.put(map.get("CONTENT"), map);
            }

            file = new File("D:\\國泰世華商業銀行_民法準則3.docx");
            encodedBytes = Base64.encodeBase64(FileUtils.readFileToByteArray(file));
            inputStream = new FileInputStream(file);
            BreakDocxService = new BrokeDocxService();
            List<Map<String, String>> NEW_D620List = BreakDocxService.brokeDocx(inputStream);

            NEW_D620List.get(3).put("ORI_CONTENT_UUID", "3");

            List<Map<String, String>> rtnList = BreakDocxService.compareRKB_INR(ORI_D620List, NEW_D620List);


            int o = 1;
            for (Map<String, String> map : rtnList) {
                map.put("SER_NO", Integer.toString(o));
                o++;

                System.out.println(map);
            }

            List<Map> D620List = new ArrayList();

            List<Map> D630List = new ArrayList();
            for (Map map : D620List) {
                String uuid = MapUtils.getString(map, "CONTENT_UUID");
                if (StringUtils.isBlank(uuid)) {
                    String newUUID = UUID.randomUUID().toString();
                    map.put("CONTENT_UUID", newUUID);
                    D630List.add(map);
                }
            }


        } catch (Exception e) {
            System.out.print(e.getMessage());
        }


    }

}
