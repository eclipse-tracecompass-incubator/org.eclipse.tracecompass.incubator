# XyApi

All URIs are relative to *https://localhost:8080/tsp/api*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getXY**](XyApi.md#getXY) | **POST** /experiments/{expUUID}/outputs/XY/{outputId}/xy | API to get the XY model |
| [**getXYTree**](XyApi.md#getXYTree) | **POST** /experiments/{expUUID}/outputs/XY/{outputId}/tree | API to get the XY tree |



## getXY

> XYResponse getXY(expUUID, outputId, requestedQueryParameters)

API to get the XY model

Unique endpoint for all xy models, ensures that the same template is followed for all endpoints.

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.XyApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        XyApi apiInstance = new XyApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        String outputId = "outputId_example"; // String | ID of the output provider to query
        RequestedQueryParameters requestedQueryParameters = new RequestedQueryParameters(); // RequestedQueryParameters | Query parameters to fetch the XY model. The object 'requested_timerange' is the requested time range and number of samples. The array 'requested_items' is the list of entryId or seriesId being requested.
        try {
            XYResponse result = apiInstance.getXY(expUUID, outputId, requestedQueryParameters);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling XyApi#getXY");
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
| **requestedQueryParameters** | [**RequestedQueryParameters**](RequestedQueryParameters.md)| Query parameters to fetch the XY model. The object &#39;requested_timerange&#39; is the requested time range and number of samples. The array &#39;requested_items&#39; is the list of entryId or seriesId being requested. | |

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
| **200** | Return the queried XYResponse |  -  |
| **400** | Missing query parameters |  -  |
| **404** | Experiment or output provider not found |  -  |
| **405** | Analysis cannot run |  -  |


## getXYTree

> XYTreeResponse getXYTree(expUUID, outputId, treeQueryParameters)

API to get the XY tree

Unique entry point for output providers, to get the tree of visible entries

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.XyApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        XyApi apiInstance = new XyApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        String outputId = "outputId_example"; // String | ID of the output provider to query
        TreeQueryParameters treeQueryParameters = new TreeQueryParameters(); // TreeQueryParameters | Query parameters to fetch the XY tree. The object 'requested_timerange' specifies the requested time range. When absent the tree for the full range is returned.
        try {
            XYTreeResponse result = apiInstance.getXYTree(expUUID, outputId, treeQueryParameters);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling XyApi#getXYTree");
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
| **treeQueryParameters** | [**TreeQueryParameters**](TreeQueryParameters.md)| Query parameters to fetch the XY tree. The object &#39;requested_timerange&#39; specifies the requested time range. When absent the tree for the full range is returned. | |

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
| **200** | Returns a list of XY entries. The returned model must be consistent, parentIds must refer to a parent which exists in the model. |  -  |
| **400** | Invalid query parameters |  -  |
| **404** | Experiment or output provider not found |  -  |
| **405** | Analysis cannot run |  -  |

