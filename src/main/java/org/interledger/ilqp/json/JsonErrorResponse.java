package org.interledger.ilqp.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.interledger.ilp.ledger.model.MessageData;

/**
 * Defines a basic JSON error response.
 */
@JsonInclude(value = Include.NON_NULL)
public class JsonErrorResponse implements MessageData {
  
  private String id;
  private String message;
  
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  @JsonProperty("message")
  public String getMessage() {
    return message;
  }

  public void setId(String id) {
    this.id = id;
  }
  
  public void setMessage(String message) {
    this.message = message;
  }
}
