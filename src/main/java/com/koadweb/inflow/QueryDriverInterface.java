package com.koadweb.inflow;

import java.util.concurrent.TimeUnit;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

public interface QueryDriverInterface {

  public QueryResult query(final Query query);

  public QueryResult query(final Query query, final TimeUnit timeUnit);

}
