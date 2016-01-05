package org.influxdb;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStreamReader;
import retrofit.ErrorHandler;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class InfluxDBHTTPErrorHandler implements ErrorHandler {

  @Override
  public Throwable handleError(final RetrofitError cause) {
    Response r = cause.getResponse();
    if (r != null && r.getStatus() >= 400) {
      try (InputStreamReader reader = new InputStreamReader(r.getBody().in(), Charsets.UTF_8)) {
        return new RuntimeException(CharStreams.toString(reader));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return cause;
  }
}
