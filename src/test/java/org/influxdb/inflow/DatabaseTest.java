package org.influxdb.inflow;

import com.google.gson.Gson;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import org.influxdb.dto.QueryResult;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

public class DatabaseTest extends AbstractTest {

  protected String dataToInsert;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    this.resultData = this.loadResourceFileDataAsString("/result.example.json");

    // return these databases when calling listDatabases()
    Mockito.when(this.mockClient.listDatabases()).thenAnswer(new Answer<String[]>() {
      @Override
      public String[] answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        return new String[]{"test123", "test"};
      }
    });

    this.dataToInsert = this.loadResourceFileDataAsString("/input.example.json");
  }

  @Test
  public void testGetterInstances() {
    assertThat(this.database.getClient(),       instanceOf(Client.class));
    assertThat(this.database.getQueryBuilder(), instanceOf(QueryBuilder.class));
  }

  @Test
  public void testQueries() throws InflowException {
    Gson gson = new Gson();
    QueryResult testQueryResult = gson.fromJson(this.resultData, QueryResult.class);
    assertEquals(this.database.query("SELECT * FROM test_metric"), testQueryResult);
    this.database.drop();
    assertEquals("DROP DATABASE influx_test_db", Client.getLastQuery());

  }

}
