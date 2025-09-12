# DiagnosticApi

All URIs are relative to *https://localhost:8080/tsp/api*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getHealthStatus**](DiagnosticApi.md#getHealthStatus) | **GET** /health | Get the health status of this server |



## getHealthStatus

> ServerStatus getHealthStatus()

Get the health status of this server

### Example

```java
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.DiagnosticApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        DiagnosticApi apiInstance = new DiagnosticApi(defaultClient);
        try {
            ServerStatus result = apiInstance.getHealthStatus();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling DiagnosticApi#getHealthStatus");
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

[**ServerStatus**](ServerStatus.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The trace server is running and ready to receive requests |  -  |
| **503** | The trace server is unavailable or in maintenance and cannot receive requests |  -  |

