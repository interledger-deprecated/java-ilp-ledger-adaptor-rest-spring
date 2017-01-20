package org.interledger.ilp.ledger.adaptor.rest.service;

import java.net.URI;

import org.interledger.ilp.core.ledger.model.LedgerMessage;
import org.interledger.ilp.ledger.adaptor.rest.exceptions.RestServiceException;
import org.interledger.ilp.ledger.adaptor.rest.json.JsonLedgerMessage;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

public class RestLedgerMessageService extends RestServiceBase {

  private URI uri;

  public RestLedgerMessageService(RestLedgerJsonConverter converter, RestTemplate restTemplate, URI uri) {
    super(converter, restTemplate);
    this.uri = uri;
  }

  public void sendMessage(LedgerMessage message) throws RestServiceException {

    JsonLedgerMessage jsonMessage = getConverter().convertLedgerMessage(message);

    try {

      log.debug("POST message");

      RequestEntity<JsonLedgerMessage> request = RequestEntity
          .post(uri)
          .contentType(MediaType.APPLICATION_JSON_UTF8).body(jsonMessage, JsonLedgerMessage.class);

      getRestTemplate().postForEntity(uri, request, String.class);

      // TODO Handle response?

    } catch (HttpStatusCodeException e) {
      switch (e.getStatusCode()) {
        case BAD_REQUEST:
          throw parseRestException(e);
        case UNPROCESSABLE_ENTITY:
          throw new RestServiceException(
              "No listeners subscribed for messages to " + message.getTo(), e);
        default:
          throw e;
      }
    }

  }

}
