package utils;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import model.S3EventParam;

public class S3EventUtils {

    public S3EventParam getS3EventParam(S3Event event,Context context) {

        S3EventNotification.S3EventNotificationRecord record = event.getRecords().getFirst();
        String srcBucket = record.getS3().getBucket().getName();
        String srcKey = record.getS3().getObject().getUrlDecodedKey();
        String region = record.getAwsRegion();
        String fileUrl = "s3://" + srcBucket + "/" + srcKey;

        LambdaLogger logger = context.getLogger();
        logger.log("RECORD: " + record);
        logger.log("SOURCE BUCKET: " + srcBucket);
        logger.log("SOURCE KEY: " + srcKey);
        logger.log("S3 Region: " + region);
        logger.log("FILE URL: " + fileUrl);

        S3EventParam s3EventParam = new S3EventParam();

        s3EventParam.setSrcBucket(srcBucket);
        s3EventParam.setSrcKey(srcKey);
        s3EventParam.setRegion(region);
        s3EventParam.setFileUrl(fileUrl);

        return s3EventParam;
    }

}
