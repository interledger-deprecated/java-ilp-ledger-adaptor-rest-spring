package org.interledger.ilp.ledger.adaptor.ws.jsonrpc;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.interledger.ilqp.json.JsonErrorResponseEnvelope;
import org.interledger.ilqp.json.JsonMessageEnvelope;
import org.junit.Test;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonRpcMessageParsingTests {

  private String getJson(String filename) throws IOException, URISyntaxException {
    Path file = Paths.get(getClass().getResource(filename).toURI());
    return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
  }

  @Test
  public final void parseJsonRpcConnect()
      throws JsonParseException, JsonMappingException, IOException, URISyntaxException {

    ObjectMapper mapper = new ObjectMapper();
    String message = getJson("connect_notification.json");
    JsonRpcMessage rpcMessage = mapper.readValue(message, JsonRpcMessage.class);
    Assert.isTrue(rpcMessage instanceof JsonRpcConnectNotification);

  }

  @Test
  public final void parseJsonRpcResponse()
      throws JsonParseException, JsonMappingException, IOException, URISyntaxException {

    ObjectMapper mapper = new ObjectMapper();
    String message = getJson("success_response.json");
    JsonRpcMessage rpcMessage = mapper.readValue(message, JsonRpcMessage.class);
    Assert.isTrue(rpcMessage instanceof JsonRpcResponseMessage);

  }

  @Test
  public final void parseJsonRpcMessageNotificationRequest()
      throws JsonParseException, JsonMappingException, IOException, URISyntaxException {

    ObjectMapper mapper = new ObjectMapper();
    String message = getJson("message_notification.json");
    JsonRpcMessage rpcMessage = mapper.readValue(message, JsonRpcMessage.class);
    Assert.isTrue(rpcMessage instanceof JsonRpcNotification);
    JsonRpcNotification rpcNotification = (JsonRpcNotification) rpcMessage;

    Assert.isInstanceOf(JsonRpcRequestMessageNotificationParams.class, rpcNotification.getParams());

    // FIXME This is wrong
    // JsonRpcRequestMessageNotificationParams params = (JsonRpcRequestMessageNotificationParams)
    // rpcNotification.getParams();
    // assertEquals(params.getMessage().getData(), "message");

  }

  @Test
  public final void parseClientErrorMessageEnevelope()
      throws JsonParseException, JsonMappingException, IOException, URISyntaxException {

    ObjectMapper mapper = new ObjectMapper();
    String message = getJson("error_message_envelope.json");
    JsonMessageEnvelope rpcMessage = mapper.readValue(message, JsonMessageEnvelope.class);
    Assert.isTrue(rpcMessage instanceof JsonErrorResponseEnvelope);

  }


  @Test
  public final void parseJsonRpcTransferNotificationRequest()
      throws JsonParseException, JsonMappingException, IOException {

    // FIXME This test needs to be updated to use a file based message

    ObjectMapper mapper = new ObjectMapper();
    String message = "{" + "\"jsonrpc\":\"2.0\"," + "\"method\":\"notify\"," + "\"id\": null, "
        + "\"params\":{" + "\"event\":\"transfer.create\"," + "\"id\":\"" + UUID.randomUUID()
        + "\", " + "\"resource\": {" + "\"id\":\"adrian\"," + "\"ledger\":\"andrew\","
        + "\"amount\":\"1000\"" + "}" + "}" + "}";

    JsonRpcMessage rpcMessage = mapper.readValue(message, JsonRpcMessage.class);
    Assert.isInstanceOf(JsonRpcNotification.class, rpcMessage);

    JsonRpcNotification rpcNotification = (JsonRpcNotification) rpcMessage;
    Assert.isInstanceOf(JsonRpcRequestTransferNotificationParams.class,
        rpcNotification.getParams());

    // JsonRpcRequestTransferNotificationParams params = (JsonRpcRequestTransferNotificationParams)
    // rpcNotification.getParams();
    // assertEquals(params.getTransfer().getId(), "adrian");
  }

}
