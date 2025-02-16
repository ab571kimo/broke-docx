import java.io.File;
import java.io.FileInputStream;
import java.util.*;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import service.BrokeDocxService;

public class test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		//國泰世華商業銀行_民法準則.docx
		//D:\人壽供測試用內規\供測試用內規\紅利分配辦法.docx
		try{
			File file = new File("D:\\國泰世華商業銀行_民法準則2.docx");
			byte[] encodedBytes = Base64.encodeBase64(FileUtils.readFileToByteArray(file));
			FileInputStream inputStream = new FileInputStream(file);
			BrokeDocxService BreakDocxService = new BrokeDocxService();


			List<Map<String, String>> ORI_D620List = BreakDocxService.brokeDocx(inputStream);

			Map<String,Map<String, String>> oriMap = new LinkedHashMap<>();
			for(Map<String, String> map : ORI_D620List){
				oriMap.put(map.get("CONTENT"),map);
			}

			file = new File("D:\\國泰世華商業銀行_民法準則3.docx");
			 encodedBytes = Base64.encodeBase64(FileUtils.readFileToByteArray(file));
			 inputStream = new FileInputStream(file);
			 BreakDocxService = new BrokeDocxService();
			List<Map<String, String>> NEW_D620List = BreakDocxService.brokeDocx(inputStream);

			for(Map<String, String> map : NEW_D620List){
				String key = map.get("CONTENT");
				if(oriMap.containsKey(key)){
					map.put("ORI_PART_NAME", oriMap.get(key).get("PART_NAME"));
					map.put("ORI_CHAPTER_NAME", oriMap.get(key).get("CHAPTER_NAME"));
					map.put("ORI_SECTION_NAME", oriMap.get(key).get("SECTION_NAME"));
					map.put("ORI_ARTICLE_NAME", oriMap.get(key).get("ARTICLE_NAME"));
					map.put("ORI_CONTENT_UUID", oriMap.get(key).get("CONTENT_UUID"));
					oriMap.remove(key); //從oriMap移除處理過的key
				}
			}

			int newListSize = NEW_D620List.size();
			int i = 0;

			for(String key :oriMap.keySet()){
				Map<String, String> map = oriMap.get(key);
				map.put("ORI_PART_NAME",map.get("PART_NAME"));
				map.put("ORI_CHAPTER_NAME",map.get("CHAPTER_NAME"));
				map.put("ORI_SECTION_NAME",map.get("SECTION_NAME"));
				map.put("ORI_ARTICLE_NAME",map.get("ARTICLE_NAME"));
				map.put("ORI_CONTENT_UUID", map.get("CONTENT_UUID"));
				map.put("ORI_CONTENT", map.get("CONTENT"));
				map.remove("PART_NAME");
				map.remove("CHAPTER_NAME");
				map.remove("SECTION_NAME");
				map.remove("ARTICLE_NAME");
				map.remove("CONTENT_UUID");
				map.remove("CONTENT");

				//將map插入NEW_D620List
				int serNo = Integer.parseInt(map.get("SER_NO"));

				if(serNo > newListSize){
					NEW_D620List.add(map);
				}else{
					NEW_D620List.add(serNo+i,map);
					i++;
				}

			}

			int o = 1;
			for (Map<String, String> map : NEW_D620List) {
				map.put("SER_NO", Integer.toString(o));
				o++;
				System.out.println(map);
			}


		}catch(Exception e) {
			System.out.print(e.getMessage());
		}




	}

}
