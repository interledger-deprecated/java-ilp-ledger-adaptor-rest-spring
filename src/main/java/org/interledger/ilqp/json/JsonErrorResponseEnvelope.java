package org.interledger.ilqp.json;

import org.interledger.ilp.core.ledger.model.MessageData;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class JsonErrorResponseEnvelope extends JsonMessageEnvelope {

  private JsonErrorResponse error;
    
  @Override
  public JsonErrorResponse getData() {
    return error;
  }

  @Override
  @JsonDeserialize(as=JsonErrorResponse.class)
  public void setData(MessageData data) {
    if(!(data instanceof JsonErrorResponse)) {
      throw new IllegalArgumentException("Only Error Response objects accepted as data.");
    }
    
    error = (JsonErrorResponse) data;
    
  }

}