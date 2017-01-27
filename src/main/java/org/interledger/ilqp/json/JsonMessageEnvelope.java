package org.interledger.ilqp.json;

import org.interledger.ilp.ledger.model.MessageData;
import org.interledger.ilp.ledger.model.MessageEnvelope;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

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
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
  
}
