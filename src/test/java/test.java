import java.io.FileInputStream;

import service.BrokeDocxService;

public class test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		//國泰世華商業銀行_民法準則.docx
		//D:\人壽供測試用內規\供測試用內規\紅利分配辦法.docx
		try(FileInputStream inputStream = new FileInputStream("D:\\國泰世華商業銀行_民法準則.docx");){
			BrokeDocxService BreakDocxService = new BrokeDocxService();
			BreakDocxService.breakDocx(inputStream);
			
		}catch(Exception e) {
			System.out.print(e.getMessage());
		}
		
		
		
		
	}

}
