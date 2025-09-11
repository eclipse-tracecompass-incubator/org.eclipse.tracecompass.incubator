package org.eclipse.tracecompass.incubator.tsp.client.core.api;

import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.Pair;

import javax.ws.rs.core.GenericType;

import org.eclipse.tracecompass.incubator.tsp.client.core.model.Bookmark;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.BookmarkQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ErrorResponse;
import java.util.UUID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2025-09-19T10:33:13.979273368-04:00[America/Toronto]", comments = "Generator version: 7.15.0")
public class BookmarksApi {
  private ApiClient apiClient;

  public BookmarksApi() {
    this(Configuration.getDefaultApiClient());
  }

  public BookmarksApi(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Get the API client
   *
   * @return API client
   */
  public ApiClient getApiClient() {
    return apiClient;
  }

  /**
   * Set the API client
   *
   * @param apiClient an instance of API client
   */
  public void setApiClient(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Create a new bookmark in an experiment
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @param bookmarkQueryParameters  (required)
   * @return Bookmark
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Bookmark created successfully </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such experiment </td><td>  -  </td></tr>
     </table>
   */
  public Bookmark createBookmark(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull BookmarkQueryParameters bookmarkQueryParameters) throws ApiException {
    return createBookmarkWithHttpInfo(expUUID, bookmarkQueryParameters).getData();
  }

  /**
   * Create a new bookmark in an experiment
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @param bookmarkQueryParameters  (required)
   * @return ApiResponse&lt;Bookmark&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Bookmark created successfully </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such experiment </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Bookmark> createBookmarkWithHttpInfo(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull BookmarkQueryParameters bookmarkQueryParameters) throws ApiException {
    // Check required parameters
    if (expUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'expUUID' when calling createBookmark");
    }
    if (bookmarkQueryParameters == null) {
      throw new ApiException(400, "Missing the required parameter 'bookmarkQueryParameters' when calling createBookmark");
    }

    // Path parameters
    String localVarPath = "/experiments/{expUUID}/bookmarks"
            .replaceAll("\\{expUUID}", apiClient.escapeString(expUUID.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<Bookmark> localVarReturnType = new GenericType<Bookmark>() {};
    return apiClient.invokeAPI("BookmarksApi.createBookmark", localVarPath, "POST", new ArrayList<>(), bookmarkQueryParameters,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Delete a bookmark from an experiment
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @param bookmarkUUID Bookmark UUID (required)
   * @return Bookmark
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Bookmark deleted successfully </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or bookmark not found </td><td>  -  </td></tr>
     </table>
   */
  public Bookmark deleteBookmark(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull UUID bookmarkUUID) throws ApiException {
    return deleteBookmarkWithHttpInfo(expUUID, bookmarkUUID).getData();
  }

  /**
   * Delete a bookmark from an experiment
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @param bookmarkUUID Bookmark UUID (required)
   * @return ApiResponse&lt;Bookmark&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Bookmark deleted successfully </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or bookmark not found </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Bookmark> deleteBookmarkWithHttpInfo(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull UUID bookmarkUUID) throws ApiException {
    // Check required parameters
    if (expUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'expUUID' when calling deleteBookmark");
    }
    if (bookmarkUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'bookmarkUUID' when calling deleteBookmark");
    }

    // Path parameters
    String localVarPath = "/experiments/{expUUID}/bookmarks/{bookmarkUUID}"
            .replaceAll("\\{expUUID}", apiClient.escapeString(expUUID.toString()))
            .replaceAll("\\{bookmarkUUID}", apiClient.escapeString(bookmarkUUID.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    GenericType<Bookmark> localVarReturnType = new GenericType<Bookmark>() {};
    return apiClient.invokeAPI("BookmarksApi.deleteBookmark", localVarPath, "DELETE", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Get a specific bookmark from an experiment
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @param bookmarkUUID Bookmark UUID (required)
   * @return Bookmark
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns the bookmark </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or bookmark not found </td><td>  -  </td></tr>
     </table>
   */
  public Bookmark getBookmark(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull UUID bookmarkUUID) throws ApiException {
    return getBookmarkWithHttpInfo(expUUID, bookmarkUUID).getData();
  }

  /**
   * Get a specific bookmark from an experiment
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @param bookmarkUUID Bookmark UUID (required)
   * @return ApiResponse&lt;Bookmark&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns the bookmark </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or bookmark not found </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Bookmark> getBookmarkWithHttpInfo(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull UUID bookmarkUUID) throws ApiException {
    // Check required parameters
    if (expUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'expUUID' when calling getBookmark");
    }
    if (bookmarkUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'bookmarkUUID' when calling getBookmark");
    }

    // Path parameters
    String localVarPath = "/experiments/{expUUID}/bookmarks/{bookmarkUUID}"
            .replaceAll("\\{expUUID}", apiClient.escapeString(expUUID.toString()))
            .replaceAll("\\{bookmarkUUID}", apiClient.escapeString(bookmarkUUID.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    GenericType<Bookmark> localVarReturnType = new GenericType<Bookmark>() {};
    return apiClient.invokeAPI("BookmarksApi.getBookmark", localVarPath, "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Get all bookmarks for an experiment
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @return List&lt;Bookmark&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns the list of bookmarks </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such experiment </td><td>  -  </td></tr>
     </table>
   */
  public List<Bookmark> getBookmarks(@javax.annotation.Nonnull UUID expUUID) throws ApiException {
    return getBookmarksWithHttpInfo(expUUID).getData();
  }

  /**
   * Get all bookmarks for an experiment
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @return ApiResponse&lt;List&lt;Bookmark&gt;&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Returns the list of bookmarks </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> No such experiment </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<List<Bookmark>> getBookmarksWithHttpInfo(@javax.annotation.Nonnull UUID expUUID) throws ApiException {
    // Check required parameters
    if (expUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'expUUID' when calling getBookmarks");
    }

    // Path parameters
    String localVarPath = "/experiments/{expUUID}/bookmarks"
            .replaceAll("\\{expUUID}", apiClient.escapeString(expUUID.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    GenericType<List<Bookmark>> localVarReturnType = new GenericType<List<Bookmark>>() {};
    return apiClient.invokeAPI("BookmarksApi.getBookmarks", localVarPath, "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
  /**
   * Update an existing bookmark in an experiment
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @param bookmarkUUID Bookmark UUID (required)
   * @param bookmarkQueryParameters  (required)
   * @return Bookmark
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Bookmark updated successfully </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or bookmark not found </td><td>  -  </td></tr>
     </table>
   */
  public Bookmark updateBookmark(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull UUID bookmarkUUID, @javax.annotation.Nonnull BookmarkQueryParameters bookmarkQueryParameters) throws ApiException {
    return updateBookmarkWithHttpInfo(expUUID, bookmarkUUID, bookmarkQueryParameters).getData();
  }

  /**
   * Update an existing bookmark in an experiment
   * 
   * @param expUUID UUID of the experiment to query (required)
   * @param bookmarkUUID Bookmark UUID (required)
   * @param bookmarkQueryParameters  (required)
   * @return ApiResponse&lt;Bookmark&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> Bookmark updated successfully </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid query parameters </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Experiment or bookmark not found </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Bookmark> updateBookmarkWithHttpInfo(@javax.annotation.Nonnull UUID expUUID, @javax.annotation.Nonnull UUID bookmarkUUID, @javax.annotation.Nonnull BookmarkQueryParameters bookmarkQueryParameters) throws ApiException {
    // Check required parameters
    if (expUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'expUUID' when calling updateBookmark");
    }
    if (bookmarkUUID == null) {
      throw new ApiException(400, "Missing the required parameter 'bookmarkUUID' when calling updateBookmark");
    }
    if (bookmarkQueryParameters == null) {
      throw new ApiException(400, "Missing the required parameter 'bookmarkQueryParameters' when calling updateBookmark");
    }

    // Path parameters
    String localVarPath = "/experiments/{expUUID}/bookmarks/{bookmarkUUID}"
            .replaceAll("\\{expUUID}", apiClient.escapeString(expUUID.toString()))
            .replaceAll("\\{bookmarkUUID}", apiClient.escapeString(bookmarkUUID.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("application/json");
    GenericType<Bookmark> localVarReturnType = new GenericType<Bookmark>() {};
    return apiClient.invokeAPI("BookmarksApi.updateBookmark", localVarPath, "PUT", new ArrayList<>(), bookmarkQueryParameters,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               null, localVarReturnType, false);
  }
}
