# ConfigurationsApi

All URIs are relative to *https://localhost:8080/tsp/api*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deleteConfiguration**](ConfigurationsApi.md#deleteConfiguration) | **DELETE** /config/types/{typeId}/configs/{configId} | Delete a configuration instance of a given configuration source type |
| [**getConfiguration**](ConfigurationsApi.md#getConfiguration) | **GET** /config/types/{typeId}/configs/{configId} | Get a configuration instance of a given configuration source type |
| [**getConfigurationType**](ConfigurationsApi.md#getConfigurationType) | **GET** /config/types/{typeId} | Get a single configuration source type defined on the server |
| [**getConfigurationTypes**](ConfigurationsApi.md#getConfigurationTypes) | **GET** /config/types | Get the list of configuration source types defined on the server |
| [**getConfigurations**](ConfigurationsApi.md#getConfigurations) | **GET** /config/types/{typeId}/configs | Get the list of configurations that are instantiated of a given configuration source type |
| [**postConfiguration**](ConfigurationsApi.md#postConfiguration) | **POST** /config/types/{typeId}/configs | Create a configuration instance for the given configuration source type |
| [**putConfiguration**](ConfigurationsApi.md#putConfiguration) | **PUT** /config/types/{typeId}/configs/{configId} | Update a configuration instance of a given configuration source type |



## deleteConfiguration

> ModelConfiguration deleteConfiguration(typeId, configId)

Delete a configuration instance of a given configuration source type

### Example

```java
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.ConfigurationsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        ConfigurationsApi apiInstance = new ConfigurationsApi(defaultClient);
        String typeId = "typeId_example"; // String | The configuration source type ID
        String configId = "configId_example"; // String | The configuration instance ID
        try {
            ModelConfiguration result = apiInstance.deleteConfiguration(typeId, configId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ConfigurationsApi#deleteConfiguration");
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
| **typeId** | **String**| The configuration source type ID | |
| **configId** | **String**| The configuration instance ID | |

### Return type

[**ModelConfiguration**](ModelConfiguration.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The configuration instance was successfully deleted |  -  |
| **404** | No such configuration source type or configuration instance |  -  |
| **500** | Internal trace-server error while trying to delete configuration instance |  -  |


## getConfiguration

> ModelConfiguration getConfiguration(typeId, configId)

Get a configuration instance of a given configuration source type

### Example

```java
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.ConfigurationsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        ConfigurationsApi apiInstance = new ConfigurationsApi(defaultClient);
        String typeId = "typeId_example"; // String | The configuration source type ID
        String configId = "configId_example"; // String | The configuration instance ID
        try {
            ModelConfiguration result = apiInstance.getConfiguration(typeId, configId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ConfigurationsApi#getConfiguration");
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
| **typeId** | **String**| The configuration source type ID | |
| **configId** | **String**| The configuration instance ID | |

### Return type

[**ModelConfiguration**](ModelConfiguration.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Get a configuration instance |  -  |
| **404** | No such configuration source type or configuration instance |  -  |


## getConfigurationType

> ConfigurationSourceType getConfigurationType(typeId)

Get a single configuration source type defined on the server

### Example

```java
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.ConfigurationsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        ConfigurationsApi apiInstance = new ConfigurationsApi(defaultClient);
        String typeId = "typeId_example"; // String | The configuration source type ID
        try {
            ConfigurationSourceType result = apiInstance.getConfigurationType(typeId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ConfigurationsApi#getConfigurationType");
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
| **404** | No such configuration type |  -  |


## getConfigurationTypes

> List&lt;ConfigurationSourceType&gt; getConfigurationTypes()

Get the list of configuration source types defined on the server

### Example

```java
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.ConfigurationsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        ConfigurationsApi apiInstance = new ConfigurationsApi(defaultClient);
        try {
            List<ConfigurationSourceType> result = apiInstance.getConfigurationTypes();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ConfigurationsApi#getConfigurationTypes");
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

[**List&lt;ConfigurationSourceType&gt;**](ConfigurationSourceType.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Returns a list of configuration source types |  -  |


## getConfigurations

> List&lt;ModelConfiguration&gt; getConfigurations(typeId)

Get the list of configurations that are instantiated of a given configuration source type

### Example

```java
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.ConfigurationsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        ConfigurationsApi apiInstance = new ConfigurationsApi(defaultClient);
        String typeId = "typeId_example"; // String | The configuration source type ID
        try {
            List<ModelConfiguration> result = apiInstance.getConfigurations(typeId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ConfigurationsApi#getConfigurations");
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
| **typeId** | **String**| The configuration source type ID | |

### Return type

[**List&lt;ModelConfiguration&gt;**](ModelConfiguration.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Get the list of configuration descriptors  |  -  |
| **404** | No such configuration source type or configuration instance |  -  |


## postConfiguration

> ModelConfiguration postConfiguration(typeId, configurationQueryParameters)

Create a configuration instance for the given configuration source type

### Example

```java
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.ConfigurationsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        ConfigurationsApi apiInstance = new ConfigurationsApi(defaultClient);
        String typeId = "typeId_example"; // String | The configuration source type ID
        ConfigurationQueryParameters configurationQueryParameters = new ConfigurationQueryParameters(); // ConfigurationQueryParameters | Query parameters to create a configuration instance. Provide all query parameter keys and values as specified in the corresponding configuration source type.
        try {
            ModelConfiguration result = apiInstance.postConfiguration(typeId, configurationQueryParameters);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ConfigurationsApi#postConfiguration");
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
| **typeId** | **String**| The configuration source type ID | |
| **configurationQueryParameters** | [**ConfigurationQueryParameters**](ConfigurationQueryParameters.md)| Query parameters to create a configuration instance. Provide all query parameter keys and values as specified in the corresponding configuration source type. | |

### Return type

[**ModelConfiguration**](ModelConfiguration.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The configuration instance was successfully created |  -  |
| **400** | Invalid query parameters |  -  |
| **404** | No such configuration source type or configuration instance |  -  |
| **500** | Internal trace-server error while trying to create configuration instance |  -  |


## putConfiguration

> ModelConfiguration putConfiguration(typeId, configId, configurationQueryParameters)

Update a configuration instance of a given configuration source type

### Example

```java
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.ConfigurationsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        ConfigurationsApi apiInstance = new ConfigurationsApi(defaultClient);
        String typeId = "typeId_example"; // String | The configuration source type ID
        String configId = "configId_example"; // String | The configuration instance ID
        ConfigurationQueryParameters configurationQueryParameters = new ConfigurationQueryParameters(); // ConfigurationQueryParameters | Query parameters to update a configuration instance. Provide all query parameter keys and values as specified in the corresponding configuration source type.
        try {
            ModelConfiguration result = apiInstance.putConfiguration(typeId, configId, configurationQueryParameters);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ConfigurationsApi#putConfiguration");
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
| **typeId** | **String**| The configuration source type ID | |
| **configId** | **String**| The configuration instance ID | |
| **configurationQueryParameters** | [**ConfigurationQueryParameters**](ConfigurationQueryParameters.md)| Query parameters to update a configuration instance. Provide all query parameter keys and values as specified in the corresponding configuration source type. | |

### Return type

[**ModelConfiguration**](ModelConfiguration.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The configuration instance was successfully updated |  -  |
| **400** | Invalid query parameters |  -  |
| **404** | No such configuration source type or configuration instance |  -  |
| **500** | Internal trace-server error while trying to update configuration instance |  -  |

