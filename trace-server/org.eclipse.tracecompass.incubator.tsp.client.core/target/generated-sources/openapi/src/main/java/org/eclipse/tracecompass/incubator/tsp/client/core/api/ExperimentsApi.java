package org.eclipse.tracecompass.incubator.tsp.client.core.api;

import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.Pair;

import javax.ws.rs.core.GenericType;

import org.eclipse.tracecompass.incubator.tsp.client.core.model.DataProvider;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ErrorResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Experiment;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ExperimentErrorResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ExperimentQueryParameters;
import java.util.UUID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.15.0")
public class ExperimentsApi {
  private ApiClient apiClient;

  public ExperimentsApi() {
    this(Configuration.getDefaultApiClient());
  }

  public ExperimentsApi(ApiClient apiClient) {
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
   * Remove an experiment from the server
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @return Experiment
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> The trace was successfully deleted, return the deleted experiment. </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such experiment </td><td>  -  </td></tr>
     </table>
   */
  public Experiment deleteExperiment(@javax.annotation.Nonnull UUID expUUID) throws ApiException {
    return deleteExperimentWithHttpInfo(expUUID).getData();
  }

  /**
   * Remove an experiment from the server
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @return ApiResponse&lt;Experiment&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> The trace was successfully deleted, return the deleted experiment. </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such experiment </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Experiment> deleteExperimentWithHttpInfo(@javax.annotation.Nonnull UUID expUUID) throws ApiException {
    // Check required parameters
    if (expUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'expUUID' when calling deleteExperiment");
    }

    // Path parameters
    String localVarPath = "/experiments/{expUUID}"
            .replaceAll("\\{expUUID}", apiClient.escapeString(expUUID.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    GenericType<Experiment> localVarReturnType = new GenericType<Experiment>() {};
    return apiClient.invokeAPI("ExperimentsApi.deleteExperiment", localVarPath, "DELETE", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Get the model object for an experiment
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @return Experiment
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Return the experiment model </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such experiment </td><td>  -  </td></tr>
     </table>
   */
  public Experiment getExperiment(@javax.annotation.Nonnull UUID expUUID) throws ApiException {
    return getExperimentWithHttpInfo(expUUID).getData();
  }

  /**
   * Get the model object for an experiment
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @return ApiResponse&lt;Experiment&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Return the experiment model </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such experiment </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Experiment> getExperimentWithHttpInfo(@javax.annotation.Nonnull UUID expUUID) throws ApiException {
    // Check required parameters
    if (expUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'expUUID' when calling getExperiment");
    }

    // Path parameters
    String localVarPath = "/experiments/{expUUID}"
            .replaceAll("\\{expUUID}", apiClient.escapeString(expUUID.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    GenericType<Experiment> localVarReturnType = new GenericType<Experiment>() {};
    return apiClient.invokeAPI("ExperimentsApi.getExperiment", localVarPath, "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Get the list of experiments on the server
   * 
   * @return List&lt;Experiment&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a list of experiments </td><td>  -  </td></tr>
     </table>
   */
  public List<Experiment> getExperiments() throws ApiException {
    return getExperimentsWithHttpInfo().getData();
  }

  /**
   * Get the list of experiments on the server
   * 
   * @return ApiResponse&lt;List&lt;Experiment&gt;&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a list of experiments </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<List<Experiment>> getExperimentsWithHttpInfo() throws ApiException {
    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    GenericType<List<Experiment>> localVarReturnType = new GenericType<List<Experiment>>() {};
    return apiClient.invokeAPI("ExperimentsApi.getExperiments", "/experiments", "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Get the output descriptor for this experiment and output
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to query (required)
   * @return DataProvider
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns the output provider descriptor </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or output provider not found </td><td>  -  </td></tr>
     </table>
   */
  public DataProvider getProvider(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId) throws ApiException {
    return getProviderWithHttpInfo(expUUID, outputId).getData();
  }

  /**
   * Get the output descriptor for this experiment and output
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @param outputId ID of the output provider to query (required)
   * @return ApiResponse&lt;DataProvider&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns the output provider descriptor </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or output provider not found </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<DataProvider> getProviderWithHttpInfo(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull String outputId) throws ApiException {
    // Check required parameters
    if (expUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'expUUID' when calling getProvider");
    }
    if (outputId == null) {
      throw new ApiException(400, "Missing the required parameter 'outputId' when calling getProvider");
    }

    // Path parameters
    String localVarPath = "/experiments/{expUUID}/outputs/{outputId}"
            .replaceAll("\\{expUUID}", apiClient.escapeString(expUUID.toString()))
            .replaceAll("\\{outputId}", apiClient.escapeString(outputId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    GenericType<DataProvider> localVarReturnType = new GenericType<DataProvider>() {};
    return apiClient.invokeAPI("ExperimentsApi.getProvider", localVarPath, "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Get the list of outputs for this experiment
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @return List&lt;DataProvider&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a list of output provider descriptors </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or output provider not found </td><td>  -  </td></tr>
     </table>
   */
  public List<DataProvider> getProviders(@javax.annotation.Nonnull UUID expUUID) throws ApiException {
    return getProvidersWithHttpInfo(expUUID).getData();
  }

  /**
   * Get the list of outputs for this experiment
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @return ApiResponse&lt;List&lt;DataProvider&gt;&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns a list of output provider descriptors </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or output provider not found </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<List<DataProvider>> getProvidersWithHttpInfo(@javax.annotation.Nonnull UUID expUUID) throws ApiException {
    // Check required parameters
    if (expUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'expUUID' when calling getProviders");
    }

    // Path parameters
    String localVarPath = "/experiments/{expUUID}/outputs"
            .replaceAll("\\{expUUID}", apiClient.escapeString(expUUID.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    GenericType<List<DataProvider>> localVarReturnType = new GenericType<List<DataProvider>>() {};
    return apiClient.invokeAPI("ExperimentsApi.getProviders", localVarPath, "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Create a new experiment on the server
   * 
   * @param experimentQueryParameters  (required)
   * @return Experiment
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> The experiment was successfully created </td><td>  -  </td></tr>
       <tr><td> 204 </td><td> The experiment has at least one trace which hasn&#39;t been created yet </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such trace </td><td>  -  </td></tr>
       <tr><td> 409 </td><td> The experiment (name) already exists and both differ. </td><td>  -  </td></tr>
       <tr><td> 500 </td><td> Internal trace-server error while trying to post experiment </td><td>  -  </td></tr>
     </table>
   */
  public Experiment postExperiment(@javax.annotation.Nonnull ExperimentQueryParameters experimentQueryParameters) throws ApiException {
    return postExperimentWithHttpInfo(experimentQueryParameters).getData();
  }

  /**
   * Create a new experiment on the server
   * 
   * @param experimentQueryParameters  (required)
   * @return ApiResponse&lt;Experiment&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> The experiment was successfully created </td><td>  -  </td></tr>
       <tr><td> 204 </td><td> The experiment has at least one trace which hasn&#39;t been created yet </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such trace </td><td>  -  </td></tr>
       <tr><td> 409 </td><td> The experiment (name) already exists and both differ. </td><td>  -  </td></tr>
       <tr><td> 500 </td><td> Internal trace-server error while trying to post experiment </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Experiment> postExperimentWithHttpInfo(@javax.annotation.Nonnull ExperimentQueryParameters experimentQueryParameters) throws ApiException {
    // Check required parameters
    if (experimentQueryParameters == null) {
      throw new ApiException(400, "Missing the required parameter 'experimentQueryParameters' when calling postExperiment");
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<Experiment> localVarReturnType = new GenericType<Experiment>() {};
    return apiClient.invokeAPI("ExperimentsApi.postExperiment", "/experiments", "POST", new ArrayList<>(), experimentQueryParameters,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
}
