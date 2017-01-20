package org.interledger.ilp.ledger.adaptor.rest.service;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.interledger.ilp.core.ledger.model.LedgerInfo;
import org.interledger.ilp.ledger.adaptor.rest.ServiceUrls;
import org.interledger.ilp.ledger.adaptor.rest.json.JsonConnectorInfo;
import org.interledger.ilp.ledger.adaptor.rest.json.JsonLedgerInfo;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

public class RestLedgerMetaService extends RestServiceBase {

  private Map<ServiceUrls, String> urls;
  
  private URI authTokenUri;
  private URI websocketUri;
  private URI messageUri;
  
  private URI baseUri;
  
  private Set<URI> connectors;

  private LedgerInfo cache;
  
  private RestLedgerJsonConverter converter;
  
  public RestLedgerMetaService(RestTemplate restTemplate, URI ledgerBaseUrl) {
    super(restTemplate);
    
    urls = new HashMap<ServiceUrls, String>();
    urls.put(ServiceUrls.LEDGER, ledgerBaseUrl.toString());

    baseUri = ledgerBaseUrl;
    
  }
  
  public LedgerInfo getLedgerInfo() {
    return getLedgerInfo(false);
  }  
  
  public LedgerInfo getLedgerInfo(boolean skipCache) {

    if(cache == null || skipCache) {
      try {

        log.debug("GET Metadata");
        
        JsonLedgerInfo jsonLedgerInfo = getRestTemplate().getForObject(baseUri, JsonLedgerInfo.class);
        
        if (jsonLedgerInfo.getId() == null) {
          jsonLedgerInfo.setId(baseUri);
        }
        
        //Creates the converter and converts the ledgerInfo
        converter = new RestLedgerJsonConverter(jsonLedgerInfo); 
        cache = converter.getLedgerInfo();
        
        // FIXME Have to fix all the URL templates because they use a non-standard format
        // Ideally the ledger would use rfc 6570 compatible templates
        Map<String, String> metaUrls = jsonLedgerInfo.getUrls();
        
        authTokenUri = URI.create(metaUrls.get(ServiceUrls.AUTH_TOKEN.getName()));
        messageUri = URI.create(metaUrls.get(ServiceUrls.MESSAGE.getName()));
        websocketUri = URI.create(metaUrls.get(ServiceUrls.WEBSOCKET.getName()));
        
        connectors = new HashSet<>();
        for (JsonConnectorInfo connector : jsonLedgerInfo.getConnectors()) {
          connectors.add(connector.getId());
        }

      } catch (HttpStatusCodeException e) {
        switch (e.getStatusCode()) {
          // No known RestExceptions for the metadata service
          default:
            throw e;
        }
      }      
    }
    
    return cache;

  }
  
  public URI getBaseUri() {
    return baseUri;
  }
  
  public Set<URI> getConnectorIds() {
    return connectors;
  }
    
  public URI getAuthTokenUri() {
    return authTokenUri;
  }
  
  public URI getMessageUri() {
    return messageUri;
  }
  
  public URI getWebsocketUri() {
    return websocketUri;
  }

  public RestLedgerJsonConverter getConverter() {
    return converter;
  }
}
