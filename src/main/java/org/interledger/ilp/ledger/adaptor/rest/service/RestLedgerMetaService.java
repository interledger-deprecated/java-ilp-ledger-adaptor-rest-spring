package org.interledger.ilp.ledger.adaptor.rest.service;

import org.interledger.ilp.ledger.adaptor.rest.ServiceUrl;
import org.interledger.ilp.ledger.adaptor.rest.json.JsonConnectorInfo;
import org.interledger.ilp.ledger.adaptor.rest.json.JsonLedgerInfo;
import org.interledger.ilp.ledger.model.LedgerInfo;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This service provides methods to interact with the REST ledger to retrieve meta-information
 * about the ledger.
 */
public class RestLedgerMetaService extends RestServiceBase {

  private Map<ServiceUrl, String> urls;
  
  private URI baseUri;
  private URI authTokenUri;
  private URI websocketUri;
  private URI messageUri;
  
  private Set<URI> connectors;

  private LedgerInfo cache;
  
  private RestLedgerJsonConverter converter;
  
  /**
   * Constructs a new <code>RestLedgerMetaService</code> instance.
   * 
   * @param restTemplate
   *  The rest template to use for interacting with the REST ledger.
   * @param ledgerBaseUrl
   *  The base URL of the ledger.
   */
  public RestLedgerMetaService(RestTemplate restTemplate, URI ledgerBaseUrl) {
    super(restTemplate);
    
    urls = new HashMap<ServiceUrl, String>();
    urls.put(ServiceUrl.LEDGER, ledgerBaseUrl.toString());

    baseUri = ledgerBaseUrl;
  }
  
  /** Retrieves information about the ledger. */
  public LedgerInfo getLedgerInfo() {
    
    return getLedgerInfo(false);
  }  
  
  /**
   * Retrieves information about the ledger.
   *
   * @param skipCache
   *  Set to true to indicate that cached results should be ignored and new information should be
   *      retrieved from the ledger.
   * @return
   *  Information about the ledger.
   */
  public LedgerInfo getLedgerInfo(boolean skipCache) {

    if (cache == null || skipCache) {
      try {

        log.debug("GET Metadata");
        
        JsonLedgerInfo jsonLedgerInfo = getRestTemplate().getForObject(baseUri,
            JsonLedgerInfo.class);
        
        if (jsonLedgerInfo.getId() == null) {
          jsonLedgerInfo.setId(baseUri);
        }
        
        //Creates the converter and converts the ledgerInfo
        converter = new RestLedgerJsonConverter(jsonLedgerInfo); 
        cache = converter.getLedgerInfo();
        
        // FIXME Have to fix all the URL templates because they use a non-standard format
        // Ideally the ledger would use rfc 6570 compatible templates
        Map<String, String> metaUrls = jsonLedgerInfo.getUrls();
        
        authTokenUri = URI.create(metaUrls.get(ServiceUrl.AUTH_TOKEN.getName()));
        messageUri = URI.create(metaUrls.get(ServiceUrl.MESSAGE.getName()));
        websocketUri = URI.create(metaUrls.get(ServiceUrl.WEBSOCKET.getName()));
        
        connectors = new HashSet<>();
        for (JsonConnectorInfo connector : jsonLedgerInfo.getConnectors()) {
          connectors.add(connector.getId());
        }

      } catch (HttpStatusCodeException sce) {
        switch (sce.getStatusCode()) {
          // No known RestExceptions for the metadata service
          default:
            throw sce;
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
