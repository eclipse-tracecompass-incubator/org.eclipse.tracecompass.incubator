# AnnotationsApi

All URIs are relative to *https://localhost:8080/tsp/api*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getAnnotationCategories**](AnnotationsApi.md#getAnnotationCategories) | **GET** /experiments/{expUUID}/outputs/{outputId}/annotations | API to get annotation categories associated to this experiment and output |
| [**getAnnotations**](AnnotationsApi.md#getAnnotations) | **POST** /experiments/{expUUID}/outputs/{outputId}/annotations | API to get the annotations associated to this experiment and output |
| [**getMarkerSets**](AnnotationsApi.md#getMarkerSets) | **GET** /experiments/{expUUID}/outputs/markerSets | API to get marker sets available for this experiment |



## getAnnotationCategories

> AnnotationCategoriesResponse getAnnotationCategories(expUUID, outputId, markerSetId)

API to get annotation categories associated to this experiment and output

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.AnnotationsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        AnnotationsApi apiInstance = new AnnotationsApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        String outputId = "outputId_example"; // String | ID of the output provider to query
        String markerSetId = "markerSetId_example"; // String | The optional requested marker set's id
        try {
            AnnotationCategoriesResponse result = apiInstance.getAnnotationCategories(expUUID, outputId, markerSetId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AnnotationsApi#getAnnotationCategories");
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
| **markerSetId** | **String**| The optional requested marker set&#39;s id | [optional] |

### Return type

[**AnnotationCategoriesResponse**](AnnotationCategoriesResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Annotation categories |  -  |
| **400** | Missing parameter outputId |  -  |
| **404** | Experiment or output provider not found |  -  |
| **405** | Analysis cannot run |  -  |


## getAnnotations

> AnnotationResponse getAnnotations(expUUID, outputId, annotationsQueryParameters)

API to get the annotations associated to this experiment and output

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.AnnotationsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        AnnotationsApi apiInstance = new AnnotationsApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        String outputId = "outputId_example"; // String | ID of the output provider to query
        AnnotationsQueryParameters annotationsQueryParameters = new AnnotationsQueryParameters(); // AnnotationsQueryParameters | Query parameters to fetch the annotations. The object 'requested_timerange' is the requested time range and number of samples. The array 'requested_items' is the list of entryId being requested. The string 'requested_marker_set' is the optional requested marker set's id. The array 'requested_marker_categories' is the list of requested annotation categories; if absent, all annotations are returned.
        try {
            AnnotationResponse result = apiInstance.getAnnotations(expUUID, outputId, annotationsQueryParameters);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AnnotationsApi#getAnnotations");
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
| **annotationsQueryParameters** | [**AnnotationsQueryParameters**](AnnotationsQueryParameters.md)| Query parameters to fetch the annotations. The object &#39;requested_timerange&#39; is the requested time range and number of samples. The array &#39;requested_items&#39; is the list of entryId being requested. The string &#39;requested_marker_set&#39; is the optional requested marker set&#39;s id. The array &#39;requested_marker_categories&#39; is the list of requested annotation categories; if absent, all annotations are returned. | |

### Return type

[**AnnotationResponse**](AnnotationResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Annotation |  -  |
| **400** | Missing query parameters |  -  |
| **404** | Experiment or output provider not found |  -  |
| **405** | Analysis cannot run |  -  |


## getMarkerSets

> MarkerSetsResponse getMarkerSets(expUUID)

API to get marker sets available for this experiment

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.AnnotationsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        AnnotationsApi apiInstance = new AnnotationsApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        try {
            MarkerSetsResponse result = apiInstance.getMarkerSets(expUUID);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AnnotationsApi#getMarkerSets");
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

### Return type

[**MarkerSetsResponse**](MarkerSetsResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | List of marker sets |  -  |
| **404** | Experiment or output provider not found |  -  |

