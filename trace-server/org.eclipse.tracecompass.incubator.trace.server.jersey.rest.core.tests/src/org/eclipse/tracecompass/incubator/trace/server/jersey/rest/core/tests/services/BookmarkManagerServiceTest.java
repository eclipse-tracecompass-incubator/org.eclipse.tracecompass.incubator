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

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.QueryParameters;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.BookmarkModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.ExperimentModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
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
    private static final @NonNull BookmarkModelStub BOOKMARK = new BookmarkModelStub(BOOKMARK_NAME, START_TIME, END_TIME);
    private ExperimentModelStub experiment;

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
    private static final String SUCCESSFUL_BOOKMARK_CREATION = "Bookmark creation should succeed";
    private static final String SUCCESSFUL_STATUS_CODE = "Response status should be 200";

    /**
     * Setup method to run before each test
     */
    @Before
    public void setUp() {
        // Create the experiment first
        experiment = assertPostExperiment(CONTEXT_SWITCHES_UST_NOT_INITIALIZED_STUB.getName(),
                CONTEXT_SWITCHES_UST_NOT_INITIALIZED_STUB);
        assertNotNull("Experiment should not be null", experiment);
        assertNotNull(NON_NULL_UUID, experiment.getUUID());
    }

    /**
     * Tear down method to run after each test
     */
    @After
    public void tearDown() {
        // Remove all bookmarks
        WebTarget application = getApplicationEndpoint();
        WebTarget bookmarkTarget = application.path(EXPERIMENTS)
                .path(experiment.getUUID().toString())
                .path(BOOKMARKS);

        try (Response response = bookmarkTarget.request().get()) {
            assertEquals("GET request for bookmarks should return 200", 200, response.getStatus());

            BookmarkModelStub[] existingBookmarks = response.readEntity(BookmarkModelStub[].class);
            assertNotNull("Bookmark array should not be null", existingBookmarks);

            for (BookmarkModelStub bookmark : existingBookmarks) {
                try (Response deleteResponse = bookmarkTarget.path(bookmark.getUUID().toString())
                        .request()
                        .delete()) {
                    assertEquals("DELETE request should return 200", 200, deleteResponse.getStatus());
                }
            }
        }
    }

    /**
     * Test the bookmark endpoints with invalid experiment UUID (i.e.,
     * non-existent).
     */
    @Test
    public void testBookmarkEndpointsInvalidExperiment() {
        WebTarget application = getApplicationEndpoint();
        WebTarget bookmarkTarget = application.path(EXPERIMENTS)
                .path(UUID.randomUUID().toString())
                .path(BOOKMARKS);

        // Test getting all bookmarks
        try (Response response = bookmarkTarget.request().get()) {
            assertEquals(NON_EXISTENT_EXPERIMENT_STATUS_CODE, 404, response.getStatus());
        }

        // Test getting a specific bookmark
        try (Response response = bookmarkTarget.path(BOOKMARK.getUUID().toString()).request().get()) {
            assertEquals(NON_EXISTENT_EXPERIMENT_STATUS_CODE, 404, response.getStatus());
        }

        // Test creating a bookmark
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(NAME, BOOKMARK_NAME);
        parameters.put(START, START_TIME);
        parameters.put(END, END_TIME);
        try (Response response = bookmarkTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            assertEquals(NON_EXISTENT_EXPERIMENT_STATUS_CODE, 404, response.getStatus());
        }

        // Test updating a bookmark
        try (Response response = bookmarkTarget.path(BOOKMARK.getUUID().toString()).request().put(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            assertEquals(NON_EXISTENT_EXPERIMENT_STATUS_CODE, 404, response.getStatus());
        }

        // Test deleting a bookmark
        try (Response response = bookmarkTarget.path(BOOKMARK.getUUID().toString()).request().delete()) {
            assertEquals(NON_EXISTENT_EXPERIMENT_STATUS_CODE, 404, response.getStatus());
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
     */
    @Test
    public void testCreateBookmark() {
        WebTarget application = getApplicationEndpoint();
        WebTarget bookmarkTarget = application.path(EXPERIMENTS)
                .path(experiment.getUUID().toString())
                .path(BOOKMARKS);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(NAME, BOOKMARK.getName());
        parameters.put(START, START_TIME);
        parameters.put(END, END_TIME);

        try (Response response = bookmarkTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            assertEquals(SUCCESSFUL_STATUS_CODE, 200, response.getStatus());

            BookmarkModelStub expStub = response.readEntity(BookmarkModelStub.class);
            assertNotNull(NON_NULL_RESPONSE_BODY, expStub);
            assertEquals(BOOKMARK_NAME_MATCH, BOOKMARK.getName(), expStub.getName());
            assertEquals(BOOKMARK_START_TIME_MATCH, BOOKMARK.getStart(), expStub.getStart());
            assertEquals(BOOKMARK_END_TIME_MATCH, BOOKMARK.getEnd(), expStub.getEnd());
            assertNotNull(NON_NULL_UUID, expStub.getUUID());
        }
    }

    /**
     * Test the creation of a bookmark with no end time (i.e., just start time).
     */
    @Test
    public void testCreateBookmarkNoEndTime() {
        WebTarget application = getApplicationEndpoint();
        WebTarget bookmarkTarget = application.path(EXPERIMENTS)
                .path(experiment.getUUID().toString())
                .path(BOOKMARKS);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(NAME, BOOKMARK.getName());
        parameters.put(START, START_TIME);
        parameters.put(END, START_TIME);

        try (Response response = bookmarkTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            assertEquals(SUCCESSFUL_STATUS_CODE, 200, response.getStatus());

            BookmarkModelStub expStub = response.readEntity(BookmarkModelStub.class);
            assertNotNull(NON_NULL_RESPONSE_BODY, expStub);
            assertEquals(BOOKMARK_NAME_MATCH, BOOKMARK.getName(), expStub.getName());
            assertEquals(BOOKMARK_START_TIME_MATCH, BOOKMARK.getStart(), expStub.getStart());
            assertEquals(BOOKMARK_END_TIME_MATCH, BOOKMARK.getStart(), expStub.getEnd());
            assertNotNull(NON_NULL_UUID, expStub.getUUID());
        }
    }

    /**
     * Test the creation of a bookmark with a repetitive data.
     */
    @Test
    public void testCreateIdenticalBookmarks() {
        WebTarget application = getApplicationEndpoint();
        WebTarget bookmarkTarget = application.path(EXPERIMENTS)
                .path(experiment.getUUID().toString())
                .path(BOOKMARKS);

        Set<UUID> uuids = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put(NAME, BOOKMARK.getName());
            parameters.put(START, START_TIME);
            parameters.put(END, END_TIME);

            try (Response response = bookmarkTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
                assertEquals(SUCCESSFUL_STATUS_CODE, 200, response.getStatus());

                BookmarkModelStub expStub = response.readEntity(BookmarkModelStub.class);
                assertNotNull(NON_NULL_RESPONSE_BODY, expStub);
                assertEquals(BOOKMARK_NAME_MATCH, BOOKMARK.getName(), expStub.getName());
                assertEquals(BOOKMARK_START_TIME_MATCH, BOOKMARK.getStart(), expStub.getStart());
                assertEquals(BOOKMARK_END_TIME_MATCH, BOOKMARK.getEnd(), expStub.getEnd());
                assertNotNull(NON_NULL_UUID, expStub.getUUID());

                // Check if the UUID is unique
                assertFalse("UUID should be unique", uuids.contains(expStub.getUUID()));
                uuids.add(expStub.getUUID());
            }
        }

    }

    /**
     * Test the fetching of all bookmarks.
     */
    @Test
    public void testGetAllBookmarks() {
        WebTarget application = getApplicationEndpoint();
        WebTarget bookmarkTarget = application.path(EXPERIMENTS)
                .path(experiment.getUUID().toString())
                .path(BOOKMARKS);

        // Initially there should be no bookmarks
        try (Response response = bookmarkTarget.request().get()) {
            BookmarkModelStub[] initialBookmarks = response.readEntity(BookmarkModelStub[].class);
            assertEquals("Should start with no bookmarks", 0, initialBookmarks.length);
        }

        // Create multiple bookmarks
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(START, START_TIME);
        parameters.put(END, END_TIME);

        // Create first bookmark
        parameters.put(NAME, "Bookmark1");
        try (Response response = bookmarkTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            assertEquals("First bookmark creation should succeed", 200, response.getStatus());
        }

        // Create second bookmark
        parameters.put(NAME, "Bookmark2");
        try (Response response = bookmarkTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            assertEquals("Second bookmark creation should succeed", 200, response.getStatus());
        }

        // Get all bookmarks
        try (Response response = bookmarkTarget.request().get()) {
            BookmarkModelStub[] allBookmarks = response.readEntity(BookmarkModelStub[].class);
            assertEquals("Should have 2 bookmarks", 2, allBookmarks.length);

            // Verify bookmark properties
            for (BookmarkModelStub bookmark : allBookmarks) {
                assertNotNull("Bookmark should not be null", bookmark);
                assertNotNull("Bookmark UUID should not be null", bookmark.getUUID());
                assertEquals(BOOKMARK_START_TIME_MATCH, START_TIME, bookmark.getStart());
                assertEquals(BOOKMARK_END_TIME_MATCH, END_TIME, bookmark.getEnd());
                assertTrue("Name should be either Bookmark1 or Bookmark2",
                        bookmark.getName().equals("Bookmark1") || bookmark.getName().equals("Bookmark2"));
            }
        }
    }

    /**
     * Test the fetching of a specific bookmark.
     */
    @Test
    public void testGetSpecificBookmark() {
        WebTarget application = getApplicationEndpoint();
        WebTarget bookmarkTarget = application.path(EXPERIMENTS)
                .path(experiment.getUUID().toString())
                .path(BOOKMARKS);

        // Create a bookmark
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(NAME, BOOKMARK.getName());
        parameters.put(START, START_TIME);
        parameters.put(END, END_TIME);

        BookmarkModelStub createdBookmark = null;
        try (Response response = bookmarkTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            assertEquals(SUCCESSFUL_BOOKMARK_CREATION, 200, response.getStatus());
            createdBookmark = response.readEntity(BookmarkModelStub.class);
            assertNotNull(NON_NULL_BOOKMARK, createdBookmark);
        }

        // Test getting non-existent bookmark
        try (Response nonExistentResponse = bookmarkTarget.path(experiment.getUUID().toString()).request().get()) {
            assertEquals(NON_EXISTENT_BOOKMARK_STATUS_CODE, 404, nonExistentResponse.getStatus());
        }

        // Test getting existing bookmark
        try (Response response = bookmarkTarget.path(createdBookmark.getUUID().toString()).request().get()) {
            assertEquals("Should successfully get existing bookmark", 200, response.getStatus());

            BookmarkModelStub retrievedBookmark = response.readEntity(BookmarkModelStub.class);
            assertEquals("Retrieved bookmark should match created bookmark", createdBookmark, retrievedBookmark);
        }
    }

    /**
     * Test updating a bookmark.
     */
    @Test
    public void testUpdateBookmark() {
        WebTarget application = getApplicationEndpoint();
        WebTarget bookmarkTarget = application.path(EXPERIMENTS)
                .path(experiment.getUUID().toString())
                .path(BOOKMARKS);

        // Create initial bookmark
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(NAME, BOOKMARK.getName());
        parameters.put(START, START_TIME);
        parameters.put(END, END_TIME);

        BookmarkModelStub originalBookmark = null;
        try (Response response = bookmarkTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            originalBookmark = response.readEntity(BookmarkModelStub.class);
            assertEquals(SUCCESSFUL_BOOKMARK_CREATION, 200, response.getStatus());
            assertNotNull(NON_NULL_BOOKMARK, originalBookmark);
        }

        // Test updating with invalid parameters
        parameters.put(START, END_TIME);
        parameters.put(END, START_TIME);
        try (Response invalidResponse = bookmarkTarget.path(originalBookmark.getUUID().toString()).request().put(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            assertEquals("Should return 400 for invalid parameters", 400, invalidResponse.getStatus());
        }

        // Test successful update
        parameters.put("name", "Updated Name");
        parameters.put(START, START_TIME + 5);
        parameters.put(END, END_TIME + 5);
        try (Response response = bookmarkTarget.path(originalBookmark.getUUID().toString()).request().put(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            assertEquals("Update should succeed", 200, response.getStatus());

            BookmarkModelStub updatedBookmark = response.readEntity(BookmarkModelStub.class);
            assertNotNull(NON_NULL_BOOKMARK, updatedBookmark);
            assertEquals("UUID should be the same", originalBookmark.getUUID(), updatedBookmark.getUUID());
            assertEquals("Name should be updated", "Updated Name", updatedBookmark.getName());
            assertEquals("Start time should be updated", START_TIME + 5, updatedBookmark.getStart());
            assertEquals("End time should be updated", END_TIME + 5, updatedBookmark.getEnd());
        }
    }

    /**
     * Test the deletion of a bookmark with various scenarios.
     */
    @Test
    public void testDeleteBookmark() {
        WebTarget application = getApplicationEndpoint();
        WebTarget bookmarkTarget = application.path(EXPERIMENTS)
                .path(experiment.getUUID().toString())
                .path(BOOKMARKS);

        // Try deleting non-existent bookmark
        try (Response nonExistentResponse = bookmarkTarget.path(experiment.getUUID().toString()).request().delete()) {
            assertEquals(NON_EXISTENT_BOOKMARK_STATUS_CODE, 404, nonExistentResponse.getStatus());
        }

        // Create a bookmark to delete
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(NAME, BOOKMARK.getName());
        parameters.put(START, START_TIME);
        parameters.put(END, END_TIME);

        BookmarkModelStub createdBookmark = null;
        try (Response response = bookmarkTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            createdBookmark = response.readEntity(BookmarkModelStub.class);
            assertEquals(SUCCESSFUL_BOOKMARK_CREATION, 200, response.getStatus());
            assertNotNull(NON_NULL_BOOKMARK, createdBookmark);
        }

        // Delete the bookmark
        try (Response response = bookmarkTarget.path(createdBookmark.getUUID().toString()).request().delete()) {
            assertEquals("Delete should succeed", 200, response.getStatus());
            BookmarkModelStub deletedBookmark = response.readEntity(BookmarkModelStub.class);
            assertEquals("Deleted bookmark should match created bookmark", createdBookmark, deletedBookmark);
        }

        // Verify the bookmark is actually deleted
        try (Response getResponse = bookmarkTarget.path(createdBookmark.getUUID().toString()).request().get()) {
            assertEquals("Should return 404 for deleted bookmark", 404, getResponse.getStatus());
        }

        // Verify it's not in the list of all bookmarks
        try (Response getAllResponse = bookmarkTarget.request().get()) {
            BookmarkModelStub[] allBookmarks = getAllResponse.readEntity(BookmarkModelStub[].class);
            for (BookmarkModelStub bookmark : allBookmarks) {
                assertNotEquals("Deleted bookmark should not be in list of all bookmarks", createdBookmark.getUUID(), bookmark.getUUID());
            }
        }
    }
}