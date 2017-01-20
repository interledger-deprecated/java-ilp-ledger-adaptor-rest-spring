package org.interledger.ilqp.json;

import org.interledger.ilp.core.ledger.model.MessageData;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class JsonQuoteResponseEnvelope extends JsonMessageEnvelope {

  private JsonQuoteResponse quote;
    
  @Override
  public JsonQuoteResponse getData() {
    return quote;
  }

  @Override
  @JsonDeserialize(as=JsonQuoteResponse.class)
  public void setData(MessageData data) {
    if(!(data instanceof JsonQuoteResponse)) {
      throw new IllegalArgumentException("Only Quote Request objects accepted as data.");
    }
    
    quote = (JsonQuoteResponse) data;
    
  }

}
