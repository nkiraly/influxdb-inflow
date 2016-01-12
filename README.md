# influxdb-inflow
## InfluxDB client library for Java
[![Build Status](https://travis-ci.org/nkiraly/influxdb-inflow.png?branch=master)](https://travis-ci.org/nkiraly/influxdb-inflow)


### Overview

Inflow is a library for using InfluxDB with Java projects that need to talk HTTP or UDP. It takes code and architecture from both https://github.com/influxdata/influxdb-php and https://github.com/influxdata/influxdb-java projects. The goals were Java code query builders and results DTO, multiple connection management, and UDP transport support.

### Maven
TODO: Where to publish this library for general consumption?
Until I sort that out, /mvn install/ this project locally and then you can use it in your maven projects like so:
```
    <dependency>
      <groupId>org.influxdb</groupId>
      <artifactId>influxdb-inflow</artifactId>
      <version>0.1.1-SNAPSHOT</version>
    </dependency>
```



## Todo

* More unit tests
* Improve documentation


## Changelog

### 0.1.1
* Implementation refinement

### 0.1.0
* Development release
* InfluxDB 0.9.4 support
