package org.influxdb.inflow;

import java.net.URI;
import java.net.URISyntaxException;

public class Database {

  protected String name;
  protected Client client;

  public Database(String name, Client client) throws InflowException {
    if (name == null || name.length() == 0) {
      throw new InflowException("Database name is zero length");
    }
    this.name = name;
    this.client = client;
  }
  
  public static Database fromURI(String uri, int timeout, boolean verifySSL) throws InflowException {

    Client client = Client.fromURI(uri, timeout, verifySSL);

    URI u;
    try {
      u = new URI(uri);
    } catch (URISyntaxException use) {
      throw new InflowException("Malformed DSN URI:" + use.getMessage(), use);
    }

    String databaseName = null;
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

}
