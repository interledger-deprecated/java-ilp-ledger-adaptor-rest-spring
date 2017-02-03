package org.interledger.ilp.ledger.adaptor.ws.jsonrpc;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.interledger.ilp.ledger.adaptor.rest.json.JsonLedgerMessage;

/**
 * Represents the parameters used in a request message notification.
 */
public class JsonRpcRequestMessageNotificationParams extends JsonRpcNotificationParams {

  private JsonLedgerMessage message;

  @JsonProperty(value = "resource")
  public JsonLedgerMessage getMessage() {
    return this.message;
  }

  public void setMessage(JsonLedgerMessage message) {
    this.message = message;
  }

}
