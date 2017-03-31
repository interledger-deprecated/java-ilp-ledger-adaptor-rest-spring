package org.interledger.ilqp.json;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.interledger.ilp.ledger.model.MessageData;

/**
 * Defines an envelope to hold a JSON Quote Request.
 */
public class JsonQuoteRequestEnvelope extends JsonMessageEnvelope {

  private JsonQuoteRequest quote;
    
  @Override
  public JsonQuoteRequest getData() {
    return quote;
  }

  @Override
  @JsonDeserialize(as = JsonQuoteRequest.class)
  public void setData(MessageData data) {
    
    if (!(data instanceof JsonQuoteRequest)) {
      throw new IllegalArgumentException("Only Quote Request objects accepted as data.");
    }
    
    quote = (JsonQuoteRequest) data;
  }
}
