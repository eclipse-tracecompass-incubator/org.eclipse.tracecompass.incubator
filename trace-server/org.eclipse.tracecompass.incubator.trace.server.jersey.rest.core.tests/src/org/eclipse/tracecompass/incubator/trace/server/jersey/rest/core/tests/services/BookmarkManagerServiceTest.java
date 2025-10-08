/*******************************************************************************
 * Copyright (c) 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.QueryParameters;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.BookmarksApi;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Bookmark;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.BookmarkParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.BookmarkQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ErrorResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Experiment;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for BookmarkManagerService
 *
 * @author Kaveh Shahedi
 */
@SuppressWarnings("null")
public class BookmarkManagerServiceTest extends RestServerTest {

    private static final String BOOKMARK_NAME = "TEST";
    private static final long START_TIME = 0L;
    private static final long END_TIME = 10L;
    private static final @NonNull Bookmark BOOKMARK = new Bookmark().name(BOOKMARK_NAME).start(START_TIME).end(END_TIME).uuid(UUID.randomUUID());
    private Experiment experiment;

    private static final String START = "start";
    private static final String END = "end";

    private static final String BOOKMARK_END_TIME_MATCH = "End time should match";
    private static final String BOOKMARK_NAME_MATCH = "Bookmark name should match";
    private static final String BOOKMARK_START_TIME_MATCH = "Start time should match";
    private static final String NON_EXISTENT_BOOKMARK_STATUS_CODE = "Should return 404 for non-existent bookmark";
    private static final String NON_EXISTENT_EXPERIMENT_STATUS_CODE = "Should return 404 for non-existent experiment";
    private static final String NON_NULL_BOOKMARK = "Created bookmark should not be null";
    private static final String NON_NULL_RESPONSE_BODY = "Response body should not be null";
    private static final String NON_NULL_UUID = "UUID should not be null";
    private static final String NON_NUMERIC_TIMES_STATUS_CODE = "Should return 400 for non-numeric times";

    private static final BookmarksApi sfBookmarksApi = new BookmarksApi(sfApiClient);

    /**
     * Setup method to run before each test
     */
    @Before
    public void setUp() {
        // Create the experiment first
        experiment = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(),
                sfContextSwitchesUstNotInitializedStub);
        assertNotNull("Experiment should not be null", experiment);
        assertNotNull(NON_NULL_UUID, experiment.getUUID());
    }

    /**
     * Tear down method to run after each test
     *
     * @throws ApiException
     *             if such exception occurs
     */
    @After
    public void tearDown() throws ApiException {
        // Remove all bookmarks
        List<Bookmark> existingBookmarks = sfBookmarksApi.getBookmarks(experiment.getUUID());
        for (Bookmark bookmark : existingBookmarks) {
            sfBookmarksApi.deleteBookmark(experiment.getUUID(), bookmark.getUuid());
        }
    }

    /**
     * Test the bookmark endpoints with invalid experiment UUID (i.e.,
     * non-existent).
     */
    @Test
    public void testBookmarkEndpointsInvalidExperiment() {

        // Test getting all bookmarks
        try {
            sfBookmarksApi.getBookmarks(UUID.randomUUID());
        } catch (ApiException ex) {
            assertEquals(NON_EXISTENT_EXPERIMENT_STATUS_CODE, Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals("No such experiment", errorResponse.getTitle());
        }

        // Test getting a specific bookmark
        try {
            sfBookmarksApi.getBookmark(UUID.randomUUID(), BOOKMARK.getUuid());
        } catch (ApiException ex) {
            assertEquals(NON_EXISTENT_EXPERIMENT_STATUS_CODE, Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals("No such experiment", errorResponse.getTitle());
        }

        // Test creating a bookmark
        BookmarkParameters params = new BookmarkParameters()
                .name(BOOKMARK_NAME)
                .start(START_TIME)
                .end(END_TIME);
        BookmarkQueryParameters queryParams = new BookmarkQueryParameters().parameters(params);

        try {
            sfBookmarksApi.createBookmark(UUID.randomUUID(), queryParams);
        } catch (ApiException ex) {
            assertEquals(NON_EXISTENT_EXPERIMENT_STATUS_CODE, Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals("No such experiment", errorResponse.getTitle());
        }

        // Test updating a bookmark
        try {
            sfBookmarksApi.updateBookmark(UUID.randomUUID(), BOOKMARK.getUuid(), queryParams);
        } catch (ApiException ex) {
            assertEquals(NON_EXISTENT_EXPERIMENT_STATUS_CODE, Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals("No such experiment", errorResponse.getTitle());
        }

        // Test deleting a bookmark
        try {
            sfBookmarksApi.deleteBookmark(UUID.randomUUID(), BOOKMARK.getUuid());
        } catch (ApiException ex) {
            assertEquals(NON_EXISTENT_EXPERIMENT_STATUS_CODE, Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals("No such experiment", errorResponse.getTitle());
        }
    }

    /**
     * Test the creation of a bookmark with invalid parameters.
     */
    @Test
    public void testCreateBookmarkInvalidParams() {
        WebTarget application = getApplicationEndpoint();
        WebTarget bookmarkTarget = application.path(EXPERIMENTS)
                .path(experiment.getUUID().toString())
                .path(BOOKMARKS);

        // Test with null name
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(NAME, null);
        parameters.put(START, START_TIME);
        parameters.put(END, END_TIME);
        try (Response response = bookmarkTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            assertEquals("Should return 400 for null name", 400, response.getStatus());

        }

        // Test with non-numeric start
        parameters.put(NAME, BOOKMARK_NAME);
        parameters.put(START, "not a number");
        try (Response response = bookmarkTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            assertEquals(NON_NUMERIC_TIMES_STATUS_CODE, 400, response.getStatus());
        }

        // Test with non-numeric end
        parameters.put(START, START_TIME);
        parameters.put(END, "not a number");
        try (Response response = bookmarkTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            assertEquals(NON_NUMERIC_TIMES_STATUS_CODE, 400, response.getStatus());
        }

        // Test with end time before start time
        parameters.put(NAME, BOOKMARK_NAME);
        parameters.put(START, END_TIME);
        parameters.put(END, START_TIME);
        try (Response response = bookmarkTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            assertEquals("Should return 400 for invalid time range", 400, response.getStatus());
        }

    }

    /**
     * Test the creation of a bookmark.
     *
     * @throws ApiException
     *             if such exception occurs
     */
    @Test
    public void testCreateBookmark() throws ApiException {
        BookmarkParameters params = new BookmarkParameters()
                .name(BOOKMARK_NAME)
                .start(START_TIME)
                .end(END_TIME);
        BookmarkQueryParameters queryParams = new BookmarkQueryParameters().parameters(params);

        Bookmark expStub = sfBookmarksApi.createBookmark(experiment.getUUID(), queryParams);
        assertNotNull(NON_NULL_RESPONSE_BODY, expStub);
        assertEquals(BOOKMARK_NAME_MATCH, BOOKMARK.getName(), expStub.getName());
        assertEquals(BOOKMARK_START_TIME_MATCH, BOOKMARK.getStart(), expStub.getStart());
        assertEquals(BOOKMARK_END_TIME_MATCH, BOOKMARK.getEnd(), expStub.getEnd());
        assertNotNull(NON_NULL_UUID, expStub.getUuid());
    }

    /**
     * Test the creation of a bookmark with no end time (i.e., just start time).
     *
     * @throws ApiException
     *             if such exception occurs
     */
    @Test
    public void testCreateBookmarkNoEndTime() throws ApiException {
        BookmarkParameters params = new BookmarkParameters()
                .name(BOOKMARK_NAME)
                .start(START_TIME)
                .end(START_TIME);
        BookmarkQueryParameters queryParams = new BookmarkQueryParameters().parameters(params);
        Bookmark expStub = sfBookmarksApi.createBookmark(experiment.getUUID(), queryParams);

        assertNotNull(NON_NULL_RESPONSE_BODY, expStub);
        assertEquals(BOOKMARK_NAME_MATCH, BOOKMARK.getName(), expStub.getName());
        assertEquals(BOOKMARK_START_TIME_MATCH, BOOKMARK.getStart(), expStub.getStart());
        assertEquals(BOOKMARK_END_TIME_MATCH, BOOKMARK.getStart(), expStub.getEnd());
        assertNotNull(NON_NULL_UUID, expStub.getUuid());
    }

    /**
     * Test the creation of a bookmark with a repetitive data.
     *
     * @throws ApiException
     *             if such exception occurs
     */
    @Test
    public void testCreateIdenticalBookmarks() throws ApiException {
        Set<UUID> uuids = new HashSet<>();
        for (int i = 0; i < 3; i++) {

            BookmarkParameters params = new BookmarkParameters()
                    .name(BOOKMARK_NAME)
                    .start(START_TIME)
                    .end(END_TIME);
            BookmarkQueryParameters queryParams = new BookmarkQueryParameters().parameters(params);
            Bookmark expStub = sfBookmarksApi.createBookmark(experiment.getUUID(), queryParams);
            assertNotNull(NON_NULL_RESPONSE_BODY, expStub);
            assertEquals(BOOKMARK_NAME_MATCH, BOOKMARK.getName(), expStub.getName());
            assertEquals(BOOKMARK_START_TIME_MATCH, BOOKMARK.getStart(), expStub.getStart());
            assertEquals(BOOKMARK_END_TIME_MATCH, BOOKMARK.getEnd(), expStub.getEnd());
            assertNotNull(NON_NULL_UUID, expStub.getUuid());

            // Check if the UUID is unique
            assertFalse("UUID should be unique", uuids.contains(expStub.getUuid()));
            uuids.add(expStub.getUuid());
        }

    }

    /**
     * Test the fetching of all bookmarks.
     *
     * @throws ApiException
     *             if such exception occurs
     */
    @Test
    public void testGetAllBookmarks() throws ApiException {
        List<Bookmark> initialBookmarks = sfBookmarksApi.getBookmarks(experiment.getUUID());
        assertEquals("Should start with no bookmarks", 0, initialBookmarks.size());

        // Create multiple bookmarks
        BookmarkParameters params = new BookmarkParameters()
                .start(START_TIME)
                .end(END_TIME);
        // Create first bookmark
        params.name("Bookmark1");
        BookmarkQueryParameters queryParams = new BookmarkQueryParameters().parameters(params);
        Bookmark createdBookmark = sfBookmarksApi.createBookmark(experiment.getUUID(), queryParams);
        assertNotNull(createdBookmark);

        // Create second bookmark
        params.name("Bookmark2");
        queryParams = new BookmarkQueryParameters().parameters(params);
        createdBookmark = sfBookmarksApi.createBookmark(experiment.getUUID(), queryParams);
        assertNotNull(createdBookmark);

        // Get all bookmarks
        List<Bookmark> allBookmarks = sfBookmarksApi.getBookmarks(experiment.getUUID());
        assertEquals("Should have 2 bookmarks", 2, allBookmarks.size());

        // Verify bookmark properties
        for (Bookmark bookmark : allBookmarks) {
            assertNotNull("Bookmark should not be null", bookmark);
            assertNotNull("Bookmark UUID should not be null", bookmark.getUuid());
            assertEquals(BOOKMARK_START_TIME_MATCH, START_TIME, bookmark.getStart().longValue());
            assertEquals(BOOKMARK_END_TIME_MATCH, END_TIME, bookmark.getEnd().longValue());
            assertTrue("Name should be either Bookmark1 or Bookmark2",
                    bookmark.getName().equals("Bookmark1") || bookmark.getName().equals("Bookmark2"));
        }
    }

    /**
     * Test the fetching of a specific bookmark.
     *
     * @throws ApiException
     *             if such exception occurs
     */
    @Test
    public void testGetSpecificBookmark() throws ApiException {
        // Create a bookmark
        BookmarkParameters params = new BookmarkParameters()
                .name(BOOKMARK_NAME)
                .start(START_TIME)
                .end(START_TIME);
        BookmarkQueryParameters queryParams = new BookmarkQueryParameters().parameters(params);
        Bookmark createdBookmark = sfBookmarksApi.createBookmark(experiment.getUUID(), queryParams);
        assertNotNull(NON_NULL_BOOKMARK, createdBookmark);

        // Test getting non-existent bookmark
        try {
            sfBookmarksApi.getBookmark(experiment.getUUID(), UUID.randomUUID());
        } catch (ApiException ex) {
            assertEquals(NON_EXISTENT_BOOKMARK_STATUS_CODE, Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals("Bookmark not found", errorResponse.getTitle());
        }

        // Test getting existing bookmark
        Bookmark retrievedBookmark = sfBookmarksApi.getBookmark(experiment.getUUID(), createdBookmark.getUuid());
        assertEquals("Retrieved bookmark should match created bookmark", createdBookmark, retrievedBookmark);
    }

    /**
     * Test updating a bookmark.
     *
     * @throws ApiException
     *             if such exception occurs
     */
    @Test
    public void testUpdateBookmark() throws ApiException {

        BookmarkParameters params = new BookmarkParameters()
                .name(BOOKMARK_NAME)
                .start(START_TIME)
                .end(END_TIME);
        BookmarkQueryParameters queryParams = new BookmarkQueryParameters().parameters(params);
        Bookmark originalBookmark = sfBookmarksApi.createBookmark(experiment.getUUID(), queryParams);
        assertNotNull(NON_NULL_BOOKMARK, originalBookmark);

        // Test updating with invalid parameters
        params = new BookmarkParameters()
                .name(BOOKMARK_NAME)
                .start(END_TIME)
                .end(START_TIME);
        queryParams = new BookmarkQueryParameters().parameters(params);

        try {
            sfBookmarksApi.updateBookmark(experiment.getUUID(), BOOKMARK.getUuid(), queryParams);
        } catch (ApiException ex) {
            assertEquals("Should return 400 for invalid parameters", Status.BAD_REQUEST.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals("Invalid query parameters: Start time cannot be after end time", errorResponse.getTitle());
        }

        // Test successful update
        params = new BookmarkParameters()
                .name("Updated Name")
                .start(START_TIME + 5)
                .end(END_TIME + 5);
        queryParams = new BookmarkQueryParameters().parameters(params);

        Bookmark updatedBookmark = sfBookmarksApi.updateBookmark(experiment.getUUID(), originalBookmark.getUuid(), queryParams);
        assertNotNull(NON_NULL_BOOKMARK, updatedBookmark);
        assertEquals("UUID should be the same", originalBookmark.getUuid(), updatedBookmark.getUuid());
        assertEquals("Name should be updated", "Updated Name", updatedBookmark.getName());
        assertEquals("Start time should be updated", START_TIME + 5, updatedBookmark.getStart().longValue());
        assertEquals("End time should be updated", END_TIME + 5, updatedBookmark.getEnd().longValue());
    }

    /**
     * Test the deletion of a bookmark with various scenarios.
     *
     * @throws ApiException
     *             if such exception occurs
     */
    @Test
    public void testDeleteBookmark() throws ApiException {

        try {
            sfBookmarksApi.deleteBookmark(experiment.getUUID(), UUID.randomUUID());
        } catch (ApiException ex) {
            assertEquals(NON_EXISTENT_BOOKMARK_STATUS_CODE, Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals("Bookmark not found", errorResponse.getTitle());
        }

        // Create a bookmark to delete
        BookmarkParameters params = new BookmarkParameters()
                .name(BOOKMARK_NAME)
                .start(START_TIME)
                .end(END_TIME);
        BookmarkQueryParameters queryParams = new BookmarkQueryParameters().parameters(params);
        Bookmark createdBookmark = sfBookmarksApi.createBookmark(experiment.getUUID(), queryParams);
        assertNotNull(NON_NULL_BOOKMARK, createdBookmark);

        // Delete the bookmark
        Bookmark deletedBookmark = sfBookmarksApi.deleteBookmark(experiment.getUUID(), createdBookmark.getUuid());
        assertEquals("Deleted bookmark should match created bookmark", createdBookmark, deletedBookmark);

        // Verify the bookmark is actually deleted
        try {
            sfBookmarksApi.getBookmark(UUID.randomUUID(), BOOKMARK.getUuid());
        } catch (ApiException ex) {
            assertEquals(NON_EXISTENT_EXPERIMENT_STATUS_CODE, Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals("No such experiment", errorResponse.getTitle());
        }

        // Verify it's not in the list of all bookmarks
        List<Bookmark> allBookmarks = sfBookmarksApi.getBookmarks(experiment.getUUID());
        for (Bookmark bookmark : allBookmarks) {
            assertNotEquals("Deleted bookmark should not be in list of all bookmarks", createdBookmark.getUuid(), bookmark.getUuid());
        }
    }
}