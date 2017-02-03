package org.interledger.ilp.ledger.adaptor.ws;

import org.interledger.ilp.ledger.adaptor.ws.jsonrpc.JsonRpcError;
import org.interledger.ilp.ledger.adaptor.ws.jsonrpc.JsonRpcRequestMessage;
import org.interledger.ilp.ledger.adaptor.ws.jsonrpc.JsonRpcResponseMessage;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A timed cache used to link request and response messages exchanged with the REST ledger. Entries
 * older than a given expiry time are evicted.
 * 
 * <p>NOTE: timed eviction copied from:
 * http://stackoverflow.com/questions/3802370/java-time-based-map-cache-with-expiring-keys
 * 
 * <p>TODO: should we rather use a Guava cache instead of rolling our own?
 */
public class JsonRpcRequestResponseMapper {

  public static final int DEFAULT_EVICION_TIME_MILLIS = 1000;
 
  private Map<String, JsonRpcRequestMessage> requestMap =
      new ConcurrentHashMap<String, JsonRpcRequestMessage>();
  
  private Map<String, JsonRpcResponseHandler> handlerMap =
      new ConcurrentHashMap<String, JsonRpcResponseHandler>();
  
  private Map<String, Long> timeMap = new ConcurrentHashMap<String, Long>();
  
  private long expiryInMillis; 

  /**
   * Constructs a new <code>JsonRpcRequestResponseMapper</code> instance.
   */
  public JsonRpcRequestResponseMapper() {
    this(DEFAULT_EVICION_TIME_MILLIS);
  }

  /**
   * Constructs a new <code>JsonRpcRequestResponseMapper</code> instance.
   * 
   * @param expiryInMillis
   *  The amount of time a request can stay in the map for linking to a response before being
   *      evicted.
   */
  public JsonRpcRequestResponseMapper(long expiryInMillis) {
    this.expiryInMillis = expiryInMillis;
    initialize();
  }

  /**
   * Stores a request message in the map for future linking.
   *
   * @param request
   *  The request to store.
   * @param responseHandler
   *  A handler to use when a response is received for the request.
   * @return
   *  The previous handler associated with the request, or no previous handler exists.
   */
  public JsonRpcResponseHandler storeRequest(JsonRpcRequestMessage request,
      JsonRpcResponseHandler responseHandler) {

    String id = request.getId();
    if (id == null) {
      throw new IllegalArgumentException("Request must have an ID.");
    }

    Date date = new Date();
    timeMap.put(id, date.getTime());
    requestMap.put(id, request);
    return handlerMap.put(id, responseHandler);
  }

  /**
   * Allows the response mapper to handle the response message, possibly calling the response
   * handler linked with the request.
   *
   * @param response
   *  The response to handle.
   */
  public void handleResponse(JsonRpcResponseMessage response) {
    
    String id = response.getId();
    if (id == null) {
      throw new IllegalArgumentException("Response must have an ID.");
    }

    JsonRpcRequestMessage request = requestMap.get(id);
    JsonRpcResponseHandler responseHandler = handlerMap.get(id);
    remove(id);

    if (responseHandler != null) {
      responseHandler.handleResponse(request, response);
    }
  }

  /** Performs the initialization required. */
  protected void initialize() {
    new CleanerThread("JsonRpcResponseTimeoutMonitor").start();
  }

  /**
   * Removes entries from the internally managed maps for a given id.
   *
   * @param id
   *  A unique request identifier
   */
  private void remove(String id) {
    timeMap.remove(id);
    handlerMap.remove(id);
    requestMap.remove(id);
  }

  /**
   * The cleaner thread performs the cache eviction for the internal maps used by the mapper.
   */
  class CleanerThread extends Thread {
    
    public CleanerThread(String string) {
      super(string);
    }

    @Override
    public void run() {
      while (true) {
        cleanMap();
        try {
          Thread.sleep(expiryInMillis / 2);
        } catch (InterruptedException ie) {
          throw new RuntimeException(ie);
        }
      }
    }

    private void cleanMap() {
      long currentTime = new Date().getTime();
      for (String id : timeMap.keySet()) {
        if (currentTime > (timeMap.get(id) + expiryInMillis)) {
          JsonRpcRequestMessage request = requestMap.get(id);
          JsonRpcResponseHandler responseHandler = handlerMap.get(id);
          if (responseHandler != null) {
            JsonRpcResponseMessage response = new JsonRpcResponseMessage();
            JsonRpcError error = new JsonRpcError();
            error.setCode(0); // TODO Use correct code
            error.setMessage("Timed out waiting for response to request. id: " + id);
            response.setId(id);
            response.setError(error);
            responseHandler.handleResponse(request, response);
          }
          remove(id);
        }
      }
    }
  }

}
