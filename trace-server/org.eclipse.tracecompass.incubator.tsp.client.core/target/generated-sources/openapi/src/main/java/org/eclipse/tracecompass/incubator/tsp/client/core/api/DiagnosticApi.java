package org.eclipse.tracecompass.incubator.tsp.client.core.api;

import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.Pair;

import javax.ws.rs.core.GenericType;

import org.eclipse.tracecompass.incubator.tsp.client.core.model.ErrorResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ServerStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.15.0")
public class DiagnosticApi {
  private ApiClient apiClient;

  public DiagnosticApi() {
    this(Configuration.getDefaultApiClient());
  }

  public DiagnosticApi(ApiClient apiClient) {
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
   * Get the health status of this server
   * 
   * @return ServerStatus
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> The trace server is running and ready to receive requests </td><td>  -  </td></tr>
       <tr><td> 503 </td><td> The trace server is unavailable or in maintenance and cannot receive requests </td><td>  -  </td></tr>
     </table>
   */
  public ServerStatus getHealthStatus() throws ApiException {
    return getHealthStatusWithHttpInfo().getData();
  }

  /**
   * Get the health status of this server
   * 
   * @return ApiResponse&lt;ServerStatus&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> The trace server is running and ready to receive requests </td><td>  -  </td></tr>
       <tr><td> 503 </td><td> The trace server is unavailable or in maintenance and cannot receive requests </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<ServerStatus> getHealthStatusWithHttpInfo() throws ApiException {
    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    GenericType<ServerStatus> localVarReturnType = new GenericType<ServerStatus>() {};
    return apiClient.invokeAPI("DiagnosticApi.getHealthStatus", "/health", "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
}
