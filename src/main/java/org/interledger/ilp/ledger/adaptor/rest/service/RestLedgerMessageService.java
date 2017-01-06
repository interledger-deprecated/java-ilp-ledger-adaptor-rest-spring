package org.interledger.ilp.ledger.adaptor.rest.service;

import java.net.URI;

import org.interledger.ilp.core.ledger.model.LedgerMessage;
import org.interledger.ilp.ledger.adaptor.rest.RestLedgerAdaptor;
import org.interledger.ilp.ledger.adaptor.rest.exceptions.RestServiceException;
import org.interledger.ilp.ledger.adaptor.rest.json.JsonLedgerMessage;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

public class RestLedgerMessageService extends RestServiceBase {

  private URI uri;

  public RestLedgerMessageService(RestLedgerAdaptor adaptor, RestTemplate restTemplate, URI uri) {
    super(adaptor, restTemplate);
    this.uri = uri;
  }

  public void sendMessage(LedgerMessage message) throws RestServiceException {

    JsonLedgerMessage jsonMessage = JsonLedgerMessage.fromLedgerMessage(message, this.adaptor);

    if (!jsonMessage.getLedger().toString().equals(adaptor.getLedgerInfo().getId())) {
      throw new IllegalArgumentException(
          "Can't send messages on other ledgers. Illegal ledger identifier: "
              + jsonMessage.getLedger());
    }

    try {

      log.debug("POST message");


      RequestEntity<JsonLedgerMessage> request = RequestEntity
          .post(uri)
          .contentType(MediaType.APPLICATION_JSON_UTF8).body(jsonMessage, JsonLedgerMessage.class);

      restTemplate.postForEntity(uri, request, String.class);

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
