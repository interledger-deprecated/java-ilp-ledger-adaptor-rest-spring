package org.interledger.ilp.ledger.adaptor.rest.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;


/**
 * JSON model of connector information as would be exchanged with the REST ledger.
 */
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