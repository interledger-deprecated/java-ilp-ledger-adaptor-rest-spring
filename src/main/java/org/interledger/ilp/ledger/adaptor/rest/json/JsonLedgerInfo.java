package org.interledger.ilp.ledger.adaptor.rest.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * JSON model of ledger information as would be exchanged with the REST ledger.
 */
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

  @Override
  public String toString() {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    } catch (JsonProcessingException jpe) {
      throw new RuntimeException(jpe);
    }
  }

}
