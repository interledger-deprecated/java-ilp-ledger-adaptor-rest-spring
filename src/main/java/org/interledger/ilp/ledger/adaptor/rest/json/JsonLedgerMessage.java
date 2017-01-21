package org.interledger.ilp.ledger.adaptor.rest.json;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonInclude(Include.NON_NULL)
public class JsonLedgerMessage {

  private URI from;
  private URI to;
  private URI ledger;

  @JsonRawValue
  private Object data;

  @JsonProperty(value = "ledger")
  public URI getLedger() {
    return this.ledger;
  }

  @JsonProperty(value = "from")
  public URI getFrom() {
    return this.from;
  }

  @JsonProperty(value = "to")
  public URI getTo() {
    return this.to;
  }

  @JsonProperty(value = "data")
  public Object getData() {
    return this.data;
  }

  public void setFrom(URI from) {
    this.from = from;
  }

  public void setTo(URI to) {
    this.to = to;
  }

  public void setData(Object data) {
    this.data = data;
  }

  public void setLedger(URI ledger) {
    this.ledger = ledger;
  }

  @Deprecated
  public void setAccount(String account) {

  }

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
