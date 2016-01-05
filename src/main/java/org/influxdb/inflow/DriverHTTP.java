package org.influxdb.inflow;

import org.influxdb.InfluxDBHTTPErrorHandler;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.squareup.okhttp.OkHttpClient;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.InfluxDB.RetentionPolicy;
import org.influxdb.TimeUtil;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import retrofit.client.Client;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.RestAdapter;
import retrofit.client.Header;
import retrofit.mime.TypedString;
import org.influxdb.InfluxDBHTTPInterface;
import org.influxdb.inflow.BatchProcessor.BatchEntry;

public class DriverHTTP implements DriverInterface, QueryDriverInterface {

  protected Map<String, String> parameters;

  protected String uri;
  protected String username;
  protected String password;
  protected Client client;
  protected RestAdapter restAdapter;
  protected InfluxDBHTTPInterface restService;

  private BatchProcessor batchProcessor;
  private final AtomicBoolean batchEnabled = new AtomicBoolean(false);
  private final AtomicLong writeCount = new AtomicLong();
  private final AtomicLong unBatchedCount = new AtomicLong();
  private final AtomicLong batchedCount = new AtomicLong();

  public DriverHTTP(final String uri, final String username, final String password, Client client) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(uri), "URI can not be null or empty");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(username), "Username can not null or empty");

    this.uri = uri;
    this.username = username;
    this.password = password;

    if (this.client == null) {
      this.client = new OkClient(new OkHttpClient());
    } else {
      this.client = client;
    }

    this.restAdapter = new RestAdapter.Builder()
            .setEndpoint(this.uri)
            .setErrorHandler(new InfluxDBHTTPErrorHandler())
            .setClient(this.client)
            .build();
    this.restService = this.restAdapter.create(InfluxDBHTTPInterface.class);
  }

  public DriverHTTP(final String uri, final String username, final String password) {
    this(uri, username, password, null);
  }

  public Pong ping() {
    Stopwatch watch = Stopwatch.createStarted();
    Response response = this.restService.ping();
    List<Header> headers = response.getHeaders();
    String version = "unknown";
    for (Header header : headers) {
      if (null != header.getName() && header.getName().equalsIgnoreCase("X-Influxdb-Version")) {
        version = header.getValue();
      }
    }
    Pong pong = new Pong();
    pong.setVersion(version);
    pong.setResponseTime(watch.elapsed(TimeUnit.MILLISECONDS));
    return pong;
  }

  public String version() {
    return ping().getVersion();
  }

  @Override
  public void write(final String database, final RetentionPolicy retentionPolicy, final Point point) throws InflowException {
    if (this.batchEnabled.get()) {
      BatchEntry batchEntry = new BatchEntry(point, database, retentionPolicy);
      this.batchProcessor.put(batchEntry);
    } else {
      BatchPoints batchPoints = BatchPoints.database(database).retentionPolicy(retentionPolicy).build();
      batchPoints.point(point);
      this.write(batchPoints);
      this.unBatchedCount.incrementAndGet();
    }
    this.writeCount.incrementAndGet();
  }

  @Override
  public void write(final String database, final RetentionPolicy retentionPolicy, final ConsistencyLevel consistency, final String records) {
    restService.writePoints(
            this.username,
            this.password,
            database,
            retentionPolicy.toString(),
            TimeUtil.toTimePrecision(TimeUnit.NANOSECONDS),
            consistency.toString(),
            new TypedString(records));
  }

  @Override
  public void write(final String database, final RetentionPolicy retentionPolicy, final ConsistencyLevel consistency, final List<String> records) {
    final String joinedRecords = Joiner.on("\n").join(records);
    write(database, retentionPolicy, consistency, joinedRecords);
  }
  
  @Override
  public void write(final BatchPoints batchPoints) {
    this.batchedCount.addAndGet(batchPoints.getPoints().size());
    TypedString lineProtocol = new TypedString(batchPoints.lineProtocol());
    restService.writePoints(
            this.username,
            this.password,
            batchPoints.getDatabase(),
            batchPoints.getRetentionPolicy().toString(),
            TimeUtil.toTimePrecision(TimeUnit.NANOSECONDS),
            batchPoints.getConsistency().toString(),
            lineProtocol);

  }

  @Override
  public QueryResult query(final Query query) {
    QueryResult response = this.restService
            .query(this.username, this.password, query.getDatabase(), query.getCommand());
    return response;
  }

  @Override
  public QueryResult query(final Query query, final TimeUnit timeUnit) {
    QueryResult response = this.restService
            .query(this.username, this.password, query.getDatabase(), TimeUtil.toTimePrecision(timeUnit), query.getCommand());
    return response;
  }

}
