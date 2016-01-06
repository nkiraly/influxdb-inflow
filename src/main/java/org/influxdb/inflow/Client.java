package org.influxdb.inflow;

import com.squareup.okhttp.OkHttpClient;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.influxdb.InfluxDB;
import retrofit.client.OkClient;
import org.influxdb.InfluxDBHTTPInterface;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

public class Client {

  public Admin admin;

  protected String host;
  protected int port = 8086;
  protected String username;
  protected String password;

  protected int timeout = 0;

  protected String scheme = "http";

  protected boolean verifySSL = false;

  protected boolean useUdp = false;

  protected int udpPort = 4444;

  protected String baseURI;

  //TODO: refactor as APIClient after port
  protected InfluxDBHTTPInterface httpClient;

  protected Map<String, Object> options;

  protected DriverInterface driver;

  protected static String lastQuery;

  public Client(String host, int port, String username, String password, boolean ssl, boolean verifySSL, int timeout) {
    this.host = host;
    this.port = port;
    this.username = username;
    this.password = password;
    this.timeout = timeout;
    this.verifySSL = verifySSL;

    this.options = new HashMap<>();

    if (ssl) {
      this.scheme = "https";
      this.options.put("verify", this.verifySSL);
    }

    // the the base URI
    this.baseURI = String.format("%s://%s:%d", this.scheme, this.host, this.port);

    // set the default driver to HTTP
    OkHttpClient okHttpClient = new OkHttpClient();
    okHttpClient.setReadTimeout(timeout, TimeUnit.SECONDS);
    // TODO: how to enforce verifySSL with OkHttpClient ?
    OkClient okClient = new OkClient(okHttpClient);
    this.driver = new DriverHTTP(this.baseURI, this.username, this.password, okClient);

    this.admin = new Admin(this);
  }

  public Client(String host, int port, String username, String password, boolean ssl, boolean verifySSL) {
    this(host, port, username, password, ssl, verifySSL, 0);
  }
  public Client(String host, int port, String username, String password, boolean ssl) {
    this(host, port, username, password, ssl, true);
  }
  public Client(String host, int port, String username, String password) {
    this(host, port, username, password, false);
  }
  public Client(String host, int port, String username) {
    this(host, port, username, null);
  }
  public Client(String host, int port) {
    this(host, port, null);
  }
  public Client(String host) {
    this(host, 8086);
  }

  /**
   * Use the specified database
   *
   * @param name
   * @return
   */
  public Database selectDB(String name) throws InflowException {
    return new Database(name, this);
  }
 
  public void write(final String database, final InfluxDB.RetentionPolicy retentionPolicy, final InfluxDB.ConsistencyLevel consistency, final String records) throws InflowException {
    this.driver.write(database, retentionPolicy, consistency, records);
  }

  public void write(final String database, final InfluxDB.RetentionPolicy retentionPolicy, final InfluxDB.ConsistencyLevel consistency, final List<String> records) throws InflowException {
    this.driver.write(database, retentionPolicy, consistency, records);
  }
  
  public void write(final String database, final InfluxDB.RetentionPolicy retentionPolicy, final Point point) throws InflowException {
    this.driver.write(database, retentionPolicy, point);
  }
  
  public void write(final BatchPoints batchPoints) throws InflowException {
    this.driver.write(batchPoints);
  }
  
  public QueryDriverInterface getQueryDriver() throws InflowException {
    if (this.driver instanceof QueryDriverInterface) {
      // driver class supports query
    } else {
      throw new InflowException("Currently configured driver does not support query operations");
    }
    return (QueryDriverInterface)this.driver;
  }

  /**
   * Query influxDB.
   * See how this is called by Database.query() calls
   *
   */
  public QueryResult query(String databaseName, String query) throws InflowException {

    QueryDriverInterface queryDriver = this.getQueryDriver();

    Client.lastQuery = query;

    Query queryObject = new Query(query, databaseName);

    return queryDriver.query(queryObject);
  }

  /**
   * List all the databases
   */
  public QueryResult.Series listDatabases() throws InflowException {
    QueryResult queryResult = this.query(null, "SHOW DATABASES");
    QueryResult.Result result = queryResult.getResults().get(0);
    QueryResult.Series series = result.getSeries().get(0);
    return series;
    // callers can use series.getValuesAsStringArray();
  }

  /**
   * List all the users
   *
   */
  public QueryResult.Series listUsers() throws InflowException {
    QueryResult queryResult = this.query(null, "SHOW USERS");
    QueryResult.Result result = queryResult.getResults().get(0);
    QueryResult.Series series = result.getSeries().get(0);
    return series;
    // calle1s can use series.getValuesAsStringArray();
  }

  /**
   * Build the client from a URI.
   * 
   * Examples:
   *
   * https+influxdb://username:pass@localhost:8086/databasename
   * udp+influxdb://username:pass@localhost:4444/databasename
   *
   */
  public static Client fromURI(String uri, int timeout, boolean verifySSL) throws InflowException {

    URI u;
    try {
      u = new URI(uri);
    } catch (URISyntaxException use) {
      throw new InflowException("Malformed DSN URI:" + use.getMessage(), use);
    }

    String[] schemeInfo = u.getScheme().split("+");
    String modifier = null;
    String scheme = schemeInfo[0].toLowerCase();
    if (schemeInfo.length > 1) {
      scheme = schemeInfo[1].toLowerCase();
    }
    if (!scheme.equals("influxdb")) {
      throw new InflowException("Invalid scheme: " + scheme);
    }
    boolean ssl = false;
    if ( modifier.equals("https")) {
      ssl = true;
    }
    String username = null;
    String password = null;
    if (!u.getUserInfo().isEmpty()) {
      String[] userInfo = u.getUserInfo().split(":");
      username = userInfo[0];
      if (userInfo.length > 1) {
        password = userInfo[1].toLowerCase();
      }
    }
    
    Client client = new Client(
            u.getHost(),
            u.getPort(),
            username,
            password,
            ssl,
            verifySSL,
            timeout
    );

    // set the UDP driver when the DSN specifies UDP
    if (modifier.equals("udp")) {
      client.setDriver(new DriverUDP(u.getHost(), u.getPort()));
    }

    return client;
  }
    
  public static Client fromURI(String uri, int timeout) throws InflowException {
    return Client.fromURI(uri, timeout, false);
  }

  public static Client fromURI(String uri) throws InflowException {
    return Client.fromURI(uri, 0);
  }

  public String getBaseURI() {
    return this.baseURI;
  }

  public int getTimeout() {
    return this.timeout;
  }

  public DriverInterface setDriver(DriverInterface driver) {
    this.driver = driver;
    return this.driver;
  }

  public DriverInterface getDriver() {
    return this.driver;
  }

  public String getHost() {
    return this.host;
  }

  public static String setLastQuery(String query) {
    Client.lastQuery = query;
    return Client.lastQuery;
  }
  public static String getLastQuery() {
    return Client.lastQuery;
  }

}
