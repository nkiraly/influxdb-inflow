package org.influxdb.inflow;

import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.influxdb.InfluxDB;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public abstract class AbstractTest {

  protected Client mockClient;

  /**
   * these various TEST_TARGET_ variables are compared for consistency in the various tests.
   *
   * when testing with your own values:
   *
   * if you change a value, change it in all the TEST_TARGET_ entries * to match
   */
  protected String TEST_TARGET_HOSTNAME = "inflow.local";
  
  protected int TEST_TARGET_PORT = 8086;

  protected String TEST_TARGET_DATABSENAME = "inflow_test_db";
  
  protected String TEST_TARGET_USERNAME = "inflow_user";
  
  protected String TEST_TARGET_PASSWORD = "inflow_password";

  protected String TEST_TARGET_URL = "http://inflow.local:8086";

  protected String TEST_TARGET_URI = "https+influxdb://inflow_user:inflow_password@inflow.local:8086/";

  protected String TEST_TARGET_URI_WITH_DB = "https+influxdb://inflow_user:inflow_password@inflow.local:8086/inflow_test_db";

  protected String EMPTY_RESULT_JSON = "{\"results\":[{}]}";

  protected Database database;

  public void beforeSuite() throws Exception {
    this.mockClient = Mockito.mock(Client.class);
    // return mockClient when Client constructor called with TEST_TARGET_HOSTNAME
    PowerMockito.whenNew(Client.class)
            .withArguments(TEST_TARGET_HOSTNAME)
            .thenReturn(this.mockClient);    

    // return TEST_TARGET_URL when calling getBaseURI()
    Mockito.when(this.mockClient.getBaseURI()).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        return TEST_TARGET_URL;
      }
    });

    // return QueryResult based on result.example.json when calling query() with any params
    final String resultJson = this.loadResourceFileDataAsString("/result.example.json");
    Mockito.when(this.mockClient.query(anyString(), anyString()))
            .thenAnswer(new Answer<QueryResult>() {
              @Override
              public QueryResult answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if ( args[0] != null ) {
                  String databaseName = args[0].toString();
                }
                if ( args[1] != null ) {
                  Client.setLastQuery(args[1].toString());
                }
                Gson gson = new Gson();
                QueryResult qr = gson.fromJson(resultJson, QueryResult.class);
                return qr;
              }
            });

    // have mockClient use the mockDriverThatDoesNothing
    DriverInterface mockDriver = this.getMockDriverThatDoesNothing();
    this.mockClient.setDriver(mockDriver);
  }
  
  protected String loadResourceFileDataAsString(String resourceFileName) throws IOException {
    assertNotNull(getClass().getResource(resourceFileName), "Test Resource File " + resourceFileName + " not found on class path");
    File resultDataFile = new File(getClass().getResource(resourceFileName).getFile());
    assertTrue(resultDataFile.exists(), "Test Resource File " + resourceFileName + " does not exist");
    String contents = FileUtils.readFileToString(resultDataFile);
    return contents;
  }

  public String getEmptyResult() {
    return this.EMPTY_RESULT_JSON;
  }
  
  public QueryResult getEmptyQueryResult() {
    Gson gson = new Gson();
    QueryResult eqr = gson.fromJson(this.getEmptyResult(), QueryResult.class);
    return eqr;
  }

  public Client getMockClient() throws Exception {
    Client client = Mockito.mock(Client.class);
    // return mockClient when Client constructor called with TEST_TARGET_HOSTNAME
    PowerMockito.whenNew(Client.class)
            .withArguments(TEST_TARGET_HOSTNAME)
            .thenReturn(client);
    return client;
  }
  
  public Client getMockClientThatReturnsEmptyQueryResult() throws Exception {
    Client client = this.getMockClient();

    // when calling query
    // with any parameters
    // return empty result data object
    Mockito.when(client.query(anyString(), anyString()))
            .thenAnswer(new Answer<QueryResult>() {
              @Override
              public QueryResult answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if ( args[0] != null ) {
                  String databaseName = args[0].toString();
                }
                if ( args[1] != null ) {
                  Client.setLastQuery(args[1].toString());
                }
                return getEmptyQueryResult();
              }
            });

    return client;
  }
  
  public DriverInterface getMockDriverThatDoesNothing() throws InflowException {
    DriverOnlyStubs mockDriver = Mockito.mock(DriverOnlyStubs.class);

    // NOTICE: same order as interface definition, keep it that way if you add more
    // stub .write() variants to do nothing
    Mockito.doNothing().when(mockDriver).write(Mockito.anyString(), Mockito.any(InfluxDB.RetentionPolicy.class), Mockito.any(InfluxDB.ConsistencyLevel.class), Mockito.anyString());
    Mockito.doNothing().when(mockDriver).write(Mockito.anyString(), Mockito.any(InfluxDB.RetentionPolicy.class), Mockito.any(InfluxDB.ConsistencyLevel.class), Mockito.any(List.class));
    Mockito.doNothing().when(mockDriver).write(Mockito.anyString(), Mockito.any(InfluxDB.RetentionPolicy.class), Mockito.any(Point.class));
    Mockito.doNothing().when(mockDriver).write(Mockito.any(BatchPoints.class));
    // stub .query() QueryDriverInterface methods to return empty QueryResult
    Mockito.when(mockDriver.query(any(Query.class)))
            .thenAnswer(new Answer<QueryResult>() {
              @Override
              public QueryResult answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if ( args[0] != null ) {
                  String databaseName = args[0].toString();
                }
                if ( args[1] != null ) {
                  Client.setLastQuery(args[1].toString());
                }
                return getEmptyQueryResult();
              }
            });
    Mockito.when(mockDriver.query(any(Query.class), any(TimeUnit.class)))
            .thenAnswer(new Answer<QueryResult>() {
              @Override
              public QueryResult answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if ( args[0] != null ) {
                  String databaseName = args[0].toString();
                }
                if ( args[1] != null ) {
                  Client.setLastQuery(args[1].toString());
                }
                return getEmptyQueryResult();
              }
            });

    return mockDriver;
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
                  Query argQuery = (Query)args[0];
                  Client.setLastQuery(argQuery.getCommand());
                }
                Gson gson = new Gson();
                QueryResult qr = gson.fromJson(databasesJson, QueryResult.class);
                return qr;
              }
            });

    return mockDriver;
  }

}
