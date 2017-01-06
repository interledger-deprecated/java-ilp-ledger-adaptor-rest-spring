package org.interledger.ilp.ledger.adaptor.rest.service;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.interledger.ilp.core.ledger.model.LedgerInfo;
import org.interledger.ilp.ledger.adaptor.rest.RestLedgerAdaptor;
import org.interledger.ilp.ledger.adaptor.rest.ServiceUrls;
import org.interledger.ilp.ledger.adaptor.rest.json.JsonConnectorInfo;
import org.interledger.ilp.ledger.adaptor.rest.json.JsonLedgerInfo;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplate;

public class RestLedgerMetaService extends RestServiceBase {

  private static final Pattern urlTemplateRegex = Pattern.compile("/\\:([A-Za-z0-9-]+)");
  private Map<ServiceUrls, String> urls;
  
  private UriBuilder transferIdUriBuilder;
  private UriBuilder accountIdUriBuilder;
  private UriBuilder rejectTransferUriBuilder;
  private UriBuilder transferFulfillmentUriBuilder;
  private URI authTokenUri;
  private URI websocketUri;
  private URI messageUri;
  
  private URI baseUri;
  
  private Set<URI> connectors;
  
  private LedgerInfo cache;
  
  public RestLedgerMetaService(RestLedgerAdaptor adaptor, RestTemplate restTemplate, URI ledgerBaseUrl) {
    super(adaptor, restTemplate);
    
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
        
        JsonLedgerInfo jsonledgerInfo = restTemplate.getForObject(baseUri, JsonLedgerInfo.class);
        
        // FIXME Have to fix all the URL templates because they use a non-standard format
        // Ideally the ledger would use rfc 6570 compatible templates
        Map<String, String> metaUrls = jsonledgerInfo.getUrls();
        
        transferIdUriBuilder = new UriBuilder(fixUriTemplates(metaUrls.get(ServiceUrls.TRANSFER.getName())));
        transferFulfillmentUriBuilder = new UriBuilder(fixUriTemplates(metaUrls.get(ServiceUrls.TRANSFER_FULFILLMENT.getName())));
        rejectTransferUriBuilder = new UriBuilder(fixUriTemplates(metaUrls.get(ServiceUrls.TRANSFER_REJECTION.getName())));
        accountIdUriBuilder = new UriBuilder(fixUriTemplates(metaUrls.get(ServiceUrls.ACCOUNT.getName())));
        
        authTokenUri = URI.create(metaUrls.get(ServiceUrls.AUTH_TOKEN.getName()));
        messageUri = URI.create(metaUrls.get(ServiceUrls.MESSAGE.getName()));
        websocketUri = URI.create(metaUrls.get(ServiceUrls.WEBSOCKET.getName()));

        if (jsonledgerInfo.getId() == null) {
          jsonledgerInfo.setId(baseUri);
        }
        
        connectors = new HashSet<>();
        for (JsonConnectorInfo connector : jsonledgerInfo.getConnectors()) {
          connectors.add(connector.getId());
        }
        
        cache = jsonledgerInfo.toLedgerInfo();

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

  private String fixUriTemplates(String input) {
    return urlTemplateRegex.matcher(input.toString()).replaceAll("/\\{$1\\}");
  }
  
  public URI getBaseUri() {
    return baseUri;
  }
  
  public Set<URI> getConnectorIds() {
    return connectors;
  }

  public UriBuilder getAccountIdUriBuilder() {
    return accountIdUriBuilder;
  }
  
  public UriBuilder getTransferIdUriBuilder() {
    return transferIdUriBuilder;
  }
    
  public UriBuilder getTransferFulfillmentUriBuilder() {
    return transferFulfillmentUriBuilder;
  }
    
  public UriBuilder getRejectTransferUriBuilder() {
    return rejectTransferUriBuilder;
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
  
  /**
   * Creates URI's from templates and tokens or extracts tokens from URIs.
   */
  public class UriBuilder extends UriTemplate {
    
    private final static String DUMMY_TOKEN = "9999999999999999999999999";

    private static final long serialVersionUID = -8207346321737500503L;

    public UriBuilder(String uriTemplate) {
      super(uriTemplate);
    }
    
    public URI getUri(String token) {
      return expand(token);
    }
    
    public String extractToken(URI uri) {
      
      String dummyUri = getUri(DUMMY_TOKEN).toString();
      String tokenUri = uri.toString();
      
      int start = dummyUri.indexOf(DUMMY_TOKEN);
      String token = tokenUri.substring(start);
      
      if(start + DUMMY_TOKEN.length() < dummyUri.length()) {
        //Need to strip a suffix
        String suffix = dummyUri.substring(start + DUMMY_TOKEN.length());
        token = token.substring(0, token.indexOf(suffix));
      }
      
      return token;
      
    }
    
    
  }

}
