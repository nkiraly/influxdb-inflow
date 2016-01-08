package org.influxdb.inflow;

import com.google.gson.Gson;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.Test;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.testng.Assert.assertEquals;

public class ClientTest extends AbstractTest {

  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  protected Client client = null;

  @Test
  public void testGetters() {
    Client client = this.getClient();

    assertThat(client.getDriver(), instanceOf(DriverInterface.class));

    assertEquals(this.TEST_TARGET_URL, client.getBaseURI());
    assertEquals(this.TEST_TARGET_HOSTNAME, client.getHost());
    assertEquals('0', client.getTimeout());
  }

  @Test
  public void testSelectDbShouldReturnDatabaseInstance() throws InflowException {
    Client client = this.getClient();

    Database database = client.selectDB(this.TEST_TARGET_DATABSENAME);

    assertThat(database, instanceOf(Database.class));

    assertEquals(this.TEST_TARGET_DATABSENAME, database.getName());
  }

  /**
   * Test that SSL = true changes connection to HTTPS
   */
  @Test
  public void testSecureInstance() throws URISyntaxException {
    Client client = this.getClient(this.TEST_TARGET_USERNAME, this.TEST_TARGET_PASSWORD, true);

    String clientBaseURI = client.getBaseURI();

    URI u;
    u = new URI(clientBaseURI);

    assertEquals("https", u.getScheme());
  }

  @Test
  public void testGetLastQuery() throws InflowException {
    this.mockClient.query("test", "SELECT * from test_metric");
    assertEquals(Client.getLastQuery(), "SELECT * from test_metric");
  }

  @Test
  public void testListDatabases() throws Exception {
    // create databases.example.json as Series object
    QueryResult.Series expectedSeries = new QueryResult.Series();

    expectedSeries.setName("databases");

    List<String> expectedColumns = new ArrayList<>();
    expectedColumns.add("name");
    expectedSeries.setColumns(expectedColumns);

    List<List<Object>> values = new ArrayList<>();

    List<Object> testObject = new ArrayList<>();
    testObject.add("test");
    values.add(testObject);

    List<Object> test1Object = new ArrayList<>();
    test1Object.add("test1");
    values.add(test1Object);

    List<Object> test2Object = new ArrayList<>();
    test2Object.add("test2");
    values.add(test2Object);

    expectedSeries.setValues(values);

    this.doTestResponse("/databases.example.json", "listDatabases", expectedSeries);
  }

  @Test
  public void testListUsers() throws Exception {
    // create users.example.json as Series object
    QueryResult.Series expectedSeries = new QueryResult.Series();

    List<String> expectedColumns = new ArrayList<>();
    expectedColumns.add("user");
    expectedColumns.add("admin");

    expectedSeries.setColumns(expectedColumns);

    this.doTestResponse(
            "/users.example.json",
            "listUsers",
            expectedSeries
    );
  }

  protected void doTestResponse(String responseFile, String method, QueryResult.Series expectedSeries) throws Exception {
    Client client = this.getClient();

    // mock and stub the QueryDriverInterface the client will use
    DriverOnlyStubs mockDriver = Mockito.mock(DriverOnlyStubs.class);

    // return responseFile contents calling driver query with any parameter
    client.setDriver(mockDriver);
    final String expectedResponseJson = this.loadResourceFileDataAsString(responseFile);
    Mockito.when(mockDriver.query(any(Query.class))).thenAnswer(new Answer<QueryResult>() {
      @Override
      public QueryResult answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        Client.setLastQuery(((Query)args[0]).getCommand());
        Gson gson = new Gson();
        QueryResult qr = gson.fromJson(expectedResponseJson, QueryResult.class);
        return qr;
      }
    });
    
    // specify the client to use the mockDriver we just built to return the expected response
    client.setDriver(mockDriver);

    Class noParam[] = {};
    //Class[] oneStringParam= new Class[1];
    //oneStringParam[0] = String.class;

    Method callMethod = Client.class.getDeclaredMethod(method, noParam);
    QueryResult.Series resultSeries = (QueryResult.Series) callMethod.invoke(client, null);
    assertEquals(expectedSeries, resultSeries);
  }
    
    @Test
  public void testURIFactorys() throws InflowException
    {
        Client referenceClient = this.getClient(this.TEST_TARGET_USERNAME, this.TEST_TARGET_PASSWORD, true);

        Client fromURIClient = Client.fromURI(this.TEST_TARGET_DSN);

        assertEquals(referenceClient, fromURIClient);

        // this is in here testing Database.fromURI because
        // the reference database object is made from referenceClient
        Database referenceDatabase = client.selectDB(this.TEST_TARGET_DATABSENAME);

        Database fromURIDatabase = Database.fromURI(this.TEST_TARGET_DSN_WITH_DB);

        assertEquals(referenceDatabase, fromURIDatabase);

    }

  protected Client getClient(String username, String password, boolean ssl) {

    return new Client(this.TEST_TARGET_HOSTNAME, 8086, username, password, ssl);
  }

  protected Client getClient(String username, String password) {

    return this.getClient(username, password, false);
  }

  protected Client getClient(String username) {

    return this.getClient(username, "");
  }

  protected Client getClient() {

    return this.getClient("");
  }

}
