package org.interledger.ilp.ledger.adaptor.rest.service;

import org.interledger.ilp.ledger.adaptor.rest.exceptions.AdaptorStateException;
import org.interledger.ilp.ledger.adaptor.rest.exceptions.RestServiceException;
import org.interledger.ilp.ledger.adaptor.rest.json.JsonAccountInfo;
import org.interledger.ilp.ledger.adaptor.ws.JsonRpcLedgerWebSocketChannel;
import org.interledger.ilp.ledger.adaptor.ws.jsonrpc.JsonRpcResponseMessage;
import org.interledger.ilp.ledger.adaptor.ws.jsonrpc.JsonRpcSubscribeAccountRequest;
import org.interledger.ilp.ledger.adaptor.ws.jsonrpc.JsonRpcSubscribeAccountRequestParams;
import org.interledger.ilp.ledger.model.AccountInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This service provides methods to interact with the REST ledger for account related activity.
 */
public class RestLedgerAccountService extends RestServiceBase {
  
  private static final Logger log = LoggerFactory.getLogger(RestLedgerAccountService.class);

  private JsonRpcLedgerWebSocketChannel websocketChannel;

  /**
   * Constructs a new <code>RestLedgerAccountService</code> instance.
   * 
   * @param converter
   *  A converter used to translate between the JSON based ledger models and the standard models
   *      defined in ilp-core.
   * @param restTemplate
   *  The rest template to use when interacting with the REST ledger.
   * @param websocketChannel
   *  The web socket channel to use when interacting with the ledger over websocket.
   */
  public RestLedgerAccountService(RestLedgerJsonConverter converter, RestTemplate restTemplate,
      JsonRpcLedgerWebSocketChannel websocketChannel) {
    
    super(converter, restTemplate);
    this.websocketChannel = websocketChannel;
  }


  /**
   * Retrieves information about the given account from the ledger.
   *
   * @param accountId
   *  The account identifier, as represented by a URI on the REST ledger
   * @return
   *  The account information, if it could be retrieved.
   */
  public AccountInfo getAccountInfo(URI accountId) throws RestServiceException {
    
    try {
      log.debug("GET Account: name = " + accountId);

      JsonAccountInfo jsonAccount = getRestTemplate().getForObject(accountId,
          JsonAccountInfo.class);

      return getConverter().convertJsonAccountInfo(jsonAccount);
    } catch (HttpStatusCodeException sce) {
      switch (sce.getStatusCode()) {
        case BAD_REQUEST:
        // deliberately fall through
        case NOT_FOUND:
          throw parseRestException(sce);
        default:
          throw sce;
      }
    }
  }

  /**
   * Subscribes the web socket channel to receive account notifications for the given account from
   * the ledger.
   *
   * @param accountId
   *  The account to subscribe to for notifications, as represented by a URI on the REST ledger.
   */
  public void subscribeToAccountNotifications(URI accountId) throws AdaptorStateException {

    if (!websocketChannel.canSend()) {
      throw new AdaptorStateException("Websocket is disconnected. No session available to send.");
    }

    log.debug("Subscribing to notifications for: " + accountId.toString());

    // TODO Generate JsonRpcRequests from a factory
    JsonRpcSubscribeAccountRequest subscribeRequest = new JsonRpcSubscribeAccountRequest();
    subscribeRequest.setId(UUID.randomUUID().toString());

    JsonRpcSubscribeAccountRequestParams params = new JsonRpcSubscribeAccountRequestParams();
    subscribeRequest.setParams(params);

    List<URI> accParam = new ArrayList<>();
    params.setAccounts(accParam);
    accParam.add(accountId);

    params.setEventType("*");

    websocketChannel.sendRpcRequest(subscribeRequest, (request, response) -> {
      if (response.getError() != null) {
        log.error("JsonRpcResponseMessage error: " + response.getError().getMessage());
        //TODO: shoudlnt we raise an exception here? silently failing is probably wrong
        return;
      }

      if (JsonRpcResponseMessage.isSuccess(response)) {
        log.info("Subscribed to notifications for: " + accountId);
      } else {
        log.error("Unable to subscribe to notifications for: " + accountId);
        //TODO: as above - if subscription fails, we should be more vocal.
      }

      log.debug(response.getResult().toString());
    });
  }

}
