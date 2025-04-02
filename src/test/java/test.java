import java.io.*;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import service.*;
import utils.VirtualVo;

@Slf4j
public class test {

    public static void main(String[] args) {
        // TODO Auto-generated method stub

        //國泰世華商業銀行_民法準則.docx
        //D:\人壽供測試用內規\供測試用內規\紅利分配辦法.docx
        try {

            //String folderPath = "D:/OneDrive_1_2025-3-14 - 複製/";  // 修改為你的目錄路徑
            String folderPath = "D:/OneDrive_1_2025-4-2/";  // 修改為你的目錄路徑
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


                List<Map> ORI_D620List = BreakDocxService.brokeDocx(inputStream);

                String filePath = folderPath + file.getName() +".txt";

                // 寫入 TXT 檔案
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                    for (Map map : ORI_D620List) {
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




            //new OriLaw().run();
            //new Interpertation().run();
            //new R0B070().run();

            /*

            File file = new File("D:\\國泰世華商業銀行_民法準則2.docx");
            byte[] encodedBytes = Base64.encodeBase64(FileUtils.readFileToByteArray(file));
            FileInputStream inputStream = new FileInputStream(file);
            BrokeDocxService BreakDocxService = new BrokeDocxService();

            Map D600Map = new HashMap();
            D600Map.put("COMP_ID","01");
            D600Map.put("RKB_INR_NO","20250320000000");
            D600Map.put("DIV_NO","0000000");
            D600Map.put("DIV_NAME","國泰世華總部");
            D600Map.put("INR_NAME","民法準則");
            D600Map.put("ANN_DATE","2020-01-01");
            D600Map.put("UPDATE_DATE","2025-02-01");
            D600Map.put("ACTIVE","1");
            D600Map.put("STATUS","10");
            D600Map.put("FLOW_NO","20250320000000");

            List<Map> ORI_D620List = BreakDocxService.brokeDocx(inputStream);

            int x = 0;
            for (Map map : ORI_D620List) {
                map.put("RKB_INR_NO","20250320000000");
                map.put("FLOW_NO","20250320000000");

            }

            //new D06100().save(D600Map,null,ORI_D620List);





            file = new File("D:\\國泰世華商業銀行_民法準則3.docx");
            encodedBytes = Base64.encodeBase64(FileUtils.readFileToByteArray(file));
            inputStream = new FileInputStream(file);
            BreakDocxService = new BrokeDocxService();
            List<Map> NEW_D620List = BreakDocxService.brokeDocx(inputStream);

            NEW_D620List = new D06100().compareRKB_INR(ORI_D620List,NEW_D620List);

            //List<Map<String, String>> rtnList = BreakDocxService.compareRKB_INR(ORI_D620List, new ArrayList());

            List<Map> RealNweList = new ArrayList<>();

            for(Map map : NEW_D620List){
                String PART_NAME = MapUtils.getString(map,"PART_NAME");
                if(StringUtils.isNotBlank(PART_NAME)){
                    RealNweList.add(map);

                }
            }

            D600Map.put("RKB_INR_NO","20250320000000");
            D600Map.put("FLOW_NO","20250320000099");
            D600Map.put("UPDATE_DATE","2025-04-01");

            for (Map map : RealNweList) {
                map.put("RKB_INR_NO","20250320000000");
                map.put("FLOW_NO","20250320000099");

            }

            //new D06100().save(D600Map,null,RealNweList);


            //站存件
            file = new File("D:\\國泰世華商業銀行_民法準則2.docx");
            encodedBytes = Base64.encodeBase64(FileUtils.readFileToByteArray(file));
            inputStream = new FileInputStream(file);
            BreakDocxService = new BrokeDocxService();
            NEW_D620List = BreakDocxService.brokeDocx(inputStream);

            D600Map.put("COMP_ID","01");
            D600Map.put("RKB_INR_NO","20250320000000");
            D600Map.put("UPDATE_DATE","2025-07-01");
            D600Map.put("ACTIVE","1");
            D600Map.put("STATUS","01");
            D600Map.put("FLOW_NO","20250320000999");

            for (Map map : NEW_D620List) {
                map.put("RKB_INR_NO","20250320000000");
                map.put("FLOW_NO","20250320000999");
            }


            VirtualVo D620 = new D06100().getVo("DBXR", "DTXRD699");
            D620.setCondition("COMP_ID","01");
            D620.setCondition("RKB_INR_NO","20250320000000");
            D620.setCondition("FLOW_NO","20250320000099");
            D620.setColumn("*");
            List<Map> theORI_D620List = D620.select(false);

            NEW_D620List = new D06100().compareRKB_INR(theORI_D620List,NEW_D620List);

            RealNweList = new ArrayList<>();

            for(Map map : NEW_D620List){
                String PART_NAME = MapUtils.getString(map,"PART_NAME");
                if(StringUtils.isNotBlank(PART_NAME)){
                    RealNweList.add(map);

                }
            }

            //new D06100().save(D600Map,null,RealNweList);

*/

        } catch (Exception e) {
            log.debug("",e);
            System.out.print(e.getMessage());
        }


    }

}
