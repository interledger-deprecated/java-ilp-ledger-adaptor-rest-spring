package org.interledger.ilp.ledger.adaptor.rest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.interledger.ilp.ledger.adaptor.rest.json.JsonLedgerInfo;
import org.junit.Before;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

/**
 * Base class with convenience methods for testing the rest ledger services.
 */
public abstract class RestLedgerServiceTestBase {

  public static final String BASE_URL = "https://ledger.example.com/ledger";
  
  protected MockRestServiceServer mockServer;
  protected RestTemplate restTemplate;
  protected RestLedgerJsonConverter converter;
  
  /**
   * Performs default initialization that's useful for testing rest services.
   */
  @Before
  public void setupForTest() {
    this.restTemplate = new RestTemplate();
    this.mockServer = MockRestServiceServer.bindTo(this.restTemplate).ignoreExpectOrder(true)
        .build();
    
    ObjectMapper mapper = new ObjectMapper();
    Resource responseBody = new ClassPathResource("ledger_metadata.json", this.getClass());
    try {
      JsonLedgerInfo info = mapper.readValue(responseBody.getInputStream(), JsonLedgerInfo.class);
      if (info.getId() == null) {
        info.setId(URI.create("https://ledger.example.com/ledger"));
      }

      converter = new RestLedgerJsonConverter(info);
    } catch (Exception ex) {
      throw new RuntimeException("error creating RestLedgerJsonConverter from test data", ex);
    }
  }
}

