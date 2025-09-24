# OutputConfigurationsApi

All URIs are relative to *https://localhost:8080/tsp/api*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createProvider**](OutputConfigurationsApi.md#createProvider) | **POST** /experiments/{expUUID}/outputs/{outputId} | Get a derived data provider from a input configuration |
| [**deleteDerivedProvider**](OutputConfigurationsApi.md#deleteDerivedProvider) | **DELETE** /experiments/{expUUID}/outputs/{outputId}/{derivedOutputId} | Delete a derived output (and its configuration). |
| [**getConfigurationType1**](OutputConfigurationsApi.md#getConfigurationType1) | **GET** /experiments/{expUUID}/outputs/{outputId}/configTypes/{typeId} | Get a single configuration source type defined on the server for a given data provider and experiment. |
| [**getConfigurationTypes1**](OutputConfigurationsApi.md#getConfigurationTypes1) | **GET** /experiments/{expUUID}/outputs/{outputId}/configTypes | Get the list of configuration types defined on the server for a given output and experiment |



## createProvider

> DataProvider createProvider(expUUID, outputId, outputConfigurationQueryParameters)

Get a derived data provider from a input configuration

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.OutputConfigurationsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        OutputConfigurationsApi apiInstance = new OutputConfigurationsApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        String outputId = "outputId_example"; // String | ID of the output provider to create a derived output from
        OutputConfigurationQueryParameters outputConfigurationQueryParameters = new OutputConfigurationQueryParameters(); // OutputConfigurationQueryParameters | Query parameters to create a configuration instance. Provide all query parameter keys and values as specified in the corresponding configuration source type.
        try {
            DataProvider result = apiInstance.createProvider(expUUID, outputId, outputConfigurationQueryParameters);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling OutputConfigurationsApi#createProvider");
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
| **outputId** | **String**| ID of the output provider to create a derived output from | |
| **outputConfigurationQueryParameters** | [**OutputConfigurationQueryParameters**](OutputConfigurationQueryParameters.md)| Query parameters to create a configuration instance. Provide all query parameter keys and values as specified in the corresponding configuration source type. | |

### Return type

[**DataProvider**](DataProvider.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Returns the derived data provider descriptor. |  -  |
| **400** | Invalid query parameters |  -  |
| **404** | Experiment, output provider or configuration type not found |  -  |


## deleteDerivedProvider

> DataProvider deleteDerivedProvider(expUUID, outputId, derivedOutputId)

Delete a derived output (and its configuration).

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.OutputConfigurationsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        OutputConfigurationsApi apiInstance = new OutputConfigurationsApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        String outputId = "outputId_example"; // String | ID of the parent output provider
        String derivedOutputId = "derivedOutputId_example"; // String | ID of the derived output provider
        try {
            DataProvider result = apiInstance.deleteDerivedProvider(expUUID, outputId, derivedOutputId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling OutputConfigurationsApi#deleteDerivedProvider");
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
| **outputId** | **String**| ID of the parent output provider | |
| **derivedOutputId** | **String**| ID of the derived output provider | |

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
| **200** | Returns the deleted derived data provider descriptor. The derived data provider (and its configuration) was successfully deleted. |  -  |
| **400** | Invalid query parameters |  -  |
| **404** | Experiment, output provider or configuration type not found |  -  |


## getConfigurationType1

> ConfigurationSourceType getConfigurationType1(expUUID, outputId, typeId)

Get a single configuration source type defined on the server for a given data provider and experiment.

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.OutputConfigurationsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        OutputConfigurationsApi apiInstance = new OutputConfigurationsApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        String outputId = "outputId_example"; // String | ID of the output provider to query
        String typeId = "typeId_example"; // String | The configuration source type ID
        try {
            ConfigurationSourceType result = apiInstance.getConfigurationType1(expUUID, outputId, typeId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling OutputConfigurationsApi#getConfigurationType1");
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
| **typeId** | **String**| The configuration source type ID | |

### Return type

[**ConfigurationSourceType**](ConfigurationSourceType.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Returns a single configuration source type |  -  |
| **400** | Invalid query parameters |  -  |
| **404** | Experiment, output provider or configuration type not found |  -  |


## getConfigurationTypes1

> List&lt;ConfigurationSourceType&gt; getConfigurationTypes1(expUUID, outputId)

Get the list of configuration types defined on the server for a given output and experiment

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.OutputConfigurationsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        OutputConfigurationsApi apiInstance = new OutputConfigurationsApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        String outputId = "outputId_example"; // String | ID of the output provider to query
        try {
            List<ConfigurationSourceType> result = apiInstance.getConfigurationTypes1(expUUID, outputId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling OutputConfigurationsApi#getConfigurationTypes1");
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

[**List&lt;ConfigurationSourceType&gt;**](ConfigurationSourceType.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Returns a list of configuration types that this output supports. |  -  |
| **404** | Experiment, output provider or configuration type not found |  -  |

