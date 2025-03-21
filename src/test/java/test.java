import java.io.*;
import java.util.*;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import service.BrokeDocxService;
import service.OriLaw;

public class test {

    public static void main(String[] args) {
        // TODO Auto-generated method stub

        //國泰世華商業銀行_民法準則.docx
        //D:\人壽供測試用內規\供測試用內規\紅利分配辦法.docx
        try {

            //String folderPath = "D:/OneDrive_1_2025-3-14 - 複製/";  // 修改為你的目錄路徑
 /*           String folderPath = "D:/OneDrive_1_2025-3-14 - 複製/";  // 修改為你的目錄路徑
            String outputFilePath = folderPath + File.separator + "merged_output.txt";

            File folder = new File(folderPath);
            File[] files = folder.listFiles();

            //for(int i = 0;i<files.length;i++){
            for(File file : files){

                if (file.isFile() && file.getName().toLowerCase().endsWith(".doc")) {
                    System.out.println("非docx " + file.getName());
                    continue;
                }

                FileInputStream inputStream = new FileInputStream(file);
                BrokeDocxService BreakDocxService = new BrokeDocxService();


                List<Map<String, String>> ORI_D620List = BreakDocxService.brokeDocx(inputStream);

                String filePath = folderPath + file.getName() +".txt";

                // 寫入 TXT 檔案
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                    for (Map<String, String> map : ORI_D620List) {
                        // 每個 Map 寫成一行
                        writer.write(map.toString());
                        writer.newLine(); // 換行
                        writer.newLine(); // 換行
                    }
                    System.out.println("檔案寫入成功: " + filePath);
                } catch (Exception e) {
                    System.out.println(file.getName() +e.getMessage());
                }

            }
*/

            new OriLaw().run();


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

            List<Map<String, String>> rtnList = BreakDocxService.compareRKB_INR(ORI_D620List, new ArrayList());


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
