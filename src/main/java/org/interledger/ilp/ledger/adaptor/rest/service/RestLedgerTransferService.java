package org.interledger.ilp.ledger.adaptor.rest.service;

import java.net.URI;
import java.util.UUID;

import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.ilp.core.ledger.model.LedgerTransfer;
import org.interledger.ilp.core.ledger.model.TransferRejectedReason;
import org.interledger.ilp.ledger.adaptor.rest.json.JsonLedgerTransfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

public class RestLedgerTransferService extends RestServiceBase {
  
  public RestLedgerTransferService(RestLedgerJsonConverter converter, RestTemplate restTemplate) {
    super(converter, restTemplate);
  }

  private static final Logger log = LoggerFactory.getLogger(RestLedgerTransferService.class);

  public void sendTransfer(LedgerTransfer transfer) {
    try {

      JsonLedgerTransfer jsonTransfer = getConverter().convertLedgerTransfer(transfer);

      log.debug("PUT Transfer - id : {}", jsonTransfer.getId());

      RequestEntity<JsonLedgerTransfer> request =
          RequestEntity.put(jsonTransfer.getId()).contentType(MediaType.APPLICATION_JSON_UTF8)
              .body(jsonTransfer, JsonLedgerTransfer.class);
      ResponseEntity<JsonLedgerTransfer> rsp =
          getRestTemplate().exchange(request, JsonLedgerTransfer.class);

      log.trace("Transfer Response: " + rsp.getBody());

    } catch (HttpStatusCodeException e) {
      switch (e.getStatusCode()) {
        case BAD_REQUEST:
        case NOT_FOUND:
          throw parseRestException(e);
        default:
          throw e;
      }
    }
  }

  public void rejectTransfer(LedgerTransfer transfer, TransferRejectedReason reason) {

    log.debug("Rejecting Transfer - id : {}", transfer.getId());

    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.TEXT_PLAIN);

      HttpEntity<Object> rejectionRequest = new HttpEntity<>(reason.toString(), headers);
      
      URI rejectTransferUri = getConverter().getRejectTransferUri(transfer.getId());
      
      getRestTemplate().exchange(
          rejectTransferUri,
          HttpMethod.PUT, rejectionRequest, String.class);

    } catch (HttpStatusCodeException e) {
      switch (e.getStatusCode()) {
        case BAD_REQUEST:
        case NOT_FOUND:
          throw parseRestException(e);
        default:
          throw e;
      }
    }
  }
  
  public void fulfillTransfer(String transferId, Fulfillment fulfillment) {
    //TODO Implement fulfill transfer

  }

  public String getNextTransferId() {
    return getConverter().convertTransferUuidToUri(UUID.randomUUID()).toString();
  }

}
