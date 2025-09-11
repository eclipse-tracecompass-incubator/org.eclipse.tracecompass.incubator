# BookmarksApi

All URIs are relative to *https://localhost:8080/tsp/api*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createBookmark**](BookmarksApi.md#createBookmark) | **POST** /experiments/{expUUID}/bookmarks | Create a new bookmark in an experiment |
| [**deleteBookmark**](BookmarksApi.md#deleteBookmark) | **DELETE** /experiments/{expUUID}/bookmarks/{bookmarkUUID} | Delete a bookmark from an experiment |
| [**getBookmark**](BookmarksApi.md#getBookmark) | **GET** /experiments/{expUUID}/bookmarks/{bookmarkUUID} | Get a specific bookmark from an experiment |
| [**getBookmarks**](BookmarksApi.md#getBookmarks) | **GET** /experiments/{expUUID}/bookmarks | Get all bookmarks for an experiment |
| [**updateBookmark**](BookmarksApi.md#updateBookmark) | **PUT** /experiments/{expUUID}/bookmarks/{bookmarkUUID} | Update an existing bookmark in an experiment |



## createBookmark

> Bookmark createBookmark(expUUID, bookmarkQueryParameters)

Create a new bookmark in an experiment

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.BookmarksApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        BookmarksApi apiInstance = new BookmarksApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        BookmarkQueryParameters bookmarkQueryParameters = new BookmarkQueryParameters(); // BookmarkQueryParameters | 
        try {
            Bookmark result = apiInstance.createBookmark(expUUID, bookmarkQueryParameters);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling BookmarksApi#createBookmark");
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
| **bookmarkQueryParameters** | [**BookmarkQueryParameters**](BookmarkQueryParameters.md)|  | |

### Return type

[**Bookmark**](Bookmark.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Bookmark created successfully |  -  |
| **400** | Invalid query parameters |  -  |
| **404** | No such experiment |  -  |


## deleteBookmark

> Bookmark deleteBookmark(expUUID, bookmarkUUID)

Delete a bookmark from an experiment

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.BookmarksApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        BookmarksApi apiInstance = new BookmarksApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        UUID bookmarkUUID = UUID.randomUUID(); // UUID | Bookmark UUID
        try {
            Bookmark result = apiInstance.deleteBookmark(expUUID, bookmarkUUID);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling BookmarksApi#deleteBookmark");
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
| **bookmarkUUID** | **UUID**| Bookmark UUID | |

### Return type

[**Bookmark**](Bookmark.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Bookmark deleted successfully |  -  |
| **404** | Experiment or bookmark not found |  -  |


## getBookmark

> Bookmark getBookmark(expUUID, bookmarkUUID)

Get a specific bookmark from an experiment

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.BookmarksApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        BookmarksApi apiInstance = new BookmarksApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        UUID bookmarkUUID = UUID.randomUUID(); // UUID | Bookmark UUID
        try {
            Bookmark result = apiInstance.getBookmark(expUUID, bookmarkUUID);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling BookmarksApi#getBookmark");
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
| **bookmarkUUID** | **UUID**| Bookmark UUID | |

### Return type

[**Bookmark**](Bookmark.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Returns the bookmark |  -  |
| **404** | Experiment or bookmark not found |  -  |


## getBookmarks

> List&lt;Bookmark&gt; getBookmarks(expUUID)

Get all bookmarks for an experiment

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.BookmarksApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        BookmarksApi apiInstance = new BookmarksApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        try {
            List<Bookmark> result = apiInstance.getBookmarks(expUUID);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling BookmarksApi#getBookmarks");
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

[**List&lt;Bookmark&gt;**](Bookmark.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Returns the list of bookmarks |  -  |
| **404** | No such experiment |  -  |


## updateBookmark

> Bookmark updateBookmark(expUUID, bookmarkUUID, bookmarkQueryParameters)

Update an existing bookmark in an experiment

### Example

```java
import java.util.UUID;
// Import classes:
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.*;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.BookmarksApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://localhost:8080/tsp/api");

        BookmarksApi apiInstance = new BookmarksApi(defaultClient);
        UUID expUUID = UUID.randomUUID(); // UUID | UUID of the experiment to query
        UUID bookmarkUUID = UUID.randomUUID(); // UUID | Bookmark UUID
        BookmarkQueryParameters bookmarkQueryParameters = new BookmarkQueryParameters(); // BookmarkQueryParameters | 
        try {
            Bookmark result = apiInstance.updateBookmark(expUUID, bookmarkUUID, bookmarkQueryParameters);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling BookmarksApi#updateBookmark");
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
| **bookmarkUUID** | **UUID**| Bookmark UUID | |
| **bookmarkQueryParameters** | [**BookmarkQueryParameters**](BookmarkQueryParameters.md)|  | |

### Return type

[**Bookmark**](Bookmark.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Bookmark updated successfully |  -  |
| **400** | Invalid query parameters |  -  |
| **404** | Experiment or bookmark not found |  -  |

