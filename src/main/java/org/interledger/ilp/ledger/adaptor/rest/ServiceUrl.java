package org.interledger.ilp.ledger.adaptor.rest;

/**
 * Enumerates the URLs of the services provided by the REST ledger.
 */
public enum ServiceUrl {
  LEDGER("ledger"),
  HEALTH("health"),
  TRANSFER("transfer"),
  TRANSFER_REJECTION("transfer_rejection"),
  TRANSFER_FULFILLMENT("transfer_fulfillment"),
  TRANSFER_STATE("transfer_state"),
  ACCOUNT("account"),
  ACCOUNTS("accounts"),
  AUTH_TOKEN("auth_token"),
  WEBSOCKET("websocket"),
  MESSAGE("message");

  private String name;

  private ServiceUrl(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  /**
   * Returns the ServiceUrl associated with the given name.
   *
   * @param name
   *  The name of the service url.
   * @return
   *  The matching ServiceUrl if the name matches. Throws an exception otherwise.
   */
  public static ServiceUrl fromName(String name) {
    for (ServiceUrl url : ServiceUrl.values()) {
      if (url.getName().equals(name)) {
        return url;
      }
    }
    throw new RuntimeException("Unknown URL name: " + name);
  }

}

