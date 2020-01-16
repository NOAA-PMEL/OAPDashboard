/**
 * 
 */
package gov.noaa.pmel.dashboard.test.server;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import gov.noaa.pmel.tws.util.JWhich;

/**
 * @author kamb
 *
 */
public class TestPoster {

    private String _url;

    public TestPoster(String url) {
        _url = url;
    }
    
    public void doPost(String msg) throws Exception {
        HttpClient client = HttpClientBuilder.create().build();
        JWhich.which(HttpClient.class.getName());
        HttpPost post = new HttpPost(_url);
        post.setEntity(new StringEntity(msg));
        HttpResponse response = client.execute(post);
        System.out.println(response);
//        URL url = new URL(_url);
//        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
//        connection.setDoOutput(true);
    }
    
    public static void main(String[] args) {
        String url = "http://localhost:8888/DBdServices/EditFlags/PRIS082008";
        File testMsgFile = new File("/Users/kamb/neo-work/oap_qc_las/data/test/testMsg3.json");
        try {
            String msg = FileUtils.readFileToString(testMsgFile);
            TestPoster tp = new TestPoster(url);
            tp.doPost(msg);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
