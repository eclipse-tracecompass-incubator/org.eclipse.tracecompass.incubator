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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.inputoutput.Disk;
import org.eclipse.tracecompass.analysis.os.linux.core.inputoutput.InputOutputAnalysisModule;
import org.eclipse.tracecompass.analysis.os.linux.core.inputoutput.InputOutputInformationProvider;
import org.eclipse.tracecompass.analysis.os.linux.core.inputoutput.IoOperationType;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.data.AnalysisManager;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.data.TraceManager;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.analysis.AnalysisModel;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.trace.TraceModel;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.xy.XYView;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Service to query the DisoIO usage View
 *
 * @author Loic Prieur-Drevon
 */
@SuppressWarnings("restriction")
@Path("/DiskActivityView")
public class DiskActivityViewService {
    private static final String DisksIOActivityViewer_Write = " write"; //$NON-NLS-1$
    private static final String DisksIOActivityViewer_Read = " read"; //$NON-NLS-1$
    private static final int BYTES_PER_SECTOR = 512;
    private static final double SECOND_TO_NANOSECOND = Math.pow(10, 9);

    @Context
    TraceManager traceManager;
    @Context
    AnalysisManager analysisManager;

    /**
     * @return the traces which have the KernelAnalysisModule available
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAnalysedTraces() {
        Map<String, TmfAbstractAnalysisModule> entries = analysisManager.getEntries(InputOutputAnalysisModule.ID);
        if (entries == null || entries.isEmpty()) {
            return Response.ok().entity(Collections.emptyList()).build();
        }
        List<AnalysisModel> analysis = new ArrayList<>();
        for (Entry<String, TmfAbstractAnalysisModule> entry : entries.entrySet()) {
            InputOutputAnalysisModule module = (InputOutputAnalysisModule) entry.getValue();
            ITmfStateSystem stateSystem = module.getStateSystem();
            if (stateSystem != null) {
                analysis.add(new AnalysisModel(entry.getKey(), stateSystem));
            }
        }

        return Response.ok().entity(analysis).build();
    }

    /**
     * Start the Kernel Analysis for an existing trace
     *
     * @param traceName
     *            the name of the existing trace
     * @return a response status
     */
    @POST
    public Response startAnalysis(@QueryParam("name") String traceName) {
        TraceModel model = traceManager.get(traceName);
        if (model == null) {
            // trace does not exist in the back-end
            return Response.status(Status.NOT_FOUND).build();
        }
        if (analysisManager.get(traceName, InputOutputAnalysisModule.ID) != null) {
            // analysis has already been started
            return Response.status(Status.FOUND).build();
        }
        InputOutputAnalysisModule analysis = new InputOutputAnalysisModule();

        ITmfTrace trace = model.getTrace();
        if (trace == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        if (!analysis.canExecute(trace)) {
            return Response.status(Status.METHOD_NOT_ALLOWED).build();
        }

        try {
            analysis.setTrace(trace);
        } catch (TmfAnalysisException e) {
            // this should not happen as the analysis is initialized above, so it should not
            // have a trace yet.
            return Response.serverError().entity(e.getMessage()).build();
        }
        analysisManager.put(traceName,InputOutputAnalysisModule.ID, analysis);
        analysis.schedule();
        return Response.ok().build();
    }

    /**
     * Query the state system for the XY view
     *
     * @param traceName
     *            name of the trace to query
     * @param start
     *            lower bound for the query
     * @param end
     *            upper bound for the query
     * @param resolution
     *            nanoseconds between two data points
     * @return an {@link XYView} with the results
     */
    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getXY(@PathParam(value = "name") String traceName, @QueryParam(value = "start") long start,
            @QueryParam(value = "end") long end, @QueryParam(value = "resolution") long resolution) {
        TmfAbstractAnalysisModule analysis = analysisManager.get(traceName, InputOutputAnalysisModule.ID);
        if (analysis == null) {
            // analysis has not been run
            return Response.status(Status.NOT_FOUND).entity("Analysis has not been run").build(); //$NON-NLS-1$
        }
        ITmfStateSystem stateSystem = ((InputOutputAnalysisModule) analysis).getStateSystem();
        if (stateSystem == null) {
            // this should not happen as we schedule the analysis upon posting it
            return Response.status(Status.NOT_FOUND).entity("Could not find a statesystem for the CpuUsage").build(); //$NON-NLS-1$
        }

        long[] xSeries = getXSeries(start, end, resolution);
        Map<String, long[]> yValues = getSeries((InputOutputAnalysisModule) analysis, xSeries);
        AnalysisModel model = new AnalysisModel(traceName, stateSystem);
        XYView view = new XYView(model, xSeries, yValues);
        return Response.ok().entity(view).build();
    }

    private static long[] getXSeries(long start, long end, long resolution) {
        long[] xSeries = new long[1 + (int) ((end - start) / resolution)];
        int i = 0;
        for (long x = start; x <= end; x += resolution) {
            xSeries[i++] = x;
        }
        return xSeries;
    }

    /**
     * Read the StateSystem to extract YSeries for disk usage. Copy Pasted from
     * org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.io.diskioactivity.DisksIOActivityViewer
     *
     * @param module
     *            the IO Analysis module for the relevant trace
     * @param xValues
     *            array of time stamps to query.
     * @return a Map of the series name to the Y values
     */
    private static Map<String, long[]> getSeries(@NonNull InputOutputAnalysisModule module, long[] xValues) {
        Collection<Disk> disks = InputOutputInformationProvider.getDisks(module);
        ITmfStateSystem stateSystem = module.getStateSystem();
        if (stateSystem == null) {
            return null;
        }
        long traceStart = stateSystem.getStartTime();
        long traceEnd = stateSystem.getCurrentEndTime();
        Map<String, long[]> map = new HashMap<>();
        for (Disk disk : disks) {
            if (!disk.hasActivity()) {
                continue;
            }

            String diskName = disk.getDiskName();

            /* Initialize serie's names and arrays */
            long[] yValuesWritten = new long[xValues.length];
            long[] yValuesRead = new long[xValues.length];
            String seriesNameWritten = diskName + DisksIOActivityViewer_Write;
            String seriesNameRead = diskName + DisksIOActivityViewer_Read;

            long prevTime = timeInRange(traceStart, traceEnd, xValues[0]);
            long prevCountRead = disk.getSectorsAt(prevTime, IoOperationType.READ);
            long prevCountWrite = disk.getSectorsAt(prevTime, IoOperationType.WRITE);
            for (int i = 1; i < xValues.length; i++) {
                long time = timeInRange(traceStart, traceEnd, xValues[i]);

                long countWrite = disk.getSectorsAt(time, IoOperationType.WRITE);
                yValuesWritten[i] = (long) ((countWrite - prevCountWrite) * BYTES_PER_SECTOR / ((time - prevTime) / SECOND_TO_NANOSECOND));
                prevCountWrite = countWrite;

                long countRead = disk.getSectorsAt(time, IoOperationType.READ);
                yValuesRead[i] = (long) ((countRead - prevCountRead) * BYTES_PER_SECTOR / ((time - prevTime) / SECOND_TO_NANOSECOND));
                prevCountRead = countRead;

                prevTime = time;
            }
            map.put(seriesNameRead, yValuesRead);
            map.put(seriesNameWritten, yValuesWritten);
        }
        return map;
    }

    /**
     * Method to fit a long to being between two bounds
     *
     * @param low
     *            low bound
     * @param high
     *            high bound
     * @param time
     *            long to adjust
     * @return a long l such that {@code l >= low} and {@code l <= high}
     */
    private static long timeInRange(long low, long high, long time) {
        if (high < low) {
            throw new IllegalArgumentException();
        }
        long prevTime = Math.max(low, time);
        return Math.min(high, prevTime);
    }

}
