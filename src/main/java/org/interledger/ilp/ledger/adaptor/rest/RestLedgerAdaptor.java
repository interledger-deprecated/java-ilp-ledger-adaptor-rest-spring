package org.interledger.ilp.ledger.adaptor.rest;

import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.ilp.InterledgerAddress;
import org.interledger.ilp.ledger.LedgerAdaptor;
import org.interledger.ilp.ledger.adaptor.rest.service.RestLedgerAccountService;
import org.interledger.ilp.ledger.adaptor.rest.service.RestLedgerAuthTokenService;
import org.interledger.ilp.ledger.adaptor.rest.service.RestLedgerJsonConverter;
import org.interledger.ilp.ledger.adaptor.rest.service.RestLedgerMessageService;
import org.interledger.ilp.ledger.adaptor.rest.service.RestLedgerMetaService;
import org.interledger.ilp.ledger.adaptor.rest.service.RestLedgerTransferService;
import org.interledger.ilp.ledger.adaptor.ws.JsonRpcLedgerWebSocketChannel;
import org.interledger.ilp.ledger.events.LedgerEventHandler;
import org.interledger.ilp.ledger.model.AccountInfo;
import org.interledger.ilp.ledger.model.LedgerInfo;
import org.interledger.ilp.ledger.model.LedgerMessage;
import org.interledger.ilp.ledger.model.LedgerTransfer;
import org.interledger.ilp.ledger.model.TransferRejectedReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A ledger adaptor implementation that currently adheres to the API of the Five Bells Ledger (
 * https://github.com/interledgerjs/five-bells-ledger) with a view to support the Common REST API 
 * https://github.com/interledger/rfcs/blob/2adeab20ce240302c0a30ed8473d88665319a5ed/0012-common-ledger-api/0012-common-ledger-api.md
 * when it is released.
 */
@Service
public class RestLedgerAdaptor implements LedgerAdaptor {

  private static final Logger log = LoggerFactory.getLogger(RestLedgerAdaptor.class);

  private UsernamePasswordAuthenticationToken accountAuthToken = null;
  
  private RestLedgerAccountService accountService;
  private RestLedgerAuthTokenService authTokenService;
  private RestLedgerTransferService transferService;
  private RestLedgerMessageService messageService;
  private RestLedgerMetaService metaService;

  private JsonRpcLedgerWebSocketChannel websocketChannel;

  private RestTemplateBuilder restTemplateBuilder;

  private LedgerEventHandler eventhandler;

  private Set<InterledgerAddress> connectors;

  private RestLedgerJsonConverter converter;

  /**
   * Constructs a new <code>RestLedgerAdaptor</code> instance.
   * 
   * @param restTemplateBuilder
   *  The spring rest template builder to use for constructing rest templates.
   * @param ledgerBaseUrl
   *  The base url of the ledger
   */
  public RestLedgerAdaptor(RestTemplateBuilder restTemplateBuilder, URI ledgerBaseUrl) {

    this.restTemplateBuilder = restTemplateBuilder;
    this.metaService = new RestLedgerMetaService(restTemplateBuilder.build(), ledgerBaseUrl);
  }

  /**
   * Connect the adaptor to the ledger.
   * <p>
   * The adaptor performs the following steps when it connects
   * <ol>
   * <li>Execute a GET against the base URL to get the ledger meta-data</li>
   * <li>Asynchronously open a web socket to the server.
   * <ol>
   * <li>Get an auth token.</li>
   * <li>Attempt to establish a connection.</li>
   * </ol>
   * <li>
   * </ol>
   * </p>
   */
  @Override
  public void connect() {

    metaService.getLedgerInfo(true);
    converter = metaService.getConverter();

    createWebsocket(metaService.getWebsocketUri());
    websocketChannel.open();
  }

  @Override
  public void disconnect() {

    throwIfNotConnected();

    // Disconnect the websocket
    try {
      this.websocketChannel.close();
    } catch (IOException ioException) {
      throw new UncheckedIOException("Error while closing websocket.", ioException);
    }
    
    this.websocketChannel = null;

    // Reset meta-data service
    metaService = new RestLedgerMetaService(restTemplateBuilder.build(), metaService.getBaseUri());
 
    //Clear connector list
    connectors = null;
  }
  
  @Override
  public boolean isConnected() {
    
    return (this.websocketChannel != null && this.websocketChannel.isOpen());
  }

  @Override
  public AccountInfo getAccountInfo(InterledgerAddress account) {

    URI accountId = converter.convertAccountAddressToUri(account);
    return getAccountService().getAccountInfo(accountId);
  }
  
  @Override
  public Set<InterledgerAddress> getConnectors() {
    
    throwIfNotConnected();

    if (connectors == null) {
      Set<URI> connectorIds = metaService.getConnectorIds();
      connectors = new HashSet<InterledgerAddress>(connectorIds.size());
      for (URI uri : connectorIds) {
        connectors.add(converter.convertAccountUriToAddress(uri));
      }
    }
    
    return Collections.unmodifiableSet(connectors);
  }

  @Override
  public LedgerInfo getLedgerInfo() {

    throwIfNotConnected();

    return metaService.getLedgerInfo();
  }

  @Override
  public void sendMessage(LedgerMessage msg) {

    getMessageService().sendMessage(msg);
  }

  @Override
  public void sendTransfer(LedgerTransfer transfer) {
    
    getTransferService().sendTransfer(transfer);
  }
  
  @Override
  public void rejectTransfer(LedgerTransfer transfer, TransferRejectedReason reason) {
    
    getTransferService().rejectTransfer(transfer, reason);
  }

  @Override
  public void fulfillTransfer(UUID transferId, Fulfillment fulfillment) {
    
    URI transferIdUri = converter.convertTransferUuidToUri(transferId);
    getTransferService().fulfillTransfer(transferIdUri, fulfillment);
  }

  @Override
  public void setEventHandler(LedgerEventHandler eventHandler) {
    
    this.eventhandler = eventHandler;
  }

  @Override
  public void subscribeToAccountNotifications(InterledgerAddress account) {
    
    URI accountId = converter.convertAccountAddressToUri(account);
    getAccountService().subscribeToAccountNotifications(accountId);
  }


  @Autowired(required = false)
  public void setAccountAuthToken(UsernamePasswordAuthenticationToken accountAuthToken) {
    
    this.accountAuthToken = accountAuthToken;
  }
  
  
  /**
   * Constructs a web socket channel that will be used for communications with the ledger.
   *
   * @param wsUri
   *  The websocket URI provided by the ledger
   */
  private void createWebsocket(URI wsUri) {

    if (this.websocketChannel == null || !this.websocketChannel.isOpen()) {
      if (this.authTokenService == null) {
        this.authTokenService = new RestLedgerAuthTokenService(
            getRestTemplateBuilderWithAuthIfAvailable().build(), metaService.getAuthTokenUri());
      }

      String token = this.authTokenService.getAuthToken();

      log.debug("Creating Notification Listener Service");

      if (wsUri == null || wsUri.getScheme() == null || !wsUri.getScheme().startsWith("ws")) {
        throw new RuntimeException("Invalid websocket URL: " + wsUri);
      }

      this.websocketChannel = new JsonRpcLedgerWebSocketChannel(wsUri, token, eventhandler,
          converter);
    }
  }

  /**
   * Convenience method to lazy load an account service and ensure that the adaptor is connected.
   *
   * @return
   *  An instance of {@link RestLedgerAccountService}
   */
  private RestLedgerAccountService getAccountService() {

    throwIfNotConnected();

    if (this.accountService == null) {
      log.debug("Creating Account Service");
      this.accountService = new RestLedgerAccountService(converter,
          getRestTemplateBuilderWithAuthIfAvailable().build(), this.websocketChannel);
    }

    return this.accountService;
  }
  
  /**
   * Convenience method to lazy load a message service and ensure that the adaptor is connected.
   *
   * @return
   *  An instance of {@link RestLedgerMessageService}
   */
  private RestLedgerMessageService getMessageService() {

    throwIfNotConnected();

    if (this.messageService == null) {
      log.debug("Creating Message Service");
      this.messageService = new RestLedgerMessageService(converter,
          getRestTemplateBuilderWithAuthIfAvailable().build(), metaService.getMessageUri());
    }

    return this.messageService;
  }

  /**
   * Convenience method to lazy load a transfer service and ensure that the adaptor is connected.
   *
   * @return
   *  An instance of {@link RestLedgerTransferService}
   */
  private RestLedgerTransferService getTransferService() {

    throwIfNotConnected();

    if (this.transferService == null) {
      log.debug("Creating Transfer Service");
      this.transferService = new RestLedgerTransferService(converter,
          getRestTemplateBuilderWithAuthIfAvailable().build());
    }

    return this.transferService;
  }

  /**
   * Convenience method to return a rest template builder configured with an authorization token
   * if one is available.
   */
  private RestTemplateBuilder getRestTemplateBuilderWithAuthIfAvailable() {

    if (accountAuthToken != null
        && (accountAuthToken.getPrincipal() != null && accountAuthToken.getCredentials() != null)) {

      return restTemplateBuilder.basicAuthorization(accountAuthToken.getPrincipal().toString(),
          accountAuthToken.getCredentials().toString());
    }

    return restTemplateBuilder;
  }

  /** validates that the adaptor is connected, otherwise throws a runtime exception. */
  private void throwIfNotConnected() {
    
    if (!isConnected()) {
      throw new RuntimeException("LedgerAdaptor is not connected.");
    }
  }
}
