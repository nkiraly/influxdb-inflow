package org.influxdb.inflow;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.influxdb.InfluxDB;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

public class DriverOnlyStubs implements DriverInterface, QueryDriverInterface {

  @Override
  public void write(String database, InfluxDB.RetentionPolicy retentionPolicy, InfluxDB.ConsistencyLevel consistency, String records) throws InflowException {
    throw new UnsupportedOperationException("Testing Interface for mocking - this should not be thrown but mocked during testing.");
  }

  @Override
  public void write(String database, InfluxDB.RetentionPolicy retentionPolicy, InfluxDB.ConsistencyLevel consistency, List<String> records) throws InflowException {
    throw new UnsupportedOperationException("Testing Interface for mocking - this should not be thrown but mocked during testing.");
  }

  @Override
  public void write(String database, InfluxDB.RetentionPolicy retentionPolicy, Point point) throws InflowException {
    throw new UnsupportedOperationException("Testing Interface for mocking - this should not be thrown but mocked during testing.");
  }

  @Override
  public void write(BatchPoints batchPoints) throws InflowException {
    throw new UnsupportedOperationException("Testing Interface for mocking - this should not be thrown but mocked during testing.");
  }

  @Override
  public QueryResult query(Query query) {
    throw new UnsupportedOperationException("Testing Interface for mocking - this should not be thrown but mocked during testing.");
  }

  @Override
  public QueryResult query(Query query, TimeUnit timeUnit) {
    throw new UnsupportedOperationException("Testing Interface for mocking - this should not be thrown but mocked during testing.");
  }
  
}
