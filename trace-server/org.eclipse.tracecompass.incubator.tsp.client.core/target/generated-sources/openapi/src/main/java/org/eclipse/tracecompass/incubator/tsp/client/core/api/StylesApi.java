package org.eclipse.tracecompass.incubator.tsp.client.core.api;

import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.Pair;

import javax.ws.rs.core.GenericType;

import org.eclipse.tracecompass.incubator.tsp.client.core.model.ErrorResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.OptionalQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.StylesResponse;
import java.util.UUID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.15.0")
public class StylesApi {
  private ApiClient apiClient;

  public StylesApi() {
    this(Configuration.getDefaultApiClient());
  }

  public StylesApi(ApiClient apiClient) {
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
   * API to get the style map associated to this experiment and output
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to query (required)
   * @param optionalQueryParameters Query parameters to fetch the style map (required)
   * @return StylesResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Style model that can be used jointly with OutputElementStyle to retrieve specific style values </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Missing query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or output provider not found </td><td>  -  </td></tr>
       <tr><td> 405 </td><td> Analysis cannot run </td><td>  -  </td></tr>
     </table>
   */
  public StylesResponse getStyles(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId, @javax.annotation.Nonnull OptionalQueryParameters optionalQueryParameters) throws ApiException {
    return getStylesWithHttpInfo(expUUID, outputId, optionalQueryParameters).getData();
  }

  /**
   * API to get the style map associated to this experiment and output
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to query (required)
   * @param optionalQueryParameters Query parameters to fetch the style map (required)
   * @return ApiResponse&lt;StylesResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Style model that can be used jointly with OutputElementStyle to retrieve specific style values </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Missing query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or output provider not found </td><td>  -  </td></tr>
       <tr><td> 405 </td><td> Analysis cannot run </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<StylesResponse> getStylesWithHttpInfo(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId, @javax.annotation.Nonnull OptionalQueryParameters optionalQueryParameters) throws ApiException {
    // Check required parameters
    if (expUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'expUUID' when calling getStyles");
    }
    if (outputId == null) {
      throw new ApiException(400, "Missing the required parameter 'outputId' when calling getStyles");
    }
    if (optionalQueryParameters == null) {
      throw new ApiException(400, "Missing the required parameter 'optionalQueryParameters' when calling getStyles");
    }

    // Path parameters
    String localVarPath = "/experiments/{expUUID}/outputs/{outputId}/style"
            .replaceAll("\\{expUUID}", apiClient.escapeString(expUUID.toString()))
            .replaceAll("\\{outputId}", apiClient.escapeString(outputId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<StylesResponse> localVarReturnType = new GenericType<StylesResponse>() {};
    return apiClient.invokeAPI("StylesApi.getStyles", localVarPath, "POST", new ArrayList<>(), optionalQueryParameters,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
}
