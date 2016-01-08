package org.influxdb.inflow;

import com.google.gson.Gson;
import java.util.concurrent.TimeUnit;
import org.influxdb.InfluxDB.RetentionPolicy;
import org.influxdb.dto.Point;
import org.influxdb.dto.QueryResult;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.Test;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.testng.Assert.assertEquals;

public class DatabaseTest extends AbstractTest {

  protected String dataToInsert;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    this.resultData = this.loadResourceFileDataAsString("/result.example.json");

    // return these databases when calling listDatabases()
    Mockito.when(this.mockClient.listDatabases()).thenAnswer(new Answer<String[]>() {
      @Override
      public String[] answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        return new String[]{"test123", "test"};
      }
    });

    this.dataToInsert = this.loadResourceFileDataAsString("/input.example.json");
  }

  @Test
  public void testGetterInstances() {
    assertThat(this.database.getClient(), instanceOf(Client.class));
    assertThat(this.database.getQueryBuilder(), instanceOf(QueryBuilder.class));
  }

  @Test
  public void testQueries() throws InflowException {
    Gson gson = new Gson();
    QueryResult testQueryResult = gson.fromJson(this.resultData, QueryResult.class);
    assertEquals(this.database.query("SELECT * FROM test_metric"), testQueryResult);
    this.database.drop();
    assertEquals("DROP DATABASE influx_test_db", Client.getLastQuery());

  }

  @Test
  public void testRetentionPolicyQueries() throws Exception {
    RetentionPolicy retentionPolicy = this.getTestRetentionPolicy();

    assertEquals(
            this.getTestDatabase().createRetentionPolicy(retentionPolicy),
            this.getEmptyQueryResult()
    );

    this.database.listRetentionPolicies();
    assertEquals("SHOW RETENTION POLICIES ON " + this.TEST_TARGET_DATABSENAME,
            Client.lastQuery
    );

    this.database.alterRetentionPolicy(this.getTestRetentionPolicy());
    assertEquals("ALTER RETENTION POLICY test_retention_policy ON " + this.TEST_TARGET_DATABSENAME + " DURATION 1d REPLICATION 1 DEFAULT",
            Client.lastQuery
    );
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEmptyDatabaseName() {
    Database d = new Database(null, this.mockClient);
  }

  @Test
  public void testCreate() throws InflowDatabaseException, InflowException {
    // test create with retention policy
    this.database.create(this.getTestRetentionPolicy(), true);
    assertEquals("CREATE RETENTION POLICY test_retention_policy ON " + this.TEST_TARGET_DATABSENAME + " DURATION 1d REPLICATION 1 DEFAULT",
            Client.lastQuery
    );

    // test creating a database without create if not exists
    this.database.create(null, true);
    assertEquals("CREATE DATABASE IF NOT EXISTS " + this.TEST_TARGET_DATABSENAME, Client.lastQuery);

    // test creating a database without create if not exists
    this.database.create(null, false);
    assertEquals("CREATE DATABASE " + this.TEST_TARGET_DATABSENAME, Client.lastQuery);
  }

  @Test(expectedExceptions = InflowException.class)
  public void testCreateException() throws InflowException, InflowDatabaseException {
    // test an exception being handled correctly
    doThrow(new Exception()).when(this.mockClient).query(this.TEST_TARGET_DATABSENAME, anyString());
    this.database.create(this.getTestRetentionPolicy(), false);
  }


  @Test
  public void testExists() throws InflowException {

    Database database = new Database("test", this.mockClient);

    assertEquals(database.exists(), true);
  }


  @Test
  public void testNotExists() throws InflowException {

    Database database = new Database("test_not_exists", this.mockClient);

    assertEquals(database.exists(), false);
  }

  @Test
  public void testWritePointsInASingleCall() throws InflowException {

    Point point1 = Point
            .measurement("cpu_load_short")
            .field("value", 0.64)
            .tag("host", "server01")
            .tag("region", "us-west")
            .field("cpucount", 10)
            .time(1435222310, TimeUnit.SECONDS) // 2015-06-25 08:51:50 GMT
            .build();

    Point point2 = Point
            .measurement("cpu_load_short")
            .field("value", 0.84)
            .build();

    // check call runs without error (driver is mocked driver)
    this.database.writePoints(new Point[]{point1, point2});

    // TODO: mock call and confirm methods
  }

  protected Database getTestDatabase(String name) throws Exception, Exception {
    return new Database(name, this.getMockClientThatReturnsEmptyQueryResult());
  }

  protected Database getTestDatabase() throws Exception {
    return getTestDatabase("test");
  }

  protected RetentionPolicy getTestRetentionPolicy(String name) {
    return new RetentionPolicy(name, "1d", 1, true);
  }

  protected RetentionPolicy getTestRetentionPolicy() {
    return getTestRetentionPolicy("test_retention_policy");
  }

}
