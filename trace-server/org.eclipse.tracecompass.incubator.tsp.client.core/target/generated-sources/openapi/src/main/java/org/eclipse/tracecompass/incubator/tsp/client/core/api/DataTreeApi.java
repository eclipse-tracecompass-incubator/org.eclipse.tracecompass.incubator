package org.eclipse.tracecompass.incubator.tsp.client.core.api;

import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.Pair;

import javax.ws.rs.core.GenericType;

import org.eclipse.tracecompass.incubator.tsp.client.core.model.DataTreeResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ErrorResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TreeQueryParameters;
import java.util.UUID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2025-09-19T10:33:13.979273368-04:00[America/Toronto]", comments = "Generator version: 7.15.0")
public class DataTreeApi {
  private ApiClient apiClient;

  public DataTreeApi() {
    this(Configuration.getDefaultApiClient());
  }

  public DataTreeApi(ApiClient apiClient) {
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
   * API to get the data tree
   * Unique entry point for output providers, to get the tree of visible entries
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to query (required)
   * @param treeQueryParameters Query parameters to fetch the data tree entries. The object &#39;requested_timerange&#39; specifies the requested time range. When absent the tree for the full range is returned. (required)
   * @return DataTreeResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a list of data tree entries. The returned model must be consistent, parentIds must refer to a parent which exists in the model. </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or output provider not found </td><td>  -  </td></tr>
       <tr><td> 405 </td><td> Analysis cannot run </td><td>  -  </td></tr>
     </table>
   */
  public DataTreeResponse getDataTree(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId, @javax.annotation.Nonnull TreeQueryParameters treeQueryParameters) throws ApiException {
    return getDataTreeWithHttpInfo(expUUID, outputId, treeQueryParameters).getData();
  }

  /**
   * API to get the data tree
   * Unique entry point for output providers, to get the tree of visible entries
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to query (required)
   * @param treeQueryParameters Query parameters to fetch the data tree entries. The object &#39;requested_timerange&#39; specifies the requested time range. When absent the tree for the full range is returned. (required)
   * @return ApiResponse&lt;DataTreeResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a list of data tree entries. The returned model must be consistent, parentIds must refer to a parent which exists in the model. </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or output provider not found </td><td>  -  </td></tr>
       <tr><td> 405 </td><td> Analysis cannot run </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<DataTreeResponse> getDataTreeWithHttpInfo(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId, @javax.annotation.Nonnull TreeQueryParameters treeQueryParameters) throws ApiException {
    // Check required parameters
    if (expUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'expUUID' when calling getDataTree");
    }
    if (outputId == null) {
      throw new ApiException(400, "Missing the required parameter 'outputId' when calling getDataTree");
    }
    if (treeQueryParameters == null) {
      throw new ApiException(400, "Missing the required parameter 'treeQueryParameters' when calling getDataTree");
    }

    // Path parameters
    String localVarPath = "/experiments/{expUUID}/outputs/data/{outputId}/tree"
            .replaceAll("\\{expUUID}", apiClient.escapeString(expUUID.toString()))
            .replaceAll("\\{outputId}", apiClient.escapeString(outputId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<DataTreeResponse> localVarReturnType = new GenericType<DataTreeResponse>() {};
    return apiClient.invokeAPI("DataTreeApi.getDataTree", localVarPath, "POST", new ArrayList<>(), treeQueryParameters,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
}
