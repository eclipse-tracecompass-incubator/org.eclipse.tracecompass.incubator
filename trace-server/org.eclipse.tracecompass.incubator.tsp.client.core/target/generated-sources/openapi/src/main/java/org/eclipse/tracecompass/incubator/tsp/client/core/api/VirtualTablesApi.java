package org.eclipse.tracecompass.incubator.tsp.client.core.api;

import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.Pair;

import javax.ws.rs.core.GenericType;

import org.eclipse.tracecompass.incubator.tsp.client.core.model.ErrorResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.LinesQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.OptionalQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TableColumnHeadersResponse;
import java.util.UUID;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.VirtualTableResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2025-09-19T10:33:13.979273368-04:00[America/Toronto]", comments = "Generator version: 7.15.0")
public class VirtualTablesApi {
  private ApiClient apiClient;

  public VirtualTablesApi() {
    this(Configuration.getDefaultApiClient());
  }

  public VirtualTablesApi(ApiClient apiClient) {
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
   * API to get table columns
   * Unique entry point for output providers, to get the column entries
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to query (required)
   * @param optionalQueryParameters Query parameters to fetch the table columns (required)
   * @return TableColumnHeadersResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a list of table headers </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or output provider not found </td><td>  -  </td></tr>
       <tr><td> 405 </td><td> Analysis cannot run </td><td>  -  </td></tr>
     </table>
   */
  public TableColumnHeadersResponse getColumns(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId, @javax.annotation.Nonnull OptionalQueryParameters optionalQueryParameters) throws ApiException {
    return getColumnsWithHttpInfo(expUUID, outputId, optionalQueryParameters).getData();
  }

  /**
   * API to get table columns
   * Unique entry point for output providers, to get the column entries
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to query (required)
   * @param optionalQueryParameters Query parameters to fetch the table columns (required)
   * @return ApiResponse&lt;TableColumnHeadersResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a list of table headers </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or output provider not found </td><td>  -  </td></tr>
       <tr><td> 405 </td><td> Analysis cannot run </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<TableColumnHeadersResponse> getColumnsWithHttpInfo(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId, @javax.annotation.Nonnull OptionalQueryParameters optionalQueryParameters) throws ApiException {
    // Check required parameters
    if (expUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'expUUID' when calling getColumns");
    }
    if (outputId == null) {
      throw new ApiException(400, "Missing the required parameter 'outputId' when calling getColumns");
    }
    if (optionalQueryParameters == null) {
      throw new ApiException(400, "Missing the required parameter 'optionalQueryParameters' when calling getColumns");
    }

    // Path parameters
    String localVarPath = "/experiments/{expUUID}/outputs/table/{outputId}/columns"
            .replaceAll("\\{expUUID}", apiClient.escapeString(expUUID.toString()))
            .replaceAll("\\{outputId}", apiClient.escapeString(outputId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<TableColumnHeadersResponse> localVarReturnType = new GenericType<TableColumnHeadersResponse>() {};
    return apiClient.invokeAPI("VirtualTablesApi.getColumns", localVarPath, "POST", new ArrayList<>(), optionalQueryParameters,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * API to get virtual table lines
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to query (required)
   * @param linesQueryParameters Query parameters to fetch the table lines. One of &#39;requested_table_index&#39; or &#39;requested_times&#39; should be present. If &#39;requested_table_index&#39; is used it is the starting index of the lines to be returned. If &#39;requested_times&#39; is used it should contain an array with a single timestamp. The returned lines starting at the given timestamp (or the nearest following) will be returned. The &#39;requested_table_count&#39; is the number of lines that should be returned. When &#39;requested_table_column_ids&#39; is absent all columns are returned. When present it is the array of requested columnIds. Use &#39;table_search_expressions&#39; for search providing a map of &lt;columnId, regular expression&gt;. Returned lines that match the search expression will be tagged. Use &#39;table_search_direction&#39; to specify search direction [NEXT, PREVIOUS]. If present, &#39;requested_table_count&#39; events are returned starting from the first matching event. Matching and not matching events are returned. Matching events will be tagged. If no matches are found, an empty list will be returned. (required)
   * @return VirtualTableResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a table model with a 2D array of strings and metadata </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or output provider not found </td><td>  -  </td></tr>
       <tr><td> 405 </td><td> Analysis cannot run </td><td>  -  </td></tr>
       <tr><td> 500 </td><td> Error reading the experiment </td><td>  -  </td></tr>
     </table>
   */
  public VirtualTableResponse getLines(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId, @javax.annotation.Nonnull LinesQueryParameters linesQueryParameters) throws ApiException {
    return getLinesWithHttpInfo(expUUID, outputId, linesQueryParameters).getData();
  }

  /**
   * API to get virtual table lines
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to query (required)
   * @param linesQueryParameters Query parameters to fetch the table lines. One of &#39;requested_table_index&#39; or &#39;requested_times&#39; should be present. If &#39;requested_table_index&#39; is used it is the starting index of the lines to be returned. If &#39;requested_times&#39; is used it should contain an array with a single timestamp. The returned lines starting at the given timestamp (or the nearest following) will be returned. The &#39;requested_table_count&#39; is the number of lines that should be returned. When &#39;requested_table_column_ids&#39; is absent all columns are returned. When present it is the array of requested columnIds. Use &#39;table_search_expressions&#39; for search providing a map of &lt;columnId, regular expression&gt;. Returned lines that match the search expression will be tagged. Use &#39;table_search_direction&#39; to specify search direction [NEXT, PREVIOUS]. If present, &#39;requested_table_count&#39; events are returned starting from the first matching event. Matching and not matching events are returned. Matching events will be tagged. If no matches are found, an empty list will be returned. (required)
   * @return ApiResponse&lt;VirtualTableResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a table model with a 2D array of strings and metadata </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or output provider not found </td><td>  -  </td></tr>
       <tr><td> 405 </td><td> Analysis cannot run </td><td>  -  </td></tr>
       <tr><td> 500 </td><td> Error reading the experiment </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<VirtualTableResponse> getLinesWithHttpInfo(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId, @javax.annotation.Nonnull LinesQueryParameters linesQueryParameters) throws ApiException {
    // Check required parameters
    if (expUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'expUUID' when calling getLines");
    }
    if (outputId == null) {
      throw new ApiException(400, "Missing the required parameter 'outputId' when calling getLines");
    }
    if (linesQueryParameters == null) {
      throw new ApiException(400, "Missing the required parameter 'linesQueryParameters' when calling getLines");
    }

    // Path parameters
    String localVarPath = "/experiments/{expUUID}/outputs/table/{outputId}/lines"
            .replaceAll("\\{expUUID}", apiClient.escapeString(expUUID.toString()))
            .replaceAll("\\{outputId}", apiClient.escapeString(outputId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<VirtualTableResponse> localVarReturnType = new GenericType<VirtualTableResponse>() {};
    return apiClient.invokeAPI("VirtualTablesApi.getLines", localVarPath, "POST", new ArrayList<>(), linesQueryParameters,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
}
