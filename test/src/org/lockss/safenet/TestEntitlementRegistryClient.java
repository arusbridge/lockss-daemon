package org.lockss.safenet;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;

import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.test.MockLockssUrlConnection;
import org.lockss.test.StringInputStream;
import org.lockss.util.urlconn.LockssUrlConnection;

public class TestEntitlementRegistryClient extends LockssTestCase {
  private MockEntitlementRegistryClient client;

  private Map<String,String> validParams;

  public void setUp() throws Exception {
    super.setUp();
    client = new MockEntitlementRegistryClient();
    MockLockssDaemon daemon = getMockLockssDaemon();
    daemon.setEntitlementRegistryClient(client);
    daemon.setDaemonInited(true);
    Properties p = new Properties();
    p.setProperty(BaseEntitlementRegistryClient.PARAM_ER_URI, "http://dev-safenet.edina.ac.uk");
    p.setProperty(BaseEntitlementRegistryClient.PARAM_ER_APIKEY, "00000000-0000-0000-0000-000000000000");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    client.initService(daemon);
    client.startService();

    validParams = new HashMap<String, String>();
    validParams.put("api_key", "00000000-0000-0000-0000-000000000000");
    validParams.put("identifier_value", "0123-456X");
    validParams.put("institution", "11111111-1111-1111-1111-111111111111");
    validParams.put("start", "20120101");
    validParams.put("end", "20151231");
  }

  public void testEntitlementRegistryError() throws Exception {
    client.expectAndReturn(client.mapToPairs(validParams), 500, "Internal server error");

    try {
      client.isUserEntitled("0123-456X", "11111111-1111-1111-1111-111111111111", "20120101", "20151231");
      fail("Expected exception not thrown");
    }
    catch(IOException e) {
      assertEquals("Error communicating with entitlement registry. Response was 500 null", e.getMessage());
    }
    client.checkDone();
  }

  public void testEntitlementRegistryInvalidResponse() throws Exception {
    client.expectAndReturn(client.mapToPairs(validParams), 200, "[]");

    try {
      client.isUserEntitled("0123-456X", "11111111-1111-1111-1111-111111111111", "20120101", "20151231");
      fail("Expected exception not thrown");
    }
    catch(IOException e) {
      assertEquals("No matching entitlements returned from entitlement registry", e.getMessage());
    }
    client.checkDone();
  }

  public void testEntitlementRegistryInvalidJson() throws Exception {
    client.expectAndReturn(client.mapToPairs(validParams), 200, "[{\"this\": isn't, JSON}]");

    try {
      client.isUserEntitled("0123-456X", "11111111-1111-1111-1111-111111111111", "20120101", "20151231");
      fail("Expected exception not thrown");
    }
    catch(IOException e) {
      assertTrue(e.getMessage().startsWith("Unrecognized token 'isn': was expecting ('true', 'false' or 'null')"));
    }
    client.checkDone();
  }

  public void testEntitlementRegistryUnexpectedJson() throws Exception {
    client.expectAndReturn(client.mapToPairs(validParams), 200, "{\"surprise\": \"object\"}");

    try {
      client.isUserEntitled("0123-456X", "11111111-1111-1111-1111-111111111111", "20120101", "20151231");
      fail("Expected exception not thrown");
    }
    catch(IOException e) {
      assertTrue(e.getMessage().startsWith("No matching entitlements returned from entitlement registry"));
    }
    client.checkDone();
  }

  public void testUserEntitled() throws Exception {
    client.expectValidSingleEntitlement(validParams);

    assertTrue(client.isUserEntitled("0123-456X", "11111111-1111-1111-1111-111111111111", "20120101", "20151231"));
    client.checkDone();
  }

  public void testUserNotEntitled() throws Exception {
    client.expectValidNoResponse(validParams);

    assertFalse(client.isUserEntitled("0123-456X", "11111111-1111-1111-1111-111111111111", "20120101", "20151231"));
    client.checkDone();
  }

  private static class MockEntitlementRegistryClient extends BaseEntitlementRegistryClient {
    private static class Expectation {
      private List<NameValuePair> params;
      private int responseCode;
      private String response;
    }

    private Queue<Expectation> expected = new LinkedList<Expectation>();

    public void expectValidSingleEntitlement(Map<String, String> params) throws IOException {
      Map<String, String> responseParams = new HashMap<String,String>(params);
      responseParams.remove("api_key");
      expectAndReturn(mapToPairs(params), 200, "[" + mapToJson(responseParams) + "]");
    }

    public void expectValidNoResponse(Map<String, String> params) throws IOException {
      expectAndReturn(mapToPairs(params), 204, "");
    }

    public void expectAndReturn(List<NameValuePair> params, int responseCode, String response) {
      Expectation e = new Expectation();
      e.params = params;
      e.responseCode = responseCode;
      e.response = response;
      expected.add(e);
    }

    public void checkDone() {
      assertEmpty(expected);
    }

    protected LockssUrlConnection openConnection(String url) throws IOException {
      try {
        URIBuilder builder = new URIBuilder(url);
        assertEquals("http", builder.getScheme());
        assertEquals("dev-safenet.edina.ac.uk", builder.getHost());
        assertEquals("/entitlements", builder.getPath());
        Expectation e = expected.poll();
        assertSameElements(e.params, builder.getQueryParams());
        MockLockssUrlConnection connection = new MockLockssUrlConnection(url);
        connection.setResponseCode(e.responseCode);
        connection.setResponseInputStream(new StringInputStream(e.response));
        return connection;
      }
      catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }

    public String mapToJson(Map<String, String> params) throws IOException {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.writeValueAsString(params);
    }

    public List<NameValuePair> mapToPairs(Map<String, String> params) {
      List<NameValuePair> pairs = new ArrayList<NameValuePair>();
      for(String key : params.keySet()) {
        pairs.add(new BasicNameValuePair(key, params.get(key)));
      }
      return pairs;
    }

  }
}


