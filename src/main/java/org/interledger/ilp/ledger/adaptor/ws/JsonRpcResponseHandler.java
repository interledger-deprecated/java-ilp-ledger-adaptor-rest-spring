package org.interledger.ilp.ledger.adaptor.ws;

import org.interledger.ilp.ledger.adaptor.ws.jsonrpc.JsonRpcRequestMessage;
import org.interledger.ilp.ledger.adaptor.ws.jsonrpc.JsonRpcResponseMessage;

@FunctionalInterface
public interface JsonRpcResponseHandler {

  /**
   * Defines a callback for responses to be processed.
   *
   * @param request
   *  The initial request that was made.
   * @param response
   *  The response received for the request.
   */
  void handleResponse(JsonRpcRequestMessage request, JsonRpcResponseMessage response);

}
