/*******************************************************************************
 * Copyright (c) 2022 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header;

import java.nio.ByteOrder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A representation of all header information required for parsing the events in
 * the FTrace file.
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceHeaderInfo {
    private String fFilePath;

    // Some metadata aboout the file
    private final BinaryFTraceVersion fVersion;
    private final ByteOrder fEndianess;
    private final int fLongValueSize;
    private final int fHostMachinePageSize;
    private final BinaryFTraceFileType fFileType;

    // Data parsed from the header
    private List<BinaryFTraceFormatField> fHeaderPageFields;
    private BinaryFTraceHeaderEvent fHeaderEventInfo;
    private Map<Integer, BinaryFTraceEventFormat> fFTraceEventFormats;
    private List<BinaryFTraceEventSystem> fEventSystems;
    private Map<String, BinaryFTraceFunctionAddressNameMapping> fFunctionMapping;
    private Map<String, String> fPrintKPointerStringMapping;
    private Map<Integer, String> fProcessIDNameMapping;
    private List<BinaryFTraceOption> fOptions;
    private Map<String, BinaryFTraceFormatField> fEventCommonFields;

    private List<BinaryFTraceFileCPU> cpus;

    /**
     * Constructor
     *
     * @param builder The builder used to construct the trace header
     */
    private BinaryFTraceHeaderInfo(BinaryFTraceHeaderInfoBuilder builder) {
        fFilePath = builder.fBuilderFilePath;
        fVersion = builder.fBuilderVersion;
        fEndianess = builder.fBuilderEndianess; // Because Java by default is
                                                // big endian.
        fLongValueSize = builder.fBuilderLongValueSize; // Usually it is 8
                                                        // bytes, but can be 4.
        fHostMachinePageSize = builder.fBuilderHostMachinePageSize;
        fFileType = builder.fBuilderFileType; // By default we just read
                                              // everything as an ASCII file

        fHeaderPageFields = builder.fBuilderHeaderPageFields;
        fHeaderEventInfo = builder.fBuilderHeaderEventInfo;
        fFTraceEventFormats = builder.fBuilderFTraceEventFormats;
        fEventSystems = builder.fBuilderEventSystems;
        fFunctionMapping = builder.fBuilderFunctionMapping;
        fPrintKPointerStringMapping = builder.fBuilderPrintKPointerStringMapping;
        fProcessIDNameMapping = builder.fBuilderProcessIDNameMapping;
        fOptions = builder.fBuilderOptions;
        fEventCommonFields = builder.fBuilderEventCommonFields;
        cpus = builder.fBuilderCpus;
    }

    /**
     * Get the file path to the trace file.
     *
     * @return The file path of the trace file.
     */
    public String getFilePath() {
        return fFilePath;
    }

    /**
     * Set the file path to the trace file.
     *
     * @param filePath
     *            The file path of the trace file.
     */
    public void setFilePath(String filePath) {
        this.fFilePath = filePath;
    }

    /**
     * Get the FTrace version of the file
     *
     * @return The FTrace version of the file
     */
    public BinaryFTraceVersion getVersion() {
        return fVersion;
    }

    /**
     * Get the endianess of the file
     *
     * @return The endianess of the file
     */
    public ByteOrder getEndianess() {
        return fEndianess;
    }

    /**
     * Get the size of long values (in bytes).
     *
     * @return The size of long values
     */
    public int getLongValueSize() {
        return fLongValueSize;
    }

    /**
     * Get the page size of the host machine.
     *
     * @return The page size of the host machine.
     */
    public int getHostMachinePageSize() {
        return fHostMachinePageSize;
    }

    /**
     * Get the FTrace event formats map. The key is the event format ID.
     *
     * @return A map of FTrace events format.
     */
    public Map<Integer, BinaryFTraceEventFormat> getFTraceEventFormats() {
        return fFTraceEventFormats;
    }

    /**
     * Get a map of common fields (fields that starts with common_). The key is
     * the field name.
     *
     * @return A map of common fields.
     */
    public Map<String, BinaryFTraceFormatField> getEventCommonFields() {
        return fEventCommonFields;
    }

    /**
     * Get the information the event header section.
     *
     * @return The information the event header section.
     */
    public BinaryFTraceHeaderEvent getHeaderEventInfo() {
        return fHeaderEventInfo;
    }

    /**
     * Get the fields of the header page section.
     *
     * @return A list of the fields of the header page section.
     */
    public List<BinaryFTraceFormatField> getHeaderPageFields() {
        return fHeaderPageFields;
    }

    /**
     * Get a list of different options from the options section of the file
     *
     * @return The list of options
     */
    public List<BinaryFTraceOption> getOptions() {
        return fOptions;
    }

    /**
     * Get event systems that contains event formats.
     *
     * @return A list of event systems that contain event formats.
     */
    public List<BinaryFTraceEventSystem> getEventSystems() {
        return fEventSystems;
    }

    /**
     * Get a map of address to function name mapping. The key is the function
     * address string
     *
     * @return The map of address to function name mapping.
     */
    public Map<String, BinaryFTraceFunctionAddressNameMapping> getFunctionMapping() {
        return fFunctionMapping;
    }

    /**
     * Get the address to string mapping used by the printk mapping as a hash
     * map. The key of the map is the physical address.
     *
     * @return A map containing the address to string mapping.
     */
    public Map<String, String> getPrintKPointerStringMapping() {
        return fPrintKPointerStringMapping;
    }

    /**
     * Get the process ID to name mapping as a map. The key is the process ID.
     *
     * @return A map of the process ID to name mapping.
     */
    public Map<Integer, String> getProcessIDNameMapping() {
        return fProcessIDNameMapping;
    }

    /**
     * Get event format by event format ID.
     *
     * @param eventTypeID
     *            The ID of the event.
     * @return The event format with the same event format ID.
     */
    public BinaryFTraceEventFormat getEventFormatByID(int eventTypeID) {
        if (fFTraceEventFormats.containsKey(eventTypeID)) {
            return fFTraceEventFormats.get(eventTypeID);
        }

        for (BinaryFTraceEventSystem eventSystem : fEventSystems) {
            if (eventSystem.getMapEventFormat().containsKey(eventTypeID)) {
                return eventSystem.getMapEventFormat().get(eventTypeID);
            }
        }

        return null;
    }

    /**
     * Get the list of CPUS.
     *
     * @return The list of CPUS of this binary file.
     */
    public List<BinaryFTraceFileCPU> getCpus() {
        return cpus;
    }

    /**
     * Get the file type of this binary file
     *
     * @return The file type
     */
    public BinaryFTraceFileType getFileType() {
        return fFileType;
    }

    /**
     * Print the information contained in the binary FTrace header. For demo and
     * testing purposes.
     *
     * @return A string that contains information of this trace header.
     */
    public String getTraceHeaderString() {
        StringBuilder strBuilder = new StringBuilder();

        strBuilder.append("Endianess: "); //$NON-NLS-1$
        if (fEndianess == ByteOrder.LITTLE_ENDIAN) {
            strBuilder.append("Little endian"); //$NON-NLS-1$
        } else if (fEndianess == ByteOrder.BIG_ENDIAN) {
            strBuilder.append("Big endian"); //$NON-NLS-1$
        }
        strBuilder.append('\n');

        strBuilder.append("Number of byte per long values: ") //$NON-NLS-1$
                .append(fLongValueSize)
                .append('\n');

        strBuilder.append("Host machine page size: ") //$NON-NLS-1$
                .append(fHostMachinePageSize)
                .append('\n');

        strBuilder.append("Header page fields:").append('\n'); //$NON-NLS-1$
        for (BinaryFTraceFormatField field : fHeaderPageFields) {
            strBuilder.append(field).append('\n');
        }

        strBuilder.append("Header event info:").append('\n').append(fHeaderEventInfo).append('\n'); //$NON-NLS-1$

        strBuilder.append("Number of FTrace formats: ").append(fFTraceEventFormats.size()).append('\n'); //$NON-NLS-1$
        String sample = "Sample:"; //$NON-NLS-1$
        if (fFTraceEventFormats.size() > 0) {
            strBuilder.append(sample).append('\n');
            int key = fFTraceEventFormats.keySet().iterator().next();
            strBuilder.append(fFTraceEventFormats.get(key));
        }
        strBuilder.append('\n');

        strBuilder.append("Number of event systems: ").append(fEventSystems.size()).append('\n'); //$NON-NLS-1$
        if (!fEventSystems.isEmpty()) {
            strBuilder.append("First system:").append('\n'); //$NON-NLS-1$
            BinaryFTraceEventSystem eventSystem = fEventSystems.get(0);
            strBuilder.append("Event system name: ").append(eventSystem.getSystemName()).append('\n'); //$NON-NLS-1$
            strBuilder.append("Number of events: ").append(eventSystem.getMapEventFormat().size()).append('\n'); //$NON-NLS-1$

            if (eventSystem.getMapEventFormat().size() > 0) {
                int key = eventSystem.getMapEventFormat().keySet().iterator().next();
                strBuilder.append(eventSystem.getMapEventFormat().get(key));
            }
        }
        strBuilder.append('\n');

        strBuilder.append("Number of address to functions mappings: ").append(fFunctionMapping.size()).append('\n'); //$NON-NLS-1$
        if (fFunctionMapping.size() > 0) {
            strBuilder.append("Sample: ").append('\n'); //$NON-NLS-1$
            String key = fFunctionMapping.keySet().iterator().next();
            strBuilder.append(fFunctionMapping.get(key));
        }
        strBuilder.append('\n').append('\n');

        strBuilder.append("Number of pointer address to string mappings: ").append(fPrintKPointerStringMapping.size()).append('\n'); //$NON-NLS-1$
        if (fPrintKPointerStringMapping.size() > 0) {
            strBuilder.append(sample).append('\n');
            String key = fPrintKPointerStringMapping.keySet().iterator().next();
            strBuilder.append("Pointer address: ").append(key); //$NON-NLS-1$
            strBuilder.append("; String: ").append(fPrintKPointerStringMapping.get(key)); //$NON-NLS-1$
        }
        strBuilder.append('\n').append('\n');

        strBuilder.append("File type: ").append(fFileType).append('\n'); //$NON-NLS-1$

        strBuilder.append("Number of process ID to name mapping: ").append(fProcessIDNameMapping.size()).append('\n'); //$NON-NLS-1$
        if (fProcessIDNameMapping.size() > 0) {
            strBuilder.append(sample).append('\n');
            int key = fProcessIDNameMapping.keySet().iterator().next();
            strBuilder.append("Process ID: ").append(key); //$NON-NLS-1$
            strBuilder.append("; Name: ").append(fProcessIDNameMapping.get(key)); //$NON-NLS-1$
        }
        strBuilder.append('\n').append('\n');

        strBuilder.append("Number of options: ").append(fOptions.size()).append('\n'); //$NON-NLS-1$
        if (!fOptions.isEmpty()) {
            strBuilder.append(sample).append('\n');
            strBuilder.append(fOptions.get(0));
        }
        strBuilder.append('\n');

        if (fFileType == BinaryFTraceFileType.FLY_RECORD && cpus != null && !cpus.isEmpty()) {
            strBuilder.append("Number of CPUS: ").append(cpus.size()).append('\n'); //$NON-NLS-1$
            for (BinaryFTraceFileCPU cpu : cpus) {
                strBuilder.append("CPU#: ").append(cpu.getCpuNumber()).append('\n') //$NON-NLS-1$
                        .append("Section size: ").append(cpu.getSectionSize()).append('\n') //$NON-NLS-1$
                        .append("Number of pages: ").append(cpu.getPages().size()).append('\n'); //$NON-NLS-1$
            }

        }

        return strBuilder.toString();
    }

    /**
     * A builder to help create immutable {@link BinaryFTraceHeaderInfo}
     * objects.
     *
     * @author Hoang Thuan Pham
     *
     */
    public static class BinaryFTraceHeaderInfoBuilder {
        private String fBuilderFilePath;

        // Some metadata aboout the file
        private BinaryFTraceVersion fBuilderVersion;
        private ByteOrder fBuilderEndianess;
        private int fBuilderLongValueSize;
        private int fBuilderHostMachinePageSize;
        private BinaryFTraceFileType fBuilderFileType;

        // Data parsed from the header
        private List<BinaryFTraceFormatField> fBuilderHeaderPageFields;
        private BinaryFTraceHeaderEvent fBuilderHeaderEventInfo;
        private Map<Integer, BinaryFTraceEventFormat> fBuilderFTraceEventFormats;
        private List<BinaryFTraceEventSystem> fBuilderEventSystems;
        private Map<String, BinaryFTraceFunctionAddressNameMapping> fBuilderFunctionMapping;
        private Map<String, String> fBuilderPrintKPointerStringMapping;
        private Map<Integer, String> fBuilderProcessIDNameMapping;
        private List<BinaryFTraceOption> fBuilderOptions;
        private Map<String, BinaryFTraceFormatField> fBuilderEventCommonFields;

        private List<BinaryFTraceFileCPU> fBuilderCpus;

        /**
         * Constructor
         */
        public BinaryFTraceHeaderInfoBuilder() {
            fBuilderFilePath = ""; //$NON-NLS-1$
            fBuilderVersion = BinaryFTraceVersion.NOT_SUPPORTED;
            fBuilderEndianess = ByteOrder.BIG_ENDIAN; // Because Java by default
                                                      // is big
            // endian.
            fBuilderLongValueSize = -1; // Usually it is 8 bytes, but can be 4.
            fBuilderHostMachinePageSize = 0;
            fBuilderFileType = BinaryFTraceFileType.LATENCY; // By default we
                                                             // just read
            // everything as an ASCII file

            fBuilderHeaderPageFields = null;
            fBuilderHeaderEventInfo = null;
            fBuilderFTraceEventFormats = null;
            fBuilderEventSystems = null;
            fBuilderFunctionMapping = null;
            fBuilderPrintKPointerStringMapping = null;
            fBuilderProcessIDNameMapping = null;
            fBuilderOptions = null;
            fBuilderEventCommonFields = null;
            fBuilderCpus = null;
        }

        /**
         * Set the file path.
         *
         * @param builderFilePath
         *            The file path
         * @return The current {@link BinaryFTraceHeaderInfoBuilder}.
         */
        public BinaryFTraceHeaderInfoBuilder filePath(String builderFilePath) {
            fBuilderFilePath = builderFilePath;
            return this;
        }

        /**
         * Set the FTrace version.
         *
         * @param builderVersion
         *            The FTrace version
         * @return The current {@link BinaryFTraceHeaderInfoBuilder}.
         */
        public BinaryFTraceHeaderInfoBuilder version(BinaryFTraceVersion builderVersion) {
            fBuilderVersion = builderVersion;
            return this;
        }

        /**
         * Set the endianess of the file.
         *
         * @param builderEndianess
         *            The endianess of the file
         * @return The current {@link BinaryFTraceHeaderInfoBuilder}.
         */
        public BinaryFTraceHeaderInfoBuilder endianess(ByteOrder builderEndianess) {
            fBuilderEndianess = builderEndianess;
            return this;
        }

        /**
         * Set the size of long values in the file (in bytes).
         *
         * @param builderLongValueSize
         *            The size of long values in the file (in bytes).
         * @return The current {@link BinaryFTraceHeaderInfoBuilder}.
         */
        public BinaryFTraceHeaderInfoBuilder longValueSize(int builderLongValueSize) {
            fBuilderLongValueSize = builderLongValueSize;
            return this;
        }

        /**
         * Set the page size of the host machine.
         *
         * @param builderHostMachinePageSize
         *            The page size of the host machine.
         * @return The current {@link BinaryFTraceHeaderInfoBuilder}.
         */
        public BinaryFTraceHeaderInfoBuilder hostMachinePageSize(int builderHostMachinePageSize) {
            fBuilderHostMachinePageSize = builderHostMachinePageSize;
            return this;
        }

        /**
         * Set the binary FTrace file type. Possible values are FLY_RECORD or
         * LATENCY.
         *
         * @param builderFileType
         *            The file type.
         * @return The current {@link BinaryFTraceHeaderInfoBuilder}.
         */
        public BinaryFTraceHeaderInfoBuilder fileType(BinaryFTraceFileType builderFileType) {
            fBuilderFileType = builderFileType;
            return this;
        }

        /**
         * Set the information used to parse the header of
         * {@link BinaryFTraceCPUDataPage}.
         *
         * @param builderHeaderPageFields
         *            The information used to parse the header of
         *            {@link BinaryFTraceCPUDataPage}/
         * @return The current {@link BinaryFTraceHeaderInfoBuilder}.
         */
        public BinaryFTraceHeaderInfoBuilder headerPageFields(List<BinaryFTraceFormatField> builderHeaderPageFields) {
            fBuilderHeaderPageFields = builderHeaderPageFields;
            return this;
        }

        /**
         * Set the information used to parse the header of a trace event
         *
         * @param builderHeaderEventInfo
         *            The information used to parse the header of trace event
         * @return The current trace event.
         */
        public BinaryFTraceHeaderInfoBuilder headerEventInfo(BinaryFTraceHeaderEvent builderHeaderEventInfo) {
            fBuilderHeaderEventInfo = builderHeaderEventInfo;
            return this;
        }

        /**
         * Set the event formats of FTrace events.
         *
         * @param builderFTraceEventFormats
         *            The event formats of FTrace events.
         * @return The current {@link BinaryFTraceHeaderInfoBuilder}.
         */
        public BinaryFTraceHeaderInfoBuilder fTraceEventFormats(Map<Integer, BinaryFTraceEventFormat> builderFTraceEventFormats) {
            fBuilderFTraceEventFormats = builderFTraceEventFormats;
            return this;
        }

        /**
         * Set the event systems that contains kernel event formats.
         *
         * @param builderEventSystems
         *            The event systems that contains kernel event formats
         * @return The current {@link BinaryFTraceHeaderInfoBuilder}.
         */
        public BinaryFTraceHeaderInfoBuilder eventSystems(List<BinaryFTraceEventSystem> builderEventSystems) {
            fBuilderEventSystems = builderEventSystems;
            return this;
        }

        /**
         * Set the function memory address to function name mapping.
         *
         * @param builderFunctionMapping
         *            The function memory address to function name mapping.
         * @return The current {@link BinaryFTraceHeaderInfoBuilder}.
         */
        public BinaryFTraceHeaderInfoBuilder functionMapping(Map<String, BinaryFTraceFunctionAddressNameMapping> builderFunctionMapping) {
            fBuilderFunctionMapping = builderFunctionMapping;
            return this;
        }

        /**
         * Set the print k pointer to string mapping.
         *
         * @param builderPrintKPointerStringMapping
         *            The print k pointer to string mapping.
         * @return The current {@link BinaryFTraceHeaderInfoBuilder}.
         */
        public BinaryFTraceHeaderInfoBuilder printKPointerStringMapping(Map<String, String> builderPrintKPointerStringMapping) {
            fBuilderPrintKPointerStringMapping = builderPrintKPointerStringMapping;
            return this;
        }

        /**
         * Set the process ID to name mapping.
         *
         * @param builderProcessIDNameMapping
         *            The process ID to name mapping
         * @return The current {@link BinaryFTraceHeaderInfoBuilder}.
         */
        public BinaryFTraceHeaderInfoBuilder processIDNameMapping(Map<Integer, String> builderProcessIDNameMapping) {
            fBuilderProcessIDNameMapping = builderProcessIDNameMapping;
            return this;
        }

        /**
         * Set the options of the file. It is some metadata that provides extra
         * information about the trace.
         *
         * @param builderOptions
         *            The options of the file
         * @return The current {@link BinaryFTraceHeaderInfoBuilder}.
         */
        public BinaryFTraceHeaderInfoBuilder options(List<BinaryFTraceOption> builderOptions) {
            fBuilderOptions = builderOptions;
            return this;
        }

        /**
         * Set the common fields of all trace events with payload.
         *
         * @param builderEventCommonFields
         *            The common fields
         * @return The current {@link BinaryFTraceHeaderInfoBuilder}.
         */
        public BinaryFTraceHeaderInfoBuilder eventCommonFields(Map<String, BinaryFTraceFormatField> builderEventCommonFields) {
            fBuilderEventCommonFields = builderEventCommonFields;
            return this;
        }

        /**
         * Set the cpu sections of the trace.
         *
         * @param builderCpus
         *            The cpu sections
         * @return The current {@link BinaryFTraceHeaderInfoBuilder}.
         */
        public BinaryFTraceHeaderInfoBuilder cpus(List<BinaryFTraceFileCPU> builderCpus) {
            fBuilderCpus = builderCpus;
            return this;
        }

        /**
         * Build an immutable {@link BinaryFTraceHeaderInfo} using the current
         * state of the builder.
         *
         * @return A {@link BinaryFTraceHeaderInfo} object.
         */
        public BinaryFTraceHeaderInfo build() {
            fBuilderEventCommonFields = getCommonFields();
            return new BinaryFTraceHeaderInfo(this);
        }

        private Map<String, BinaryFTraceFormatField> getCommonFields() {
            if (!fBuilderFTraceEventFormats.isEmpty()) {
                return getFirst(fBuilderFTraceEventFormats);
            } else if (!fBuilderEventSystems.isEmpty() &&
                    fBuilderEventSystems.get(0).getMapEventFormat().size() > 0) {
                Map<Integer, BinaryFTraceEventFormat> map = fBuilderEventSystems.get(0).getMapEventFormat();
                return getFirst(map);
            }
            return Collections.emptyMap();
        }

        private static Map<String, BinaryFTraceFormatField> getFirst(Map<Integer, BinaryFTraceEventFormat> map) {
            Optional<BinaryFTraceEventFormat> first = map.values().stream().findFirst();
            if (first.isPresent()) {
                return first.get().getCommonFields();
            }
            return Collections.emptyMap();
        }
    }
}
