/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.Activator;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.EventView;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest.ExecutionType;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Trace Event Table Service.
 *
 * @author Loic Prieur-Drevon
 */
@Path("/traces/{uuid}/eventTable")
public class EventTableService {

    /**
     * Query a trace for a list of events to populate a virtual table
     *
     * @param uuid
     *            The queried trace's uuid
     *
     * @param low
     *            rank of the first event to return
     * @param size
     *            total number of events to return
     * @return a {@link Response} encapsulating an error code and message, or the
     *         trace model objects and the table of queried events
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getEvents(@PathParam("uuid") @NotNull UUID uuid,
            @QueryParam("low") @Min(0) long low,
            @QueryParam("size") @Min(0) int size) {
        ITmfTrace trace = TraceManagerService.getTraceByUUID(uuid);
        if (trace == null) {
            return Response.status(Status.NOT_FOUND).entity("No trace with uuid: " + uuid).build(); //$NON-NLS-1$
        }
        try {
            List<List<String>> events = query(trace, low, size);
            EventView view = new EventView(trace, low, size, events);
            return Response.ok().entity(view).build();
        } catch (InterruptedException e) {
            Activator.getInstance().logError("Failed to query the trace", e); //$NON-NLS-1$
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    /**
     * Query a trace for a list of events to populate a virtual table
     *
     * @param uuid
     *            The queried trace's {@link UUID}
     * @param low
     *            rank of the first event to return
     * @param size
     *            total number of events to return
     * @param multivaluedMap
     *            map of columns to filters
     * @return a {@link Response} encapsulating an error code and message, or the
     *         trace objects and the table of queried events
     */
    @PUT
    @Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getFilteredEvents(@PathParam("uuid") @NotNull UUID uuid,
            @QueryParam("low") @Min(0) long low,
            @QueryParam("size") @Min(0) int size,
            MultivaluedMap<String, String> multivaluedMap) {

        if (multivaluedMap == null) {
            return Response.status(Status.BAD_REQUEST).entity("bad filter (null)").build(); //$NON-NLS-1$
        }
        ITmfTrace trace = TraceManagerService.getTraceByUUID(uuid);
        if (trace == null) {
            return Response.status(Status.NOT_FOUND).entity("No trace with uuid: " + uuid).build(); //$NON-NLS-1$
        }
        try {
            Pair<List<List<String>>, Integer> events = filteredQuery(trace, low, size, multivaluedMap);
            EventView view = new EventView(trace, low, size, multivaluedMap, events.getFirst(), events.getSecond());
            return Response.ok().entity(view).build();
        } catch (InterruptedException e) {
            Activator.getInstance().logError("Failed to query the trace", e); //$NON-NLS-1$
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    /**
     * Query the backing trace
     *
     * @param trace
     *            the trace to query on
     * @param low
     *            rank of the first event to return
     * @param size
     *            total number of events to return
     * @return a list of events, where each is represented by a list of its column
     *         values
     * @throws InterruptedException
     *             if the request was cancelled
     */
    private static List<List<String>> query(ITmfTrace trace, long low, int size) throws InterruptedException {
        List<List<String>> events = new ArrayList<>(size);
        List<@NonNull ITmfEventAspect<?>> eventAspects = Lists.newArrayList(trace.getEventAspects());
        TmfEventRequest request = new TmfEventRequest(ITmfEvent.class, TmfTimeRange.ETERNITY, low, size, ExecutionType.FOREGROUND) {
            @Override
            public void handleData(ITmfEvent event) {
                events.add(Lists.transform(eventAspects, a -> String.valueOf(a.resolve(event))));
            }
        };

        trace.sendRequest(request);
        request.waitForCompletion();
        return events;
    }

    /**
     * Query the backing trace
     *
     * @param trace
     *            the trace to query on
     * @param low
     *            index of the lowest event in the filtered event list
     * @param size
     *            number of filtered events to return
     * @param multivaluedMap
     *            HTTP query form from which to extract filters
     * @return a list of events, where each is represented by a list of its column
     *         values
     * @throws InterruptedException
     *             if the request was cancelled
     */
    public Pair<List<List<String>>, Integer> filteredQuery(ITmfTrace trace, long low, int size, MultivaluedMap<String, String> multivaluedMap) throws InterruptedException {
        List<List<String>> events = new ArrayList<>(size);
        List<ITmfEventAspect<?>> eventAspects = Lists.newArrayList(trace.getEventAspects());
        List<Predicate<String>> predicates = Lists.transform(eventAspects, c -> compileReqexes(multivaluedMap.get(c.getName())));
        TmfEventRequest request = new TmfEventRequest(ITmfEvent.class, TmfTimeRange.ETERNITY, 0, ITmfEventRequest.ALL_DATA, ExecutionType.FOREGROUND) {
            /**
             * Override number of read events with number of events that match filter
             */
            int nbRead = 0;

            @Override
            public void handleData(ITmfEvent event) {

                List<String> buildLine = buildLine(event, eventAspects, predicates);
                if (buildLine != null) {
                    nbRead++;
                    if (nbRead >= low && nbRead <= low + size) {
                        events.add(buildLine);
                    }
                }
            }

            @Override
            public synchronized int getNbRead() {
                return nbRead;
            }
        };
        trace.sendRequest(request);
        request.waitForCompletion();
        return new Pair<>(events, request.getNbRead());
    }

    private static Predicate<String> compileReqexes(List<String> regexes) {
        if (regexes == null || regexes.isEmpty()) {
            return t -> true;
        }
        List<Pattern> list = new ArrayList<>();
        for (String regex : regexes) {
            try {
                list.add(Pattern.compile(regex));
            } catch (PatternSyntaxException e) {
            }
        }
        return s -> Iterables.all(list, p -> p.matcher(s).find());
    }

    private static @Nullable List<String> buildLine(@NonNull ITmfEvent event, List<ITmfEventAspect<?>> aspects, List<Predicate<String>> predicates) {
        List<String> line = new ArrayList<>(aspects.size());
        int i = 0;
        for (ITmfEventAspect<?> aspect : aspects) {
            String text = String.valueOf(aspect.resolve(event));
            Predicate<String> predicate = predicates.get(i++);
            if (predicate.test(text)) {
                line.add(text);
            } else {
                // return null if one of the regular expressions does not match
                return null;
            }
        }
        return line;
    }

}