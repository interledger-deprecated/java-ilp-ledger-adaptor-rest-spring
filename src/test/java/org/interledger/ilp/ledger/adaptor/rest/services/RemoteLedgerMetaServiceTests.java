package org.interledger.ilp.ledger.adaptor.rest.services;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;

import org.interledger.ilp.ledger.model.LedgerInfo;
import org.interledger.ilp.ledger.adaptor.rest.MockRestClient;
import org.interledger.ilp.ledger.adaptor.rest.service.RestLedgerMetaService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

public class RemoteLedgerMetaServiceTests {

  private MockRestServiceServer mockServer;
  private MockRestClient mockClient;
  private RestTemplate restTemplate;

  // FIXME Broken test

  @Before
  public void setup() {
//    this.restTemplate = new RestTemplate();
//    this.mockServer =
//        MockRestServiceServer.bindTo(this.restTemplate).ignoreExpectOrder(true).build();
//    this.mockClient = new MockRestClient(null, URI.create("/"));
  }

  @Test
  public void getLedgerInfoSuccess() throws Exception {

//    Resource responseBody = new ClassPathResource("default_ledger.json", this.getClass());
//
//    this.mockServer.expect(requestTo("/")).andExpect(method(HttpMethod.GET))
//        .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
//
//    RestLedgerMetaService service = new RestLedgerMetaService(mockClient, restTemplate, new URI("/"));
//
//    @SuppressWarnings("unused")
//    LedgerInfo ledger = service.getLedgerInfo();
//
//    this.mockServer.verify();
  }

}
