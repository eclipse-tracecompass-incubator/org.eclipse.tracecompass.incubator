# ExperimentsApi

All URIs are relative to *https://localhost:8080/tsp/api*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deleteExperiment**](ExperimentsApi.md#deleteExperiment) | **DELETE** /experiments/{expUUID} | Remove an experiment from the server |
| [**getExperiment**](ExperimentsApi.md#getExperiment) | **GET** /experiments/{expUUID} | Get the model object for an experiment |
| [**getExperiments**](ExperimentsApi.md#getExperiments) | **GET** /experiments | Get the list of experiments on the server |
| [**getProvider**](ExperimentsApi.md#getProvider) | **GET** /experiments/{expUUID}/outputs/{outputId} | Get the output descriptor for this experiment and output |
| [**getProviders**](ExperimentsApi.md#getProviders) | **GET** /experiments/{expUUID}/outputs | Get the list of outputs for this experiment |
| [**postExperiment**](ExperimentsApi.md#postExperiment) | **POST** /experiments | Create a new experiment on the server |



## deleteExperiment

> Experiment deleteExperiment(expUUID)

Remove an experiment from the server

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.ExperimentsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        ExperimentsApi apiInstance = new ExperimentsApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        try {
            Experiment result = apiInstance.deleteExperiment(expUUID);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ExperimentsApi#deleteExperiment");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **expUUID** | **UUID**| UUID of the experiment to query | |

### Return type

[**Experiment**](Experiment.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The trace was successfully deleted, return the deleted experiment. |  -  |
| **404** | No such experiment |  -  |


## getExperiment

> Experiment getExperiment(expUUID)

Get the model object for an experiment

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.ExperimentsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        ExperimentsApi apiInstance = new ExperimentsApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        try {
            Experiment result = apiInstance.getExperiment(expUUID);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ExperimentsApi#getExperiment");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **expUUID** | **UUID**| UUID of the experiment to query | |

### Return type

[**Experiment**](Experiment.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Return the experiment model |  -  |
| **404** | No such experiment |  -  |


## getExperiments

> List&lt;Experiment&gt; getExperiments()

Get the list of experiments on the server

### Example

```java
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.ExperimentsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        ExperimentsApi apiInstance = new ExperimentsApi(defaultClient);
        try {
            List<Experiment> result = apiInstance.getExperiments();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ExperimentsApi#getExperiments");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters

This endpoint does not need any parameter.

### Return type

[**List&lt;Experiment&gt;**](Experiment.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Returns a list of experiments |  -  |


## getProvider

> DataProvider getProvider(expUUID, outputId)

Get the output descriptor for this experiment and output

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.ExperimentsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        ExperimentsApi apiInstance = new ExperimentsApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        String outputId = "outputId_example"; // String | ID of the output provider to query
        try {
            DataProvider result = apiInstance.getProvider(expUUID, outputId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ExperimentsApi#getProvider");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **expUUID** | **UUID**| UUID of the experiment to query | |
| **outputId** | **String**| ID of the output provider to query | |

### Return type

[**DataProvider**](DataProvider.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Returns the output provider descriptor |  -  |
| **404** | Experiment or output provider not found |  -  |


## getProviders

> List&lt;DataProvider&gt; getProviders(expUUID)

Get the list of outputs for this experiment

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.ExperimentsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        ExperimentsApi apiInstance = new ExperimentsApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        try {
            List<DataProvider> result = apiInstance.getProviders(expUUID);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ExperimentsApi#getProviders");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **expUUID** | **UUID**| UUID of the experiment to query | |

### Return type

[**List&lt;DataProvider&gt;**](DataProvider.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Returns a list of output provider descriptors |  -  |
| **404** | Experiment or output provider not found |  -  |


## postExperiment

> Experiment postExperiment(experimentQueryParameters)

Create a new experiment on the server

### Example

```java
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.ExperimentsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        ExperimentsApi apiInstance = new ExperimentsApi(defaultClient);
        ExperimentQueryParameters experimentQueryParameters = new ExperimentQueryParameters(); // ExperimentQueryParameters | 
        try {
            Experiment result = apiInstance.postExperiment(experimentQueryParameters);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ExperimentsApi#postExperiment");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **experimentQueryParameters** | [**ExperimentQueryParameters**](ExperimentQueryParameters.md)|  | |

### Return type

[**Experiment**](Experiment.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The experiment was successfully created |  -  |
| **204** | The experiment has at least one trace which hasn&#39;t been created yet |  -  |
| **400** | Invalid query parameters |  -  |
| **404** | No such trace |  -  |
| **409** | The experiment (name) already exists and both differ. |  -  |
| **500** | Internal trace-server error while trying to post experiment |  -  |

