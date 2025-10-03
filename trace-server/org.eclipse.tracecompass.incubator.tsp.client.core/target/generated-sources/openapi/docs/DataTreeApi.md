# DataTreeApi

All URIs are relative to *https://localhost:8080/tsp/api*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getDataTree**](DataTreeApi.md#getDataTree) | **POST** /experiments/{expUUID}/outputs/data/{outputId}/tree | API to get the data tree |



## getDataTree

> DataTreeResponse getDataTree(expUUID, outputId, treeQueryParameters)

API to get the data tree

Unique entry point for output providers, to get the tree of visible entries

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.DataTreeApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        DataTreeApi apiInstance = new DataTreeApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        String outputId = "outputId_example"; // String | ID of the output provider to query
        TreeQueryParameters treeQueryParameters = new TreeQueryParameters(); // TreeQueryParameters | Query parameters to fetch the data tree entries. The object 'requested_timerange' specifies the requested time range. When absent the tree for the full range is returned.
        try {
            DataTreeResponse result = apiInstance.getDataTree(expUUID, outputId, treeQueryParameters);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling DataTreeApi#getDataTree");
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
| **treeQueryParameters** | [**TreeQueryParameters**](TreeQueryParameters.md)| Query parameters to fetch the data tree entries. The object &#39;requested_timerange&#39; specifies the requested time range. When absent the tree for the full range is returned. | |

### Return type

[**DataTreeResponse**](DataTreeResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Returns a list of data tree entries. The returned model must be consistent, parentIds must refer to a parent which exists in the model. |  -  |
| **400** | Invalid query parameters |  -  |
| **404** | Experiment or output provider not found |  -  |
| **405** | Analysis cannot run |  -  |

