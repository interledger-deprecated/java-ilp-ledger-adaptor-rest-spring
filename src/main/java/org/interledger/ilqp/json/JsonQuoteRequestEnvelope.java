package org.interledger.ilqp.json;

import org.interledger.ilp.core.ledger.model.MessageData;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class JsonQuoteRequestEnvelope extends JsonMessageEnvelope {

  private JsonQuoteRequest quote;
    
  @Override
  public JsonQuoteRequest getData() {
    return quote;
  }

  @Override
  @JsonDeserialize(as=JsonQuoteRequest.class)
  public void setData(MessageData data) {
    if(!(data instanceof JsonQuoteRequest)) {
      throw new IllegalArgumentException("Only Quote Request objects accepted as data.");
    }
    
    quote = (JsonQuoteRequest) data;
    
  }

}
