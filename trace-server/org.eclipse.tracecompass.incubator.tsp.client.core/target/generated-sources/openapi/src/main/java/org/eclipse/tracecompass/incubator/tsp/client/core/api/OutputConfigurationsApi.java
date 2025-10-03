package org.eclipse.tracecompass.incubator.tsp.client.core.api;

import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.Pair;

import javax.ws.rs.core.GenericType;

import org.eclipse.tracecompass.incubator.tsp.client.core.model.ConfigurationSourceType;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.DataProvider;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ErrorResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.OutputConfigurationQueryParameters;
import java.util.UUID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2025-09-19T10:33:13.979273368-04:00[America/Toronto]", comments = "Generator version: 7.15.0")
public class OutputConfigurationsApi {
  private ApiClient apiClient;

  public OutputConfigurationsApi() {
    this(Configuration.getDefaultApiClient());
  }

  public OutputConfigurationsApi(ApiClient apiClient) {
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
   * Get a derived data provider from a input configuration
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to create a derived output from (required)
   * @param outputConfigurationQueryParameters Query parameters to create a configuration instance. Provide all query parameter keys and values as specified in the corresponding configuration source type. (required)
   * @return DataProvider
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns the derived data provider descriptor. </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment, output provider or configuration type not found </td><td>  -  </td></tr>
     </table>
   */
  public DataProvider createProvider(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId, @javax.annotation.Nonnull OutputConfigurationQueryParameters outputConfigurationQueryParameters) throws ApiException {
    return createProviderWithHttpInfo(expUUID, outputId, outputConfigurationQueryParameters).getData();
  }

  /**
   * Get a derived data provider from a input configuration
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to create a derived output from (required)
   * @param outputConfigurationQueryParameters Query parameters to create a configuration instance. Provide all query parameter keys and values as specified in the corresponding configuration source type. (required)
   * @return ApiResponse&lt;DataProvider&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns the derived data provider descriptor. </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment, output provider or configuration type not found </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<DataProvider> createProviderWithHttpInfo(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId, @javax.annotation.Nonnull OutputConfigurationQueryParameters outputConfigurationQueryParameters) throws ApiException {
    // Check required parameters
    if (expUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'expUUID' when calling createProvider");
    }
    if (outputId == null) {
      throw new ApiException(400, "Missing the required parameter 'outputId' when calling createProvider");
    }
    if (outputConfigurationQueryParameters == null) {
      throw new ApiException(400, "Missing the required parameter 'outputConfigurationQueryParameters' when calling createProvider");
    }

    // Path parameters
    String localVarPath = "/experiments/{expUUID}/outputs/{outputId}"
            .replaceAll("\\{expUUID}", apiClient.escapeString(expUUID.toString()))
            .replaceAll("\\{outputId}", apiClient.escapeString(outputId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<DataProvider> localVarReturnType = new GenericType<DataProvider>() {};
    return apiClient.invokeAPI("OutputConfigurationsApi.createProvider", localVarPath, "POST", new ArrayList<>(), outputConfigurationQueryParameters,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Delete a derived output (and its configuration).
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the parent output provider (required)
   * @param derivedOutputId ID of the derived output provider (required)
   * @return DataProvider
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns the deleted derived data provider descriptor. The derived data provider (and its configuration) was successfully deleted. </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment, output provider or configuration type not found </td><td>  -  </td></tr>
     </table>
   */
  public DataProvider deleteDerivedProvider(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId, @javax.annotation.Nonnull String derivedOutputId) throws ApiException {
    return deleteDerivedProviderWithHttpInfo(expUUID, outputId, derivedOutputId).getData();
  }

  /**
   * Delete a derived output (and its configuration).
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the parent output provider (required)
   * @param derivedOutputId ID of the derived output provider (required)
   * @return ApiResponse&lt;DataProvider&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns the deleted derived data provider descriptor. The derived data provider (and its configuration) was successfully deleted. </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment, output provider or configuration type not found </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<DataProvider> deleteDerivedProviderWithHttpInfo(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId, @javax.annotation.Nonnull String derivedOutputId) throws ApiException {
    // Check required parameters
    if (expUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'expUUID' when calling deleteDerivedProvider");
    }
    if (outputId == null) {
      throw new ApiException(400, "Missing the required parameter 'outputId' when calling deleteDerivedProvider");
    }
    if (derivedOutputId == null) {
      throw new ApiException(400, "Missing the required parameter 'derivedOutputId' when calling deleteDerivedProvider");
    }

    // Path parameters
    String localVarPath = "/experiments/{expUUID}/outputs/{outputId}/{derivedOutputId}"
            .replaceAll("\\{expUUID}", apiClient.escapeString(expUUID.toString()))
            .replaceAll("\\{outputId}", apiClient.escapeString(outputId.toString()))
            .replaceAll("\\{derivedOutputId}", apiClient.escapeString(derivedOutputId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    GenericType<DataProvider> localVarReturnType = new GenericType<DataProvider>() {};
    return apiClient.invokeAPI("OutputConfigurationsApi.deleteDerivedProvider", localVarPath, "DELETE", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Get a single configuration source type defined on the server for a given data provider and experiment.
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to query (required)
   * @param typeId The configuration source type ID (required)
   * @return ConfigurationSourceType
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a single configuration source type </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment, output provider or configuration type not found </td><td>  -  </td></tr>
     </table>
   */
  public ConfigurationSourceType getConfigurationType1(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId, @javax.annotation.Nonnull String typeId) throws ApiException {
    return getConfigurationType1WithHttpInfo(expUUID, outputId, typeId).getData();
  }

  /**
   * Get a single configuration source type defined on the server for a given data provider and experiment.
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to query (required)
   * @param typeId The configuration source type ID (required)
   * @return ApiResponse&lt;ConfigurationSourceType&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a single configuration source type </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment, output provider or configuration type not found </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<ConfigurationSourceType> getConfigurationType1WithHttpInfo(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId, @javax.annotation.Nonnull String typeId) throws ApiException {
    // Check required parameters
    if (expUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'expUUID' when calling getConfigurationType1");
    }
    if (outputId == null) {
      throw new ApiException(400, "Missing the required parameter 'outputId' when calling getConfigurationType1");
    }
    if (typeId == null) {
      throw new ApiException(400, "Missing the required parameter 'typeId' when calling getConfigurationType1");
    }

    // Path parameters
    String localVarPath = "/experiments/{expUUID}/outputs/{outputId}/configTypes/{typeId}"
            .replaceAll("\\{expUUID}", apiClient.escapeString(expUUID.toString()))
            .replaceAll("\\{outputId}", apiClient.escapeString(outputId.toString()))
            .replaceAll("\\{typeId}", apiClient.escapeString(typeId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    GenericType<ConfigurationSourceType> localVarReturnType = new GenericType<ConfigurationSourceType>() {};
    return apiClient.invokeAPI("OutputConfigurationsApi.getConfigurationType1", localVarPath, "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Get the list of configuration types defined on the server for a given output and experiment
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to query (required)
   * @return List&lt;ConfigurationSourceType&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a list of configuration types that this output supports. </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment, output provider or configuration type not found </td><td>  -  </td></tr>
     </table>
   */
  public List<ConfigurationSourceType> getConfigurationTypes1(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId) throws ApiException {
    return getConfigurationTypes1WithHttpInfo(expUUID, outputId).getData();
  }

  /**
   * Get the list of configuration types defined on the server for a given output and experiment
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to query (required)
   * @return ApiResponse&lt;List&lt;ConfigurationSourceType&gt;&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a list of configuration types that this output supports. </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment, output provider or configuration type not found </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<List<ConfigurationSourceType>> getConfigurationTypes1WithHttpInfo(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId) throws ApiException {
    // Check required parameters
    if (expUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'expUUID' when calling getConfigurationTypes1");
    }
    if (outputId == null) {
      throw new ApiException(400, "Missing the required parameter 'outputId' when calling getConfigurationTypes1");
    }

    // Path parameters
    String localVarPath = "/experiments/{expUUID}/outputs/{outputId}/configTypes"
            .replaceAll("\\{expUUID}", apiClient.escapeString(expUUID.toString()))
            .replaceAll("\\{outputId}", apiClient.escapeString(outputId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    GenericType<List<ConfigurationSourceType>> localVarReturnType = new GenericType<List<ConfigurationSourceType>>() {};
    return apiClient.invokeAPI("OutputConfigurationsApi.getConfigurationTypes1", localVarPath, "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
}
