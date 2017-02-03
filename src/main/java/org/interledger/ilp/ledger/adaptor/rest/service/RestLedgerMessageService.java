package org.interledger.ilp.ledger.adaptor.rest.service;

import org.interledger.ilp.ledger.adaptor.rest.exceptions.RestServiceException;
import org.interledger.ilp.ledger.adaptor.rest.json.JsonLedgerMessage;
import org.interledger.ilp.ledger.model.LedgerMessage;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

/**
 * This service provides methods to interact with the REST ledger for sending messages.
 */
public class RestLedgerMessageService extends RestServiceBase {

  private URI uri;

  /**
   * Constructs a new <code>RestLedgerMessageService</code> instance.
   * 
   * @param converter
   *  A converter to use when translating between ilp-core models and the native JSON models used
   *      by the REST ledger.
   * @param restTemplate
   *  A rest template to use for interacting with the REST ledger.
   * @param uri
   *  The URI of the messaging service provideded by the REST ledger.
   */
  public RestLedgerMessageService(RestLedgerJsonConverter converter, RestTemplate restTemplate,
      URI uri) {
    
    super(converter, restTemplate);
    this.uri = uri;
  }

  /**
   * Sends a message to the ledger.
   *
   * @param message
   *  The message to send.
   */
  public void sendMessage(LedgerMessage message) throws RestServiceException {

    JsonLedgerMessage jsonMessage = getConverter().convertLedgerMessage(message);

    try {
      log.debug("POST message");

      RequestEntity<JsonLedgerMessage> request = RequestEntity
          .post(uri)
          .contentType(MediaType.APPLICATION_JSON_UTF8).body(jsonMessage, JsonLedgerMessage.class);

      getRestTemplate().postForEntity(uri, request, String.class);

      // TODO Handle response?

    } catch (HttpStatusCodeException sce) {
      switch (sce.getStatusCode()) {
        case BAD_REQUEST:
          throw parseRestException(sce);
        case UNPROCESSABLE_ENTITY:
          throw new RestServiceException(
              "No listeners subscribed for messages to " + message.getTo(), sce);
        default:
          throw sce;
      }
    }

  }

}
