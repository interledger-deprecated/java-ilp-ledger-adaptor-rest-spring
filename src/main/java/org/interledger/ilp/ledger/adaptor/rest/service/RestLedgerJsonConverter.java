package org.interledger.ilp.ledger.adaptor.rest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.interledger.cryptoconditions.uri.CryptoConditionUri;
import org.interledger.cryptoconditions.uri.UriEncodingException;
import org.interledger.ilp.InterledgerAddress;
import org.interledger.ilp.client.exceptions.DataModelTranslationException;
import org.interledger.ilp.client.model.ClientAccountInfo;
import org.interledger.ilp.client.model.ClientLedgerInfo;
import org.interledger.ilp.client.model.ClientLedgerMessage;
import org.interledger.ilp.client.model.ClientLedgerTransfer;
import org.interledger.ilp.ledger.adaptor.rest.ServiceUrl;
import org.interledger.ilp.ledger.adaptor.rest.json.JsonAccountInfo;
import org.interledger.ilp.ledger.adaptor.rest.json.JsonLedgerInfo;
import org.interledger.ilp.ledger.adaptor.rest.json.JsonLedgerMessage;
import org.interledger.ilp.ledger.adaptor.rest.json.JsonLedgerTransfer;
import org.interledger.ilp.ledger.adaptor.rest.json.JsonLedgerTransferAccountEntry;
import org.interledger.ilp.ledger.adaptor.rest.json.JsonValidator;
import org.interledger.ilp.ledger.model.AccountInfo;
import org.interledger.ilp.ledger.model.LedgerInfo;
import org.interledger.ilp.ledger.model.LedgerMessage;
import org.interledger.ilp.ledger.model.LedgerTransfer;
import org.interledger.ilp.ledger.money.format.LedgerSpecificDecimalMonetaryAmountFormat;
import org.interledger.ilqp.client.model.ClientQuoteErrorResponse;
import org.interledger.ilqp.client.model.ClientQuoteResponse;
import org.interledger.ilqp.json.JsonErrorResponse;
import org.interledger.ilqp.json.JsonErrorResponseEnvelope;
import org.interledger.ilqp.json.JsonQuoteRequest;
import org.interledger.ilqp.json.JsonQuoteRequestEnvelope;
import org.interledger.ilqp.json.JsonQuoteResponse;
import org.interledger.ilqp.json.JsonQuoteResponseEnvelope;
import org.interledger.quoting.model.QuoteRequest;
import org.springframework.web.util.UriTemplate;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import javax.money.UnknownCurrencyException;
import javax.money.format.MonetaryAmountFormat;


/**
 * Provides translation services between the native ledger JSON models and the java models defined
 * in ilp-core.
 */
public class RestLedgerJsonConverter {

  private static final Pattern urlTemplateRegex = Pattern.compile("/\\:([A-Za-z0-9-]+)");
  
  private UriBuilder transferIdUriBuilder;
  private UriBuilder accountIdUriBuilder;
  private UriBuilder rejectTransferUriBuilder;
  private UriBuilder transferFulfillmentUriBuilder;

  private Map<InterledgerAddress, MonetaryAmountFormat> formats;
  private URI ledgerId;
  private LedgerInfo ledgerInfo;
  
  /**
   * Constructs a new <code>RestLedgerJsonConverter</code> instance.
   * 
   * @param jsonLedgerInfo
   *  The JSON model holding information about the ledger.
   */
  public RestLedgerJsonConverter(JsonLedgerInfo jsonLedgerInfo) {
    
    // FIXME Have to fix all the URL templates because they use a non-standard format
    // Ideally the ledger would use rfc 6570 compatible templates
    Map<String, String> metaUrls = jsonLedgerInfo.getUrls();

    transferIdUriBuilder = new UriBuilder(
        fixUriTemplates(metaUrls.get(ServiceUrl.TRANSFER.getName())));
    
    transferFulfillmentUriBuilder = new UriBuilder(
        fixUriTemplates(metaUrls.get(ServiceUrl.TRANSFER_FULFILLMENT.getName())));
    
    rejectTransferUriBuilder = new UriBuilder(
        fixUriTemplates(metaUrls.get(ServiceUrl.TRANSFER_REJECTION.getName())));
    
    accountIdUriBuilder = new UriBuilder(
        fixUriTemplates(metaUrls.get(ServiceUrl.ACCOUNT.getName())));
    
    this.ledgerInfo = convertJsonLedgerInfo(jsonLedgerInfo);
    
    this.formats = new HashMap<>();
    this.formats.put(ledgerInfo.getAddressPrefix(), ledgerInfo.getMonetaryAmountFormat());
    this.ledgerId = jsonLedgerInfo.getId();
  }
  
  /**
   * Translates a JSON ledger info model to a {@link LedgerInfo} model.
   *
   * @param jsonLedgerInfo
   *  The JSON ledger info object
   * @return
   *  An equivalent {@link LedgerInfo} object
   */
  public static LedgerInfo convertJsonLedgerInfo(JsonLedgerInfo jsonLedgerInfo) {
    
    ClientLedgerInfo ledgerInfo = new ClientLedgerInfo();
    ledgerInfo.setId(jsonLedgerInfo.getId().toString());
    ledgerInfo.setPrefix(new InterledgerAddress(jsonLedgerInfo.getIlpPrefix()));    
    ledgerInfo.setPrecision(jsonLedgerInfo.getPrecision());
    ledgerInfo.setScale(jsonLedgerInfo.getScale());
    
    try {
      CurrencyUnit currency = Monetary.getCurrency(jsonLedgerInfo.getCurrencyCode());
      ledgerInfo.setCurrencyUnit(currency);
    } catch (UnknownCurrencyException uce) {
      throw new DataModelTranslationException(
          "Unrecognized currency code: " + jsonLedgerInfo.getCurrencyCode(), jsonLedgerInfo, uce);
    }
    
    MonetaryAmountFormat format = new LedgerSpecificDecimalMonetaryAmountFormat(
          ledgerInfo.getCurrency(), ledgerInfo.getPrecision(), ledgerInfo.getScale());
    ledgerInfo.setMonetaryAmountFormat(format);
    
    //TODO Decode public key
    ledgerInfo.setConditionSignPublicKey(null);
    
    return ledgerInfo;
  }  
  
  public LedgerInfo getLedgerInfo() {
    return this.ledgerInfo;
  }
  
  /**
   * Translates an account id represented by the REST ledgers URI format into an interledger 
   * address.
   *
   * @param accountId
   *  The URI of the account on the REST ledger.
   * @return
   *  An interledger address.
   */
  public InterledgerAddress convertAccountUriToAddress(URI accountId) {
    
    String account = accountIdUriBuilder.extractToken(accountId);
    return InterledgerAddress.fromPrefixAndPath(ledgerInfo.getAddressPrefix(), account);
  }
  
  /**
   * Translates an interledger address into a URI representing an account on the REST ledger.
   *
   * @param account
   *  The interledger account address.
   * @return
   * A URI for an account on the REST ledger.
   */
  public URI convertAccountAddressToUri(InterledgerAddress account) {
    
    String accountSuffix = account.trimPrefix(ledgerInfo.getAddressPrefix()).toString();
    return accountIdUriBuilder.getUri(accountSuffix);
  }  
  
  /**
   * Translates a transfer id to a URI for the transfer on the REST ledger.
   *
   * @param transferId
   *  An id representing the transfer.
   * @return
   *  A URI representing the transfer on the REST ledger.
   */
  public URI convertTransferUuidToUri(UUID transferId) {
    
    return transferIdUriBuilder.getUri(transferId.toString());
  }
  
  /**
   * Translates the URI of a transfer on the REST ledger into a UUID representing the transfer id.
   *
   * @param transferId
   *  The URI of the transfer on the REST ledger.
   * @return
   *  A UUID identifying the transfer.
   */
  public UUID convertTransferUriToUuid(URI transferId) {
    
    String uuidString = transferIdUriBuilder.extractToken(transferId);
    return UUID.fromString(uuidString);
  }
  
  /**
   * Translates the id of a rejected transfer to a URI for the rejection on the REST ledger.
   *
   * @param transferId
   *  The id representing the transfer being rejected.
   * @return
   *  A URI representing the transfer rejection on the REST ledger 
   */
  public URI convertRejectTransferToUri(UUID transferId) {
    
    return rejectTransferUriBuilder.getUri(transferId.toString());
  }
  
  /**
   * Translates the id of a fulfillment transfer to a URI for the fulfillment on the REST ledger.
   *
   * @param transferId
   *  The id representing the transfer being fulfilled.
   * @return
   *  A URI representing the transfer fulfillment on the REST ledger 
   */
  public URI convertTransferFulfillmentToUri(UUID transferId) {
    
    return transferFulfillmentUriBuilder.getUri(transferId.toString());
  }
  
  /**
   * Translates a JSON ledger transfer model to a {@link LedgerTransfer} object.
   *
   * @param jsonTransfer
   *  The JSON ledger transfer object.
   * @return
   *  An equivalent {@link LedgerTransfer} object.
   */
  public LedgerTransfer convertJsonLedgerTransfer(JsonLedgerTransfer jsonTransfer) {
    
    if (jsonTransfer.getCredits().size() != 1 || jsonTransfer.getDebits().size() != 1) {
      throw new RuntimeException("Only single transaction transfers are supported.");
    }

    JsonLedgerTransferAccountEntry creditEntry = jsonTransfer.getCredits().get(0);
    JsonLedgerTransferAccountEntry debitEntry = jsonTransfer.getDebits().get(0);

    ClientLedgerTransfer transfer = new ClientLedgerTransfer();
    transfer.setId(convertTransferUriToUuid(jsonTransfer.getId()));
    
    // FIXME Process the debit and credit entries fully
    transfer.setToAccount(convertAccountUriToAddress(creditEntry.getAccount()));
    transfer.setFromAccount(convertAccountUriToAddress(debitEntry.getAccount()));
    
    transfer.setAmount(this.convertJsonFormattedAmount(debitEntry.getAmount()));
    transfer.setAuthorized(debitEntry.isAuthorized());
    
    if (debitEntry.getInvoice() != null) {
      transfer.setInvoice(debitEntry.getInvoice().toString());
    }
    
    if (debitEntry.getMemo() != null) {
      Object data = debitEntry.getMemo();
      if (data instanceof Map) {
        try {
          ObjectMapper mapper = new ObjectMapper();
          String dataValue = mapper.writeValueAsString(data);
          transfer.setData(dataValue.getBytes(Charset.forName("UTF-8")));
        } catch (JsonProcessingException jpe) {
          throw new RuntimeException("Unable to reserialize transfer data.", jpe);
        }
      } else {
        transfer.setData(Base64.getDecoder().decode(data.toString()));
      }
    }
    transfer.setRejected(debitEntry.isRejected());
    transfer.setRejectionMessage(debitEntry.getRejectionMessage());

    if (jsonTransfer.getCancellationCondition() != null) {
      try {
        transfer.setCancellationCondition(
            CryptoConditionUri.parse(jsonTransfer.getCancellationCondition()));
      } catch (UriEncodingException uee) {
        throw new RuntimeException("Error parsing cancellation condition.", uee);
      }
    }
    
    if (jsonTransfer.getExecutionCondition() != null) {
      try {
        transfer.setExecutionCondition(
            CryptoConditionUri.parse(jsonTransfer.getExecutionCondition()));
      } catch (UriEncodingException uee) {
        throw new RuntimeException("Error parsing execution condition.", uee);
      }
    }
    
    transfer.setExpiresAt(jsonTransfer.getExpiresAt());

    return transfer;    
  }
  
  /**
   * Translates a {@link LedgerTransfer} to a JSON ledger transfer model understood by the REST
   * ledger.
   *
   * @param transfer
   *  The ledger transfer to translate.
   * @return
   *  An equivalent JSON ledger transfer object.
   */
  public JsonLedgerTransfer convertLedgerTransfer(LedgerTransfer transfer) {
    
    JsonLedgerTransfer jsonTransfer = new JsonLedgerTransfer();

    jsonTransfer.setId(convertTransferUuidToUri(transfer.getId()));
    jsonTransfer.setLedger(ledgerId);
    
    JsonLedgerTransferAccountEntry jsonCreditEntry = new JsonLedgerTransferAccountEntry();
    jsonCreditEntry.setAccount(convertAccountAddressToUri(transfer.getToAccount()));
    jsonCreditEntry.setAmount(convertMonetaryAmount(transfer.getAmount()));
    
    if (transfer.getData() != null) {
      // TODO Undocumented assumptions made here.
      // If the provided data is valid UTF8 JSON then embed otherwise base64url encode and send as {
      // "base64url" : "<data>"}.
      String data = new String(transfer.getData(), Charset.forName("UTF-8"));
      if (JsonValidator.isValid(data)) {
        jsonCreditEntry.setMemo(data);
      } else {
        jsonCreditEntry.setMemo("{\"base64url\":\""
            + Base64.getUrlEncoder().encodeToString(transfer.getData()) + "\"}");
      }
    }
    
    List<JsonLedgerTransferAccountEntry> credits = new LinkedList<>();
    credits.add(jsonCreditEntry);
    jsonTransfer.setCredits(credits);

    JsonLedgerTransferAccountEntry jsonDebitEntry = new JsonLedgerTransferAccountEntry();
    jsonDebitEntry.setAccount(convertAccountAddressToUri(transfer.getFromAccount()));
    jsonDebitEntry.setAmount(convertMonetaryAmount(transfer.getAmount()));
    
    if (transfer.getData() != null) {
      // TODO Undocumented assumptions made here.
      // If the provided data is valid UTF8 JSON then embed otherwise base64url encode and send as {
      // "base64url" : "<data>"}.
      String data = new String(transfer.getNoteToSelf(), Charset.forName("UTF-8"));
      if (JsonValidator.isValid(data)) {
        jsonDebitEntry.setMemo(data);
      } else {
        jsonDebitEntry.setMemo("{\"base64url\":\""
            + Base64.getUrlEncoder().encodeToString(transfer.getData()) + "\"}");
      }
    }
    jsonDebitEntry.setAuthorized(true);
    
    List<JsonLedgerTransferAccountEntry> debits = new LinkedList<>();
    debits.add(jsonDebitEntry);
    jsonTransfer.setDebits(debits);

    if (transfer.getCancellationCondition() != null) {
      jsonTransfer.setCancellationCondition(transfer.getCancellationCondition().getUri());
    }
    
    if (transfer.getExecutionCondition() != null) {
      jsonTransfer.setExecutionCondition(transfer.getExecutionCondition().getUri());
    }
    
    jsonTransfer.setExpiresAt(transfer.getExpiresAt());

    return jsonTransfer;
  }
  
  /**
   * Translates a JSON ledger message model to a {@link LedgerMessage} model.
   *
   * @param jsonMessage
   *  A JSON ledger message object.
   * @return
   *  A {@link LedgerMessage} object.
   */
  public LedgerMessage convertJsonLedgerMessage(JsonLedgerMessage jsonMessage) {
    
    ClientLedgerMessage clientMessage = new ClientLedgerMessage();
    
    clientMessage.setFrom(convertAccountUriToAddress(jsonMessage.getFrom()));
    clientMessage.setTo(convertAccountUriToAddress(jsonMessage.getTo()));
    
    /*
     * FIXME Would be great if message had some type info for data so we didn't have to "detect" the
     * type.
     * See https://github.com/interledger/rfcs/issues/127#issuecomment-270411273 
     * For now we do "duck typing"
     */
    Object data = jsonMessage.getData();
    
    if (data instanceof Map) {
      //Looks like this is JSON data
      @SuppressWarnings("unchecked")
      Map<String, Object> jsonData = (Map<String, Object>) data;
      try {
        //TODO We're re-encoding the Map and then decoding. Not efficient
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(data);
        String method = (String) jsonData.get("method");
        
        if ("quote_response".equals(method)) {
          JsonQuoteResponseEnvelope messageData = mapper.readValue(json,
              JsonQuoteResponseEnvelope.class);

          clientMessage.setId(UUID.fromString(messageData.getId()));
          clientMessage.setType("quote_response");

          JsonQuoteResponse jsonResponse = messageData.getData();

          ClientQuoteResponse quoteResponse = new ClientQuoteResponse();
          quoteResponse.setSourceConnectorAccount(
              new InterledgerAddress(jsonResponse.getSourceConnectorAccount()));

          quoteResponse
              .setDestinationLedger(new InterledgerAddress(jsonResponse.getDestinationLedger()));

          quoteResponse.setSourceLedger(new InterledgerAddress(jsonResponse.getSourceLedger()));

          quoteResponse.setDestinationExpiryDuration(
              Duration.of(jsonResponse.getDestinationExpiryDuration(), ChronoUnit.MILLIS));

          quoteResponse.setSourceExpiryDuration(
              Duration.of(jsonResponse.getSourceExpiryDuration(), ChronoUnit.MILLIS));

          quoteResponse.setDestinationAmount(convertJsonFormattedAmount(
              jsonResponse.getDestinationAmount(), quoteResponse.getDestinationLedger()));

          quoteResponse.setSourceAmount(convertJsonFormattedAmount(jsonResponse.getSourceAmount()));

          clientMessage.setData(quoteResponse);
        } else if ("error".equals(method)) {
          JsonErrorResponseEnvelope messageData = mapper.readValue(json,
              JsonErrorResponseEnvelope.class);

          clientMessage.setId(UUID.fromString(messageData.getId()));
          clientMessage.setType("error");

          JsonErrorResponse jsonResponse = messageData.getData();

          ClientQuoteErrorResponse errorResponse = new ClientQuoteErrorResponse();
          errorResponse.setId(jsonResponse.getId());
          errorResponse.setMessage(jsonResponse.getMessage());

          clientMessage.setData(errorResponse);
        } else {
          clientMessage.setData(jsonData);
        }
      } catch (IOException ioe) {
        throw new RuntimeException("Unable to reserialize message data.", ioe);
      }
    } else {
      //the data isnt a map, we assume its a base64 encoded string
      clientMessage.setData(Base64.getDecoder().decode(data.toString()));
    }
    
    return clientMessage;    
  }

  /**
   * Translates a {@link LedgerMessage} model into a JSON ledger message model for the REST ledger.
   *
   * @param message
   *  The {@link LedgerMessage} to translate.
   * @return
   *  A JSON ledger message object.
   */
  public JsonLedgerMessage convertLedgerMessage(LedgerMessage message) {
    
    JsonLedgerMessage jsonMessage = new JsonLedgerMessage();
    jsonMessage.setLedger(ledgerId);
    jsonMessage.setFrom(convertAccountAddressToUri(message.getFrom()));
    jsonMessage.setTo(convertAccountAddressToUri(message.getTo()));

    Object messageData = message.getData();
    
    if (messageData == null) {
      throw new IllegalArgumentException("Message data cannot be null.");
    }
    
    // FIXME String constant!
    if ("quote_request".equals(message.getType())) {
      QuoteRequest quoteReq = (QuoteRequest) messageData;
      
      JsonQuoteRequest jsonQuote = new JsonQuoteRequest();
      jsonQuote.setSourceAddress(quoteReq.getSourceAddress().toString());
      jsonQuote.setDestinationAddress(quoteReq.getDestinationAddress().toString());

      if (quoteReq.getSourceExpiryDuration() != null) {
        jsonQuote.setSourceExpiryDuration(quoteReq.getSourceExpiryDuration().toMillis());
      }
      
      if (quoteReq.getDestinationExpiryDuration() != null) {
        jsonQuote.setDestinationExpiryDuration(quoteReq.getDestinationExpiryDuration().toMillis());
      }

      if (quoteReq.getSourceAmount() != null) {
        jsonQuote.setSourceAmount(convertMonetaryAmount(quoteReq.getSourceAmount()));
      }

      if (quoteReq.getDestinationAmount() != null) {
        // FIXME: This is using the source ledger formatting rules because the quote request doesn't
        // provide currency, precision, scale.
        jsonQuote.setDestinationAmount(convertMonetaryAmount(quoteReq.getDestinationAmount()));
      }
      
      JsonQuoteRequestEnvelope envelope = new JsonQuoteRequestEnvelope();
      envelope.setId(message.getId().toString());
      envelope.setData(jsonQuote);
      
      jsonMessage.setData(envelope);
    } else if (messageData instanceof byte[]) {
      //If this is binary then Base64url encode
      jsonMessage.setData(Base64.getUrlEncoder().encodeToString((byte[]) messageData));
    } else {
      //Assume this can be JSON encoded
      jsonMessage.setData(messageData);
    }
    
    return jsonMessage;
  }
  
  /**
   * Converts a JSON account info model into an {@link AccountInfo} model.
   *
   * @param jsonAccount
   *  The JSON account info object to convert.
   * @return
   *  An equivalent AccountInfo object.
   */
  public AccountInfo convertJsonAccountInfo(JsonAccountInfo jsonAccount) {
    
    ClientAccountInfo accountInfo = new ClientAccountInfo();
    
    // TODO URI normalization issue?
    if (ledgerId.equals(jsonAccount.getLedger())) {
      accountInfo.setLedger(ledgerInfo.getAddressPrefix());
    } else {
      throw new DataModelTranslationException(
          "Unable to determine ledger prefix for ledger with id: "
              + jsonAccount.getLedger().toString(),
          this);
    }
    
    //Translate account id into address
    accountInfo.setId(jsonAccount.getId().toString());
    accountInfo.setAddress(convertAccountUriToAddress(jsonAccount.getId()));
    accountInfo.setName(jsonAccount.getName());
    accountInfo.setIsDisabled(jsonAccount.isDisabled());

    accountInfo.setBalance(convertJsonFormattedAmount(jsonAccount.getBalance()));
    
    //TODO Investigate negative infinity in MonetaryAmounts
    if ("infinity".equalsIgnoreCase(jsonAccount.getMinimumAllowedBalance())) {
      accountInfo.setMinimumAllowedBalance(null);
    } else {
      accountInfo.setMinimumAllowedBalance(
          convertJsonFormattedAmount(jsonAccount.getMinimumAllowedBalance()));
    }
    
    //TODO Is this base64 encoded?
    accountInfo.setCertificateFingerprint(null);
    
    //TODO Decode and build key
    accountInfo.setPublicKey(null);
    
    return accountInfo;
  }
  
  /**
   * Translates a string representing an amount into a {@link MonetaryAmount} based on the REST 
   * ledgers monetary format.
   *
   * @param amount
   *  The amount to convert
   * @return
   *  An equivalent MonetaryAmount
   */
  protected MonetaryAmount convertJsonFormattedAmount(String amount) {
    
    return ledgerInfo.getMonetaryAmountFormat().parse(amount);
  }
  
  /**
   * Translates a string amount to a {@link MonetaryAmount} based on the monetary format of the
   * ledger specified, or the the local ledger, if the ledger format is unknown.
   *
   * @param amount
   *  The amount to convert.
   * @param ledger
   *  The ledger on which the amount is relevant.
   * @return
   *  An equivalent MonetaryAmount in the representation for the ledger if possible.
   */
  public MonetaryAmount convertJsonFormattedAmount(String amount, InterledgerAddress ledger) {
    
    MonetaryAmountFormat format = formats.get(ledger);
    if (format == null) {
      format = ledgerInfo.getMonetaryAmountFormat();
    }
    return format.parse(amount);
  }
  
  /**
   * Translates the {@link MonetaryAmount} to a simple string representing the same amount.
   *
   * @param amount
   *  The amount to convert.
   * @return
   *  A string representing the same amount value in the format of the local ledger.
   */
  public String convertMonetaryAmount(MonetaryAmount amount) {
    
    return ledgerInfo.getMonetaryAmountFormat().format(amount);
  }
  
  /**
   * Replaces the REST ledger URL templates of the form http://example.com/accounts/:name into 
   * the standard http://example.com/accounts/{name}
   *
   * @param input
   *  A URL template to convert
   * @return
   *  A converted URL template
   */
  private String fixUriTemplates(String input) {
    
    return urlTemplateRegex.matcher(input.toString()).replaceAll("/\\{$1\\}");
  }
  
  /**
   * Creates URI's from templates and tokens or extracts tokens from URIs.
   */
  public class UriBuilder extends UriTemplate {
    
    private static final String DUMMY_TOKEN = "9999999999999999999999999";

    private static final long serialVersionUID = -8207346321737500503L;

    /**
     * Constructs a new <code>UriBuilder</code> instance.
     * 
     * @param uriTemplate
     *  The URI template string
     */
    public UriBuilder(String uriTemplate) {
      super(uriTemplate);
    }
    
    /**
     * Creates a full URI based on the template and token.
     *
     * @param value
     *  A value to substitute the template placeholder in the URI.
     * @return
     *  A URI with the template replaced with the given value.
     */
    public URI getUri(String value) {
      
      return expand(value);
    }
    
    /**
     * Extracts a token (or placeholder) from a URI, for example given http://example.com/{name},
     * the method will return 'name'
     *
     * @param uri
     *  A URI containing a token.
     * @return
     *  The token embedded in the URI.
     */
    public String extractToken(URI uri) {

      String dummyUri = getUri(DUMMY_TOKEN).toString();
      String tokenUri = uri.toString();

      int start = dummyUri.indexOf(DUMMY_TOKEN);
      String token = tokenUri.substring(start);

      if (start + DUMMY_TOKEN.length() < dummyUri.length()) {
        // Need to strip a suffix
        String suffix = dummyUri.substring(start + DUMMY_TOKEN.length());
        token = token.substring(0, token.indexOf(suffix));
      }

      return token;
    }
    
  }
  
}
