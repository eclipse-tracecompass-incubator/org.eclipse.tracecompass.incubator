# GenericXyApi

All URIs are relative to *https://localhost:8080/tsp/api*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getGenericXY**](GenericXyApi.md#getGenericXY) | **POST** /experiments/{expUUID}/outputs/genericXY/{outputId}/xy | API to get the xy model |
| [**getGenericXYChartTree**](GenericXyApi.md#getGenericXYChartTree) | **POST** /experiments/{expUUID}/outputs/genericXY/{outputId}/tree | API to get the tree for generic xy chart |



## getGenericXY

> XYResponse getGenericXY(expUUID, outputId, genericXYQueryParameters)

API to get the xy model

Unique endpoint for all xy models, ensures that the same template is followed for all endpoints.

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.GenericXyApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        GenericXyApi apiInstance = new GenericXyApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        String outputId = "outputId_example"; // String | ID of the output provider to query
        GenericXYQueryParameters genericXYQueryParameters = new GenericXYQueryParameters(); // GenericXYQueryParameters | Query parameters to fetch the xy model. The object 'requested_timerange' is the requested time range and number of samples. The array 'requested_items' is the list of entryId or seriesId being requested.
        try {
            XYResponse result = apiInstance.getGenericXY(expUUID, outputId, genericXYQueryParameters);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling GenericXyApi#getGenericXY");
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
| **genericXYQueryParameters** | [**GenericXYQueryParameters**](GenericXYQueryParameters.md)| Query parameters to fetch the xy model. The object &#39;requested_timerange&#39; is the requested time range and number of samples. The array &#39;requested_items&#39; is the list of entryId or seriesId being requested. | |

### Return type

[**XYResponse**](XYResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Return the queried xy response |  -  |
| **400** | Missing query parameters |  -  |
| **404** | Experiment or output provider not found |  -  |
| **405** | Analysis cannot run |  -  |


## getGenericXYChartTree

> XYTreeResponse getGenericXYChartTree(expUUID, outputId, treeQueryParameters)

API to get the tree for generic xy chart

Unique entry point for output providers, to get the tree of visible entries

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.GenericXyApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        GenericXyApi apiInstance = new GenericXyApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        String outputId = "outputId_example"; // String | ID of the output provider to query
        TreeQueryParameters treeQueryParameters = new TreeQueryParameters(); // TreeQueryParameters | Query parameters to fetch the generic XY tree. The object 'requested_timerange' specifies the requested time range. When absent the tree for the full range is returned.
        try {
            XYTreeResponse result = apiInstance.getGenericXYChartTree(expUUID, outputId, treeQueryParameters);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling GenericXyApi#getGenericXYChartTree");
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
| **treeQueryParameters** | [**TreeQueryParameters**](TreeQueryParameters.md)| Query parameters to fetch the generic XY tree. The object &#39;requested_timerange&#39; specifies the requested time range. When absent the tree for the full range is returned. | |

### Return type

[**XYTreeResponse**](XYTreeResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Returns a list of generic xy chart entries. The returned model must be consistent, parentIds must refer to a parent which exists in the model. |  -  |
| **400** | Invalid query parameters |  -  |
| **404** | Experiment or output provider not found |  -  |
| **405** | Analysis cannot run |  -  |

