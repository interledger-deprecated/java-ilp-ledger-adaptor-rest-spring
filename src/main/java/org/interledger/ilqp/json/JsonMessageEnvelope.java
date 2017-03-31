package org.interledger.ilqp.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.interledger.ilp.ledger.model.MessageData;
import org.interledger.ilp.ledger.model.MessageEnvelope;

/**
 * A base class for a JSON message envelop that could hold various payloads. This is used to manage
 * JSON exchanges as the JSON message definitions for ILQP aren't well suited for strongly typed 
 * languages.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "method")
@JsonSubTypes({
    @Type(value = JsonErrorResponseEnvelope.class, name = "error"),
    @Type(value = JsonQuoteResponseEnvelope.class, name = "quote_response"),
    @Type(value = JsonQuoteRequestEnvelope.class, name = "quote_request")})
public abstract class JsonMessageEnvelope implements MessageEnvelope {

  private String id;

  @JsonProperty("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public abstract MessageData getData();
  
  public abstract void setData(MessageData data);
  
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
