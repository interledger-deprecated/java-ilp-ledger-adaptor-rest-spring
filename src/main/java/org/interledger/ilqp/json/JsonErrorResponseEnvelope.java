package org.interledger.ilqp.json;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.interledger.ilp.ledger.model.MessageData;

/**
 * An envelope for holding JsonErrorResponses.
 */
public class JsonErrorResponseEnvelope extends JsonMessageEnvelope {

  private JsonErrorResponse error;
    
  @Override
  public JsonErrorResponse getData() {
    return error;
  }

  @Override
  @JsonDeserialize(as = JsonErrorResponse.class)
  public void setData(MessageData data) {
    
    if (!(data instanceof JsonErrorResponse)) {
      throw new IllegalArgumentException("Only Error Response objects accepted as data.");
    }
    
    error = (JsonErrorResponse) data;
  }

}
