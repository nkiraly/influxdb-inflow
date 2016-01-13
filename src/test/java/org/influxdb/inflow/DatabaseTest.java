package org.influxdb.inflow;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import org.influxdb.InfluxDB.RetentionPolicy;
import org.influxdb.dto.Point;
import org.influxdb.dto.QueryResult;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

public class DatabaseTest extends AbstractTest {

  @BeforeSuite()
  @Override
  public void beforeSuite() throws Exception {
    super.beforeSuite();
    
    this.database = new Database(TEST_TARGET_DATABSENAME, this.mockClient);
  }

  @Test
  public void testGetterInstances() {
    assertThat(this.database.getClient(), instanceOf(Client.class));
    assertThat(this.database.getQueryBuilder(), instanceOf(QueryBuilder.class));
  }
  
  @Test
  public void testURIFactory() throws InflowException {
    // compare database objects made with selectDB() and fromURI()
    // the reference database object is made from referenceClient
    Database referenceDatabase = new Database(
            this.TEST_TARGET_DATABSENAME,
            new Client(this.TEST_TARGET_HOSTNAME, this.TEST_TARGET_PORT, this.TEST_TARGET_USERNAME, this.TEST_TARGET_PASSWORD)
    );
    Database fromURIDatabase = Database.fromURI(this.TEST_TARGET_URI_WITH_DB);

    assertEquals(
            referenceDatabase.getName(),
            fromURIDatabase.getName()
    );

    assertEquals(
            fromURIDatabase.getClient().getHost(),
            referenceDatabase.getClient().getHost()
    );

    assertEquals(
            fromURIDatabase.getClient().getBaseURI(),
            referenceDatabase.getClient().getBaseURI()
    );
  }

  @Test
  public void testSelectQuery() throws InflowException, IOException {
    final String resultJson = this.loadResourceFileDataAsString("/result.example.json");

    Gson gson = new Gson();
    QueryResult testQueryResult = gson.fromJson(resultJson, QueryResult.class);

    assertEquals(this.database.query("SELECT * FROM test_metric"), testQueryResult);

    assertEquals(Client.getLastQuery(), "SELECT * FROM test_metric");
  }
  
  @Test
  public void testDrop() throws InflowException, IOException {
    this.database.drop();
    assertEquals("DROP DATABASE " + this.TEST_TARGET_DATABSENAME, Client.getLastQuery());
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

  @Test
  public void testExists() throws InflowException, Exception {
    
    // use mock driver that will list dbs based on databases.example.json when asked
    DriverInterface mockDriver = this.getMockClientThatListsTestDbs();

    Client client = new Client(this.TEST_TARGET_HOSTNAME, this.TEST_TARGET_PORT, this.TEST_TARGET_USERNAME, this.TEST_TARGET_PASSWORD);
    client.setDriver(mockDriver);
    
    // create db object for test db with mocked client that will return it
    Database database = new Database("test", client);

    assertEquals(database.exists(), true);
  }

  @Test
  public void testNotExists() throws InflowException, Exception {

    // use mock driver that will list dbs based on databases.example.json when asked
    DriverInterface mockDriver = this.getMockClientThatListsTestDbs();

    Client client = new Client(this.TEST_TARGET_HOSTNAME, this.TEST_TARGET_PORT, this.TEST_TARGET_USERNAME, this.TEST_TARGET_PASSWORD);
    client.setDriver(mockDriver);
    
    // create db object for test db with mocked client that will not return it
    Database database = new Database("test_not_exists", client);

    assertEquals(database.exists(), false);
  }

  @Test
  public void testWritePointsInASingleCall() throws InflowException, IOException {
    
    String pointsJson = this.loadResourceFileDataAsString("/input.example.json");

    // points to insert same as input.example.json
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

    // use nop mock driver to verify calls
    Client client = new Client(this.TEST_TARGET_HOSTNAME, this.TEST_TARGET_PORT, this.TEST_TARGET_USERNAME, this.TEST_TARGET_PASSWORD);

    DriverInterface mockDriver = this.getMockDriverThatDoesNothing();

    client.setDriver(mockDriver);

    Database database = new Database(TEST_TARGET_DATABSENAME, client);

    // check call runs without error (driver is mocked driver)
    database.writePoints(new Point[]{point1, point2});

    // TODO: confirm driver write() calls made correctly
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
