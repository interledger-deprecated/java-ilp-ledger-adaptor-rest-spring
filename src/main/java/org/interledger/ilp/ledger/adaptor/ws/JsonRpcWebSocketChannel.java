package org.interledger.ilp.ledger.adaptor.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.interledger.ilp.ledger.adaptor.ws.jsonrpc.JsonRpcMessage;
import org.interledger.ilp.ledger.adaptor.ws.jsonrpc.JsonRpcRequestMessage;
import org.interledger.ilp.ledger.adaptor.ws.jsonrpc.JsonRpcResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;

/**
 * Base class representing a JSON-RPC web socket channel.
 */
public abstract class JsonRpcWebSocketChannel implements Closeable {

  protected final Logger log = LoggerFactory.getLogger(this.getClass());

  private ObjectMapper jsonObjectMapper;
  private WebSocketSession session;
  private JsonRpcWebSocketHandler handler;
  private WebSocketConnectionManager connectionManager;
  private JsonRpcRequestResponseMapper responseMapper;

  private boolean isClosing = false;
  private boolean autoReconnect;
  private int maxConnectAttempts;
  private int connectAttempts = 0;

  /**
   * Constructs a new <code>JsonRpcWebSocketChannel</code> instance.
   * 
   * @param uri
   *  The URI of the websocket to connect to.
   * @param autoReconnect
   *  Indicates if the channel should automatically attempt to reconnect if disconnected.
   * @param maxConnectAttempts
   *  The maximum number of attempts to try when establishing a connection.
   */
  public JsonRpcWebSocketChannel(URI uri, boolean autoReconnect, int maxConnectAttempts) {
    
    this.jsonObjectMapper = new ObjectMapper();
    this.handler = new JsonRpcWebSocketHandler(this, jsonObjectMapper);
    
    this.connectionManager = new WebSocketConnectionManager(new StandardWebSocketClient(), handler,
        uri.toString());
    
    this.responseMapper = new JsonRpcRequestResponseMapper(5 * 60 * 1000);
    this.autoReconnect = autoReconnect;
    this.maxConnectAttempts = maxConnectAttempts;
  }

  /**
   * Attempts to open (connect) the websocket channel.
   */
  public void open() {
    
    connectAttempts++;
    connectionManager.start();
    // TODO Wait for connected event - this is a synchronous operation?
  }

  @Override
  public void close() throws IOException {
    
    isClosing = true;
    session.close();
    connectionManager.stop();
  }

  /** Indicates whether the channel is able to send data. */
  public boolean canSend() {
    
    return session.isOpen();
  }

  /** Indicates whether the channel is open or not.*/
  public boolean isOpen() {
    
    return connectionManager.isRunning();
  }

  /**
   * Sends a JSON-RPC request over the channel.
   *
   * @param request
   *  The JSON-RPC request to send.
   * @param responseHandler
   *  A handler to invoke when a response is recieved.
   */
  public void sendRpcRequest(JsonRpcRequestMessage request,
      JsonRpcResponseHandler responseHandler) {

    if (request.getId() == null && responseHandler != null) {
      throw new IllegalArgumentException(
          "No request ID provided. ResponseHandler can only be used when a request ID is present.");
    }

    if (request.getId() != null) {
      responseMapper.storeRequest(request, responseHandler);
    }

    try {
      String rpcPayload =
          jsonObjectMapper.writeValueAsString(request).replaceAll("\r", "").replaceAll("\n", "");
      log.trace("Sending Json Rpc message: " + rpcPayload);
      session.sendMessage(new TextMessage(rpcPayload));
    } catch (JsonProcessingException jpe) {
      log.error("Error serializing Json Rpc message.", jpe);
    } catch (IOException ioe) {
      throw new UncheckedIOException("Error sending message via websocket.", ioe);
    }
  }

  /**
   * Performs any activities required once a connection has been established.
   *
   * @param session
   *  The web socket session that has been established.
   */
  public void onConnectionEstablished(WebSocketSession session) {
    
    this.session = session;
    this.connectAttempts = 0;
  }

  /**
   * Subclasses should implement this method to processes messages received from the web socket
   * channel.
   *
   * @param message
   *  The message received from the channel.
   */
  public void onMessage(JsonRpcMessage message) {}

  /**
   * Subclasses must implement this method to handle errors during message transport.
   *
   * @param exception
   *  The exception encountered while transporting a message.
   */
  public abstract void onTransportError(Throwable exception);

  /**
   * Performs any activities required when the underlying connection is closed. 
   *
   * @param status
   *  Represents a WebSocket close status code and reason.
   */
  public void onConnectionClosed(CloseStatus status) {

    if (autoReconnect && !isClosing && (connectAttempts < maxConnectAttempts)) {

      // Reconnecting
      log.debug(
          "Attempting to reconnect. " + "Attempt " + connectAttempts + " of " + maxConnectAttempts);
      if (!this.session.isOpen()) {
        connectionManager.stop();
      }

      connectionManager.start();
    }
  }

  /**
   * Processes a response message received from the channel.
   *
   * @param response
   *  The response received.
   */
  public void onResponse(JsonRpcResponseMessage response) {
    if (response.getId() != null) {
      responseMapper.handleResponse(response);
    }
  }

  /** Provides access to the underlying web socket handler. */ 
  protected JsonRpcWebSocketHandler getHandler() {
    return this.handler;
  }

  /** Provides access to the underlying web socket session. */ 
  protected WebSocketSession getSession() {
    return this.session;
  }
  
}
