package org.interledger.ilp.ledger.adaptor.rest.service;

import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.ilp.ledger.adaptor.rest.json.JsonLedgerTransfer;
import org.interledger.ilp.ledger.model.LedgerTransfer;
import org.interledger.ilp.ledger.model.TransferRejectedReason;
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

import java.net.URI;
import java.util.Base64;

/**
 * This service provides methods to interact with the REST ledger for transfers.
 */
public class RestLedgerTransferService extends RestServiceBase {
  
  private static final Logger log = LoggerFactory.getLogger(RestLedgerTransferService.class);

  /**
   * Constructs a new <code>RestLedgerTransferService</code> instance.
   * 
   * @param converter
   *  A converter to use when translating from ilp-core models to the ledgers native JSON model.
   * @param restTemplate
   *  A rest template to use for interacting with the REST ledger.
   */
  public RestLedgerTransferService(RestLedgerJsonConverter converter, RestTemplate restTemplate) {
    
    super(converter, restTemplate);
  }


  /**
   * Sends a transfer to the ledger.
   *
   * @param transfer
   *  The transfer to send.
   */
  public void sendTransfer(LedgerTransfer transfer) {
    
    try {

      JsonLedgerTransfer jsonTransfer = getConverter().convertLedgerTransfer(transfer);

      log.debug("PUT Transfer - id : {}", jsonTransfer.getId());

      RequestEntity<JsonLedgerTransfer> request = RequestEntity.put(jsonTransfer.getId())
          .contentType(MediaType.APPLICATION_JSON_UTF8)
          .body(jsonTransfer, JsonLedgerTransfer.class);
      
      ResponseEntity<JsonLedgerTransfer> rsp = getRestTemplate().exchange(request,
          JsonLedgerTransfer.class);

      log.trace("Transfer Response: " + rsp.getBody());

    } catch (HttpStatusCodeException sce) {
      switch (sce.getStatusCode()) {
        case BAD_REQUEST:
        case NOT_FOUND:
          throw parseRestException(sce);
        default:
          throw sce;
      }
    }
  }

  /**
   * Reject the transfer with the given reason.
   *
   * @param transfer
   *  The transfer to reject.
   * @param reason
   *  The reason for rejecting the transfer.
   */
  public void rejectTransfer(LedgerTransfer transfer, TransferRejectedReason reason) {

    log.debug("Rejecting Transfer - id : {}", transfer.getId());

    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.TEXT_PLAIN);

      HttpEntity<Object> rejectionRequest = new HttpEntity<>(reason.toString(), headers);
      
      URI rejectTransferUri = getConverter().convertRejectTransferToUri(transfer.getId());
      
      getRestTemplate().exchange(
          rejectTransferUri,
          HttpMethod.PUT, rejectionRequest, String.class);

    } catch (HttpStatusCodeException sce) {
      switch (sce.getStatusCode()) {
        case BAD_REQUEST:
        case NOT_FOUND:
          throw parseRestException(sce);
        default:
          throw sce;
      }
    }
  }
  
  /**
   * Notify the ledger of a fulfillment of a transfer.
   *
   * @param transferIdUri
   *  The URI of the transfer on the ledger.
   * @param fulfillment
   *  The fulfillment to send to the ledger.
   */
  public void fulfillTransfer(URI transferIdUri, Fulfillment fulfillment) {

    log.debug("Submitting Fulfillment for Transfer - id : {}", transferIdUri);

    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.TEXT_PLAIN);

      String fulfillmentBase64url = Base64.getUrlEncoder().encodeToString(fulfillment.getEncoded());
      HttpEntity<Object> fulfillmentRequest = new HttpEntity<>(fulfillmentBase64url, headers);
      
      getRestTemplate().exchange(
          transferIdUri,
          HttpMethod.PUT, fulfillmentRequest, String.class);

    } catch (HttpStatusCodeException sce) {
      switch (sce.getStatusCode()) {
        case BAD_REQUEST:
        case UNPROCESSABLE_ENTITY:
          throw parseRestException(sce);
        default:
          throw sce;
      }
    }
  }
}
