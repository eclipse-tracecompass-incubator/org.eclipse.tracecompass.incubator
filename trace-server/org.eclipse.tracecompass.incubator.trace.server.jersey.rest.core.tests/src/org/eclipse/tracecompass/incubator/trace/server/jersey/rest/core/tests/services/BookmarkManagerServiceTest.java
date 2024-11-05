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
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.QueryParameters;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.BookmarkModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.ExperimentModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for BookmarkManagerService
 *
 * @author Kaveh Shahedi
 * @since 10.1
 */
public class BookmarkManagerServiceTest extends RestServerTest {

    private static final String BOOKMARK_NAME = "TEST";
    private static final long START_TIME = 0L;
    private static final long END_TIME = 10L;
    private static final @NonNull BookmarkModelStub BOOKMARK = new BookmarkModelStub(BOOKMARK_NAME, START_TIME, END_TIME);
    private ExperimentModelStub experiment;

    /**
     * Setup method to run before each test. Creates a clean experiment and removes all
     * existing bookmarks.
     */
    @Before
    public void setUp() {
        // Create the experiment first
        experiment = assertPostExperiment(CONTEXT_SWITCHES_UST_NOT_INITIALIZED_STUB.getName(),
                                       CONTEXT_SWITCHES_UST_NOT_INITIALIZED_STUB);
        assertNotNull("Experiment should not be null", experiment);
        assertNotNull("Experiment UUID should not be null", experiment.getUUID());

        // Get all existing bookmarks and delete them
        WebTarget application = getApplicationEndpoint();
        WebTarget bookmarkTarget = application.path(EXPERIMENTS)
                .path(experiment.getUUID().toString())
                .path(BOOKMARKS);

        Response response = bookmarkTarget.request().get();
        assertEquals("GET request for bookmarks should return 200", 200, response.getStatus());

        if (response.getStatus() == 200) {
            BookmarkModelStub[] existingBookmarks = response.readEntity(BookmarkModelStub[].class);
            assertNotNull("Bookmark array should not be null", existingBookmarks);

            for (BookmarkModelStub bookmark : existingBookmarks) {
                Response deleteResponse = bookmarkTarget.path(bookmark.getUUID().toString())
                                                      .request()
                                                      .delete();
                assertEquals("DELETE request should return 200", 200, deleteResponse.getStatus());
            }
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
        parameters.put("start", START_TIME);
        parameters.put("end", END_TIME);

        Response response = bookmarkTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
        assertEquals("Should return 400 for null name", 400, response.getStatus());

        // Test with non-numeric start and end times
        parameters.put(NAME, BOOKMARK_NAME);
        parameters.put("start", "not a number");
        parameters.put("end", "not a number");

        response = bookmarkTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
        assertEquals("Should return 400 for non-numeric times", 400, response.getStatus());

        // Test with end time before start time
        parameters.put(NAME, BOOKMARK_NAME);
        parameters.put("start", END_TIME);
        parameters.put("end", START_TIME);

        response = bookmarkTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
        assertEquals("Should return 400 for invalid time range", 400, response.getStatus());
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
        parameters.put("start", START_TIME);
        parameters.put("end", END_TIME);

        Response response = bookmarkTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
        assertEquals("Response status should be 200", 200, response.getStatus());

        BookmarkModelStub expStub = response.readEntity(BookmarkModelStub.class);
        assertNotNull("Response body should not be null", expStub);
        assertEquals("Bookmark name should match", BOOKMARK.getName(), expStub.getName());
        assertEquals("Start time should match", BOOKMARK.getStart(), expStub.getStart());
        assertEquals("End time should match", BOOKMARK.getEnd(), expStub.getEnd());
        assertNotNull("UUID should not be null", expStub.getUUID());
    }

    /**
     * Test the creation of a bookmark with a repetitive name.
     */
    @Test
    public void testCreateBookmarkRepetitiveName() {
        WebTarget application = getApplicationEndpoint();
        WebTarget bookmarkTarget = application.path(EXPERIMENTS)
                .path(experiment.getUUID().toString())
                .path(BOOKMARKS);

        // Create first bookmark
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(NAME, BOOKMARK.getName());
        parameters.put("start", START_TIME);
        parameters.put("end", END_TIME);

        Response response = bookmarkTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
        BookmarkModelStub firstBookmark = response.readEntity(BookmarkModelStub.class);
        assertEquals("First bookmark creation should succeed", 200, response.getStatus());
        assertNotNull("First bookmark should not be null", firstBookmark);

        // Try to create second bookmark with same name but different times
        parameters.replace("start", START_TIME + 1);
        parameters.replace("end", END_TIME + 1);

        response = bookmarkTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
        assertEquals("Should return conflict for duplicate name", 409, response.getStatus());

        // Verify the original bookmark wasn't modified
        Response getResponse = bookmarkTarget.path(firstBookmark.getUUID().toString()).request().get();
        BookmarkModelStub retrievedBookmark = getResponse.readEntity(BookmarkModelStub.class);
        assertEquals("Original bookmark should remain unchanged", firstBookmark, retrievedBookmark);
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
        Response response = bookmarkTarget.request().get();
        BookmarkModelStub[] initialBookmarks = response.readEntity(BookmarkModelStub[].class);
        assertEquals("Should start with no bookmarks", 0, initialBookmarks.length);

        // Create multiple bookmarks
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("start", START_TIME);
        parameters.put("end", END_TIME);

        // Create first bookmark
        parameters.put(NAME, "Bookmark1");
        response = bookmarkTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
        assertEquals("First bookmark creation should succeed", 200, response.getStatus());

        // Create second bookmark
        parameters.put(NAME, "Bookmark2");
        response = bookmarkTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
        assertEquals("Second bookmark creation should succeed", 200, response.getStatus());

        // Get all bookmarks
        response = bookmarkTarget.request().get();
        BookmarkModelStub[] allBookmarks = response.readEntity(BookmarkModelStub[].class);
        assertEquals("Should have 2 bookmarks", 2, allBookmarks.length);

        // Verify bookmark properties
        for (BookmarkModelStub bookmark : allBookmarks) {
            assertNotNull("Bookmark should not be null", bookmark);
            assertNotNull("Bookmark UUID should not be null", bookmark.getUUID());
            assertEquals("Start time should match", START_TIME, bookmark.getStart());
            assertEquals("End time should match", END_TIME, bookmark.getEnd());
            assertTrue("Name should be either Bookmark1 or Bookmark2",
                      bookmark.getName().equals("Bookmark1") || bookmark.getName().equals("Bookmark2"));
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
        parameters.put("start", START_TIME);
        parameters.put("end", END_TIME);

        Response response = bookmarkTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
        assertEquals("Bookmark creation should succeed", 200, response.getStatus());
        BookmarkModelStub createdBookmark = response.readEntity(BookmarkModelStub.class);

        // Test getting non-existent bookmark
        Response nonExistentResponse = bookmarkTarget.path("non-existent-uuid").request().get();
        assertEquals("Should return 404 for non-existent bookmark", 404, nonExistentResponse.getStatus());

        // Test getting existing bookmark
        response = bookmarkTarget.path(createdBookmark.getUUID().toString()).request().get();
        assertEquals("Should successfully get existing bookmark", 200, response.getStatus());

        BookmarkModelStub retrievedBookmark = response.readEntity(BookmarkModelStub.class);
        assertEquals("Retrieved bookmark should match created bookmark", createdBookmark, retrievedBookmark);
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
        parameters.put("start", START_TIME);
        parameters.put("end", END_TIME);

        Response response = bookmarkTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
        BookmarkModelStub originalBookmark = response.readEntity(BookmarkModelStub.class);
        assertEquals("Initial bookmark creation should succeed", 200, response.getStatus());

        // Test updating non-existent bookmark
        WebTarget nonExistentTarget = bookmarkTarget.path("non-existent-uuid");
        Response nonExistentResponse = nonExistentTarget.request()
                .put(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
        assertEquals("Should return 404 for non-existent bookmark", 404, nonExistentResponse.getStatus());

        // Test updating with invalid parameters
        parameters.put("start", END_TIME);
        parameters.put("end", START_TIME);
        Response invalidResponse = bookmarkTarget.path(originalBookmark.getUUID().toString())
                .request()
                .put(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
        assertEquals("Should return 400 for invalid parameters", 400, invalidResponse.getStatus());

        // Test successful update
        parameters.put("name", "Updated Name");
        parameters.put("start", START_TIME + 5);
        parameters.put("end", END_TIME + 5);

        response = bookmarkTarget.path(originalBookmark.getUUID().toString())
                .request()
                .put(Entity.json(new QueryParameters(parameters, Collections.emptyList())));

        assertEquals("Update should succeed", 200, response.getStatus());
        BookmarkModelStub updatedBookmark = response.readEntity(BookmarkModelStub.class);

        assertNotNull("Updated bookmark should not be null", updatedBookmark);
        assertEquals("UUID should remain the same", originalBookmark.getUUID(), updatedBookmark.getUUID());
        assertEquals("Name should be updated", "Updated Name", updatedBookmark.getName());
        assertEquals("Start time should be updated", START_TIME + 5, updatedBookmark.getStart());
        assertEquals("End time should be updated", END_TIME + 5, updatedBookmark.getEnd());
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
        Response nonExistentResponse = bookmarkTarget.path("non-existent-uuid")
                .request()
                .delete();
        assertEquals("Should return 404 for non-existent bookmark", 404, nonExistentResponse.getStatus());

        // Create a bookmark to delete
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(NAME, BOOKMARK.getName());
        parameters.put("start", START_TIME);
        parameters.put("end", END_TIME);

        Response response = bookmarkTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
        BookmarkModelStub createdBookmark = response.readEntity(BookmarkModelStub.class);
        assertEquals("Bookmark creation should succeed", 200, response.getStatus());

        // Delete the bookmark
        response = bookmarkTarget.path(createdBookmark.getUUID().toString())
                .request()
                .delete();
        assertEquals("Delete should succeed", 200, response.getStatus());
        BookmarkModelStub deletedBookmark = response.readEntity(BookmarkModelStub.class);
        assertEquals("Deleted bookmark should match created bookmark", createdBookmark, deletedBookmark);

        // Verify the bookmark is actually deleted
        Response getResponse = bookmarkTarget.path(createdBookmark.getUUID().toString())
                .request()
                .get();
        assertEquals("Should return 404 for deleted bookmark", 404, getResponse.getStatus());

        // Verify it's not in the list of all bookmarks
        Response getAllResponse = bookmarkTarget.request().get();
        BookmarkModelStub[] allBookmarks = getAllResponse.readEntity(BookmarkModelStub[].class);
        assertEquals("Should have no bookmarks after deletion", 0, allBookmarks.length);
    }
}