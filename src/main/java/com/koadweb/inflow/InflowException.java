package com.koadweb.inflow;

public class InflowException extends Exception {

  public InflowException(String message) {
    super(message);
  }

  public InflowException(String message, Exception cause) {
    super(message, cause);
  }

}
