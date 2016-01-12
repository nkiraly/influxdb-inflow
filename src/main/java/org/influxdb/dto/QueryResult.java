package org.influxdb.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Query Result series data transfer object
 *
 * for Serie details, see https://influxdb.com/docs/v0.9/guides/querying_data.html
 *
 * @author stefan
 *
 */
// {
// "results": [
// {
// "series": [{}],
// "error": "...."
// }
// ],
// "error": "...."
// }
// {"results":[{"series":[{"name":"cpu","columns":["time","value"],"values":[["2015-06-06T14:55:27.195Z",90],["2015-06-06T14:56:24.556Z",90]]}]}]}
// {"results":[{"series":[{"name":"databases","columns":["name"],"values":[["mydb"]]}]}]}
public class QueryResult {

  private List<Result> results;
  private String error;

  /**
   * @return the results
   */
  public List<Result> getResults() {
    return this.results;
  }

  /**
   * @param results the results to set
   */
  public void setResults(final List<Result> results) {
    this.results = results;
  }

  /**
   * Checks if this QueryResult has an error message.
   *
   * @return <code>true</code> if there is an error message, <code>false</code> if not.
   */
  public boolean hasError() {
    return this.error != null;
  }

  /**
   * @return the error
   */
  public String getError() {
    return this.error;
  }

  /**
   * @param error the error to set
   */
  public void setError(final String error) {
    this.error = error;
  }
  
  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (!(o instanceof QueryResult)) {
      return false;
    }

    QueryResult queryResult = (QueryResult) o;

    if (!Objects.equals(this.error, queryResult.error)) {
      return false;
    }
    if (!Objects.equals(this.results, queryResult.results)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 79 * hash + Objects.hashCode(this.results);
    hash = 79 * hash + Objects.hashCode(this.error);
    return hash;
  }

  public static class Result {

    private List<Series> series;
    private String error;

    /**
     * @return the series
     */
    public List<Series> getSeries() {
      return this.series;
    }

    /**
     * @param series the series to set
     */
    public void setSeries(final List<Series> series) {
      this.series = series;
    }

    /**
     * Checks if this Result has an error message.
     *
     * @return <code>true</code> if there is an error message, <code>false</code> if not.
     */
    public boolean hasError() {
      return this.error != null;
    }

    /**
     * @return the error
     */
    public String getError() {
      return this.error;
    }

    /**
     * @param error the error to set
     */
    public void setError(final String error) {
      this.error = error;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("Result [series=");
      builder.append(this.series);
      builder.append(", error=");
      builder.append(this.error);
      builder.append("]");
      return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }
      if (!(o instanceof Result)) {
        return false;
      }

      Result result = (Result) o;

      if (!Objects.equals(this.error, result.error)) {
        return false;
      }
      if (!Objects.equals(this.series, result.series)) {
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int hash = 5;
      hash = 67 * hash + Objects.hashCode(this.series);
      hash = 67 * hash + Objects.hashCode(this.error);
      return hash;
    }

  }

  public static class Series {

    private String name;
    private Map<String, String> tags;
    private List<String> columns;
    private List<List<Object>> values;

    /**
     * @return the name
     */
    public String getName() {
      return this.name;
    }

    /**
     * @param name the name to set
     */
    public void setName(final String name) {
      this.name = name;
    }

    /**
     * @return the tags
     */
    public Map<String, String> getTags() {
      return this.tags;
    }

    /**
     * @param tags the tags to set
     */
    public void setTags(final Map<String, String> tags) {
      this.tags = tags;
    }

    /**
     * @return the columns
     */
    public List<String> getColumns() {
      return this.columns;
    }

    /**
     * @param columns the columns to set
     */
    public void setColumns(final List<String> columns) {
      this.columns = columns;
    }

    /**
     * @return the values
     */
    public List<List<Object>> getValues() {
      return this.values;
    }

    /**
     * @param values the values to set
     */
    public void setValues(final List<List<Object>> values) {
      this.values = values;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("Series [name=");
      builder.append(this.name);
      builder.append(", tags=");
      builder.append(this.tags);
      builder.append(", columns=");
      builder.append(this.columns);
      builder.append(", values=");
      builder.append(this.values);
      builder.append("]");
      return builder.toString();
    }
    
    public String[] getValuesAsStringArray() {
      // create a flat list of all the right side values of the values list of lists
      List<String> listValues = new ArrayList<>();

      for (List<Object> valuesItem : this.values) {
        for (Object o : valuesItem) {
          listValues.add(o.toString());
        }
      }

      // oracle says that a flat array should be "safe" in that there are no references to it
      // this creates a safe referenceless array of the values
      String[] stringVals = listValues.toArray(new String[0]);
      return stringVals;
    }
    
    @Override
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }
      if (!(o instanceof Series)) {
        return false;
      }

      Series series = (Series) o;

      if (!Objects.equals(this.name, series.name)) {
        return false;
      }
      if (!Objects.equals(this.tags, series.tags)) {
        return false;
      }

      // compare tags
      if (this.tags != null && series.tags != null) {
        // size check first and return as not equal if not
        if (this.tags.size() != series.tags.size()) {
          return false;
        }
        // check tags and values are the same
        for (String key : this.tags.keySet()) {
          if (!this.tags.get(key).equals(series.tags.get(key))) {
            return false;
          }
        }
      } else if (this.tags != null && series.tags == null) {
        return false;
      } else if (this.tags == null && series.tags != null) {
        return false;
      }

      // List.equals will return true if values and order the same
      if (!Objects.equals(this.columns, series.columns)) {
        return false;
      }
      
      // compare values lists
      if (this.values != null && series.values != null) {
        // size check first and return as not equal if not
        if (this.values.size() != series.values.size()) {
          return false;
        }
        // check that specified series (v2) contains all values of this (v1)
        for (Object v1 : this.values) {
          if (!series.values.contains(v1)) {
            return false;
          }
        }
        // check that this (v1) contains all values of specified series (v2)
        for (Object v2 : series.values) {
          if (!this.values.contains(v2)) {
            return false;
          }
        }
        // TODO: other List of Lists comparisons ?
      } else if (this.values == null && series.values != null) {
        return false;
      } else if (this.values != null && series.values == null) {
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int hash = 5;
      hash = 97 * hash + Objects.hashCode(this.name);
      hash = 97 * hash + Objects.hashCode(this.tags);
      hash = 97 * hash + Objects.hashCode(this.columns);
      hash = 97 * hash + Objects.hashCode(this.values);
      return hash;
    }

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("QueryResult [results=");
    builder.append(this.results);
    builder.append(", error=");
    builder.append(this.error);
    builder.append("]");
    return builder.toString();
  }

}
