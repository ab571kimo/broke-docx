package controller;

import java.io.InputStream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;

import model.S3EventParam;
import service.BrokeDocxService;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import utils.S3EventUtils;

// Handler value: example.HandlerInteger
public class BrokeDocxController implements RequestHandler<S3Event, String> {

	private final S3Client s3Client = S3Client.builder().build();

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

		// Create the S3 client
		S3Client s3Client = S3Client.builder().build();

		// Build the GetObjectRequest
		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(srcBucket)
				.key(srcKey)
				.build();

		InputStream contents = s3Client.getObject(getObjectRequest);

		String breakDocx = new BrokeDocxService().breakDocx(contents);

		s3Client.putObject(PutObjectRequest.builder().bucket(srcBucket).key(srcKey+".csv").build(), RequestBody.fromString(breakDocx));

		return srcBucket + "/" + srcKey;
	}
}
