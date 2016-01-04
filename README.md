# influxdb-inflow
## InfluxDB client library for Java
[![Build Status](https://travis-ci.org/nkiraly/influxdb-inflow.png?branch=master)](https://travis-ci.org/nkiraly/influxdb-inflow)


### Overview

Inflow is a library for using InfluxDB with Java.

The influxdb-inflow library was created in the spirit of the Python influxdb client, in an effort to maintain a common structure between various programming languages talking to InfluxDB.

### Maven
TODO: Where to publish this library for general consumption?
Until I sort that out, /mvn install/ this project locally and then you can use it in your maven projects like so:
```
    <dependency>
      <groupId>org.influxdb</groupId>
      <artifactId>influxdb-inflow</artifactId>
      <version>0.1-SNAPSHOT</version>
    </dependency>
```



## Todo

* More unit tests
* Improve documentation


## Changelog

###0.2.0
* Initial release
* InfluxDB 0.9.4 support
