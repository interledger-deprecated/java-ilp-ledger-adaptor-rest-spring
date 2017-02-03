package org.interledger.ilp.ledger.adaptor.rest.service;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.interledger.ilp.InterledgerAddress;
import org.interledger.ilp.client.model.ClientLedgerTransfer;
import org.interledger.ilp.ledger.model.TransferRejectedReason;
import org.interledger.ilp.ledger.money.format.LedgerSpecificDecimalMonetaryAmountFormat;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.UUID;

import javax.money.Monetary;
import javax.money.format.MonetaryAmountFormat;

/**
 * Basic test of the {@link RestLedgerTransferService} class.
 */
public class RestLedgerTransferServiceTest extends RestLedgerServiceTestBase {

  @Test
  public void test_sendLedgerTransfer() {
    UUID transferId = UUID.fromString("155dff3f-4915-44df-a707-acc4b527bcbd");
    
    Resource responseBody = new ClassPathResource("ledger_transfer_simple.json", this.getClass());

    mockServer.expect(requestTo(BASE_URL + "/transfers/" + transferId.toString()))
        .andExpect(method(HttpMethod.PUT))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

    MonetaryAmountFormat format = new LedgerSpecificDecimalMonetaryAmountFormat(
        Monetary.getCurrency("ZAR"), 10, 2);

    ClientLedgerTransfer transfer = new ClientLedgerTransfer();
    transfer.setId(transferId);
    transfer.setFromAccount(new InterledgerAddress("za.zar.ledger.alice"));
    transfer.setToAccount(new InterledgerAddress("za.zar.ledger.bob"));
    transfer.setAmount(format.parse("10"));

    RestLedgerTransferService service = new RestLedgerTransferService(converter, restTemplate);
    service.sendTransfer(transfer);

    mockServer.verify();
  }
  
  @Test
  public void sendLedgerRejectionSuccess() throws Exception {
    UUID transferId = UUID.randomUUID();

    mockServer.expect(requestTo(BASE_URL + "/transfers/" + transferId.toString() + "/rejection"))
        .andExpect(method(HttpMethod.PUT)).andExpect(content().contentType(MediaType.TEXT_PLAIN))
        .andExpect(content().string("REJECTED_BY_RECEIVER"))
        .andRespond(withSuccess("REJECTED_BY_RECEIVER", MediaType.TEXT_PLAIN));

    // the only thing required is the id.
    ClientLedgerTransfer transfer = new ClientLedgerTransfer();
    transfer.setId(transferId);

    RestLedgerTransferService service = new RestLedgerTransferService(converter, restTemplate);

    service.rejectTransfer(transfer, TransferRejectedReason.REJECTED_BY_RECEIVER);

    mockServer.verify();
  }

}
