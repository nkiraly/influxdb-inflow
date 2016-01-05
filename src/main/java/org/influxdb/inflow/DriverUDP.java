package org.influxdb.inflow;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.influxdb.InfluxDB;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;

public class DriverUDP implements DriverInterface {

  protected String host;
  protected InetAddress ipAddress;
  protected int port;

  public DriverUDP(String host, int port) {
    this.host = host;
    this.port = port;
  }
  
  @Override
  public void write(final String database, final InfluxDB.RetentionPolicy retentionPolicy, final InfluxDB.ConsistencyLevel consistency, final String records) throws InflowException {
    throw new InflowException("TODO: implement DriverUDP.write(database, retentionPolicy, consistencyLevel, records)");
  }

  @Override
  public void write(final String database, final InfluxDB.RetentionPolicy retentionPolicy, final InfluxDB.ConsistencyLevel consistency, final List<String> records) throws InflowException {
    throw new InflowException("TODO: implement DriverUDP.write(database, retentionPolicy, consistencyLevel, records)");
  }

  @Override
  public void write(final String database, final InfluxDB.RetentionPolicy retentionPolicy, final Point point) throws InflowException {
    DatagramSocket datagramSocket;
    try {
      datagramSocket = new DatagramSocket();
    } catch (SocketException se) {
      throw new InflowException("DatagramSocket SocketException: " + se.getMessage(), se);
    }
    
    if ( this.ipAddress == null ) {
      try {
        this.ipAddress = InetAddress.getByName(this.host);
      } catch (UnknownHostException uhe) {
        throw new InflowException("InetAddress UnknownHostException: " + uhe.getMessage(), uhe);
      }
    }
    
    // see https://docs.influxdata.com/influxdb/v0.9/write_protocols/write_syntax/
    String line = point.lineProtocol();

    byte[] sendData = line.getBytes(StandardCharsets.UTF_8);

    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, this.ipAddress, this.port);

    try {
      datagramSocket.send(sendPacket);
    } catch (IOException ioe) {
      throw new InflowException("DatagramSocket IOException: " + ioe.getMessage(), ioe);
    }

    datagramSocket.close();
  }

  @Override
  public void write(final BatchPoints batchPoints) throws InflowException {
    throw new InflowException("TODO: implement DriverUDP.write(BatchPoints batchPoints)");
  }

}
