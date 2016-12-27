package org.interledger.ilp.ledger.adaptor.rest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.interledger.ilp.core.ledger.LedgerAdaptor;
import org.interledger.ilp.core.ledger.events.LedgerEventHandler;
import org.interledger.ilp.core.ledger.model.Account;
import org.interledger.ilp.core.ledger.model.LedgerInfo;
import org.interledger.ilp.core.ledger.model.LedgerMessage;
import org.interledger.ilp.core.ledger.model.LedgerTransfer;
import org.interledger.ilp.core.ledger.model.TransferRejectedReason;
import org.interledger.ilp.ledger.adaptor.rest.exceptions.RestServiceException;
import org.interledger.ilp.ledger.adaptor.rest.json.JsonLedgerInfo;
import org.interledger.ilp.ledger.adaptor.rest.service.RestLedgerAccountService;
import org.interledger.ilp.ledger.adaptor.rest.service.RestLedgerAuthTokenService;
import org.interledger.ilp.ledger.adaptor.rest.service.RestLedgerMessageService;
import org.interledger.ilp.ledger.adaptor.rest.service.RestLedgerMetaService;
import org.interledger.ilp.ledger.adaptor.rest.service.RestLedgerTransferService;
import org.interledger.ilp.ledger.adaptor.ws.JsonRpcLedgerWebSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriTemplate;

@Service
public class RestLedgerAdaptor implements LedgerAdaptor {

  private static final Logger log = LoggerFactory.getLogger(RestLedgerAdaptor.class);

  private static final Pattern urlTemplateRegex = Pattern.compile("/\\:([A-Za-z0-9-]+)");

  private UsernamePasswordAuthenticationToken accountAuthToken = null;

  private RestLedgerAccountService accountService;
  private RestLedgerAuthTokenService authTokenService;
  private LedgerEventHandler eventhandler;
  private JsonLedgerInfo ledgerInfo = null;

  private RestLedgerMessageService messageService;

  private RestLedgerMetaService metaService;

  private RestTemplateBuilder restTemplateBuilder;

  private RestLedgerTransferService transferService;
  private Map<ServiceUrls, String> urls;

  private JsonRpcLedgerWebSocketChannel websocketChannel;

  public RestLedgerAdaptor(RestTemplateBuilder restTemplateBuilder, String ledgerBaseUrl) {

    this.restTemplateBuilder = restTemplateBuilder;

    urls = new HashMap<ServiceUrls, String>();
    urls.put(ServiceUrls.LEDGER, ledgerBaseUrl);

  }

  @Override
  public void connect() {

    // Get ledger info
    if (metaService == null) {
      metaService = new RestLedgerMetaService(this, restTemplateBuilder.build());
    }
    ledgerInfo = metaService.getLedgerInfo();

    // FIXME Have to fix all the URL templates because they use a non-standard format
    Map<String, URI> metaUrls = ledgerInfo.getUrls();
    for (String urlName : metaUrls.keySet()) {
      urls.put(ServiceUrls.fromName(urlName), fixUriTemplates(metaUrls.get(urlName).toString()));
    }

    if (ledgerInfo.getId() == null) {
      ledgerInfo.setId(URI.create(getServiceUrl(ServiceUrls.LEDGER)));
    }

    // Connect to socket for events
    createWebsocket();
    websocketChannel.open();

  }

  private void createWebsocket() throws RestServiceException {

    throwIfNotConnected();
    if (this.websocketChannel == null || !this.websocketChannel.isOpen()) {

      if (this.authTokenService == null) {
        this.authTokenService = new RestLedgerAuthTokenService(this,
            getRestTemplateBuilderWithAuthIfAvailable().build());
      }

      String token = this.authTokenService.getAuthToken();

      log.debug("Creating Notification Listener Service");
      URI wsUri = URI.create(getServiceUrl(ServiceUrls.WEBSOCKET));

      if (wsUri == null || wsUri.getScheme() == null || !wsUri.getScheme().startsWith("ws")) {
        throw new RuntimeException("Invalid websocket URL: " + wsUri);
      }

      this.websocketChannel = new JsonRpcLedgerWebSocketChannel(URI.create(getLedgerInfo().getId()),
          wsUri, token, eventhandler);
    }

  }

  @Override
  public void disconnect() {

    throwIfNotConnected();

    try {
      this.websocketChannel.close();
    } catch (IOException e) {
      throw new UncheckedIOException("Error while closing websocket.", e);
    }
    this.websocketChannel = null;

    ledgerInfo = null;
    String ledgerUri = getServiceUrl(ServiceUrls.LEDGER);
    urls.clear();
    urls.put(ServiceUrls.LEDGER, ledgerUri);

  }

  private String fixUriTemplates(String input) {
    return urlTemplateRegex.matcher(input.toString()).replaceAll("/\\{$1\\}");
  }

  @Override
  public Account getAccount(String accountName) {
    return getAccountService().getAccount(accountName);
  }

  // FIXME May be unsafe
  public URI getAccountIdentifier(String account) {

    URI id = URI.create(account);

    if (!id.isAbsolute()) {
      return new UriTemplate(getServiceUrl(ServiceUrls.ACCOUNT)).expand(account);
    }

    return id;
  }

  private RestLedgerAccountService getAccountService() {

    throwIfNotConnected();

    if (this.accountService == null) {
      log.debug("Creating Account Service");
      this.accountService = new RestLedgerAccountService(this,
          getRestTemplateBuilderWithAuthIfAvailable().build(), this.websocketChannel);
    }

    return this.accountService;

  }

  @Override
  public LedgerInfo getLedgerInfo() {

    throwIfNotConnected();

    return ledgerInfo;
  }

  @Override
  public String getNextTransferId() {
    return getTransferService().getNextTransferId();
  }

  private RestTemplateBuilder getRestTemplateBuilderWithAuthIfAvailable() {

    if (accountAuthToken != null
        && (accountAuthToken.getPrincipal() != null && accountAuthToken.getCredentials() != null)) {

      return restTemplateBuilder.basicAuthorization(accountAuthToken.getPrincipal().toString(),
          accountAuthToken.getCredentials().toString());

    }

    return restTemplateBuilder;

  }

  public String getServiceUrl(ServiceUrls name) {
    if (urls.containsKey(name)) {
      return urls.get(name);
    }
    throw new RuntimeException("No URL on record for name: " + name.getName());
  }

  private RestLedgerTransferService getTransferService() {

    throwIfNotConnected();

    if (this.transferService == null) {
      log.debug("Creating Transfer Service");
      this.transferService =
          new RestLedgerTransferService(this, getRestTemplateBuilderWithAuthIfAvailable().build());
    }

    return this.transferService;

  }

  public boolean isConnected() {
    return (this.ledgerInfo != null);
  }

  @Override
  public void rejectTransfer(LedgerTransfer transfer, TransferRejectedReason reason) {
    getTransferService().rejectTransfer(transfer, reason);
  }

  @Override
  public void sendMessage(LedgerMessage msg) {

    throwIfNotConnected();

    if (messageService == null) {
      messageService =
          new RestLedgerMessageService(this, getRestTemplateBuilderWithAuthIfAvailable().build());
    }

    messageService.sendMessage(msg);

  }

  @Override
  public void sendTransfer(LedgerTransfer transfer) {
    getTransferService().sendTransfer(transfer);
  }

  @Autowired(required = false)
  public void setAccountAuthToken(UsernamePasswordAuthenticationToken accountAuthToken) {
    this.accountAuthToken = accountAuthToken;
  }

  @Override
  public void setEventHandler(LedgerEventHandler eventHandler) {
    this.eventhandler = eventHandler;
  }

  @Override
  public void subscribeToAccountNotifications(String accountName) {
    getAccountService().subscribeToAccountNotifications(accountName);
  }

  private void throwIfNotConnected() {
    if (!isConnected()) {
      throw new RuntimeException("Client is not connected.");
    }
  }

}
