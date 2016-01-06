package org.influxdb;

public interface InfluxDB {

  /**
   * Controls the level of logging of the REST layer.
   */
  public enum LogLevel {
    /**
     * No logging.
     */
    NONE,
    /**
     * Log only the request method and URL and the response status code and execution time.
     */
    BASIC,
    /**
     * Log the basic information along with request and response headers.
     */
    HEADERS,
    /**
     * Log the headers, body, and metadata for both requests and responses.
     * <p>
     * Note: This requires that the entire request and response body be buffered in memory!
     */
    FULL;
  }

  /**
   * ConsistencyLevel for write Operations.
   */
  public enum ConsistencyLevel {
    /**
     * Write succeeds only if write reached all cluster members.
     */
    ALL("all"),
    /**
     * Write succeeds if write reached any cluster members.
     */
    ANY("any"),
    /**
     * Write succeeds if write reached at least one cluster members.
     */
    ONE("one"),
    /**
     * Write succeeds only if write reached a quorum of cluster members.
     */
    QUORUM("quorum");
    private final String level;

    private ConsistencyLevel(final String level) {
      this.level = level;
    }
    
    public boolean equals(String privilege) {
      if (privilege == null) {
        return false;
      }
      return this.level.equals(privilege);
    }

    public boolean equals(ConsistencyLevel consistencyLevel) {
      if (consistencyLevel == null) {
        return false;
      }
      return this.level.equals(consistencyLevel.toString());
    }

    @Override
    public String toString() {
      return this.level;
    }
  }
  
  public enum UserPrivilege {

    READ("READ"),
    WRITE("WRITE"),
    ALL("ALL");

    private final String privilege;

    private UserPrivilege(String privilege) {
      this.privilege = privilege;
    }

    public boolean equals(String privilege) {
      if (privilege == null) {
        return false;
      }
      return this.privilege.equals(privilege);
    }

    public boolean equals(UserPrivilege userPrivilege) {
      if (userPrivilege == null) {
        return false;
      }
      return this.privilege.equals(userPrivilege.toString());
    }

    @Override
    public String toString() {
      return this.privilege;
    }

  }
  
  public class RetentionPolicy {

    protected String name;
    protected String duration;
    protected int replication;
    protected boolean isDefault;

    public RetentionPolicy(String name, String duration, int replication, boolean isDefault) {
      this.name = name;
      this.duration = duration;
      this.replication = replication;
      this.isDefault = isDefault;
    }

    public RetentionPolicy(String name, String duration, int replication) {
      this(name, duration, replication, false);
    }

    public RetentionPolicy(String name, String duration) {
      this(name, duration, 1);
    }

    public RetentionPolicy(String name) {
      this(name, "1d");
    }

    public static String toQueryString(String method, RetentionPolicy retentionPolicy, String databaseName) {
      String query = String.format(
              "%s RETENTION POLICY %s ON %s DURATION %s REPLICATION %s",
              method,
              retentionPolicy.name,
              databaseName,
              retentionPolicy.duration,
              retentionPolicy.replication
      );
      if (retentionPolicy.isDefault) {
        query += " DEFAULT";
      }
      return query;
    }
    
    @Override
    public String toString() {
      return this.name;
    }

  }

}
