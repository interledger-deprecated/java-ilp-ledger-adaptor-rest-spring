package org.interledger.ilp.ledger.adaptor.rest.service;

import java.net.URI;

import org.interledger.ilp.ledger.adaptor.rest.exceptions.RestServiceException;
import org.interledger.ilp.ledger.adaptor.rest.json.JsonAuthToken;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

public class RestLedgerAuthTokenService extends RestServiceBase {

  private URI uri;

  public RestLedgerAuthTokenService(RestTemplate restTemplate, URI uri) {
    super(restTemplate);
    this.uri = uri;
  }

  public String getAuthToken() throws RestServiceException {

    try {

      log.debug("GET Auth Token");
      JsonAuthToken token =
          getRestTemplate().getForObject(uri, JsonAuthToken.class);
      return token.getToken();

    } catch (HttpStatusCodeException e) {
      switch (e.getStatusCode()) {
        case UNAUTHORIZED:
          throw parseRestException(e);
        default:
          throw e;
      }
    }

  }

}
