package org.influxdb.inflow;

import com.google.gson.Gson;
import org.influxdb.InfluxDB.UserPrivilege;
import org.influxdb.dto.QueryResult;
import static org.mockito.Matchers.anyString;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

public class AdminTest extends AbstractTest {
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }
  
  @Test
  public void testCreateUser() throws InflowException, Exception {
    Admin adminObject = this.getAdminObject();

    assertEquals(
            this.getEmptyQueryResult(),
            adminObject.createUser("test", "test", UserPrivilege.ALL)
    );
  }

  @Test
  public void testChangeUserPassword() throws InflowException, Exception {
    Admin adminObject = this.getAdminObject();

    assertEquals(
            this.getEmptyQueryResult(),
            adminObject.changeUserPassword("test", "test")
    );
  }

  @Test
  public void testShowUsers() throws InflowException, Exception {
    String usersResultJson = this.loadResourceFileDataAsString("/result-test-users.example.json");
    Gson gson = new Gson();
    final QueryResult usersQueryResult = gson.fromJson(usersResultJson, QueryResult.class);

    Client clientMock = this.getClientMock();

    // mock intended query result of show users when calling query()
    Mockito.when(clientMock.query(this.TEST_ARG_DATABSENAME, anyString()))
            .thenAnswer(new Answer<QueryResult>() {
              @Override
              public QueryResult answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return usersQueryResult;
              }
            });

    Admin adminMock = new Admin(clientMock);

    assertEquals(usersQueryResult, adminMock.showUsers());
  }

  private Admin getAdminObject() throws Exception {
    return new Admin(this.getClientMock(true));
  }

}
