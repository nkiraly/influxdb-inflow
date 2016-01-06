package org.influxdb.inflow;

import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.influxdb.InfluxDB;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.QueryResult;
import static org.mockito.Matchers.anyString;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public abstract class AbstractTest {

  protected Client mockClient;
  
  protected DriverInterface mockDriver;

  protected String TEST_ARG_LOCALHOST = "localhost";
  
  protected String TEST_ARG_DATABSENAME = "influxtestdb";

  protected String emptyResult = "{\"results\":[{}]}";

  protected String resultData;

  protected QueryResult mockQueryResult;

  protected Database database;

  public void setUp() throws Exception {
    this.mockClient = Mockito.mock(Client.class);
    // return mockClient when Client constructor called with TEST_ARG_LOCALHOST
    PowerMockito.whenNew(Client.class)
            .withArguments(TEST_ARG_LOCALHOST)
            .thenReturn(this.mockClient);

    // load result example json
    this.resultData = this.loadResourceFileDataAsString("/result.example.json");

    // return this string when calling getBaseURI()
    Mockito.when(this.mockClient.getBaseURI()).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        return "http://localhost:8086";
      }
    });

    // return resultData QueryResult when calling query
    Mockito.when(this.mockClient.query(anyString(), anyString())).thenAnswer(new Answer<QueryResult>() {
      @Override
      public QueryResult answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        String databaseName = args[0].toString();
        Client.setLastQuery(args[1].toString());
        Gson gson = new Gson();
        QueryResult qr = gson.fromJson(resultData, QueryResult.class);
        return qr;
      }
    });

    // mock and stub the DriverInterface the client is using
    this.mockDriver = Mockito.mock(DriverInterface.class);
    
    // stub driver write variants to do nothing
    // NOTICE: same order as interface definition, keep it that way if you add more
    Mockito.doNothing().when(this.mockDriver).write(Mockito.anyString(), Mockito.any(InfluxDB.RetentionPolicy.class), Mockito.any(InfluxDB.ConsistencyLevel.class), Mockito.anyString());
    Mockito.doNothing().when(this.mockDriver).write(Mockito.anyString(), Mockito.any(InfluxDB.RetentionPolicy.class), Mockito.any(InfluxDB.ConsistencyLevel.class), Mockito.any(List.class));
    Mockito.doNothing().when(this.mockDriver).write(Mockito.anyString(), Mockito.any(InfluxDB.RetentionPolicy.class), Mockito.any(Point.class));
    Mockito.doNothing().when(this.mockDriver).write(Mockito.any(BatchPoints.class));

    // specify mockDriver as the driver to use for mockClient
    this.mockClient.setDriver(this.mockDriver);

    this.database = new Database(TEST_ARG_DATABSENAME, this.mockClient);
  }
  
  protected String loadResourceFileDataAsString(String resourceFileName) throws IOException {
    assertNotNull(getClass().getResource(resourceFileName), "Test Resource File " + resourceFileName + " not found on class path");
    File resultDataFile = new File(getClass().getResource(resourceFileName).getFile());
    assertTrue(resultDataFile.exists(), "Test Resource File " + resourceFileName + " does not exist");
    String contents = FileUtils.readFileToString(resultDataFile);
    return contents;
  }

  public QueryResult getMockQueryResult() {
    return this.mockQueryResult;
  }

  public QueryResult setMockQueryResult(QueryResult mockQueryResult) {
    this.mockQueryResult = mockQueryResult;
    return this.mockQueryResult;
  }

  public String getEmptyResult() {
    return this.emptyResult;
  }

  public Client getClientMock(boolean queryReturnsEmptyResult) throws Exception {
    Client client = Mockito.mock(Client.class);
    // return mockClient when Client constructor called with TEST_ARG_LOCALHOST
    PowerMockito.whenNew(Client.class)
            .withArguments(TEST_ARG_LOCALHOST)
            .thenReturn(client);

    // if specified to return emptyResult
    if (queryReturnsEmptyResult) {

      // return resultData QueryResult when calling query
      Mockito.when(client.query(anyString(), anyString())).thenAnswer(new Answer<QueryResult>() {
        @Override
        public QueryResult answer(InvocationOnMock invocation) throws Throwable {
          Object[] args = invocation.getArguments();
          Gson gson = new Gson();
          QueryResult qr = gson.fromJson(emptyResult, QueryResult.class);
          return qr;
        }
      });
    }

    return client;
  }

  public Client getClientMock() throws Exception {
    return this.getClientMock(false);
  }
}
