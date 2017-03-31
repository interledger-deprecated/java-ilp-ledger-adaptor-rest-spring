package org.interledger.ilp.ledger.adaptor.rest.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON model of an error message that could be sent by the REST ledger.
 */
public class JsonError {

  private String id;
  private String message;

  public static JsonError fromJson(String json) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(json, JsonError.class);
  }

  @JsonProperty(value = "id")
  public String getId() {
    return id;
  }


  @JsonProperty(value = "message")
  public String getMessage() {
    return message;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public String toString() {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    } catch (JsonProcessingException jpe) {
      throw new RuntimeException(jpe);
    }
  }
}
