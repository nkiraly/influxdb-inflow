package com.koadweb.inflow;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.RetentionPolicy;
import org.influxdb.dto.Point;
import org.influxdb.dto.QueryResult;
import org.influxdb.dto.QueryResult.Result;
import org.influxdb.dto.QueryResult.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Database {

  private final static Logger logger = LoggerFactory.getLogger(Client.class);

  protected String name;
  protected Client client;

  public Database(String name, Client client) {
    if (name == null) {
      throw new IllegalArgumentException("Database name is null");
    }
    if (name.length() == 0) {
      throw new IllegalArgumentException("Database name is zero length");
    }
    this.name = name;
    this.client = client;
  }
  
  /**
   * Build the database object from a URI.
   *
   * Examples:
   *
   * https://username:pass@localhost:8086/databasename
   * udp://username:pass@localhost:4444/databasename
   *
   */
  public static Database fromURI(String uri, int timeout, boolean verifySSL) throws InflowException {
    logger.debug("Database.fromURI() " + uri);

    Client client = Client.fromURI(uri, timeout, verifySSL);

    String databaseName = null;

    URI u;
    try {
      u = new URI(uri);
    } catch (URISyntaxException use) {
      throw new InflowException("Malformed URI:" + use.getMessage(), use);
    }

    if (!u.getPath().isEmpty()) {
      databaseName = u.getPath().substring(1);
    }

    return new Database(databaseName, client);
  }

  public static Database fromURI(String uri, int timeout) throws InflowException {
    return Database.fromURI(uri, timeout, false);
  }

  public static Database fromURI(String uri) throws InflowException {
    return Database.fromURI(uri, 0);
  }

  public String getName() {
    return this.name;
  }

  public QueryResult query(String query) throws InflowException {
    return this.client.query(this.name, query);
  }

  /**
   * Create this database
   *
   * @param retentionPolicy
   * @param createIfNotExists Only create the database if it does not yet exist
   */
  public QueryResult create(RetentionPolicy retentionPolicy, boolean createIfNotExists) throws InflowDatabaseException {
    QueryResult queryResult = null;
    try {
      String query = String.format(
              "CREATE DATABASE %s%s",
              (createIfNotExists ? "IF NOT EXISTS " : ""),
              this.name
      );

      queryResult = this.query(query);

      if (retentionPolicy != null) {
        this.createRetentionPolicy(retentionPolicy);
      }
    } catch (Exception ex) {
      throw new InflowDatabaseException("Failed to created database %s" + this.name + "\n" + ex.getMessage(), ex);
    }
    return queryResult;
  }

  public QueryResult create(RetentionPolicy retentionPolicy) throws InflowDatabaseException {
    return this.create(retentionPolicy, true);
  }

  public QueryResult create() throws InflowDatabaseException {
    return this.create(null);
  }

  public void writePoints(Point[] points, TimeUnit precision) throws InflowException {

    List<String> lines = new ArrayList<>();
    for (Point point : points) {
      lines.add(point.lineProtocol());
    }

    // TODO: get retention polcy and consistency levels passed
    // or refactor a write() that does not require them and use them as defined in the driver?
    this.client.driver.write(
            this.name,
            new RetentionPolicy("default"),
            InfluxDB.ConsistencyLevel.ONE,
            lines
    );
  }

  public void writePoints(Point[] points) throws InflowException {
    this.writePoints(points, TimeUnit.NANOSECONDS);
  }
  
  public void writePoint(Point point, TimeUnit precision) throws InflowException {

    // TODO: get retention polcy and consistency levels passed
    // or refactor a write() that does not require them and use them as defined in the driver?
    this.client.driver.write(
            this.name,
            new RetentionPolicy("default"),
            point
    );
  }
  
  public void writePoint(Point point) throws InflowException {
    this.writePoint(point, TimeUnit.NANOSECONDS);
  }

  public boolean exists() throws InflowException {
    Series databaseSeries = this.client.listDatabases();
    String[] databases = databaseSeries.getValuesAsStringArray();
    return Arrays.asList(databases).contains(this.name);
  }

  
  public QueryResult createRetentionPolicy(RetentionPolicy retentionPolicy) throws InflowException {
    return this.query(RetentionPolicy.toQueryString("CREATE", retentionPolicy, this.name));
  }

  public QueryResult alterRetentionPolicy(RetentionPolicy retentionPolicy) throws InflowException {
    return this.query(RetentionPolicy.toQueryString("ALTER", retentionPolicy, this.name));
  }

  public Series listRetentionPolicies() throws InflowException {
    QueryResult queryResult = this.query(String.format("SHOW RETENTION POLICIES ON %s", this.name));
    List<Result> results = queryResult.getResults();
    Result result = results.get(0);
    List<Series> series = result.getSeries();
    Series serie = series.get(0);
    return serie;
    // callers can use series.getValuesAsStringArray();
  }

  public QueryResult drop() throws InflowException {
    return this.query(String.format("DROP DATABASE %s", this.name));
  }

  /**
   * Retrieve a query builder for this database
   *
   */
  public QueryBuilder getQueryBuilder() {
    return new QueryBuilder(this);
  }

  public Client getClient() {
    return this.client;
  }

}
