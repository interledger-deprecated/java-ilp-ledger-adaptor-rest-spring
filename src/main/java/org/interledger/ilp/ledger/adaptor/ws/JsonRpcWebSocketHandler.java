package org.interledger.ilp.ledger.adaptor.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.interledger.ilp.ledger.adaptor.ws.jsonrpc.JsonRpcMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * A JSON-RPC based extension to a web socket handler.
 */
public class JsonRpcWebSocketHandler extends TextWebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(JsonRpcWebSocketHandler.class);

  private final ObjectMapper mapper;
  private JsonRpcWebSocketChannel channel;

  /**
   * Constructs a new <code>JsonRpcWebSocketHandler</code> instance.
   * @param channel
   *  The channel that this handler belongs to.
   * @param mapper
   *  An object mapper to use when parsing JSON messages.
   */
  public JsonRpcWebSocketHandler(JsonRpcWebSocketChannel channel, ObjectMapper mapper) {
    
    this.mapper = mapper;
    this.channel = channel;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    
    log.debug("Opening session id {} ", session.getId());
    this.channel.onConnectionEstablished(session);
    super.afterConnectionEstablished(session);
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    
    log.trace("Json Rpc message received: " + message.getPayload());
    try {
      JsonRpcMessage rpcMessage = mapper.readValue(message.getPayload(), JsonRpcMessage.class);
      this.channel.onMessage(rpcMessage);
    } catch (JsonProcessingException jpe) {
      log.error("Invalid json-rpc message received:\n {}", message.getPayload(), jpe);
      throw jpe;
    }
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    
    log.error("Error in session id {}", session.getId(), exception);
    this.channel.onTransportError(exception);
    super.handleTransportError(session, exception);
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    
    log.debug("Closing session id {}, close status is {} ", session.getId(), status);
    this.channel.onConnectionClosed(status);
    super.afterConnectionClosed(session, status);
  }

}
