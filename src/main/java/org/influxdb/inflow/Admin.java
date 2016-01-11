package org.influxdb.inflow;

import org.influxdb.InfluxDB.UserPrivilege;
import org.influxdb.dto.QueryResult;

public class Admin {

  protected Client client;

  public Admin(Client client) {
    this.client = client;
  }

  public QueryResult createUser(String username, String password, UserPrivilege privilege) throws InflowException {
    String query = String.format("CREATE USER %s WITH PASSWORD '%s'", username, password);

    if (privilege != null) {
      query += " WITH " + privilege + " PRIVILEGES";
    }

    return this.client.query(null, query);
  }

  public QueryResult createUser(String username, String password) throws InflowException {
    return this.createUser(username, password, null);
  }

  public QueryResult dropUser(String username) throws InflowException {
    String query = "DROP USER " + username;
    return this.client.query(null, query);
  }

  public QueryResult changeUserPassword(String username, String newPassword) throws InflowException {
    String query = String.format("SET PASSWORD FOR %s = '%s'", username, newPassword);
    return this.client.query(null, query);
  }

  public QueryResult showUsers() throws InflowException {
    String query = "SHOW USERS";
    return this.client.query(null, query);
  }

  public QueryResult grant(UserPrivilege privilege, String username, String database) throws InflowException {
    return this.executePrivilege("GRANT", privilege, username, database);
  }

  public QueryResult grant(UserPrivilege privilege, String username) throws InflowException {
    return this.grant(privilege, username, null);
  }

  public QueryResult revoke(UserPrivilege privilege, String username, String database) throws InflowException {
    return this.executePrivilege("REVOKE", privilege, username, database);
  }

  public QueryResult revoke(UserPrivilege privilege, String username) throws InflowException {
    return this.revoke(privilege, username, null);
  }

  protected QueryResult executePrivilege(String type, UserPrivilege privilege, String username, String database) throws InflowException {
    if (privilege != UserPrivilege.ALL && database == null) {
      throw new InflowException("Only grant ALL cluster-wide privileges are allowed");
    }

    String query = String.format("%s %s", type, privilege);

    if (database != null) {
      query += " ON " + database;
    } else {
      query += " PRIVILEGES ";
    }

    if (username != null) {
      if (type.equals("GRANT")) {
        query += "TO " + username;
      } else if (type.equals("REVOKE")) {
        query += "FROM " + username;
      }
    }

    return this.client.query(null, query);
  }

  protected QueryResult executePrivilege(String type, UserPrivilege privilege, String username) {
    return this.executePrivilege(type, privilege, username);
  }
}
