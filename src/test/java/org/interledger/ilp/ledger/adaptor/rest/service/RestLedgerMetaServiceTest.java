package org.interledger.ilp.ledger.adaptor.rest.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.interledger.ilp.ledger.model.LedgerInfo;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.net.URI;

/**
 * Basic test of the {@link RestLedgerMetaService} class.
 */
public class RestLedgerMetaServiceTest extends RestLedgerServiceTestBase {

  @Test
  public void getLedgerInfoSuccess() throws Exception {

    Resource responseBody = new ClassPathResource("ledger_metadata.json", this.getClass());
    
    mockServer.expect(requestTo("/")).andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

    RestLedgerMetaService service = new RestLedgerMetaService(restTemplate, new URI("/"));

    LedgerInfo ledger = service.getLedgerInfo();

    mockServer.verify();
    
    /* we do not validate the contents */
    assertNotNull(ledger);
    
    assertNotNull(service.getAuthTokenUri());
    assertNotNull(service.getConnectorIds());
    assertNotNull(service.getMessageUri());
    assertNotNull(service.getWebsocketUri());
  }

  @Test
  public void getLedgerInfo_Cached() throws Exception {

    Resource responseBody = new ClassPathResource("ledger_metadata.json", this.getClass());
    
    mockServer.expect(requestTo("/")).andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

    RestLedgerMetaService service = new RestLedgerMetaService(restTemplate, new URI("/"));

    LedgerInfo ledger = service.getLedgerInfo();

    mockServer.verify();
    
    assertNotNull(ledger);
    
    
    LedgerInfo cached = service.getLedgerInfo(false);
    assertEquals(ledger, cached);
    
    mockServer.verify();
  }  
  
}
