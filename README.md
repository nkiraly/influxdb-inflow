# influxdb-inflow
## InfluxDB client library for Java
[![Build Status](https://travis-ci.org/nkiraly/influxdb-inflow.png?branch=master)](https://travis-ci.org/nkiraly/influxdb-inflow)


### Overview

Inflow is a library for using InfluxDB with Java projects that need to talk HTTP or UDP. It takes code and architecture from both https://github.com/influxdata/influxdb-php and https://github.com/influxdata/influxdb-java projects. The goals are Java code query builders and results DTO, multiple connection management, and UDP transport support.

### Maven
TODO: Where to publish this library for general consumption?
Until I sort that out, /mvn install/ this project locally and then you can use it in your maven projects like so:
```
    <dependency>
      <groupId>com.koadweb.inflow</groupId>
      <artifactId>inflow</artifactId>
      <version>0.1.3</version>
    </dependency>
```

For an example of using the artifact and java code that will talk to InfluxDB, check out the Inflow examples project: https://github.com/nkiraly/influxdb-inflow-examples

### Getting started

Initialize a new client object:

```java
Client client = new Client("influxdb.local", 8086);
```

This will create a new client object which you can use to read and write points to InfluxDB.

It's also possible to create a client or database object for queries from a URI:

```java
// client to talk at URI
Client client = Client.fromURI("http://influxdb.local:8086");

// database object to query and write to specific database URI
Database database = Database.fromURI("http://influxdb.local:8086/datinflowtho");
```

### Reading data

To fetch records from InfluxDB, you can:
    
1) Do a manual query directly on a database:

```java
String influxdbURI = "http://inflowexample:inflow011@influxdb.local:8086/inflow_test";
Database database = Database.fromURI(influxdbURI);

// executing a raw query string yields a QueryResult object
String query1 = "SELECT * FROM test_metric LIMIT 5";
QueryResult result1 = database.query(query1);

// get the point values from the QueryResult as an array with the collapser getValuesAsStringArray
String[] values = result1.getValuesAsStringArray();
```

2) Query for data using the QueryBuilder object

```java
// retrieve points array with the query builder
String[] points2 = database.getQueryBuilder()
  .select("cpucount")
  .from("test_metric")
  .limit(2)
  .getQueryResult()
  .getValuesAsStringArray();
  
// get the query command from the QueryBuilder
String query3 = database.getQueryBuilder()
  .select("cpucount")
  .from("test_metric")
  .where("region = 'us-west'")
  .getQueryCommand();
```
Make sure that you enter single quotes when doing a where query on strings.
Otherwise InfluxDB will return an empty result.

You can get the last executed query from the client static method getLastQuery()

```java
String lastQuery1 = client.getLastQuery();
```

### Writing data

Writing data is done by providing an array of points to the writePoints method on a database:

```java
    Client client = new Client("influxdb.local", 8086);
    
    // specify to use inflow_test database
    Database database = client.selectDB("inflow_test");
    
    Point point1 = Point
            .measurement("cpu_load_short")
            .field("value", 0.64)
            .tag("host", "server01")
            .tag("region", "us-west")
            .field("cpucount", 10)
            .time(1452129125, TimeUnit.SECONDS) // 2016-01-07 01:12:05 GMT
            .build();
    // Note: It's possible to add multiple fields (see https://influxdb.com/docs/v0.9/concepts/key_concepts.html) when writing measurements to InfluxDB.

    Point point2 = Point
            .measurement("cpu_load_short")
            .field("value", 0.84)
            .build();
    // Note: No .time() - InfluxDB uses the current time as the default timestamp.
    
    // data points of test_metric measurement, which include optional tags and fields
    Point point3 = Point
            .measurement("test_metric")         // name of the measurement
            .field("value", 0.64)               // the measurement value
            .tag("host", "server01")            // optional tags
            .tag("region", "us-west")
            .field("cpucount", 10)              // optional additional fields
            .time(1452659705, TimeUnit.SECONDS) // 2016-01-13 04:34:05 GMT
            .build();
    
    Point point4 = Point
            .measurement("test_metric")         // name of the measurement
            .field("value", 0.84)               // the measurement value
            .tag("host", "server01")            // optional tags
            .tag("region", "us-west")
            .field("cpucount", 10)              // optional additional fields
            .time(1452659709, TimeUnit.SECONDS) // 2016-01-13 04:34:09 GMT
            .build();

    Point[] points = new Point[]{point1, point2, point3, point4};

    database.writePoints(points);
```

It's possible to add multiple [fields](https://influxdb.com/docs/v0.9/concepts/key_concepts.html) when writing
measurements to InfluxDB. The point class allows one to easily write data in batches to influxDB.

The name of a measurement and the value are mandatory. Additional fields, tags and a timestamp are optional.
InfluxDB takes the current time as the default timestamp.


#### Writing data using UDP

First, set your InfluxDB host to support incoming UDP sockets:

```ini
[udp]
  enabled = true
  bind-address = ":4444"
  # if you don't specify database, measurements received will go into a database called udp
  database = "inflow_test"
```

Then, tell the client to use the UDP driver:

```java
String influxdbURI = "http://inflowexample:inflow011@influxdb.local:8086";

Client client = Client.fromURI(influxdbURI);

DriverInterface driver = new DriverUDP(client.getHost(), 4444);
client.setDriver(driver);
    
  Point point5 = Point
          .measurement("test_metric")
          .field("value", 0.85)
          .tag("host", "server01")
          .tag("region", "us-west")
          .tag("proto", "udp")
          .field("cpucount", 10)
          .build();

  // write the point using UDP
  database.writePoint(point5);
```

#### Timestamp precision

It's important to provide the correct precision when adding a timestamp to a Point object. This is because
if you specify a timestamp in seconds and the default (nanosecond) precision is set; the entered timestamp will be invalid. See use of TimeUnit enums for examples on specifying a specific precision.

### Creating databases

When creating a database a default retention policy is added. This retention policy does not have a duration
so the data will be flushed with the memory. 

This library makes it easy to provide a retention policy when creating a database:

```java
// create the client
Client client = new Client("influxdb.local", 8086, "inflowexample", "inflow011");

// select the database
$database = client.selectDB('influx_test_db');

// create the database with a retention policy
database.create(new RetentionPolicy("test", "5d", 1, true));
     
// check if a database exists then create it if it doesn't
Database database = client.selectDB("inflow_test");
    
if (!database.exists()) {
  database.create(new RetentionPolicy("test", "1d", 2, true));   
}  
```

You can also alter retention policies:

```java
database.alterRetentionPolicy(new RetentionPolicy("test", "2d", 5, true));
```

and list them:

```java
Series series = database.listRetentionPolicies();
```

You can add more retention policies to a database:

```java
database.createRetentionPolicy(new RetentionPolicy("test2", "30d", 1, false));
```

### Client functions

Some functions are too general for a database. So these are available in the client:

```java
// list users
Series usersSeries = client.listUsers();

// list databases
Series databasesSeries = client.listDatabases();
```

### Admin functionality

You can use the client's client.admin functionality to administer InfluxDB via the API.

```java
// add a new user without privileges
client.createUser("testuser123", "testpassword");

// add a new user with ALL cluster-wide privileges
client.admin.createUser("admin_user", "password", InfluxDB.UserPrivilege.ALL);

// drop user testuser123
client.admin.dropUser("testuser123");
```

List all the users:

```java
// show a list of all users
QueryResult usersResults = client.admin.showUsers();

String[] users = usersResults.getValuesAsStringArray();
```

#### Granting and revoking privileges

Granting permissions can be done on both the database level and cluster-wide. 
To grant a user specific privileges on a database, provide a database object or a database name.

```java
// grant permissions using a database object
Database database = client.selectDB("test_db");
client.admin.grant(InfluxDB.UserPrivilege.READ, "testuser123", database);

// give user testuser123 read privileges on database test_db
client.admin.grant(InfluxDB.UserPrivilege.READ, "testuser123", 'test_db');

// revoke user testuser123's read privileges on database test_db
client.admin.revoke(InfluxDB.UserPrivilege.READ, "testuser123", 'test_db');

// grant a user cluster-wide privileges
client.admin.grant(InfluxDB.UserPrivilege.READ, "testuser123");

// Revoke an admin's cluster-wide privileges
client.admin.revoke(InfluxDB.UserPrivilege.ALL, "admin_user");
```


## Changelog

### 0.1.3
* Update packaging to com.koadweb.inflow

### 0.1.2
* More SLF4J logger emissions, code cleanup, docs updates

### 0.1.1
* Implementation refinement

### 0.1.0
* Development release
* InfluxDB 0.9.4 support
