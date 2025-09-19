package org.eclipse.tracecompass.incubator.tsp.client.core.api;

import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.Pair;

import javax.ws.rs.core.GenericType;

import org.eclipse.tracecompass.incubator.tsp.client.core.model.ConfigurationQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ConfigurationSourceType;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ErrorResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ModelConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2025-09-19T10:33:13.979273368-04:00[America/Toronto]", comments = "Generator version: 7.15.0")
public class ConfigurationsApi {
  private ApiClient apiClient;

  public ConfigurationsApi() {
    this(Configuration.getDefaultApiClient());
  }

  public ConfigurationsApi(ApiClient apiClient) {
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
   * Delete a configuration instance of a given configuration source type
   * 
   * @param typeId The configuration source type ID (required)
   * @param configId The configuration instance ID (required)
   * @return ModelConfiguration
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> The configuration instance was successfully deleted </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such configuration source type or configuration instance </td><td>  -  </td></tr>
       <tr><td> 500 </td><td> Internal trace-server error while trying to delete configuration instance </td><td>  -  </td></tr>
     </table>
   */
  public ModelConfiguration deleteConfiguration(@javax.annotation.Nonnull String typeId, @javax.annotation.Nonnull String configId) throws ApiException {
    return deleteConfigurationWithHttpInfo(typeId, configId).getData();
  }

  /**
   * Delete a configuration instance of a given configuration source type
   * 
   * @param typeId The configuration source type ID (required)
   * @param configId The configuration instance ID (required)
   * @return ApiResponse&lt;ModelConfiguration&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> The configuration instance was successfully deleted </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such configuration source type or configuration instance </td><td>  -  </td></tr>
       <tr><td> 500 </td><td> Internal trace-server error while trying to delete configuration instance </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<ModelConfiguration> deleteConfigurationWithHttpInfo(@javax.annotation.Nonnull String typeId, @javax.annotation.Nonnull String configId) throws ApiException {
    // Check required parameters
    if (typeId == null) {
      throw new ApiException(400, "Missing the required parameter 'typeId' when calling deleteConfiguration");
    }
    if (configId == null) {
      throw new ApiException(400, "Missing the required parameter 'configId' when calling deleteConfiguration");
    }

    // Path parameters
    String localVarPath = "/config/types/{typeId}/configs/{configId}"
            .replaceAll("\\{typeId}", apiClient.escapeString(typeId.toString()))
            .replaceAll("\\{configId}", apiClient.escapeString(configId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    GenericType<ModelConfiguration> localVarReturnType = new GenericType<ModelConfiguration>() {};
    return apiClient.invokeAPI("ConfigurationsApi.deleteConfiguration", localVarPath, "DELETE", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Get a configuration instance of a given configuration source type
   * 
   * @param typeId The configuration source type ID (required)
   * @param configId The configuration instance ID (required)
   * @return ModelConfiguration
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Get a configuration instance </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such configuration source type or configuration instance </td><td>  -  </td></tr>
     </table>
   */
  public ModelConfiguration getConfiguration(@javax.annotation.Nonnull String typeId, @javax.annotation.Nonnull String configId) throws ApiException {
    return getConfigurationWithHttpInfo(typeId, configId).getData();
  }

  /**
   * Get a configuration instance of a given configuration source type
   * 
   * @param typeId The configuration source type ID (required)
   * @param configId The configuration instance ID (required)
   * @return ApiResponse&lt;ModelConfiguration&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Get a configuration instance </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such configuration source type or configuration instance </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<ModelConfiguration> getConfigurationWithHttpInfo(@javax.annotation.Nonnull String typeId, @javax.annotation.Nonnull String configId) throws ApiException {
    // Check required parameters
    if (typeId == null) {
      throw new ApiException(400, "Missing the required parameter 'typeId' when calling getConfiguration");
    }
    if (configId == null) {
      throw new ApiException(400, "Missing the required parameter 'configId' when calling getConfiguration");
    }

    // Path parameters
    String localVarPath = "/config/types/{typeId}/configs/{configId}"
            .replaceAll("\\{typeId}", apiClient.escapeString(typeId.toString()))
            .replaceAll("\\{configId}", apiClient.escapeString(configId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    GenericType<ModelConfiguration> localVarReturnType = new GenericType<ModelConfiguration>() {};
    return apiClient.invokeAPI("ConfigurationsApi.getConfiguration", localVarPath, "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Get a single configuration source type defined on the server
   * 
   * @param typeId The configuration source type ID (required)
   * @return ConfigurationSourceType
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a single configuration source type </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such configuration type </td><td>  -  </td></tr>
     </table>
   */
  public ConfigurationSourceType getConfigurationType(@javax.annotation.Nonnull String typeId) throws ApiException {
    return getConfigurationTypeWithHttpInfo(typeId).getData();
  }

  /**
   * Get a single configuration source type defined on the server
   * 
   * @param typeId The configuration source type ID (required)
   * @return ApiResponse&lt;ConfigurationSourceType&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a single configuration source type </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such configuration type </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<ConfigurationSourceType> getConfigurationTypeWithHttpInfo(@javax.annotation.Nonnull String typeId) throws ApiException {
    // Check required parameters
    if (typeId == null) {
      throw new ApiException(400, "Missing the required parameter 'typeId' when calling getConfigurationType");
    }

    // Path parameters
    String localVarPath = "/config/types/{typeId}"
            .replaceAll("\\{typeId}", apiClient.escapeString(typeId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    GenericType<ConfigurationSourceType> localVarReturnType = new GenericType<ConfigurationSourceType>() {};
    return apiClient.invokeAPI("ConfigurationsApi.getConfigurationType", localVarPath, "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Get the list of configuration source types defined on the server
   * 
   * @return List&lt;ConfigurationSourceType&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a list of configuration source types </td><td>  -  </td></tr>
     </table>
   */
  public List<ConfigurationSourceType> getConfigurationTypes() throws ApiException {
    return getConfigurationTypesWithHttpInfo().getData();
  }

  /**
   * Get the list of configuration source types defined on the server
   * 
   * @return ApiResponse&lt;List&lt;ConfigurationSourceType&gt;&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a list of configuration source types </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<List<ConfigurationSourceType>> getConfigurationTypesWithHttpInfo() throws ApiException {
    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    GenericType<List<ConfigurationSourceType>> localVarReturnType = new GenericType<List<ConfigurationSourceType>>() {};
    return apiClient.invokeAPI("ConfigurationsApi.getConfigurationTypes", "/config/types", "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Get the list of configurations that are instantiated of a given configuration source type
   * 
   * @param typeId The configuration source type ID (required)
   * @return List&lt;ModelConfiguration&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Get the list of configuration descriptors  </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such configuration source type or configuration instance </td><td>  -  </td></tr>
     </table>
   */
  public List<ModelConfiguration> getConfigurations(@javax.annotation.Nonnull String typeId) throws ApiException {
    return getConfigurationsWithHttpInfo(typeId).getData();
  }

  /**
   * Get the list of configurations that are instantiated of a given configuration source type
   * 
   * @param typeId The configuration source type ID (required)
   * @return ApiResponse&lt;List&lt;ModelConfiguration&gt;&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Get the list of configuration descriptors  </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such configuration source type or configuration instance </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<List<ModelConfiguration>> getConfigurationsWithHttpInfo(@javax.annotation.Nonnull String typeId) throws ApiException {
    // Check required parameters
    if (typeId == null) {
      throw new ApiException(400, "Missing the required parameter 'typeId' when calling getConfigurations");
    }

    // Path parameters
    String localVarPath = "/config/types/{typeId}/configs"
            .replaceAll("\\{typeId}", apiClient.escapeString(typeId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    GenericType<List<ModelConfiguration>> localVarReturnType = new GenericType<List<ModelConfiguration>>() {};
    return apiClient.invokeAPI("ConfigurationsApi.getConfigurations", localVarPath, "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Create a configuration instance for the given configuration source type
   * 
   * @param typeId The configuration source type ID (required)
   * @param configurationQueryParameters Query parameters to create a configuration instance. Provide all query parameter keys and values as specified in the corresponding configuration source type. (required)
   * @return ModelConfiguration
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> The configuration instance was successfully created </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such configuration source type or configuration instance </td><td>  -  </td></tr>
       <tr><td> 500 </td><td> Internal trace-server error while trying to create configuration instance </td><td>  -  </td></tr>
     </table>
   */
  public ModelConfiguration postConfiguration(@javax.annotation.Nonnull String typeId, @javax.annotation.Nonnull ConfigurationQueryParameters configurationQueryParameters) throws ApiException {
    return postConfigurationWithHttpInfo(typeId, configurationQueryParameters).getData();
  }

  /**
   * Create a configuration instance for the given configuration source type
   * 
   * @param typeId The configuration source type ID (required)
   * @param configurationQueryParameters Query parameters to create a configuration instance. Provide all query parameter keys and values as specified in the corresponding configuration source type. (required)
   * @return ApiResponse&lt;ModelConfiguration&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> The configuration instance was successfully created </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such configuration source type or configuration instance </td><td>  -  </td></tr>
       <tr><td> 500 </td><td> Internal trace-server error while trying to create configuration instance </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<ModelConfiguration> postConfigurationWithHttpInfo(@javax.annotation.Nonnull String typeId, @javax.annotation.Nonnull ConfigurationQueryParameters configurationQueryParameters) throws ApiException {
    // Check required parameters
    if (typeId == null) {
      throw new ApiException(400, "Missing the required parameter 'typeId' when calling postConfiguration");
    }
    if (configurationQueryParameters == null) {
      throw new ApiException(400, "Missing the required parameter 'configurationQueryParameters' when calling postConfiguration");
    }

    // Path parameters
    String localVarPath = "/config/types/{typeId}/configs"
            .replaceAll("\\{typeId}", apiClient.escapeString(typeId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<ModelConfiguration> localVarReturnType = new GenericType<ModelConfiguration>() {};
    return apiClient.invokeAPI("ConfigurationsApi.postConfiguration", localVarPath, "POST", new ArrayList<>(), configurationQueryParameters,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Update a configuration instance of a given configuration source type
   * 
   * @param typeId The configuration source type ID (required)
   * @param configId The configuration instance ID (required)
   * @param configurationQueryParameters Query parameters to update a configuration instance. Provide all query parameter keys and values as specified in the corresponding configuration source type. (required)
   * @return ModelConfiguration
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> The configuration instance was successfully updated </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such configuration source type or configuration instance </td><td>  -  </td></tr>
       <tr><td> 500 </td><td> Internal trace-server error while trying to update configuration instance </td><td>  -  </td></tr>
     </table>
   */
  public ModelConfiguration putConfiguration(@javax.annotation.Nonnull String typeId, @javax.annotation.Nonnull String configId, @javax.annotation.Nonnull ConfigurationQueryParameters configurationQueryParameters) throws ApiException {
    return putConfigurationWithHttpInfo(typeId, configId, configurationQueryParameters).getData();
  }

  /**
   * Update a configuration instance of a given configuration source type
   * 
   * @param typeId The configuration source type ID (required)
   * @param configId The configuration instance ID (required)
   * @param configurationQueryParameters Query parameters to update a configuration instance. Provide all query parameter keys and values as specified in the corresponding configuration source type. (required)
   * @return ApiResponse&lt;ModelConfiguration&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> The configuration instance was successfully updated </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such configuration source type or configuration instance </td><td>  -  </td></tr>
       <tr><td> 500 </td><td> Internal trace-server error while trying to update configuration instance </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<ModelConfiguration> putConfigurationWithHttpInfo(@javax.annotation.Nonnull String typeId, @javax.annotation.Nonnull String configId, @javax.annotation.Nonnull ConfigurationQueryParameters configurationQueryParameters) throws ApiException {
    // Check required parameters
    if (typeId == null) {
      throw new ApiException(400, "Missing the required parameter 'typeId' when calling putConfiguration");
    }
    if (configId == null) {
      throw new ApiException(400, "Missing the required parameter 'configId' when calling putConfiguration");
    }
    if (configurationQueryParameters == null) {
      throw new ApiException(400, "Missing the required parameter 'configurationQueryParameters' when calling putConfiguration");
    }

    // Path parameters
    String localVarPath = "/config/types/{typeId}/configs/{configId}"
            .replaceAll("\\{typeId}", apiClient.escapeString(typeId.toString()))
            .replaceAll("\\{configId}", apiClient.escapeString(configId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<ModelConfiguration> localVarReturnType = new GenericType<ModelConfiguration>() {};
    return apiClient.invokeAPI("ConfigurationsApi.putConfiguration", localVarPath, "PUT", new ArrayList<>(), configurationQueryParameters,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
}
