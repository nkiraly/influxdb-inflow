package com.koadweb.inflow;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.influxdb.InfluxDB.RetentionPolicy;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;

/**
 * Collects single point writes and aggregates them to BatchPoints for better write performance.
 *
 * @author stefan.majer [at] gmail.com
 *
 */
public class BatchProcessor {

  protected final BlockingQueue<BatchEntry> queue = new LinkedBlockingQueue<>();
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  final DriverInterface inflowDriver;
  final int actions;
  private final TimeUnit flushIntervalUnit;
  private final int flushInterval;

  /**
   * The Builder to create a BatchProcessor instance.
   */
  public static final class Builder {

    private final DriverInterface inflowDriver;
    private int actions;
    private TimeUnit flushIntervalUnit;
    private int flushInterval;

    public Builder(final DriverInterface inflowDriver) {
      this.inflowDriver = inflowDriver;
    }

    /**
     * The number of actions after which a batch write must be performed.
     *
     * @param maxActions number of Points written after which a write must happen.
     * @return this Builder to use it fluent
     */
    public Builder actions(final int maxActions) {
      this.actions = maxActions;
      return this;
    }

    /**
     * The interval at which at least should issued a write.
     *
     * @param interval the interval
     * @param unit the TimeUnit of the interval
     *
     * @return this Builder to use it fluent
     */
    public Builder interval(final int interval, final TimeUnit unit) {
      this.flushInterval = interval;
      this.flushIntervalUnit = unit;
      return this;
    }

    /**
     * Create the BatchProcessor.
     *
     * @return the BatchProcessor instance.
     */
    public BatchProcessor build() {
      Preconditions.checkNotNull(this.actions, "actions may not be null");
      Preconditions.checkNotNull(this.flushInterval, "flushInterval may not be null");
      Preconditions.checkNotNull(this.flushIntervalUnit, "flushIntervalUnit may not be null");
      return new BatchProcessor(this.inflowDriver, this.actions, this.flushIntervalUnit, this.flushInterval);
    }
  }

  static class BatchEntry {

    private final Point point;
    private final String db;
    private final RetentionPolicy rp;

    public BatchEntry(final Point point, final String db, final RetentionPolicy rp) {
      super();
      this.point = point;
      this.db = db;
      this.rp = rp;
    }

    public Point getPoint() {
      return this.point;
    }

    public String getDb() {
      return this.db;
    }

    public RetentionPolicy getRp() {
      return this.rp;
    }
  }

  public static Builder builder(final DriverInterface inflowDriver) {
    return new Builder(inflowDriver);
  }

  BatchProcessor(final DriverInterface inflowDriver, final int actions, final TimeUnit flushIntervalUnit,
          final int flushInterval) {
    super();
    this.inflowDriver = inflowDriver;
    this.actions = actions;
    this.flushIntervalUnit = flushIntervalUnit;
    this.flushInterval = flushInterval;

    // Flush at specified Rate
    this.scheduler.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        try {
          write();
        } catch (InflowException ie) {
          Logger.getLogger(BatchProcessor.class.getName()).log(Level.SEVERE, null, ie);
        }
      }
    }, this.flushInterval, this.flushInterval, this.flushIntervalUnit);

  }

  void write() throws InflowException {
    if (this.queue.isEmpty()) {
      return;
    }

    Map<String, BatchPoints> databaseToBatchPoints = Maps.newHashMap();
    List<BatchEntry> batchEntries = new ArrayList<>(this.queue.size());
    this.queue.drainTo(batchEntries);

    for (BatchEntry batchEntry : batchEntries) {
      String dbName = batchEntry.getDb();
      if (!databaseToBatchPoints.containsKey(dbName)) {
        BatchPoints batchPoints = BatchPoints.database(dbName).retentionPolicy(batchEntry.getRp()).build();
        databaseToBatchPoints.put(dbName, batchPoints);
      }
      Point point = batchEntry.getPoint();
      databaseToBatchPoints.get(dbName).point(point);
    }

    for (BatchPoints batchPoints : databaseToBatchPoints.values()) {
      BatchProcessor.this.inflowDriver.write(batchPoints);
    }
  }

  /**
   * Put a single BatchEntry to the cache for later processing.
   *
   * @param batchEntry the batchEntry to write to the cache.
   */
  void put(final BatchEntry batchEntry) throws InflowException {
    this.queue.add(batchEntry);
    if (this.queue.size() >= this.actions) {
      write();
    }
  }

  /**
   * Flush the current open writes and stop the reaper thread. This should only be called if no
   * batch processing is needed anymore.
   *
   */
  void flush() throws InflowException {
    this.write();
    this.scheduler.shutdown();
  }

}
