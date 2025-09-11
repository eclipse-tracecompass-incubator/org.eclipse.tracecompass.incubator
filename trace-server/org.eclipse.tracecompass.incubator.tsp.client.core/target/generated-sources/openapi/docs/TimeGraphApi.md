# TimeGraphApi

All URIs are relative to *https://localhost:8080/tsp/api*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getArrows**](TimeGraphApi.md#getArrows) | **POST** /experiments/{expUUID}/outputs/timeGraph/{outputId}/arrows | API to get the Time Graph arrows |
| [**getStates**](TimeGraphApi.md#getStates) | **POST** /experiments/{expUUID}/outputs/timeGraph/{outputId}/states | API to get the Time Graph states |
| [**getTimeGraphTooltip**](TimeGraphApi.md#getTimeGraphTooltip) | **POST** /experiments/{expUUID}/outputs/timeGraph/{outputId}/tooltip | API to get a Time Graph tooltip |
| [**getTimeGraphTree**](TimeGraphApi.md#getTimeGraphTree) | **POST** /experiments/{expUUID}/outputs/timeGraph/{outputId}/tree | API to get the Time Graph tree |



## getArrows

> TimeGraphArrowsResponse getArrows(expUUID, outputId, arrowsQueryParameters)

API to get the Time Graph arrows

Unique entry point for all TimeGraph models, ensures that the same template is followed for all models

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.TimeGraphApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        TimeGraphApi apiInstance = new TimeGraphApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        String outputId = "outputId_example"; // String | ID of the output provider to query
        ArrowsQueryParameters arrowsQueryParameters = new ArrowsQueryParameters(); // ArrowsQueryParameters | Query parameters to fetch the timegraph arrows. The object 'requested_timerange' is the requested time range and number of samples.
        try {
            TimeGraphArrowsResponse result = apiInstance.getArrows(expUUID, outputId, arrowsQueryParameters);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling TimeGraphApi#getArrows");
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
| **arrowsQueryParameters** | [**ArrowsQueryParameters**](ArrowsQueryParameters.md)| Query parameters to fetch the timegraph arrows. The object &#39;requested_timerange&#39; is the requested time range and number of samples. | |

### Return type

[**TimeGraphArrowsResponse**](TimeGraphArrowsResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Returns a sampled list of TimeGraph arrows |  -  |
| **400** | Missing query parameters |  -  |
| **404** | Experiment or output provider not found |  -  |
| **405** | Analysis cannot run |  -  |


## getStates

> TimeGraphStatesResponse getStates(expUUID, outputId, requestedQueryParameters)

API to get the Time Graph states

Unique entry point for all TimeGraph states, ensures that the same template is followed for all views

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.TimeGraphApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        TimeGraphApi apiInstance = new TimeGraphApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        String outputId = "outputId_example"; // String | ID of the output provider to query
        RequestedQueryParameters requestedQueryParameters = new RequestedQueryParameters(); // RequestedQueryParameters | Query parameters to fetch the timegraph states. The object 'requested_timerange' is the requested time range and number of samples. The array 'requested_items' is the list of entryId being requested. The object 'filter_query_parameters' contains requests for search/filter queries. The object 'filter_expressions_map' is the list of query requests, where the key 1 is DIMMED and 4 is EXCLUDED, and the value is an array of the desired search query ('thread=1' or 'process=ls' or 'duration>10ms'). The 'strategy' flag is an optional parameter within 'filter_query_parameters', and if omitted then 'SAMPLED' search would be the default value. If 'strategy' is set to 'DEEP' then the full time range between the first and last requested timestamp should be searched for filter matches. For timegraphs, only one matching state per gap in requested timestamps needs to be returned in the response. If matches to the queries from the 'filter_expressions_map' are found there'll be a field 'tags' in 'states'. The TimeGraphState class has a bit-mask called tags. If a state is supposed to be dimmed the tag will be the corresponding bit set.
        try {
            TimeGraphStatesResponse result = apiInstance.getStates(expUUID, outputId, requestedQueryParameters);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling TimeGraphApi#getStates");
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
| **requestedQueryParameters** | [**RequestedQueryParameters**](RequestedQueryParameters.md)| Query parameters to fetch the timegraph states. The object &#39;requested_timerange&#39; is the requested time range and number of samples. The array &#39;requested_items&#39; is the list of entryId being requested. The object &#39;filter_query_parameters&#39; contains requests for search/filter queries. The object &#39;filter_expressions_map&#39; is the list of query requests, where the key 1 is DIMMED and 4 is EXCLUDED, and the value is an array of the desired search query (&#39;thread&#x3D;1&#39; or &#39;process&#x3D;ls&#39; or &#39;duration&gt;10ms&#39;). The &#39;strategy&#39; flag is an optional parameter within &#39;filter_query_parameters&#39;, and if omitted then &#39;SAMPLED&#39; search would be the default value. If &#39;strategy&#39; is set to &#39;DEEP&#39; then the full time range between the first and last requested timestamp should be searched for filter matches. For timegraphs, only one matching state per gap in requested timestamps needs to be returned in the response. If matches to the queries from the &#39;filter_expressions_map&#39; are found there&#39;ll be a field &#39;tags&#39; in &#39;states&#39;. The TimeGraphState class has a bit-mask called tags. If a state is supposed to be dimmed the tag will be the corresponding bit set. | |

### Return type

[**TimeGraphStatesResponse**](TimeGraphStatesResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Returns a list of time graph rows |  -  |
| **400** | Missing query parameters |  -  |
| **404** | Experiment or output provider not found |  -  |
| **405** | Analysis cannot run |  -  |


## getTimeGraphTooltip

> TimeGraphTooltipResponse getTimeGraphTooltip(expUUID, outputId, tooltipQueryParameters)

API to get a Time Graph tooltip

Endpoint to retrieve tooltips for time graph

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.TimeGraphApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        TimeGraphApi apiInstance = new TimeGraphApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        String outputId = "outputId_example"; // String | ID of the output provider to query
        TooltipQueryParameters tooltipQueryParameters = new TooltipQueryParameters(); // TooltipQueryParameters | Query parameters to fetch the timegraph tooltip. The array 'requested_times' is an array with a single timestamp. The array 'requested_items' is an array with a single entryId being requested.  The object 'requested_element' is the element for which the tooltip is requested.
        try {
            TimeGraphTooltipResponse result = apiInstance.getTimeGraphTooltip(expUUID, outputId, tooltipQueryParameters);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling TimeGraphApi#getTimeGraphTooltip");
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
| **tooltipQueryParameters** | [**TooltipQueryParameters**](TooltipQueryParameters.md)| Query parameters to fetch the timegraph tooltip. The array &#39;requested_times&#39; is an array with a single timestamp. The array &#39;requested_items&#39; is an array with a single entryId being requested.  The object &#39;requested_element&#39; is the element for which the tooltip is requested. | |

### Return type

[**TimeGraphTooltipResponse**](TimeGraphTooltipResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Returns a map of tooltip keys to values |  -  |
| **400** | Missing query parameters |  -  |
| **404** | Experiment or output provider not found |  -  |
| **405** | Analysis cannot run |  -  |


## getTimeGraphTree

> TimeGraphTreeResponse getTimeGraphTree(expUUID, outputId, treeQueryParameters)

API to get the Time Graph tree

Unique entry point for output providers, to get the tree of visible entries

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.TimeGraphApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        TimeGraphApi apiInstance = new TimeGraphApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        String outputId = "outputId_example"; // String | ID of the output provider to query
        TreeQueryParameters treeQueryParameters = new TreeQueryParameters(); // TreeQueryParameters | Query parameters to fetch the timegraph tree. The object 'requested_timerange' specifies the requested time range. When absent the tree for the full range is returned.
        try {
            TimeGraphTreeResponse result = apiInstance.getTimeGraphTree(expUUID, outputId, treeQueryParameters);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling TimeGraphApi#getTimeGraphTree");
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
| **treeQueryParameters** | [**TreeQueryParameters**](TreeQueryParameters.md)| Query parameters to fetch the timegraph tree. The object &#39;requested_timerange&#39; specifies the requested time range. When absent the tree for the full range is returned. | |

### Return type

[**TimeGraphTreeResponse**](TimeGraphTreeResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Returns a list of Time Graph entries. The returned model must be consistent, parentIds must refer to a parent which exists in the model. |  -  |
| **400** | Invalid query parameters |  -  |
| **404** | Experiment or output provider not found |  -  |
| **405** | Analysis cannot run |  -  |

