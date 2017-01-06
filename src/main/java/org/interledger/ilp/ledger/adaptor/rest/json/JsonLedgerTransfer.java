package org.interledger.ilp.ledger.adaptor.rest.json;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.money.Monetary;
import javax.money.MonetaryAmount;

import org.interledger.ilp.core.ledger.model.LedgerTransfer;
import org.interledger.ilp.ledger.adaptor.rest.RestLedgerAdaptor;
import org.interledger.ilp.ledger.client.exceptions.DataModelTranslationException;
import org.interledger.ilp.ledger.client.model.ClientLedgerTransfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class JsonLedgerTransfer {

  private List<JsonLedgerTransferAccountEntry> credits;
  private List<JsonLedgerTransferAccountEntry> debits;
  private URI executionCondition;
  private URI cancellationCondition;
  private ZonedDateTime expiresAt;
  private URI id;
  private URI ledgerId;

  @JsonSerialize(contentAs = JsonLedgerTransferAccountEntry.class)
  public List<JsonLedgerTransferAccountEntry> getCredits() {
    return credits;
  }

  @JsonDeserialize(contentAs = JsonLedgerTransferAccountEntry.class)
  public void setCredits(List<JsonLedgerTransferAccountEntry> credits) {
    this.credits = credits;
  }

  @JsonSerialize(contentAs = JsonLedgerTransferAccountEntry.class)
  public List<JsonLedgerTransferAccountEntry> getDebits() {
    return debits;
  }

  @JsonDeserialize(contentAs = JsonLedgerTransferAccountEntry.class)
  public void setDebits(List<JsonLedgerTransferAccountEntry> debits) {
    this.debits = debits;
  }

  @JsonProperty("execution_condition")
  public URI getExecutionCondition() {
    return executionCondition;
  }

  @JsonProperty("execution_condition")
  public void setExecutionCondition(URI executionCondition) {
    this.executionCondition = executionCondition;
  }

  @JsonProperty("cancellation_condition")
  public URI getCancellationCondition() {
    return cancellationCondition;
  }

  @JsonProperty("cancellation_condition")
  public void setCancellationCondition(URI cancellationCondition) {
    this.cancellationCondition = cancellationCondition;
  }

  @JsonProperty("expires_at")
  public ZonedDateTime getExpiresAt() {
    return expiresAt;
  }

  @JsonProperty("expires_at")
  public void setExpiresAt(ZonedDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }

  @JsonProperty("id")
  public URI getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(URI id) {
    this.id = id;
  }

  @JsonProperty("ledger")
  public URI getLedger() {
    return ledgerId;
  }

  @JsonProperty("ledger")
  public void setLedger(URI ledger) {
    this.ledgerId = ledger;
  }

  @Override
  public String toString() {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    } catch (JsonProcessingException jpe) {
      throw new RuntimeException(jpe);
    }
  }

  public LedgerTransfer toLedgerTransfer(RestLedgerAdaptor adaptor) {

    if (getCredits().size() != 1 || getDebits().size() != 1) {
      throw new RuntimeException("Only single transaction transfers are supported.");
    }

    JsonLedgerTransferAccountEntry creditEntry = getCredits().get(0);
    JsonLedgerTransferAccountEntry debitEntry = getDebits().get(0);

    ClientLedgerTransfer transfer = new ClientLedgerTransfer();
    transfer.setId(adaptor.getTransferUuid(getId()));
    
    // FIXME Process the debit and credit entries fully
    transfer.setToAccount(adaptor.getAccountAddress(creditEntry.getAccount()));
    transfer.setFromAccount(adaptor.getAccountAddress(debitEntry.getAccount()));
    
    MonetaryAmount debitAmount = Monetary.getDefaultAmountFactory()
        .setCurrency(adaptor.getLedgerInfo().getCurrencyUnit())
        .setNumber(new BigDecimal(debitEntry.getAmount()))
        .create();
    transfer.setAmount(debitAmount);
    transfer.setAuthorized(debitEntry.isAuthorized());
    
    if (debitEntry.getInvoice() != null) {
      transfer.setInvoice(debitEntry.getInvoice().toString());
    }
    
    if (debitEntry.getMemo() != null) {
      Object data = debitEntry.getMemo();
      if (data instanceof Map) {
        try {
          ObjectMapper mapper = new ObjectMapper();
          String dataValue = mapper.writeValueAsString(data);
          transfer.setData(dataValue.getBytes(Charset.forName("UTF-8")));
        } catch (JsonProcessingException e) {
          throw new RuntimeException("Unable to reserialize transfer data.", e);
        }
      } else {
        transfer.setData(Base64.getDecoder().decode(data.toString()));
      }
    }
    transfer.setRejected(debitEntry.isRejected());
    transfer.setRejectionMessage(debitEntry.getRejectionMessage());
    
    //FIXME Need Crypto-condition URI parser
//    transfer.setCancellationCondition(getCancellationCondition());
//    transfer.setExecutionCondition(getExecutionCondition());
    transfer.setExpiresAt(getExpiresAt());

    return transfer;
  }

  public static JsonLedgerTransfer fromLedgerTransfer(LedgerTransfer transfer,
      RestLedgerAdaptor ledgerAdaptor) {

    JsonLedgerTransfer jsonTransfer = new JsonLedgerTransfer();

    try {
      jsonTransfer.setId(ledgerAdaptor.getTransferIdentifier(transfer.getId()));
    } catch (Exception e) {
      throw new DataModelTranslationException("Invalid transfer ID.", transfer, e);
    }
    
    try {
      jsonTransfer.setLedger(new URI(ledgerAdaptor.getLedgerInfo().getId()));
    } catch (URISyntaxException e) {
      throw new DataModelTranslationException("Invalid ledger ID.", transfer, e);
    }
    

    List<JsonLedgerTransferAccountEntry> credits = new LinkedList<>();
    
    JsonLedgerTransferAccountEntry jsonCreditEntry = new JsonLedgerTransferAccountEntry();
    jsonCreditEntry.setAccount(ledgerAdaptor.getAccountIdentifier(transfer.getToAccount()));
    jsonCreditEntry.setAmount(transfer.getAmount().toString());
    if (transfer.getData() != null) {
      // TODO Undocumented assumptions made here.
      // If the provided data is valid UTF8 JSON then embed otherwise base64url encode and send as {
      // "base64url" : "<data>"}.
      String data = new String(transfer.getData(), Charset.forName("UTF-8"));
      if (JsonValidator.isValid(data)) {
        jsonCreditEntry.setMemo(data);
      } else {
        jsonCreditEntry.setMemo("{\"base64url\":\""
            + Base64.getUrlEncoder().encodeToString(transfer.getData()) + "\"}");
      }
    }
    credits.add(jsonCreditEntry);
    jsonTransfer.setCredits(credits);

    List<JsonLedgerTransferAccountEntry> debits = new LinkedList<>();
    
    JsonLedgerTransferAccountEntry jsonDebitEntry = new JsonLedgerTransferAccountEntry();
    jsonDebitEntry.setAccount(ledgerAdaptor.getAccountIdentifier(transfer.getFromAccount()));
    jsonDebitEntry.setAmount(transfer.getAmount().toString());
    if (transfer.getData() != null) {
      // TODO Undocumented assumptions made here.
      // If the provided data is valid UTF8 JSON then embed otherwise base64url encode and send as {
      // "base64url" : "<data>"}.
      String data = new String(transfer.getNoteToSelf(), Charset.forName("UTF-8"));
      if (JsonValidator.isValid(data)) {
        jsonDebitEntry.setMemo(data);
      } else {
        jsonDebitEntry.setMemo("{\"base64url\":\""
            + Base64.getUrlEncoder().encodeToString(transfer.getData()) + "\"}");
      }
    }
    jsonDebitEntry.setAuthorized(true);
    debits.add(jsonDebitEntry);
    jsonTransfer.setDebits(debits);

    jsonTransfer.setCancellationCondition(transfer.getCancellationCondition().getUri());
    jsonTransfer.setExecutionCondition(transfer.getExecutionCondition().getUri());
    jsonTransfer.setExpiresAt(transfer.getExpiresAt());

    return jsonTransfer;
  }


}
