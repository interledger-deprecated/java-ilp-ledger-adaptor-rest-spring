package org.interledger.ilp.ledger.adaptor.rest.service;

import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.net.URI;

/**
 * Basic test for the {@link RestLedgerAuthTokenService} class.
 */
public class RestLedgerAuthTokenServiceTest extends RestLedgerServiceTestBase {

  @Test
  public void test_getAuthToken() {
    
    Resource responseBody = new ClassPathResource("ledger_auth_token.json", this.getClass());
    
    mockServer.expect(requestTo(BASE_URL + "/auth_token")).andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

    RestLedgerAuthTokenService service = new RestLedgerAuthTokenService(restTemplate,
        URI.create(BASE_URL + "/auth_token"));
    
    String token = service.getAuthToken();
    
    mockServer.verify();
    
    assertNotNull(token);
  }

}

