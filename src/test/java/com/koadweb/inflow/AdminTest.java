package com.koadweb.inflow;

import com.google.gson.Gson;
import org.influxdb.InfluxDB.UserPrivilege;
import org.influxdb.dto.QueryResult;
import static org.mockito.Matchers.anyString;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

public class AdminTest extends AbstractTest {

  @BeforeSuite()
  @Override
  public void beforeSuite() throws Exception {
    super.beforeSuite();
  }
  
  @Test
  public void testCreateUser() throws InflowException, Exception {
    Admin adminObject = this.getAdminObject();

    // a successful createUser() call
    // CREAT USER .. WITH PASSWORD ..
    // will return an empty result set, if no no errors, etc
    assertEquals(
            adminObject.createUser("bofh234", "compy567", UserPrivilege.ALL),
            this.getEmptyQueryResult()
    );
    
    // the default mockClient getAdminObject() uses from getMockClientThatReturnsEmptyQueryResult()
    // sets the last query run, so validate last query value
    assertEquals(
            Client.getLastQuery(),
            "CREATE USER bofh234 WITH PASSWORD 'compy567' WITH ALL PRIVILEGES"
    );
    
    // same thing but do it for bobby ro the read only analyst
    assertEquals(
            adminObject.createUser("bobbyro32", "pomp78", UserPrivilege.READ),
            this.getEmptyQueryResult()
    );
    assertEquals(
            Client.getLastQuery(),
            "CREATE USER bobbyro32 WITH PASSWORD 'pomp78' WITH READ PRIVILEGES"
    );
  }

  @Test
  public void testChangeUserPassword() throws InflowException, Exception {
    Admin adminObject = this.getAdminObject();
    
    // expected influx query to be run to reset user password
    String expectedInfluxQuery = "SET PASSWORD FOR " + this.TEST_TARGET_USERNAME + " = '" + this.TEST_TARGET_PASSWORD + "'";

    // a successful changeUserPassword()
    // SET PASSWORD FOR .. = '..'
    // will return an empty result set, if no no errors, etc
    assertEquals(
            adminObject.changeUserPassword(this.TEST_TARGET_USERNAME, this.TEST_TARGET_PASSWORD),
            this.getEmptyQueryResult()
    );
    
    // if the mock returned empty ResultSet,
    // make sure the last query run was in fact the expectedInfluxQuery to reset the password
    assertEquals(
            Client.getLastQuery(),
            expectedInfluxQuery
    );
  }

  @Test
  public void testShowUsers() throws InflowException, Exception {
    String usersResultJson = this.loadResourceFileDataAsString("/result-test-users.example.json");
    Gson gson = new Gson();
    final QueryResult usersQueryResult = gson.fromJson(usersResultJson, QueryResult.class);

    Client clientMock = this.getMockClient();

    // mock intended query result of show users when calling query()
    Mockito.when(clientMock.query(anyString(), anyString()))
            .thenAnswer(new Answer<QueryResult>() {
              @Override
              public QueryResult answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                if ( args[0] != null ) {
                  String databaseName = args[0].toString();
                }
                if ( args[1] != null ) {
                  Client.setLastQuery(args[1].toString());
                }
                return usersQueryResult;
              }
            });

    Admin adminMock = new Admin(clientMock);

    assertEquals(usersQueryResult, adminMock.showUsers());
  }

  private Admin getAdminObject() throws Exception {
    // make an Admin object that uses a mock client that returns empty query results
    return new Admin(this.getMockClientThatReturnsEmptyQueryResult());
  }

}
