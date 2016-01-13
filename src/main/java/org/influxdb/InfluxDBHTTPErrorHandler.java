package org.influxdb;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit.ErrorHandler;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class InfluxDBHTTPErrorHandler implements ErrorHandler {

  private final Logger logger = LoggerFactory.getLogger(InfluxDBHTTPErrorHandler.class);

  @Override
  public Throwable handleError(final RetrofitError cause) {
    Response r = cause.getResponse();
    if (r != null && r.getStatus() >= 400) {
      try (InputStreamReader reader = new InputStreamReader(r.getBody().in(), Charsets.UTF_8)) {
        return new RuntimeException(CharStreams.toString(reader));
      } catch (IOException ioe) {
        logger.error("IOException during stream read: " + ioe.getMessage(), ioe);
      }
    }
    return cause;
  }
}
