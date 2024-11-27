package controller;

import java.io.InputStream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;

import model.S3EventParam;
import service.BrokeDocxService;
import utils.S3EventUtils;

// Handler value: example.HandlerInteger
public class BrokeDocxController implements RequestHandler<S3Event, String> {

	@Override
	public String handleRequest(S3Event event, Context context) {

		LambdaLogger logger = context.getLogger();

		S3EventUtils s3EventUtils = new S3EventUtils();
		S3EventParam s3EventParam = s3EventUtils.getS3EventParam(event,context);
		String srcKey = s3EventParam.getSrcKey();
		String srcBucket = s3EventParam.getSrcBucket();

		if (!srcKey.endsWith(".docx")) {
			// 不是docx
			logger.log("不是docx: " + srcKey);
			return "不是docx: " + srcKey;
		}
		
		AmazonS3 client = AmazonS3ClientBuilder.standard().build();;
		S3Object xFile = client.getObject(srcBucket, srcKey);
		InputStream contents = xFile.getObjectContent();
		
		new BrokeDocxService().breakDocx(contents);
		
		return srcBucket + "/" + srcKey;
	}
}
