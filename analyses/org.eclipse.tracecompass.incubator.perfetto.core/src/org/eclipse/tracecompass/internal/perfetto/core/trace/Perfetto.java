/*******************************************************************************
 * Copyright (c) 2025 AMD
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.perfetto.core.trace;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.perfetto.core.Activator;
import org.eclipse.tracecompass.incubator.internal.perfetto.core.Messages;
import org.eclipse.tracecompass.incubator.internal.perfetto.core.event.PerfettoEvent;
import org.eclipse.tracecompass.incubator.internal.perfetto.core.event.PerfettoEventData;
import org.eclipse.tracecompass.incubator.internal.perfetto.protos.TraceOuterClass;
import org.eclipse.tracecompass.incubator.internal.perfetto.protos.TracePacketOuterClass;
import org.eclipse.tracecompass.incubator.internal.perfetto.protos.TrackEventOuterClass;
import org.eclipse.tracecompass.incubator.internal.perfetto.protos.TrackEventOuterClass.EventName;
import org.eclipse.tracecompass.incubator.internal.perfetto.protos.DebugAnnotationOuterClass.DebugAnnotation;
import org.eclipse.tracecompass.incubator.internal.perfetto.protos.DebugAnnotationOuterClass.DebugAnnotationName;
import org.eclipse.tracecompass.incubator.internal.perfetto.protos.InternedDataOuterClass;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.project.model.ITmfPropertiesProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTraceWithPreDefinedEvents;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTraceKnownSize;
import org.eclipse.tracecompass.tmf.core.trace.TmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEventType;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.indexer.ITmfPersistentlyIndexable;
import org.eclipse.tracecompass.tmf.core.trace.indexer.ITmfTraceIndexer;
import org.eclipse.tracecompass.tmf.core.trace.indexer.TmfBTreeTraceIndexer;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.CodedInputStream;

/**
 * Perfetto trace. Can read Perfetto traces.
 *
 * @author Ammar ELWazir
 */
@SuppressWarnings("restriction")
public class Perfetto extends TmfTrace implements ITmfTraceKnownSize, ITmfTraceWithPreDefinedEvents {
    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private List<TracePacketOuterClass.TracePacket> packets;
    private List<DebugAnnotationName> extraDebugAnnotationsNames = null;
    private Hashtable<Long, String> categories = new Hashtable<Long, String>();
    private Hashtable<Long, String> names = new Hashtable<Long, String>();
    private boolean flag = true;
    private static String previousName = "N/A";
    private static String previousCat = "N/A";

    private final Map<@NonNull String, @NonNull TmfEventType> fContainedEventTypes = Collections.synchronizedMap(new HashMap<>());

    // The trace packet location
    private int fLocation = 0;

    private static String getEventName(long id, List<DebugAnnotationName> names) {
      for (DebugAnnotationName name : names) {
          if(name.getIid() == id) {
              return name.getName();
          }
      }
      return "";
    }

    public Perfetto() {
        super();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation sets the confidence to 100 if the trace is a valid
     * CTF trace in the "ust" domain.
     */
    @Override
    public IStatus validate(final IProject project, final String path) {
        String pluginId = Activator.PLUGIN_ID;
        if(path.contains(".pftrace")) {
            return new Status(IStatus.OK, pluginId, Messages.Invalid_trace_file);
        }
        return new Status(IStatus.ERROR, pluginId, "Trace File is valid");
    }

    @Override
    public void initTrace(IResource resource, String path, Class<? extends ITmfEvent> type) throws TmfTraceException {
        File file = new File(path);

        try (FileInputStream fis = new FileInputStream(file)) {
            CodedInputStream codedInput = CodedInputStream.newInstance(fis);
            TraceOuterClass.Trace perfetto_trace = TraceOuterClass.Trace.parseFrom(codedInput);
            if(perfetto_trace.getPacketCount() > 0) {
                packets = perfetto_trace.getPacketList();
            }
        } catch (Exception e) {
            throw new TmfTraceException("Protobuf parsing failed: " + e.getMessage(), e);
        }
        super.initTrace(resource, path, type);
    }

    @Override
    public TmfEvent parseEvent(ITmfContext context) {
        if(fLocation >= packets.size()) {
            return null;
        }
        TracePacketOuterClass.TracePacket packet = packets.get(fLocation);
        long timestamp = 0;
        String name = "N/A";
        String cat = "N/A";
        String namePrefix = "Name";
        Hashtable<String, String> extras = new Hashtable<String, String>();
        if(packet.hasTrackEvent()) {
            TrackEventOuterClass.TrackEvent track_event = packet.getTrackEvent();
            InternedDataOuterClass.InternedData data = packet.getInternedData();
            if(data.getEventCategoriesCount() > 0) {
                cat = data.getEventCategories(0).getName();
                if(!categories.containsKey(track_event.getCategoryIids(0))) {
                    categories.put(track_event.getCategoryIids(0), cat);
                }
            } else {
                if(track_event.getCategoryIidsCount() > 0) {
                    cat = categories.get(track_event.getCategoryIids(0));
                }
            }
            if(track_event.getType().equals(TrackEventOuterClass.TrackEvent.Type.TYPE_SLICE_BEGIN)) {
              if(flag) {extraDebugAnnotationsNames = data.getDebugAnnotationNamesList(); flag = false;}
              name = track_event.getName();
              long name_id = track_event.getNameIid();
              for (EventName ename : data.getEventNamesList()) {
                  if(!ename.getName().isBlank()) {
                    names.put(ename.getIid(), ename.getName());
                }
              }
              if(name.isBlank() && names.containsKey(name_id)) {
                  name = names.get(name_id);
              }
              if(name.isBlank() && data.getEventNamesCount() > 0) {
                  if(data.getEventNamesCount() > name_id) {
                    name = data.getEventNames((int)name_id).getName();
                  } else {
                    name = data.getEventNames(0).getName();
                  }
              }
              previousName = name;
              previousCat = cat;
              namePrefix = "Start: " + namePrefix;
              timestamp = packet.getTimestamp();

              if(track_event.getDebugAnnotationsCount() > 0) {
                  for (DebugAnnotation item :  track_event.getDebugAnnotationsList()) {
                      String itemName = getEventName(item.getNameIid(), extraDebugAnnotationsNames);
                      String itemValue = "N/A";
                      switch (item.getValueCase()) {
                          case STRING_VALUE:
                              itemValue = item.getStringValue();
                              break;
                          case BOOL_VALUE:
                              itemValue = item.getBoolValue() + "";
                              break;
                          case DOUBLE_VALUE:
                              itemValue = item.getDoubleValue() + "";
                              break;
                          case INT_VALUE:
                              itemValue = item.getIntValue() + "";
                              break;
                          case LEGACY_JSON_VALUE:
                              itemValue = item.getLegacyJsonValue();
                              break;
                          case NESTED_VALUE:
                              itemValue = item.getNestedValue().toString();
                              break;
                          case POINTER_VALUE:
                              itemValue = item.getPointerValue() + "";
                              break;
                          case STRING_VALUE_IID:
                              itemValue = item.getStringValueIid() + "";
                              break;
                          case UINT_VALUE:
                              itemValue = item.getUintValue() + "";
                              break;
                    case VALUE_NOT_SET:
                          default:
                      }
                      extras.put(itemName, itemValue);
                  }
              }
          } else if (track_event.getType().equals(TrackEventOuterClass.TrackEvent.Type.TYPE_SLICE_END)) {
              name = previousName;
              cat = previousCat;
              namePrefix = "End: " + namePrefix;
              timestamp = packet.getTimestamp();
          }
        }
        fLocation++;
        return new PerfettoEvent(this, fLocation, name, timestamp, new TmfEventType(cat, null), new PerfettoEventData(namePrefix, name, extras));
    }

    @Override
    public synchronized void dispose() {
        fContainedEventTypes.clear();
        super.dispose();
    }

    @Override
    public synchronized ITmfEvent getNext(ITmfContext context) {
        return super.getNext(context);
    }

    @Override
    public ITmfLocation getCurrentLocation() {
        // TODO Auto-generated method stub
        return new TmfLongLocation(fLocation);
    }

    @Override
    public double getLocationRatio(ITmfLocation location) {
        if (getNbEvents() > 0 && location instanceof TmfLongLocation) {
            return (double) ((TmfLongLocation) location).getLocationInfo() / getNbEvents();
        }
        return 0;
    }

    @Override
    public ITmfContext seekEvent(double ratio) {
        // TODO Auto-generated method stub
        return seekEvent(new TmfLongLocation((long) (ratio*(fLocation+1))));
    }

    @Override
    public int progress() {
        // TODO Auto-generated method stub
        return fLocation+1;
    }

    @Override
    public int size() {
        return packets.size();
    }

    @Override
    public synchronized TmfContext seekEvent(ITmfLocation location) {
        fLocation = (int) ((location != null) ? ((TmfLongLocation) location).getLocationInfo() : 0);
        return new TmfContext(new TmfLongLocation(fLocation), fLocation);
    }

    /**
     * Register an event type to this trace.
     *
     * Public visibility so that {@link CtfTmfEvent#getType} can call it.
     *
     * FIXME This could probably be made cleaner?
     *
     * @param eventType
     *            The event type to register
     */
    public void registerEventType(TmfEventType eventType) {
        fContainedEventTypes.put(eventType.getName(), eventType);
    }

    /**
     * Gets the list of declared events
     */
    @Override
    public Set<@NonNull TmfEventType> getContainedEventTypes() {
        return ImmutableSet.copyOf(fContainedEventTypes.values());
    }
}
