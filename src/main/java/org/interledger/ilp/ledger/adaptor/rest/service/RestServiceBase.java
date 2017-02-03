package org.interledger.ilp.ledger.adaptor.rest.service;

import org.interledger.ilp.ledger.adaptor.rest.exceptions.RestServiceException;
import org.interledger.ilp.ledger.adaptor.rest.json.JsonError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * Base class intended for interactions with the REST ledger.
 */
public abstract class RestServiceBase {

  protected static final Logger log = LoggerFactory.getLogger(RestServiceBase.class);

  private RestTemplate restTemplate;
  private RestLedgerJsonConverter converter;
  
  /**
   * Constructs a new <code>RestServiceBase</code> instance.
   * 
   * @param restTemplate
   *  The rest template to use for interacting with the REST ledger.
   */
  public RestServiceBase(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }
  
  /**
   * Constructs a new <code>RestServiceBase</code> instance.
   * 
   * @param converter
   *  A converter to use when translating between the ilp-core models and the native JSON models
   *      of the ledger.
   * @param restTemplate
   *  The rest template to use for interacting with the REST ledger.
   */
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

  /**
   * Inspects an Http exception thrown by the rest template for a JSON encoded error message sent
   * by the REST ledger.
   *
   * @param knownException
   *  The exception thrown by the rest template.
   * @return
   *  A {@link RestServiceException} containing the error details, if any were included.
   */
  protected RestServiceException parseRestException(HttpStatusCodeException knownException) {
    
    JsonError error;
    try {
      error = JsonError.fromJson(knownException.getResponseBodyAsString());
    } catch (Exception ex) {
      // Can't parse JSON
      error = new JsonError();
      error.setId("-1");
      error.setId("Unknown RestServiceException, unable to parse details from response. "
          + "See innerException for raw response.");
    }
    
    return new RestServiceException(error, knownException);
  }

}
