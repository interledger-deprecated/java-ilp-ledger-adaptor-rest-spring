package org.interledger.ilp.ledger.adaptor.rest;

import java.net.URI;

import org.interledger.ilp.ledger.model.LedgerInfo;
import org.interledger.ilp.ledger.adaptor.rest.RestLedgerAdaptor;
import org.interledger.ilp.ledger.adaptor.rest.ServiceUrl;
import org.springframework.boot.web.client.RestTemplateBuilder;

public class MockRestClient extends RestLedgerAdaptor {

  private String ledgerBaseUrl;

  public MockRestClient(RestTemplateBuilder restTemplateBuilder, URI ledgerBaseUrl) {
    super(restTemplateBuilder, ledgerBaseUrl);
    this.ledgerBaseUrl = ledgerBaseUrl.toString();
  }

  @Override
  public LedgerInfo getLedgerInfo() {
    // TODO Implement mock
    return null;
  }

  public String getServiceUrl(ServiceUrl name) {

    switch (name) {
      case ACCOUNT:
        return this.ledgerBaseUrl + "/accounts/{id}";
      case ACCOUNTS:
        return this.ledgerBaseUrl + "/accounts";
      case AUTH_TOKEN:
        return this.ledgerBaseUrl + "/auth_token";
      case HEALTH:
        return this.ledgerBaseUrl + "/health";
      case LEDGER:
        return this.ledgerBaseUrl;
      case MESSAGE:
        return this.ledgerBaseUrl + "/message";
      case TRANSFER:
        return this.ledgerBaseUrl + "/transfer/{id}";
      case TRANSFER_FULFILLMENT:
        return this.ledgerBaseUrl + "/transfer/{id}/fulfillment";
      case TRANSFER_REJECTION:
        return this.ledgerBaseUrl + "/transfer/{id}/rejection";
      case TRANSFER_STATE:
        return this.ledgerBaseUrl + "/transfer/{id}/state";
      case WEBSOCKET:
        return this.ledgerBaseUrl.replaceFirst("http", "ws");
      default:
        throw new RuntimeException("Unknown url name");
    }

  }



}
