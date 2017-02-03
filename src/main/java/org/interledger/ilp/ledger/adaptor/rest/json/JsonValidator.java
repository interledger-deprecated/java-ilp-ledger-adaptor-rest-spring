package org.interledger.ilp.ledger.adaptor.rest.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JsonValidator {
  
  /**
   * Sanity check that the json encoded string provided is valid JSON. 
   *
   * @param json
   *  The json encoded string to validate
   * @return
   *  True if the string contains valid JSON encoded data, false otherwise. 
   */
  public static boolean isValid(String json) {
    boolean retValue = true;
    try {
      JsonParser parser = factory.createParser(json);
      JsonNode jsonObj = mapper.readTree(parser);
    } catch (JsonParseException jpe) {
      retValue = false;
    } catch (IOException ioe) {
      retValue = false;
    }
    return retValue;
  }

  private static ObjectMapper mapper = new ObjectMapper();
  private static JsonFactory factory;

  static {
    mapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    factory = mapper.getFactory();
  }
}


