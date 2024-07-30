package dxctest;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<Object, Object> {

    public Object handleRequest(final Object input, final Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");
        try {
            //Fetch SSM Param Value
            SsmClient ssmClient = SsmClient.builder().region(Region.US_EAST_1).build();
            GetParameterResponse parameterResponse = ssmClient.getParameter(GetParameterRequest.builder()
                    .name("UserName").withDecryption(false)
                    .build());
            String paramValue = parameterResponse.parameter().value();

            //Write SSM Param Value to file in S3 Bucket
            S3Client s3Client = S3Client.builder().region(Region.US_EAST_1).build();
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket("arbo-dxctest").key("ssmParam.txt").build();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(paramValue.getBytes()));
            System.out.println("SSM Param has been put in S3 Bucket successfully");
            return new GatewayResponse("Success", headers, 200);
        } catch (Exception e) {
            return new GatewayResponse("Failure", headers, 500);
        }
    }

    private String getPageContents(String address) throws IOException{
        URL url = new URL(address);
        try(BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
