package service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.apache.commons.collections4.MapUtils;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Interpertation {

    public void run() throws Exception {

        String ORI_FILE_PATH = "D:/testRKB/File/"; //extMap.get("FILE_PATH") + "File/"; TODO

        String FILE_PATH = "D:/testRKB/File2/"; //XR_Z10600.parseFILE_PATH("XRR0B071"); TODO

        //每一份檔案代表一部法規，內含多版本
        File fileJ = new File("D:\\FL000565.json");

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
            String RKB_EXT_NO = MapUtils.getString(Data, "LawID");
            String FLOW_NO = MapUtils.getString(Data, "AmendDate") + "000" + MapUtils.getString(Data, "SerialNo");


        }


    }

    int x = 0;
}
