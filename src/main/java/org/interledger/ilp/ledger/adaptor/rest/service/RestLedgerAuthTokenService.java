package org.interledger.ilp.ledger.adaptor.rest.service;

import org.interledger.ilp.ledger.adaptor.rest.exceptions.RestServiceException;
import org.interledger.ilp.ledger.adaptor.rest.json.JsonAuthToken;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

/**
 * This service provides methods to interact with the REST ledger for authorization token activity.
 */
public class RestLedgerAuthTokenService extends RestServiceBase {

  private URI uri;

  /**
   * Constructs a new <code>RestLedgerAuthTokenService</code> instance.
   * 
   * @param restTemplate
   *  The rest template to use when interacting with the REST ledger
   * @param uri
   *  The URI of the REST ledger for auth token services.
   */
  public RestLedgerAuthTokenService(RestTemplate restTemplate, URI uri) {
    
    super(restTemplate);
    this.uri = uri;
  }

  /**
   * Retrieves an authorization token from the REST ledger that can be used to authenticate 
   * future requests.
   *
   * @return
   *  An authorization token supplied by the ledger.
   */
  public String getAuthToken() throws RestServiceException {

    try {

      log.debug("GET Auth Token");
      JsonAuthToken token = getRestTemplate().getForObject(uri, JsonAuthToken.class);
      return token.getToken();
    } catch (HttpStatusCodeException sce) {
      switch (sce.getStatusCode()) {
        case UNAUTHORIZED:
          throw parseRestException(sce);
        default:
          throw sce;
      }
    }

  }

}
