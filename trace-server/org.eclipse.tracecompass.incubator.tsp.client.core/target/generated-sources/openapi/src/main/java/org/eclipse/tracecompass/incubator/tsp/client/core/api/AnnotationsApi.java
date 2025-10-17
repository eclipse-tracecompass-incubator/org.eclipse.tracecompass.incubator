package org.eclipse.tracecompass.incubator.tsp.client.core.api;

import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.Pair;

import javax.ws.rs.core.GenericType;

import org.eclipse.tracecompass.incubator.tsp.client.core.model.AnnotationCategoriesResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.AnnotationResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.AnnotationsQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ErrorResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.MarkerSetsResponse;
import java.util.UUID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.15.0")
public class AnnotationsApi {
  private ApiClient apiClient;

  public AnnotationsApi() {
    this(Configuration.getDefaultApiClient());
  }

  public AnnotationsApi(ApiClient apiClient) {
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
   * API to get annotation categories associated to this experiment and output
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to query (required)
   * @param markerSetId The optional requested marker set&#39;s id (optional)
   * @return AnnotationCategoriesResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Annotation categories </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Missing parameter outputId </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or output provider not found </td><td>  -  </td></tr>
       <tr><td> 405 </td><td> Analysis cannot run </td><td>  -  </td></tr>
     </table>
   */
  public AnnotationCategoriesResponse getAnnotationCategories(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId, @javax.annotation.Nullable String markerSetId) throws ApiException {
    return getAnnotationCategoriesWithHttpInfo(expUUID, outputId, markerSetId).getData();
  }

  /**
   * API to get annotation categories associated to this experiment and output
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to query (required)
   * @param markerSetId The optional requested marker set&#39;s id (optional)
   * @return ApiResponse&lt;AnnotationCategoriesResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Annotation categories </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Missing parameter outputId </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or output provider not found </td><td>  -  </td></tr>
       <tr><td> 405 </td><td> Analysis cannot run </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<AnnotationCategoriesResponse> getAnnotationCategoriesWithHttpInfo(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId, @javax.annotation.Nullable String markerSetId) throws ApiException {
    // Check required parameters
    if (expUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'expUUID' when calling getAnnotationCategories");
    }
    if (outputId == null) {
      throw new ApiException(400, "Missing the required parameter 'outputId' when calling getAnnotationCategories");
    }

    // Path parameters
    String localVarPath = "/experiments/{expUUID}/outputs/{outputId}/annotations"
            .replaceAll("\\{expUUID}", apiClient.escapeString(expUUID.toString()))
            .replaceAll("\\{outputId}", apiClient.escapeString(outputId.toString()));

    // Query parameters
    List<Pair> localVarQueryParams = new ArrayList<>(
            apiClient.parameterToPairs("", "markerSetId", markerSetId)
    );

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    GenericType<AnnotationCategoriesResponse> localVarReturnType = new GenericType<AnnotationCategoriesResponse>() {};
    return apiClient.invokeAPI("AnnotationsApi.getAnnotationCategories", localVarPath, "GET", localVarQueryParams, null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * API to get the annotations associated to this experiment and output
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to query (required)
   * @param annotationsQueryParameters Query parameters to fetch the annotations. The object &#39;requested_timerange&#39; is the requested time range and number of samples. The array &#39;requested_items&#39; is the list of entryId being requested. The string &#39;requested_marker_set&#39; is the optional requested marker set&#39;s id. The array &#39;requested_marker_categories&#39; is the list of requested annotation categories; if absent, all annotations are returned. (required)
   * @return AnnotationResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Annotation </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Missing query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or output provider not found </td><td>  -  </td></tr>
       <tr><td> 405 </td><td> Analysis cannot run </td><td>  -  </td></tr>
     </table>
   */
  public AnnotationResponse getAnnotations(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId, @javax.annotation.Nonnull AnnotationsQueryParameters annotationsQueryParameters) throws ApiException {
    return getAnnotationsWithHttpInfo(expUUID, outputId, annotationsQueryParameters).getData();
  }

  /**
   * API to get the annotations associated to this experiment and output
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to query (required)
   * @param annotationsQueryParameters Query parameters to fetch the annotations. The object &#39;requested_timerange&#39; is the requested time range and number of samples. The array &#39;requested_items&#39; is the list of entryId being requested. The string &#39;requested_marker_set&#39; is the optional requested marker set&#39;s id. The array &#39;requested_marker_categories&#39; is the list of requested annotation categories; if absent, all annotations are returned. (required)
   * @return ApiResponse&lt;AnnotationResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Annotation </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Missing query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or output provider not found </td><td>  -  </td></tr>
       <tr><td> 405 </td><td> Analysis cannot run </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<AnnotationResponse> getAnnotationsWithHttpInfo(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId, @javax.annotation.Nonnull AnnotationsQueryParameters annotationsQueryParameters) throws ApiException {
    // Check required parameters
    if (expUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'expUUID' when calling getAnnotations");
    }
    if (outputId == null) {
      throw new ApiException(400, "Missing the required parameter 'outputId' when calling getAnnotations");
    }
    if (annotationsQueryParameters == null) {
      throw new ApiException(400, "Missing the required parameter 'annotationsQueryParameters' when calling getAnnotations");
    }

    // Path parameters
    String localVarPath = "/experiments/{expUUID}/outputs/{outputId}/annotations"
            .replaceAll("\\{expUUID}", apiClient.escapeString(expUUID.toString()))
            .replaceAll("\\{outputId}", apiClient.escapeString(outputId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<AnnotationResponse> localVarReturnType = new GenericType<AnnotationResponse>() {};
    return apiClient.invokeAPI("AnnotationsApi.getAnnotations", localVarPath, "POST", new ArrayList<>(), annotationsQueryParameters,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * API to get marker sets available for this experiment
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @return MarkerSetsResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> List of marker sets </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or output provider not found </td><td>  -  </td></tr>
     </table>
   */
  public MarkerSetsResponse getMarkerSets(@javax.annotation.Nonnull UUID expUUID) throws ApiException {
    return getMarkerSetsWithHttpInfo(expUUID).getData();
  }

  /**
   * API to get marker sets available for this experiment
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @return ApiResponse&lt;MarkerSetsResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> List of marker sets </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or output provider not found </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<MarkerSetsResponse> getMarkerSetsWithHttpInfo(@javax.annotation.Nonnull UUID expUUID) throws ApiException {
    // Check required parameters
    if (expUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'expUUID' when calling getMarkerSets");
    }

    // Path parameters
    String localVarPath = "/experiments/{expUUID}/outputs/markerSets"
            .replaceAll("\\{expUUID}", apiClient.escapeString(expUUID.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    GenericType<MarkerSetsResponse> localVarReturnType = new GenericType<MarkerSetsResponse>() {};
    return apiClient.invokeAPI("AnnotationsApi.getMarkerSets", localVarPath, "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
}
