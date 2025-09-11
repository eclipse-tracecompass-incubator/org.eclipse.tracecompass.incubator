# VirtualTablesApi

All URIs are relative to *https://localhost:8080/tsp/api*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getColumns**](VirtualTablesApi.md#getColumns) | **POST** /experiments/{expUUID}/outputs/table/{outputId}/columns | API to get table columns |
| [**getLines**](VirtualTablesApi.md#getLines) | **POST** /experiments/{expUUID}/outputs/table/{outputId}/lines | API to get virtual table lines |



## getColumns

> TableColumnHeadersResponse getColumns(expUUID, outputId, optionalQueryParameters)

API to get table columns

Unique entry point for output providers, to get the column entries

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.VirtualTablesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        VirtualTablesApi apiInstance = new VirtualTablesApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        String outputId = "outputId_example"; // String | ID of the output provider to query
        OptionalQueryParameters optionalQueryParameters = new OptionalQueryParameters(); // OptionalQueryParameters | Query parameters to fetch the table columns
        try {
            TableColumnHeadersResponse result = apiInstance.getColumns(expUUID, outputId, optionalQueryParameters);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling VirtualTablesApi#getColumns");
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
| **optionalQueryParameters** | [**OptionalQueryParameters**](OptionalQueryParameters.md)| Query parameters to fetch the table columns | |

### Return type

[**TableColumnHeadersResponse**](TableColumnHeadersResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Returns a list of table headers |  -  |
| **400** | Invalid query parameters |  -  |
| **404** | Experiment or output provider not found |  -  |
| **405** | Analysis cannot run |  -  |


## getLines

> VirtualTableResponse getLines(expUUID, outputId, linesQueryParameters)

API to get virtual table lines

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.VirtualTablesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        VirtualTablesApi apiInstance = new VirtualTablesApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        String outputId = "outputId_example"; // String | ID of the output provider to query
        LinesQueryParameters linesQueryParameters = new LinesQueryParameters(); // LinesQueryParameters | Query parameters to fetch the table lines. One of 'requested_table_index' or 'requested_times' should be present. If 'requested_table_index' is used it is the starting index of the lines to be returned. If 'requested_times' is used it should contain an array with a single timestamp. The returned lines starting at the given timestamp (or the nearest following) will be returned. The 'requested_table_count' is the number of lines that should be returned. When 'requested_table_column_ids' is absent all columns are returned. When present it is the array of requested columnIds. Use 'table_search_expressions' for search providing a map of <columnId, regular expression>. Returned lines that match the search expression will be tagged. Use 'table_search_direction' to specify search direction [NEXT, PREVIOUS]. If present, 'requested_table_count' events are returned starting from the first matching event. Matching and not matching events are returned. Matching events will be tagged. If no matches are found, an empty list will be returned.
        try {
            VirtualTableResponse result = apiInstance.getLines(expUUID, outputId, linesQueryParameters);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling VirtualTablesApi#getLines");
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
| **linesQueryParameters** | [**LinesQueryParameters**](LinesQueryParameters.md)| Query parameters to fetch the table lines. One of &#39;requested_table_index&#39; or &#39;requested_times&#39; should be present. If &#39;requested_table_index&#39; is used it is the starting index of the lines to be returned. If &#39;requested_times&#39; is used it should contain an array with a single timestamp. The returned lines starting at the given timestamp (or the nearest following) will be returned. The &#39;requested_table_count&#39; is the number of lines that should be returned. When &#39;requested_table_column_ids&#39; is absent all columns are returned. When present it is the array of requested columnIds. Use &#39;table_search_expressions&#39; for search providing a map of &lt;columnId, regular expression&gt;. Returned lines that match the search expression will be tagged. Use &#39;table_search_direction&#39; to specify search direction [NEXT, PREVIOUS]. If present, &#39;requested_table_count&#39; events are returned starting from the first matching event. Matching and not matching events are returned. Matching events will be tagged. If no matches are found, an empty list will be returned. | |

### Return type

[**VirtualTableResponse**](VirtualTableResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Returns a table model with a 2D array of strings and metadata |  -  |
| **400** | Invalid query parameters |  -  |
| **404** | Experiment or output provider not found |  -  |
| **405** | Analysis cannot run |  -  |
| **500** | Error reading the experiment |  -  |

