package org.eclipse.tracecompass.incubator.tsp.client.core.api;

import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.Pair;

import javax.ws.rs.core.GenericType;

import org.eclipse.tracecompass.incubator.tsp.client.core.model.ErrorResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Trace;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TraceErrorResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TraceQueryParameters;
import java.util.UUID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2025-09-19T10:33:13.979273368-04:00[America/Toronto]", comments = "Generator version: 7.15.0")
public class TracesApi {
  private ApiClient apiClient;

  public TracesApi() {
    this(Configuration.getDefaultApiClient());
  }

  public TracesApi(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Get the API client
   *
   * @return API client
   */
  public ApiClient getApiClient() {
    return apiClient;
  }

  /**
   * Set the API client
   *
   * @param apiClient an instance of API client
   */
  public void setApiClient(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Remove a trace from the server and disk
   * 
   * @param uuid UUID of the trace to query (required)
   * @return Trace
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> The trace was successfully deleted </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such trace </td><td>  -  </td></tr>
       <tr><td> 409 </td><td> The trace is in use by at least one experiment thus cannot be deleted. </td><td>  -  </td></tr>
     </table>
   */
  public Trace deleteTrace(@javax.annotation.Nonnull UUID uuid) throws ApiException {
    return deleteTraceWithHttpInfo(uuid).getData();
  }

  /**
   * Remove a trace from the server and disk
   * 
   * @param uuid UUID of the trace to query (required)
   * @return ApiResponse&lt;Trace&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> The trace was successfully deleted </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such trace </td><td>  -  </td></tr>
       <tr><td> 409 </td><td> The trace is in use by at least one experiment thus cannot be deleted. </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Trace> deleteTraceWithHttpInfo(@javax.annotation.Nonnull UUID uuid) throws ApiException {
    // Check required parameters
    if (uuid == null) {
      throw new ApiException(400, "Missing the required parameter 'uuid' when calling deleteTrace");
    }

    // Path parameters
    String localVarPath = "/traces/{uuid}"
            .replaceAll("\\{uuid}", apiClient.escapeString(uuid.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    GenericType<Trace> localVarReturnType = new GenericType<Trace>() {};
    return apiClient.invokeAPI("TracesApi.deleteTrace", localVarPath, "DELETE", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Get the model object for a trace
   * 
   * @param uuid UUID of the trace to query (required)
   * @return Trace
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Return the trace model </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such trace </td><td>  -  </td></tr>
     </table>
   */
  public Trace getTrace(@javax.annotation.Nonnull UUID uuid) throws ApiException {
    return getTraceWithHttpInfo(uuid).getData();
  }

  /**
   * Get the model object for a trace
   * 
   * @param uuid UUID of the trace to query (required)
   * @return ApiResponse&lt;Trace&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Return the trace model </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such trace </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Trace> getTraceWithHttpInfo(@javax.annotation.Nonnull UUID uuid) throws ApiException {
    // Check required parameters
    if (uuid == null) {
      throw new ApiException(400, "Missing the required parameter 'uuid' when calling getTrace");
    }

    // Path parameters
    String localVarPath = "/traces/{uuid}"
            .replaceAll("\\{uuid}", apiClient.escapeString(uuid.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    GenericType<Trace> localVarReturnType = new GenericType<Trace>() {};
    return apiClient.invokeAPI("TracesApi.getTrace", localVarPath, "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Get the list of physical traces imported on the server
   * 
   * @return List&lt;Trace&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a list of traces </td><td>  -  </td></tr>
     </table>
   */
  public List<Trace> getTraces() throws ApiException {
    return getTracesWithHttpInfo().getData();
  }

  /**
   * Get the list of physical traces imported on the server
   * 
   * @return ApiResponse&lt;List&lt;Trace&gt;&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a list of traces </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<List<Trace>> getTracesWithHttpInfo() throws ApiException {
    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    GenericType<List<Trace>> localVarReturnType = new GenericType<List<Trace>>() {};
    return apiClient.invokeAPI("TracesApi.getTraces", "/traces", "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Import a trace
   * Import a trace to the trace server. Return some base information once imported.
   * @param traceQueryParameters  (required)
   * @return Trace
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> The trace has been successfully added to the trace server </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Missing query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such trace </td><td>  -  </td></tr>
       <tr><td> 406 </td><td> Cannot read this trace type </td><td>  -  </td></tr>
       <tr><td> 409 </td><td> The trace (name) already exists and both differ </td><td>  -  </td></tr>
       <tr><td> 500 </td><td> Trace resource creation failed </td><td>  -  </td></tr>
       <tr><td> 501 </td><td> Trace type not supported </td><td>  -  </td></tr>
     </table>
   */
  public Trace putTrace(@javax.annotation.Nonnull TraceQueryParameters traceQueryParameters) throws ApiException {
    return putTraceWithHttpInfo(traceQueryParameters).getData();
  }

  /**
   * Import a trace
   * Import a trace to the trace server. Return some base information once imported.
   * @param traceQueryParameters  (required)
   * @return ApiResponse&lt;Trace&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> The trace has been successfully added to the trace server </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Missing query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such trace </td><td>  -  </td></tr>
       <tr><td> 406 </td><td> Cannot read this trace type </td><td>  -  </td></tr>
       <tr><td> 409 </td><td> The trace (name) already exists and both differ </td><td>  -  </td></tr>
       <tr><td> 500 </td><td> Trace resource creation failed </td><td>  -  </td></tr>
       <tr><td> 501 </td><td> Trace type not supported </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Trace> putTraceWithHttpInfo(@javax.annotation.Nonnull TraceQueryParameters traceQueryParameters) throws ApiException {
    // Check required parameters
    if (traceQueryParameters == null) {
      throw new ApiException(400, "Missing the required parameter 'traceQueryParameters' when calling putTrace");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<Trace> localVarReturnType = new GenericType<Trace>() {};
    return apiClient.invokeAPI("TracesApi.putTrace", "/traces", "POST", new ArrayList<>(), traceQueryParameters,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
}
