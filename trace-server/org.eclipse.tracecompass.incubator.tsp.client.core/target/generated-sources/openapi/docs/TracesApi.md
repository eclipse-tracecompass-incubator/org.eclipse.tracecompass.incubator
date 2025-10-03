# TracesApi

All URIs are relative to *https://localhost:8080/tsp/api*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deleteTrace**](TracesApi.md#deleteTrace) | **DELETE** /traces/{uuid} | Remove a trace from the server and disk |
| [**getTrace**](TracesApi.md#getTrace) | **GET** /traces/{uuid} | Get the model object for a trace |
| [**getTraces**](TracesApi.md#getTraces) | **GET** /traces | Get the list of physical traces imported on the server |
| [**putTrace**](TracesApi.md#putTrace) | **POST** /traces | Import a trace |



## deleteTrace

> Trace deleteTrace(uuid)

Remove a trace from the server and disk

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.TracesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        TracesApi apiInstance = new TracesApi(defaultClient);
        UUID uuid = UUID.randomUUID(); // UUID | UUID of the trace to query
        try {
            Trace result = apiInstance.deleteTrace(uuid);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling TracesApi#deleteTrace");
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
| **uuid** | **UUID**| UUID of the trace to query | |

### Return type

[**Trace**](Trace.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The trace was successfully deleted |  -  |
| **404** | No such trace |  -  |
| **409** | The trace is in use by at least one experiment thus cannot be deleted. |  -  |


## getTrace

> Trace getTrace(uuid)

Get the model object for a trace

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.TracesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        TracesApi apiInstance = new TracesApi(defaultClient);
        UUID uuid = UUID.randomUUID(); // UUID | UUID of the trace to query
        try {
            Trace result = apiInstance.getTrace(uuid);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling TracesApi#getTrace");
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
| **uuid** | **UUID**| UUID of the trace to query | |

### Return type

[**Trace**](Trace.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Return the trace model |  -  |
| **404** | No such trace |  -  |


## getTraces

> List&lt;Trace&gt; getTraces()

Get the list of physical traces imported on the server

### Example

```java
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.TracesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        TracesApi apiInstance = new TracesApi(defaultClient);
        try {
            List<Trace> result = apiInstance.getTraces();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling TracesApi#getTraces");
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

[**List&lt;Trace&gt;**](Trace.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Returns a list of traces |  -  |


## putTrace

> Trace putTrace(traceQueryParameters)

Import a trace

Import a trace to the trace server. Return some base information once imported.

### Example

```java
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.TracesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        TracesApi apiInstance = new TracesApi(defaultClient);
        TraceQueryParameters traceQueryParameters = new TraceQueryParameters(); // TraceQueryParameters | 
        try {
            Trace result = apiInstance.putTrace(traceQueryParameters);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling TracesApi#putTrace");
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
| **traceQueryParameters** | [**TraceQueryParameters**](TraceQueryParameters.md)|  | |

### Return type

[**Trace**](Trace.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The trace has been successfully added to the trace server |  -  |
| **400** | Missing query parameters |  -  |
| **404** | No such trace |  -  |
| **406** | Cannot read this trace type |  -  |
| **409** | The trace (name) already exists and both differ |  -  |
| **500** | Trace resource creation failed |  -  |
| **501** | Trace type not supported |  -  |

