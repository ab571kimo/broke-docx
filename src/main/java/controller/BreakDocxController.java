package controller;

import java.io.InputStream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;

import service.BreakDocxService;

// Handler value: example.HandlerInteger
public class BreakDocxController implements RequestHandler<S3Event, String> {

	@Override
	public String handleRequest(S3Event event, Context context) {

		LambdaLogger logger = context.getLogger();
		S3EventNotificationRecord record = event.getRecords().get(0);
		String srcBucket = record.getS3().getBucket().getName();
		// Object key may have spaces or unicode non-ASCII characters.
		String srcKey = record.getS3().getObject().getUrlDecodedKey();
		logger.log("RECORD: " + record);
		logger.log("SOURCE BUCKET: " + srcBucket);
		logger.log("SOURCE KEY: " + srcKey);
		// log execution details
		
		if (!srcKey.endsWith(".docx")) {
			// 不是docx
			logger.log("不是docx: " + srcKey);
			return null;
		}
		
		AmazonS3 client = AmazonS3ClientBuilder.standard().build();;
		S3Object xFile = client.getObject(srcBucket, srcKey);
		InputStream contents = xFile.getObjectContent();
		
		new BreakDocxService().breakDocx(contents);
		
		return srcBucket + "/" + srcKey;
	}
}
