/*******************************************************************************
 * Copyright (c) 2021, 2022 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.TreeModelWrapper.TreeColumnHeader;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.Experiment;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.Trace;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.IVirtualTableLine;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.VirtualTableCell;
import org.eclipse.tracecompass.internal.tmf.core.markers.MarkerSet;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigParamDescriptor;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.model.IAxisDomain;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.annotations.Annotation;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ISeriesModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXYAxisDescription;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

/**
 * Creates a provider that supplies context information to resource classes and other providers.
 *
 * This provider uses @Link JacksonJaxbJsonProvider}
 *
 * @author Bernd Hufmann
 */
@SuppressWarnings({"restriction", "null"})
@Provider
public class JacksonObjectMapperProvider implements ContextResolver<ObjectMapper> {
    private ObjectMapper fDefaultObjectMapper;

    @Override
    public ObjectMapper getContext(Class<?> type) {
        ObjectMapper mapper = fDefaultObjectMapper;
        if (mapper == null) {
            mapper = new ObjectMapper();

            SimpleModule module = new SimpleModule();
            module.addSerializer(Trace.class, new TraceSerializer());
            module.addSerializer(Experiment.class, new ExperimentSerializer());
            module.addSerializer(DataProviderDescriptor.class, new DataProviderDescriptorSerializer());
            module.addSerializer(ITmfXyModel.class, new XYModelSerializer());
            module.addSerializer(ISeriesModel.class, new SeriesModelSerializer());
            module.addSerializer(TimeGraphState.class, new TimeGraphStateSerializer());
            module.addSerializer(ITimeGraphArrow.class, new TimeGraphArrowSerializer());
            module.addSerializer(TimeGraphRowModel.class, new TimeGraphRowModelSerializer());
            module.addSerializer(TimeGraphEntryModel.class, new TimeGraphEntryModelSerializer());
            module.addSerializer(Annotation.class, new AnnotationSerializer());
            module.addSerializer(TmfTreeDataModel.class, new TmfTreeModelSerializer());
            module.addSerializer(TreeColumnHeader.class, new TreeColumnHeaderSerializer());
            module.addSerializer(OutputElementStyle.class, new OutputElementStyleSerializer());
            module.addSerializer(IVirtualTableLine.class, new VirtualTableLineSerializer());
            module.addSerializer(VirtualTableCell.class, new VirtualTableCellSerializer());
            module.addSerializer(MarkerSet.class, new MarkerSetSerializer());
            module.addSerializer(ITmfConfiguration.class, new TmfConfigurationSerializer());
            module.addSerializer(ITmfConfigurationSourceType.class, new TmfConfigurationSourceTypeSerializer());
            module.addSerializer(ITmfConfigParamDescriptor.class, new TmfConfigParamDescriptorSerializer());
            module.addSerializer(IAxisDomain.class, new AxisDomainSerializer());
            module.addSerializer(TmfXYAxisDescription.class, new TmfXYAxisDescriptionSerializer());

            // create JsonProvider to provide custom ObjectMapper
            JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider();
            provider.setMapper(mapper);
            mapper.registerModule(module);
            fDefaultObjectMapper = mapper;
        }
        return mapper;
    }
}