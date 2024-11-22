package model;

import lombok.Data;

@Data
public class S3EventParam {

    private String srcBucket;

    private  String srcKey ;

    private  String region ;

    private  String fileUrl ;

}
