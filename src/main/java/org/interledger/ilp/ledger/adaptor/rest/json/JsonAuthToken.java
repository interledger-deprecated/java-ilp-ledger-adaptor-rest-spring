package org.interledger.ilp.ledger.adaptor.rest.json;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON model of an authorization token as would be exchanged with the REST ledger.
 */
public class JsonAuthToken {

  private String token;

  @JsonProperty(value = "token")
  public String getToken() {
    return this.token;
  }

  public void setToken(String token) {
    this.token = token;
  }
}
