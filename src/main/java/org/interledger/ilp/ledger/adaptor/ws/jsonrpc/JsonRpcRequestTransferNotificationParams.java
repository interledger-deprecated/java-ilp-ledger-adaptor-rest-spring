package org.interledger.ilp.ledger.adaptor.ws.jsonrpc;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.interledger.ilp.ledger.adaptor.rest.json.JsonLedgerTransfer;

/**
 * Defines the parameters used when making a transfer notification request over JSON-RPC.
 */
public class JsonRpcRequestTransferNotificationParams extends JsonRpcNotificationParams {

  private JsonLedgerTransfer transfer;

  @JsonProperty(value = "resource")
  public JsonLedgerTransfer getTransfer() {
    return this.transfer;
  }

  public void setTransfer(JsonLedgerTransfer message) {
    this.transfer = message;
  }
}
