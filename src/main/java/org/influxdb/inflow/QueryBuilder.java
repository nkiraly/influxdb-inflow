package org.influxdb.inflow;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.influxdb.dto.QueryResult;

/**
 * Class Builder
 *
 * Abstraction class for getting time series out of InfluxDB
 *
 * Sample usage:
 *
 * qb = new QueryBuilder(db);
 * qb->percentile(95)->setTimeRange(timeFrom, timeTo)->getResult();
 *
 * QueryResult result = qb->select("*")->from("*")->getResult();
 *
 * @todo add inner join
 * @todo add merge
 *
 */
public class QueryBuilder {

  protected Database db;

  protected String selection = "*";

  protected List<String> where;

  protected String startTime;

  protected String endTime;

  protected String metric;

  protected String limitClause;

  protected List<String> groupBy;

  public QueryBuilder(Database db) {

    this.db = db;

    this.where = new ArrayList<>();
    
    this.groupBy = new ArrayList<>();
  }

  /**
   * @param metric The metric to select (required)
   */
  public QueryBuilder from(String metric) {

    this.metric = metric;

    return this;
  }

  /**
   * Custom select method
   *
   * example:
   *
   * qb->select("sum(value),")
   *
   * @param customSelect
   */
  public QueryBuilder select(String customSelect) {

    this.selection = customSelect;

    return this;
  }

  /**
   * @param conditions
   *
   * Example: String[] { "time > now()", "time < now() -1d" }
   */
  public QueryBuilder where(String[] conditions) {

    for (String condition : conditions) {
      this.where.add(condition);
    }

    return this;
  }
  
  /**
   * @param condition
   *
   * Example: "time < now() -1d"
   */
  public QueryBuilder where(String condition) {

    this.where.add(condition);

    return this;
  }

  public QueryBuilder count(String field) {

    this.selection = String.format("count(%s)", field);

    return this;
  }

  public QueryBuilder count() {

    return this.count("type");
  }

  public QueryBuilder median(String field) {

    this.selection = String.format("median(%s)", field);

    return this;
  }

  public QueryBuilder median() {

    return this.median("type");
  }
  
  public QueryBuilder mean(String field) {

    this.selection = String.format("mean(%s)", field);

    return this;
  }

  public QueryBuilder mean() {

    return this.mean("type");
  }
  
  public QueryBuilder sum(String field) {

    this.selection = String.format("sum(%s)", field);

    return this;
  }

  public QueryBuilder sum() {

    return this.sum("type");
  }

  public QueryBuilder first(String field) {

    this.selection = String.format("first(%s)", field);

    return this;
  }

  public QueryBuilder first() {

    return this.first("type");
  }
  
  public QueryBuilder last(String field) {

    this.selection = String.format("last(%s)", field);

    return this;
  }

  public QueryBuilder last() {

    return this.last("type");
  }

  public QueryBuilder groupBy(String field) {

    this.groupBy.add(field);

    return this;
  }

  /**
   * Set the time range to select data from
   *
   * @param from
   * @param to
   */
  public QueryBuilder setTimeRange(long from, long to) {

    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    String fromDate = df.format(new Date(from));
    String toDate = df.format(new Date(to));

    this.where(String.format("time > '%s'", fromDate));
    this.where(String.format("time < '%s'", toDate));

    return this;
  }

  /**
   * @param percentile Percentage to select (e.g. 95 for 95th percentile)
   */
  public QueryBuilder percentile(int percentile) {

    this.selection = String.format("percentile(value, %d)", percentile);

    return this;
  }

  /**
   * Limit the QueryResult to n records
   *
   * @param count
   */
  public QueryBuilder limit(int count) {

    this.limitClause = String.format(" LIMIT %s", count);

    return this;
  }

  public String getQuery() {

    return this.parseQuery();
  }

  /**
   * Get the result from the database (builds and runs the query)
   *
   */
  public QueryResult getQueryResult() throws InflowException {

    return this.db.query(this.parseQuery());
  }

  protected String parseQuery() {

    String query = String.format("SELECT %s FROM %s", this.selection, this.metric);

    if (this.metric == null) {
      throw new IllegalArgumentException("No metric provided to from()");
    }

    for (int i = 0; i < this.where.size(); i++) {
      String clause = "WHERE";

      if (i > 0) {
        clause = "AND";
      }

      clause += this.where.get(i);

      query += " " + selection + " " + clause;

    }

    if (this.groupBy.size() > 0) {
      query += " GROUP BY ";

      for (String groupItem : this.groupBy) {
        query += groupItem + ",";
      }

      // cut off last comma
      query = query.substring(0, query.length() - 2);
    }

    if (this.limitClause != null) {
      query += this.limitClause;
    }

    return query;
  }
}
