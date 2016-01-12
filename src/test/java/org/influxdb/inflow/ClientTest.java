package org.influxdb.inflow;

import com.google.gson.Gson;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

public class ClientTest extends AbstractTest {

  @BeforeSuite()
  @Override
  public void beforeSuite() throws Exception {
    super.beforeSuite();
  }

  @Test
  public void testGetters() {
    Client client = this.getClient(this.TEST_TARGET_USERNAME, this.TEST_TARGET_PASSWORD);

    assertThat(client.getDriver(), instanceOf(DriverInterface.class));

    // confirm basic getters return expected values
    assertEquals(
            client.getBaseURI(),
            this.TEST_TARGET_URL
    );
    assertEquals(
            client.getHost(),
            this.TEST_TARGET_HOSTNAME
    );
    assertEquals(
            client.getTimeout(),
            0
    );
  }

  @Test
  public void testSelectDbShouldReturnDatabaseInstance() throws InflowException {
    Client client = this.getClient(this.TEST_TARGET_USERNAME, this.TEST_TARGET_PASSWORD);

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
  public void testListDatabasesWithMockDriver() throws Exception {
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

    // setup client with mock driver that will list dbs when asked
    Client client = this.getClient(this.TEST_TARGET_USERNAME, this.TEST_TARGET_PASSWORD);
    DriverInterface mockDriver = this.getMockClientThatListsTestDbs();
    client.setDriver(mockDriver);
    
    QueryResult.Series resultSeries = client.listDatabases();

    // compare list database results to expected
    assertEquals(
            resultSeries,
            expectedSeries
    );

    // make sure SHOW DATABASES is the query that was run
    assertEquals(
            Client.getLastQuery(),
            "SHOW DATABASES"
    );
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
    Client client = this.getClient(this.TEST_TARGET_USERNAME, this.TEST_TARGET_PASSWORD);

    // setup a mock driver that always returns responseFile contents as QueryResult object
    DriverOnlyStubs mockDriver = Mockito.mock(DriverOnlyStubs.class);

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
    assertEquals(
            resultSeries,
            expectedSeries
    );
  }
    
  @Test
  public void testURIFactory() throws InflowException {
    // compare clients made with new Client and fromURI factory
    Client referenceClient = new Client(this.TEST_TARGET_HOSTNAME, this.TEST_TARGET_PORT, this.TEST_TARGET_USERNAME, this.TEST_TARGET_PASSWORD);
    Client fromURIClient = Client.fromURI(this.TEST_TARGET_URI);

    assertEquals(
            fromURIClient.getHost(),
            referenceClient.getHost()
    );

    assertEquals(
            fromURIClient.getBaseURI(),
            referenceClient.getBaseURI()
    );
  }

  protected Client getClient(String username, String password, boolean ssl) {

    return new Client(this.TEST_TARGET_HOSTNAME, this.TEST_TARGET_PORT, username, password, ssl);
  }

  protected Client getClient(String username, String password) {

    return this.getClient(username, password, false);
  }

  protected DriverInterface getMockClientThatListsTestDbs() throws Exception {
    Database database = new Database(TEST_TARGET_DATABSENAME, this.getMockClient());

    DriverOnlyStubs mockDriver = Mockito.mock(DriverOnlyStubs.class);

    // when mockDriver.query() with a query object of SHOW DATABASES target null database
    // return a QueryResult deserialzed from databases.example.json
    Query query = new Query("SHOW DATABASES", null);

    final String databasesJson = this.loadResourceFileDataAsString("/databases.example.json");

    Mockito.when(mockDriver.query(eq(query)))
            .thenAnswer(new Answer<QueryResult>() {
              @Override
              public QueryResult answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if (args[0] != null) {
                  Client.setLastQuery(args[0].toString());
                }
                Gson gson = new Gson();
                QueryResult qr = gson.fromJson(databasesJson, QueryResult.class);
                return qr;
              }
            });

    return mockDriver;
  }

}
