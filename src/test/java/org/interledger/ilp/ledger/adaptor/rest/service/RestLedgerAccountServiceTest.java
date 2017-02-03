package org.interledger.ilp.ledger.adaptor.rest.service;

import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.interledger.ilp.ledger.model.AccountInfo;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.net.URI;

/**
 * Basic tests of the {@link RestLedgerAccountService} class.
 */
public class RestLedgerAccountServiceTest extends RestLedgerServiceTestBase {

  @Test
  public void getLedgerInfoSuccess() throws Exception {
    
    Resource responseBody = new ClassPathResource("ledger_account.json", this.getClass());
    
    mockServer.expect(requestTo(BASE_URL + "/accounts/bob")).andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
    
    RestLedgerAccountService service = new RestLedgerAccountService(converter, restTemplate, null);
    
    AccountInfo info = service.getAccountInfo(URI.create(BASE_URL + "/accounts/bob"));
    
    mockServer.verify();
    
    //we don't inspect the account information returned here to avoid testing the RestJsonConverter
    assertNotNull(info);
  }
}

