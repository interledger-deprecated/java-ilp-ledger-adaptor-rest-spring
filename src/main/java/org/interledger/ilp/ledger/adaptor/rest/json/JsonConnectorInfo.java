package org.interledger.ilp.ledger.adaptor.rest.json;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonConnectorInfo {

  private URI id;
  private String name;

  @JsonProperty(value = "id")
  public URI getId() {
    return id;
  }

  @JsonProperty(value = "name")
  public String getName() {
    return name;
  }

  public void setId(URI id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }

}