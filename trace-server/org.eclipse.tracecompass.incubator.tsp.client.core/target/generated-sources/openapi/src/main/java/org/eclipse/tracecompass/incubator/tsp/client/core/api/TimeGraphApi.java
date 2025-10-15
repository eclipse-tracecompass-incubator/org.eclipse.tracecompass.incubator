package org.eclipse.tracecompass.incubator.tsp.client.core.api;

import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.Pair;

import javax.ws.rs.core.GenericType;

import org.eclipse.tracecompass.incubator.tsp.client.core.model.ArrowsQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ErrorResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.RequestedQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TimeGraphArrowsResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TimeGraphStatesResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TimeGraphTooltipResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TimeGraphTreeResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TooltipQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TreeQueryParameters;
import java.util.UUID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.15.0")
public class TimeGraphApi {
  private ApiClient apiClient;

  public TimeGraphApi() {
    this(Configuration.getDefaultApiClient());
  }

  public TimeGraphApi(ApiClient apiClient) {
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
   * API to get the Time Graph arrows
   * Unique entry point for all TimeGraph models, ensures that the same template is followed for all models
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to query (required)
   * @param arrowsQueryParameters Query parameters to fetch the timegraph arrows. The object &#39;requested_timerange&#39; is the requested time range and number of samples. (required)
   * @return TimeGraphArrowsResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a sampled list of TimeGraph arrows </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Missing query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or output provider not found </td><td>  -  </td></tr>
       <tr><td> 405 </td><td> Analysis cannot run </td><td>  -  </td></tr>
     </table>
   */
  public TimeGraphArrowsResponse getArrows(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId, @javax.annotation.Nonnull ArrowsQueryParameters arrowsQueryParameters) throws ApiException {
    return getArrowsWithHttpInfo(expUUID, outputId, arrowsQueryParameters).getData();
  }

  /**
   * API to get the Time Graph arrows
   * Unique entry point for all TimeGraph models, ensures that the same template is followed for all models
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to query (required)
   * @param arrowsQueryParameters Query parameters to fetch the timegraph arrows. The object &#39;requested_timerange&#39; is the requested time range and number of samples. (required)
   * @return ApiResponse&lt;TimeGraphArrowsResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a sampled list of TimeGraph arrows </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Missing query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or output provider not found </td><td>  -  </td></tr>
       <tr><td> 405 </td><td> Analysis cannot run </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<TimeGraphArrowsResponse> getArrowsWithHttpInfo(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId, @javax.annotation.Nonnull ArrowsQueryParameters arrowsQueryParameters) throws ApiException {
    // Check required parameters
    if (expUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'expUUID' when calling getArrows");
    }
    if (outputId == null) {
      throw new ApiException(400, "Missing the required parameter 'outputId' when calling getArrows");
    }
    if (arrowsQueryParameters == null) {
      throw new ApiException(400, "Missing the required parameter 'arrowsQueryParameters' when calling getArrows");
    }

    // Path parameters
    String localVarPath = "/experiments/{expUUID}/outputs/timeGraph/{outputId}/arrows"
            .replaceAll("\\{expUUID}", apiClient.escapeString(expUUID.toString()))
            .replaceAll("\\{outputId}", apiClient.escapeString(outputId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<TimeGraphArrowsResponse> localVarReturnType = new GenericType<TimeGraphArrowsResponse>() {};
    return apiClient.invokeAPI("TimeGraphApi.getArrows", localVarPath, "POST", new ArrayList<>(), arrowsQueryParameters,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * API to get the Time Graph states
   * Unique entry point for all TimeGraph states, ensures that the same template is followed for all views
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to query (required)
   * @param requestedQueryParameters Query parameters to fetch the timegraph states. The object &#39;requested_timerange&#39; is the requested time range and number of samples. The array &#39;requested_items&#39; is the list of entryId being requested. The object &#39;filter_query_parameters&#39; contains requests for search/filter queries. The object &#39;filter_expressions_map&#39; is the list of query requests, where the key 1 is DIMMED and 4 is EXCLUDED, and the value is an array of the desired search query (&#39;thread&#x3D;1&#39; or &#39;process&#x3D;ls&#39; or &#39;duration&gt;10ms&#39;). The &#39;strategy&#39; flag is an optional parameter within &#39;filter_query_parameters&#39;, and if omitted then &#39;SAMPLED&#39; search would be the default value. If &#39;strategy&#39; is set to &#39;DEEP&#39; then the full time range between the first and last requested timestamp should be searched for filter matches. For timegraphs, only one matching state per gap in requested timestamps needs to be returned in the response. If matches to the queries from the &#39;filter_expressions_map&#39; are found there&#39;ll be a field &#39;tags&#39; in &#39;states&#39;. The TimeGraphState class has a bit-mask called tags. If a state is supposed to be dimmed the tag will be the corresponding bit set. (required)
   * @return TimeGraphStatesResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a list of time graph rows </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Missing query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or output provider not found </td><td>  -  </td></tr>
       <tr><td> 405 </td><td> Analysis cannot run </td><td>  -  </td></tr>
     </table>
   */
  public TimeGraphStatesResponse getStates(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId, @javax.annotation.Nonnull RequestedQueryParameters requestedQueryParameters) throws ApiException {
    return getStatesWithHttpInfo(expUUID, outputId, requestedQueryParameters).getData();
  }

  /**
   * API to get the Time Graph states
   * Unique entry point for all TimeGraph states, ensures that the same template is followed for all views
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to query (required)
   * @param requestedQueryParameters Query parameters to fetch the timegraph states. The object &#39;requested_timerange&#39; is the requested time range and number of samples. The array &#39;requested_items&#39; is the list of entryId being requested. The object &#39;filter_query_parameters&#39; contains requests for search/filter queries. The object &#39;filter_expressions_map&#39; is the list of query requests, where the key 1 is DIMMED and 4 is EXCLUDED, and the value is an array of the desired search query (&#39;thread&#x3D;1&#39; or &#39;process&#x3D;ls&#39; or &#39;duration&gt;10ms&#39;). The &#39;strategy&#39; flag is an optional parameter within &#39;filter_query_parameters&#39;, and if omitted then &#39;SAMPLED&#39; search would be the default value. If &#39;strategy&#39; is set to &#39;DEEP&#39; then the full time range between the first and last requested timestamp should be searched for filter matches. For timegraphs, only one matching state per gap in requested timestamps needs to be returned in the response. If matches to the queries from the &#39;filter_expressions_map&#39; are found there&#39;ll be a field &#39;tags&#39; in &#39;states&#39;. The TimeGraphState class has a bit-mask called tags. If a state is supposed to be dimmed the tag will be the corresponding bit set. (required)
   * @return ApiResponse&lt;TimeGraphStatesResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a list of time graph rows </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Missing query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or output provider not found </td><td>  -  </td></tr>
       <tr><td> 405 </td><td> Analysis cannot run </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<TimeGraphStatesResponse> getStatesWithHttpInfo(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId, @javax.annotation.Nonnull RequestedQueryParameters requestedQueryParameters) throws ApiException {
    // Check required parameters
    if (expUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'expUUID' when calling getStates");
    }
    if (outputId == null) {
      throw new ApiException(400, "Missing the required parameter 'outputId' when calling getStates");
    }
    if (requestedQueryParameters == null) {
      throw new ApiException(400, "Missing the required parameter 'requestedQueryParameters' when calling getStates");
    }

    // Path parameters
    String localVarPath = "/experiments/{expUUID}/outputs/timeGraph/{outputId}/states"
            .replaceAll("\\{expUUID}", apiClient.escapeString(expUUID.toString()))
            .replaceAll("\\{outputId}", apiClient.escapeString(outputId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<TimeGraphStatesResponse> localVarReturnType = new GenericType<TimeGraphStatesResponse>() {};
    return apiClient.invokeAPI("TimeGraphApi.getStates", localVarPath, "POST", new ArrayList<>(), requestedQueryParameters,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * API to get a Time Graph tooltip
   * Endpoint to retrieve tooltips for time graph
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to query (required)
   * @param tooltipQueryParameters Query parameters to fetch the timegraph tooltip. The array &#39;requested_times&#39; is an array with a single timestamp. The array &#39;requested_items&#39; is an array with a single entryId being requested.  The object &#39;requested_element&#39; is the element for which the tooltip is requested. (required)
   * @return TimeGraphTooltipResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a map of tooltip keys to values </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Missing query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or output provider not found </td><td>  -  </td></tr>
       <tr><td> 405 </td><td> Analysis cannot run </td><td>  -  </td></tr>
     </table>
   */
  public TimeGraphTooltipResponse getTimeGraphTooltip(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId, @javax.annotation.Nonnull TooltipQueryParameters tooltipQueryParameters) throws ApiException {
    return getTimeGraphTooltipWithHttpInfo(expUUID, outputId, tooltipQueryParameters).getData();
  }

  /**
   * API to get a Time Graph tooltip
   * Endpoint to retrieve tooltips for time graph
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to query (required)
   * @param tooltipQueryParameters Query parameters to fetch the timegraph tooltip. The array &#39;requested_times&#39; is an array with a single timestamp. The array &#39;requested_items&#39; is an array with a single entryId being requested.  The object &#39;requested_element&#39; is the element for which the tooltip is requested. (required)
   * @return ApiResponse&lt;TimeGraphTooltipResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a map of tooltip keys to values </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Missing query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or output provider not found </td><td>  -  </td></tr>
       <tr><td> 405 </td><td> Analysis cannot run </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<TimeGraphTooltipResponse> getTimeGraphTooltipWithHttpInfo(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId, @javax.annotation.Nonnull TooltipQueryParameters tooltipQueryParameters) throws ApiException {
    // Check required parameters
    if (expUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'expUUID' when calling getTimeGraphTooltip");
    }
    if (outputId == null) {
      throw new ApiException(400, "Missing the required parameter 'outputId' when calling getTimeGraphTooltip");
    }
    if (tooltipQueryParameters == null) {
      throw new ApiException(400, "Missing the required parameter 'tooltipQueryParameters' when calling getTimeGraphTooltip");
    }

    // Path parameters
    String localVarPath = "/experiments/{expUUID}/outputs/timeGraph/{outputId}/tooltip"
            .replaceAll("\\{expUUID}", apiClient.escapeString(expUUID.toString()))
            .replaceAll("\\{outputId}", apiClient.escapeString(outputId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<TimeGraphTooltipResponse> localVarReturnType = new GenericType<TimeGraphTooltipResponse>() {};
    return apiClient.invokeAPI("TimeGraphApi.getTimeGraphTooltip", localVarPath, "POST", new ArrayList<>(), tooltipQueryParameters,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * API to get the Time Graph tree
   * Unique entry point for output providers, to get the tree of visible entries
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to query (required)
   * @param treeQueryParameters Query parameters to fetch the timegraph tree. The object &#39;requested_timerange&#39; specifies the requested time range. When absent the tree for the full range is returned. (required)
   * @return TimeGraphTreeResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a list of Time Graph entries. The returned model must be consistent, parentIds must refer to a parent which exists in the model. </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or output provider not found </td><td>  -  </td></tr>
       <tr><td> 405 </td><td> Analysis cannot run </td><td>  -  </td></tr>
     </table>
   */
  public TimeGraphTreeResponse getTimeGraphTree(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId, @javax.annotation.Nonnull TreeQueryParameters treeQueryParameters) throws ApiException {
    return getTimeGraphTreeWithHttpInfo(expUUID, outputId, treeQueryParameters).getData();
  }

  /**
   * API to get the Time Graph tree
   * Unique entry point for output providers, to get the tree of visible entries
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to query (required)
   * @param treeQueryParameters Query parameters to fetch the timegraph tree. The object &#39;requested_timerange&#39; specifies the requested time range. When absent the tree for the full range is returned. (required)
   * @return ApiResponse&lt;TimeGraphTreeResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a list of Time Graph entries. The returned model must be consistent, parentIds must refer to a parent which exists in the model. </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or output provider not found </td><td>  -  </td></tr>
       <tr><td> 405 </td><td> Analysis cannot run </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<TimeGraphTreeResponse> getTimeGraphTreeWithHttpInfo(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId, @javax.annotation.Nonnull TreeQueryParameters treeQueryParameters) throws ApiException {
    // Check required parameters
    if (expUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'expUUID' when calling getTimeGraphTree");
    }
    if (outputId == null) {
      throw new ApiException(400, "Missing the required parameter 'outputId' when calling getTimeGraphTree");
    }
    if (treeQueryParameters == null) {
      throw new ApiException(400, "Missing the required parameter 'treeQueryParameters' when calling getTimeGraphTree");
    }

    // Path parameters
    String localVarPath = "/experiments/{expUUID}/outputs/timeGraph/{outputId}/tree"
            .replaceAll("\\{expUUID}", apiClient.escapeString(expUUID.toString()))
            .replaceAll("\\{outputId}", apiClient.escapeString(outputId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<TimeGraphTreeResponse> localVarReturnType = new GenericType<TimeGraphTreeResponse>() {};
    return apiClient.invokeAPI("TimeGraphApi.getTimeGraphTree", localVarPath, "POST", new ArrayList<>(), treeQueryParameters,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
}
