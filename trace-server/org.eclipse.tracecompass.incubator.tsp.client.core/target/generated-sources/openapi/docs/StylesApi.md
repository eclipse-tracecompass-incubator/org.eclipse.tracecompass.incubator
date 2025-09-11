# StylesApi

All URIs are relative to *https://localhost:8080/tsp/api*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getStyles**](StylesApi.md#getStyles) | **POST** /experiments/{expUUID}/outputs/{outputId}/style | API to get the style map associated to this experiment and output |



## getStyles

> StylesResponse getStyles(expUUID, outputId, optionalQueryParameters)

API to get the style map associated to this experiment and output

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.StylesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        StylesApi apiInstance = new StylesApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        String outputId = "outputId_example"; // String | ID of the output provider to query
        OptionalQueryParameters optionalQueryParameters = new OptionalQueryParameters(); // OptionalQueryParameters | Query parameters to fetch the style map
        try {
            StylesResponse result = apiInstance.getStyles(expUUID, outputId, optionalQueryParameters);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling StylesApi#getStyles");
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
| **optionalQueryParameters** | [**OptionalQueryParameters**](OptionalQueryParameters.md)| Query parameters to fetch the style map | |

### Return type

[**StylesResponse**](StylesResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Style model that can be used jointly with OutputElementStyle to retrieve specific style values |  -  |
| **400** | Missing query parameters |  -  |
| **404** | Experiment or output provider not found |  -  |
| **405** | Analysis cannot run |  -  |

