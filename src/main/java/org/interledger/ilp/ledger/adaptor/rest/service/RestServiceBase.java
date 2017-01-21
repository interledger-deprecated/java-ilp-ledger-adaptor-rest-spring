package org.interledger.ilp.ledger.adaptor.rest.service;

import org.interledger.ilp.ledger.adaptor.rest.exceptions.RestServiceException;
import org.interledger.ilp.ledger.adaptor.rest.json.JsonError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

public class RestServiceBase {

  protected static final Logger log = LoggerFactory.getLogger(RestServiceBase.class);

  private RestTemplate restTemplate;
  private RestLedgerJsonConverter converter;
  
  public RestServiceBase(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }
  
  public RestServiceBase(RestLedgerJsonConverter converter, RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
    this.converter = converter;
  }
  
  public RestLedgerJsonConverter getConverter() {
    return this.converter;
  }

  public RestTemplate getRestTemplate() {
    return this.restTemplate;
  }

  protected RestServiceException parseRestException(HttpStatusCodeException knownException) {
    JsonError error;
    try {
      error = JsonError.fromJson(knownException.getResponseBodyAsString());
    } catch (Exception e) {
      // Can't parse JSON
      error = new JsonError();
      error.setId("-1");
      error.setId(
          "Unknown RestServiceException, unable to parse details from response. See innerException for raw response.");
    }
    return new RestServiceException(error, knownException);
  }

}
