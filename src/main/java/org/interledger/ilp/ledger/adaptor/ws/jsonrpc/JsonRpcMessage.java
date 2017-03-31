package org.interledger.ilp.ledger.adaptor.ws.jsonrpc;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Defines a basic JSON-RPC message, see http://json-rpc.org
 */
@JsonDeserialize(using = JsonRpcMessageDeserializer.class)
public abstract class JsonRpcMessage {

  private static String VERSION = "2.0";
  private String id;

  @JsonProperty(value = "jsonrpc")
  public String getVersion() {
    return VERSION;
  }

  @JsonProperty(value = "id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  /**
   * Sets the JSON-RPC version. Currently only 2.0 is supported.
   */
  public void setVersion(String version) {
    
    if (!VERSION.equals(version)) {
      throw new RuntimeException("Invalid JSON-RPC version: " + version);
    }
  }

  // TODO Need a better toString()
}
