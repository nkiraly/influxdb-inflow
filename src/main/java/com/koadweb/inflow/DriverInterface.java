package com.koadweb.inflow;

import java.util.List;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.InfluxDB.RetentionPolicy;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;

public interface DriverInterface {

  public void write(final String database, final RetentionPolicy retentionPolicy, final ConsistencyLevel consistency, final String records) throws InflowException;

  public void write(final String database, final RetentionPolicy retentionPolicy, final ConsistencyLevel consistency, final List<String> records) throws InflowException;
  
  public void write(final String database, final RetentionPolicy retentionPolicy, final Point point) throws InflowException;
  
  public void write(final BatchPoints batchPoints) throws InflowException;

}
