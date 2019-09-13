package no.difa.eik;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import com.amazonaws.ApacheHttpClientConfig;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.PrivateKeyDetails;
import org.apache.http.ssl.PrivateKeyStrategy;
import org.apache.http.ssl.SSLContexts;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.annotation.Order;
import org.springframework.util.ResourceUtils;

public class MinioTest {
  private static String bucketName = "test";
  private static String keyName = "healthTreatment.json";
  private static String uploadFileName = "healthTreatment.json";
  private static AWSCredentials credentials;
  private static ClientConfiguration clientConfiguration;
  private static AmazonS3 s3Client;

  @Before
  public void setUp() {

  }

  @Test
  @Order(2)
  public void testputFileFromBucket() throws Exception {
    AWSCredentials credentials = new BasicAWSCredentials("6AZSGIU7HD0OY9ZI1TCD", "46BCItoMHFxto7lYMrz3HkjnQiEh9MTwZ+qfmBod");
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    clientConfiguration.setSignerOverride("AWSS3V4SignerType");
    String CERT_ALIAS = "sysco", CERT_PASSWORD = "server";
    KeyStore identityKeyStore = KeyStore.getInstance("jks");
    FileInputStream identityKeyStoreFile = new FileInputStream(new File("certs/identity.jks"));
    identityKeyStore.load(identityKeyStoreFile, "client".toCharArray());

    KeyStore trustKeyStore = KeyStore.getInstance("jks");
    FileInputStream trustKeyStoreFile = new FileInputStream(new File("certs/truststore.jks"));
    trustKeyStore.load(trustKeyStoreFile, "server".toCharArray());

    SSLContext sslContext = SSLContexts.custom()
        // load identity keystore
        .loadKeyMaterial(identityKeyStore, "client".toCharArray(), new PrivateKeyStrategy() {
          @Override
          public String chooseAlias(Map<String, PrivateKeyDetails> aliases, Socket socket) {
            return "capgemini";
          }
        })
        // load trust keystore
        .loadTrustMaterial(trustKeyStore, null)
        .build();



    SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext,
        new String[]{"TLSv1.2", "TLSv1.1"},
        null,
        NoopHostnameVerifier.INSTANCE);
    clientConfiguration.getApacheHttpClientConfig().withSslSocketFactory(sslConnectionSocketFactory);

    s3Client = AmazonS3ClientBuilder
        .standard()
        .withEndpointConfiguration(
            new AwsClientBuilder.EndpointConfiguration("https://localhost:8092",
                Regions.US_EAST_1.name()))
        .withPathStyleAccessEnabled(true)
        .withClientConfiguration(clientConfiguration)
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .build();

    try {
      System.out.println("Uploading a new object to S3 from a file\n");
      ClassLoader cl = ClassLoader.getSystemClassLoader();

      URL[] urls = ((URLClassLoader)cl).getURLs();

      File file = ResourceUtils.getFile("src/test/resources/healthTreatment.json");
      // Upload file
      s3Client.putObject(new PutObjectRequest(bucketName, keyName, file));
      assertEquals("passed testputFileFromBucket", true, true);

    } catch (Exception e) {
      throw e;
    }
  }

  @Test
  @Order(1)
  public void testMtlsConnection () throws Exception {
    String CERT_ALIAS = "sysco", CERT_PASSWORD = "server";
    KeyStore identityKeyStore = KeyStore.getInstance("jks");
    FileInputStream identityKeyStoreFile = new FileInputStream(new File("certs/identity.jks"));
    identityKeyStore.load(identityKeyStoreFile, "client".toCharArray());

    KeyStore trustKeyStore = KeyStore.getInstance("jks");
    FileInputStream trustKeyStoreFile = new FileInputStream(new File("certs/truststore.jks"));
    trustKeyStore.load(trustKeyStoreFile, "server".toCharArray());

    SSLContext sslContext = SSLContexts.custom()
        // load identity keystore
        .loadKeyMaterial(identityKeyStore, "client".toCharArray(), new PrivateKeyStrategy() {
          @Override
          public String chooseAlias(Map<String, PrivateKeyDetails> aliases, Socket socket) {
            return "capgemini";
          }
        })
        // load trust keystore
        .loadTrustMaterial(trustKeyStore, null)
        .build();



    SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext,
        new String[]{"TLSv1.2", "TLSv1.1"},
        null,
        NoopHostnameVerifier.INSTANCE);

    final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
        .register("http", new PlainConnectionSocketFactory())
        .register("https", sslConnectionSocketFactory)
        .build();

    final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
    cm.setMaxTotal(100);

    CloseableHttpClient client = HttpClients.custom()
        .setConnectionManager(cm)
        .setSSLSocketFactory(sslConnectionSocketFactory)
        .build();

    // Call a SSL-endpoint
    callEndPoint (client, "https://localhost:8092/",
        new JSONObject()
            .put("param1", "value1")
            .put("param2", "value2")
    );
  }

  private static void callEndPoint (CloseableHttpClient aHTTPClient, String aEndPointURL, JSONObject aPostParams) throws Exception{
    System.out.println("Calling URL: " + aEndPointURL);
    HttpPost post = new HttpPost(aEndPointURL);
    post.setHeader("Accept", "application/json");
    post.setHeader("Content-type", "application/json");

    StringEntity entity = new StringEntity(aPostParams.toString());
    post.setEntity(entity);

    System.out.println("**POST** request Url: " + post.getURI());
    System.out.println("Parameters : " + aPostParams);

    HttpResponse response = aHTTPClient.execute(post);

    int responseCode = response.getStatusLine().getStatusCode();
    System.out.println("Response Code: " + responseCode);
    assertEquals(405, responseCode);
    System.out.println("Content:-\n");
    BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
    String line = "";
    while ((line = rd.readLine()) != null) {
      System.out.println(line);
    }
  }

  @Test
  @Order(3)
  public void testGetFileFromBucket () throws Exception {

    try {
      AWSCredentials credentials = new BasicAWSCredentials("6AZSGIU7HD0OY9ZI1TCD", "46BCItoMHFxto7lYMrz3HkjnQiEh9MTwZ+qfmBod");
      ClientConfiguration clientConfiguration = new ClientConfiguration();
      clientConfiguration.setSignerOverride("AWSS3V4SignerType");
      String CERT_ALIAS = "sysco", CERT_PASSWORD = "server";
      KeyStore identityKeyStore = KeyStore.getInstance("jks");
      FileInputStream identityKeyStoreFile = new FileInputStream(new File("certs/identity.jks"));
      identityKeyStore.load(identityKeyStoreFile, "client".toCharArray());

      KeyStore trustKeyStore = KeyStore.getInstance("jks");
      FileInputStream trustKeyStoreFile = new FileInputStream(new File("certs/truststore.jks"));
      trustKeyStore.load(trustKeyStoreFile, "server".toCharArray());

      SSLContext sslContext = SSLContexts.custom()
          // load identity keystore
          .loadKeyMaterial(identityKeyStore, "client".toCharArray(), new PrivateKeyStrategy() {
            @Override
            public String chooseAlias(Map<String, PrivateKeyDetails> aliases, Socket socket) {
              return "capgemini";
            }
          })
          // load trust keystore
          .loadTrustMaterial(trustKeyStore, null)
          .build();



      SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext,
          new String[]{"TLSv1.2", "TLSv1.1"},
          null,
          NoopHostnameVerifier.INSTANCE);
      clientConfiguration.getApacheHttpClientConfig().withSslSocketFactory(sslConnectionSocketFactory);

      s3Client = AmazonS3ClientBuilder
          .standard()
          .withEndpointConfiguration(
              new AwsClientBuilder.EndpointConfiguration("https://localhost:8092",
                  Regions.US_EAST_1.name()))
          .withPathStyleAccessEnabled(true)
          .withClientConfiguration(clientConfiguration)
          .withCredentials(new AWSStaticCredentialsProvider(credentials))
          .build();
      GetObjectRequest rangeObjectRequest = new GetObjectRequest(bucketName, keyName);
      S3Object objectPortion = s3Client.getObject(rangeObjectRequest);
      System.out.println("Printing bytes retrieved:");
      displayTextInputStream(objectPortion.getObjectContent());
      assertEquals("passed testGetFileFromBucket", true, true);

    } catch (Exception e) {
      if (e instanceof AmazonS3Exception) {
        Map details = ((AmazonS3Exception) e).getAdditionalDetails();
        String response = ((AmazonS3Exception) e).getRawResponseContent();
        Map headers = ((AmazonS3Exception) e).getHttpHeaders();
        System.out.println(details);
        System.out.println(response);
        System.out.println(headers);
      }
      throw e;
    }

  }

  private static void displayTextInputStream(InputStream input) throws IOException {
    // Read one text line at a time and display.
    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
    while (true) {
      String line = reader.readLine();
      if (line == null)
        break;

      System.out.println("    " + line);
    }
    System.out.println();
  }
}
