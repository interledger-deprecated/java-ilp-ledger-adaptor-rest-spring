package org.interledger.ilp.ledger.adaptor.rest.json;

import java.math.BigDecimal;
import java.net.URI;

import javax.money.Monetary;
import javax.money.MonetaryAmount;

import org.interledger.ilp.core.ledger.model.AccountInfo;
import org.interledger.ilp.core.ledger.model.LedgerInfo;
import org.interledger.ilp.ledger.adaptor.rest.RestLedgerAdaptor;
import org.interledger.ilp.ledger.client.exceptions.DataModelTranslationException;
import org.interledger.ilp.ledger.client.model.ClientAccountInfo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonAccount {

  private URI ledger;
  private URI id;
  private String name;
  
  private String balance;
  private String certificateFingerprint;
  private boolean isAdmin;
  private boolean isDisabled;
  private String minimumAllowedBalance;
  private String password;
  private String publicKey;

  @JsonProperty(value = "balance")
  public String getBalance() {
    return balance;
  }

  @JsonProperty(value = "fingerprint")
  public String getCertificateFingerprint() {
    return certificateFingerprint;
  }

  @JsonProperty(value = "id")
  public URI getId() {
    return id;
  }

  @JsonProperty(value = "ledger")
  public URI getLedger() {
    return ledger;
  }

  @JsonProperty(value = "minimum_allowed_balance")
  public String getMinimumAllowedBalance() {
    return minimumAllowedBalance;
  }

  @JsonProperty(value = "name")
  public String getName() {
    return name;
  }

  @JsonProperty(value = "password")
  public String getPassword() {
    return password;
  }

  @JsonProperty(value = "public_key")
  public String getPublicKey() {
    return publicKey;
  }

  @JsonProperty(value = "is_admin")
  public boolean isAdmin() {
    return isAdmin;
  }

  @JsonProperty(value = "is_disabled")
  public boolean isDisabled() {
    return isDisabled;
  }

  public void setAdmin(boolean isAdmin) {
    this.isAdmin = isAdmin;
  }

  public void setBalance(String balance) {
    this.balance = balance;
  }

  public void setCertificateFingerprint(String certificateFingerprint) {
    this.certificateFingerprint = certificateFingerprint;
  }

  public void setDisabled(boolean isDisabled) {
    this.isDisabled = isDisabled;
  }

  public void setId(URI id) {
    this.id = id;
  }

  public void setLedger(URI ledger) {
    this.ledger = ledger;
  }

  public void setMinimumAllowedBalance(String minimumAllowedBalance) {
    this.minimumAllowedBalance = minimumAllowedBalance;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }

  public AccountInfo toAccount(RestLedgerAdaptor adaptor) {
    
    LedgerInfo ledgerInfo = adaptor.getLedgerInfo();
    ClientAccountInfo accountInfo = new ClientAccountInfo();
    
    //TODO URI normalization issue?
    if(ledgerInfo.getId().equals(getLedger().toString())) {
      accountInfo.setLedger(ledgerInfo.getAddressPrefix());
    } else {
      throw new DataModelTranslationException("Unable to determine ledger prefix for ledger with id: " + getLedger().toString(), this);
    }
    
    //Translate account id into address
    accountInfo.setId(getId().toString());
    accountInfo.setAddress(adaptor.getAccountAddress(getId()));
    accountInfo.setName(getName());
    accountInfo.setIsDisabled(isDisabled());

    //Translate amounts
    MonetaryAmount balance = Monetary.getDefaultAmountFactory()
        .setCurrency(ledgerInfo.getCurrencyUnit())
        .setNumber(new BigDecimal(getBalance()))
        .create();
    accountInfo.setBalance(balance);
    if("infinity".equalsIgnoreCase(getMinimumAllowedBalance())) {
      accountInfo.setMinimumAllowedBalance(null);
    } else {
      MonetaryAmount minBalance = Monetary.getDefaultAmountFactory()
          .setCurrency(ledgerInfo.getCurrencyUnit())
          .setNumber(new BigDecimal(getMinimumAllowedBalance()))
          .create();
      accountInfo.setMinimumAllowedBalance(minBalance);
    }
    
    //TODO Is this base64 encoded?
    accountInfo.setCertificateFingerprint(null);
    
    //TODO Decode and build key
    accountInfo.setPublicKey(null);
    
    return accountInfo;
  }

  @Override
  public String toString() {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

}
