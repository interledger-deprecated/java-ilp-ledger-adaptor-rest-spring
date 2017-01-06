package org.interledger.ilp.ledger.adaptor.rest.json;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryException;
import javax.money.UnknownCurrencyException;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryFormats;

import org.interledger.ilp.core.InterledgerAddress;
import org.interledger.ilp.core.ledger.model.LedgerInfo;
import org.interledger.ilp.ledger.client.exceptions.DataModelTranslationException;
import org.interledger.ilp.ledger.client.model.ClientLedgerInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonLedgerInfo {

  private String conditionSignPublicKey;
  private List<JsonConnectorInfo> connectors;
  private String currencyCode;
  private String currencySymbol;
  private URI id;
  private String ledgerPrefix;
  private String notificationSignPublicKey;
  private int precision;
  private int scale;
  private Map<String, String> urls;

  @JsonProperty(value = "condition_sign_public_key")
  public String getConditionSignPublicKey() {
    return this.conditionSignPublicKey;
  }

  @JsonProperty(value = "connectors")
  public List<JsonConnectorInfo> getConnectors() {
    return connectors;
  }

  @JsonProperty(value = "currency_code")
  public String getCurrencyCode() {
    return currencyCode;
  }

  @JsonProperty(value = "currency_symbol")
  public String getCurrencySymbol() {
    return currencySymbol;
  }

  public URI getId() {
    return id;
  }

  @JsonProperty(value = "ilp_prefix")
  public String getIlpPrefix() {
    return this.ledgerPrefix;
  }

  @JsonProperty(value = "notification_sign_public_key")
  public String getNotificationSignPublicKey() {
    return this.notificationSignPublicKey;
  }

  @JsonProperty(value = "precision")
  public int getPrecision() {
    return precision;
  }

  @JsonProperty(value = "scale")
  public int getScale() {
    return scale;
  }

  @JsonProperty(value = "urls")
  public Map<String, String> getUrls() {
    return urls;
  }

  public void setConditionSignPublicKey(String key) {
    this.conditionSignPublicKey = key;
  }

  public void setConnectors(List<JsonConnectorInfo> connectors) {
    this.connectors = connectors;
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }

  public void setCurrencySymbol(String currencySymbol) {
    this.currencySymbol = currencySymbol;
  }

  public void setId(URI id) {
    this.id = id;
  }

  public void setIlpPrefix(String ledgerPrefix) {
    this.ledgerPrefix = ledgerPrefix;
  }

  public void setNotificationSignPublicKey(String key) {
    this.notificationSignPublicKey = key;
  }

  public void setPrecision(int precision) {
    this.precision = precision;
  }

  public void setScale(int scale) {
    this.scale = scale;
  }

  public void setUrls(Map<String, String> urls) {
    this.urls = urls;
  }

  public LedgerInfo toLedgerInfo() {
    
    ClientLedgerInfo ledgerInfo = new ClientLedgerInfo();
    ledgerInfo.setId(getId().toString());
    ledgerInfo.setPrefix(new InterledgerAddress(getIlpPrefix()));    
    ledgerInfo.setPrecision(getPrecision());
    ledgerInfo.setScale(getScale());
    
    try {
      CurrencyUnit currency = Monetary.getCurrency(getCurrencyCode());
      ledgerInfo.setCurrencyUnit(currency);
    } catch (UnknownCurrencyException e) {
      throw new DataModelTranslationException("Unrecognized currency code: " + getCurrencyCode(), this, e);
    }
    
    try {
      //TODO Set the style using the provided symbol 
      MonetaryAmountFormat format = MonetaryFormats.getAmountFormat(Locale.getDefault());
      ledgerInfo.setMonetaryAmountFormat(format);
    } catch (MonetaryException e) {
      throw new DataModelTranslationException("Unable to load currency formatter.", this, e);
    }
    
    //TODO Decode public key
    ledgerInfo.setConditionSignPublicKey(null);
    
    return ledgerInfo;
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
