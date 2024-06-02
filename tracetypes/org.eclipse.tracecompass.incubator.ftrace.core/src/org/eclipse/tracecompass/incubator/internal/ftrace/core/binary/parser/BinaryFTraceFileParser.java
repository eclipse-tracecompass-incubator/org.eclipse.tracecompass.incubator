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

package org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.parser;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.event.BinaryFTraceConstants;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceEventFormat;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceEventSystem;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceFileCPU;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceFileType;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceFormatField;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceFunctionAddressNameMapping;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceHeaderEvent;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceHeaderInfo;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceHeaderInfo.BinaryFTraceHeaderInfoBuilder;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceOption;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceVersionHeader;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;

/**
 * An implementation of {@link AbstractBinaryFTraceFileParser}. The class
 * provides the reading logic for the binary FTrace header, such as the order of
 * which the different sections are read.
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceFileParser extends AbstractBinaryFTraceFileParser {
    private static final String UNSECURE_TRACE_ERROR_MESSAGE = "Buffer overrun stopped"; //$NON-NLS-1$

    /**
     * Constructor
     */
    private BinaryFTraceFileParser() {
        // Do nothing
    }

    private static void validate(BinaryFTraceByteBuffer buffer, long bytesToRead) throws TmfTraceException {
        /*
         * Validate if read reading bytesToRead amount of bytes will go over the
         * file size limit. There is no need to wrap IOException to
         * TmfTraceException, it will be done in the public functions.
         */
        if (buffer.getCurrentOffset() + bytesToRead > buffer.getFileSize()) {
            /*
             * When Trace Compass encounter a unsecure trace, it needs to halt
             * the parsing process and return an error message to the user.
             *
             * In addition, since the occurrences of unsecure trace is much less
             * likely compared to good traces, and this function is called in
             * multiple places, making this function return a boolean and write
             * an if-else statement every time to check for the return value is
             * not efficient.
             */
            throw new TmfTraceException(getUnsecureFileErrorMessage(buffer.getCurrentOffset(), buffer.getFileSize(), bytesToRead));
        }
    }

    /**
     * Get the magic values and FTrace version
     *
     * @param path
     *            The path to the file
     * @return A BinaryFTraceVersionHeader containing the magic values and
     *         FTrace version
     * @throws TmfTraceException
     *             Cannot open or parse the file
     */
    public static BinaryFTraceVersionHeader getFtraceVersionHeader(String path) throws TmfTraceException {
        try {
            BinaryFTraceByteBuffer buffer = new BinaryFTraceByteBuffer(path);
            return getFtraceVersionHeader(buffer);
        } catch (IOException e) {
            throw new TmfTraceException("Cannot open trace file", e); //$NON-NLS-1$
        }
    }

    private static BinaryFTraceVersionHeader getFtraceVersionHeader(BinaryFTraceByteBuffer buffer) throws TmfTraceException {
        validate(buffer, BinaryFTraceHeaderElementSize.getMagicValueSectionSize());

        byte[] bytes = buffer.getNextBytes(10);
        String strVersion = buffer.getNextString().trim();
        return getMagicValuesAndFtraceVersion(bytes, strVersion);
    }

    /**
     * Parse the binary FTrace header information and try to map the pages in
     * the CPU data section.
     *
     * @param path
     *            The path to the file
     * @return A BinaryFTraceHeaderInfo container all the header information
     * @throws TmfTraceException
     *             Cannot open or parse the file
     */
    public static BinaryFTraceHeaderInfo parse(String path) throws TmfTraceException {
        try {
            BinaryFTraceHeaderInfoBuilder builder = new BinaryFTraceHeaderInfoBuilder();
            builder.filePath(path);

            BinaryFTraceFileMapping traceMapping = new BinaryFTraceFileMapping(path);
            BinaryFTraceByteBuffer buffer = new BinaryFTraceByteBuffer(traceMapping);

            // Parse initial data section
            BinaryFTraceVersionHeader versionHeader = getFtraceVersionHeader(buffer);
            builder.version(versionHeader.getFTraceVersion());

            ByteOrder endianess = getFileEndianess(buffer);
            builder.endianess(endianess);

            // File content from now on has the specified endianess
            traceMapping.order(endianess);

            builder.longValueSize(getLongValueSize(buffer));

            int pageSize = getHostPageSize(buffer);

            builder.hostMachinePageSize(pageSize);
            builder.headerPageFields(parseHeaderPage(buffer));
            builder.headerEventInfo(parseHeaderEvent(buffer));
            builder.fTraceEventFormats(parseTraceEventsFormat(buffer));
            builder.eventSystems(parseEventSystemsAndFormats(buffer));
            builder.functionMapping(parseFunctionMapping(buffer));
            builder.printKPointerStringMapping(parseTracePrintKInfo(buffer));
            builder.processIDNameMapping(parseProcessToFunctionNameMapping(buffer));

            int cpuCount = parseCPUCount(buffer);

            // Now we parse the remaining sections of the file
            // First we check if an option section exists
            String sectionType = buffer.getNextBytesAsString(10).trim();

            if (sectionType.equals(BinaryFTraceConstants.OPTIONS_SECTION_NAME)) {
                builder.options(parseOptionsSection(buffer));
                // read the next section name
                sectionType = buffer.getNextBytesAsString(10).trim();
            }

            if (sectionType.equals(BinaryFTraceConstants.LATENCY_SECTION_NAME)) {
                builder.fileType(BinaryFTraceFileType.LATENCY);
            } else if (sectionType.equals(BinaryFTraceConstants.FLYRECORD_SECTION_NAME)) {
                builder.fileType(BinaryFTraceFileType.FLY_RECORD);
                builder.cpus(parseFlyRecordSection(buffer, cpuCount, pageSize));
            }
            return builder.build(traceMapping);
        } catch (IOException ex) {
            throw new TmfTraceException(ex.getMessage(), ex);
        }
    }

    private static List<BinaryFTraceFormatField> parseHeaderPage(BinaryFTraceByteBuffer buffer) throws TmfTraceException {
        validate(buffer, BinaryFTraceHeaderElementSize.getHeaderPageSectionHeaderSize());
        buffer.getNextBytesAsString(12); // Skipping the section name
        long headerPageSize = buffer.getNextLong();

        validate(buffer, headerPageSize);
        String headerPageContent = buffer.getNextBytesAsString(headerPageSize);
        return extractHeaderPageContent(headerPageContent);
    }

    private static BinaryFTraceHeaderEvent parseHeaderEvent(BinaryFTraceByteBuffer buffer) throws TmfTraceException {
        validate(buffer, BinaryFTraceHeaderElementSize.getHeaderEventSectionHeaderSize());
        buffer.getNextBytesAsString(13); // Skipping the section header
        long headerEventSize = buffer.getNextLong();

        validate(buffer, headerEventSize);
        String strHeaderEventInfo = buffer.getNextBytesAsString(headerEventSize);
        return extractHeaderEventContent(strHeaderEventInfo);
    }

    private static Map<Integer, BinaryFTraceEventFormat> parseTraceEventsFormat(BinaryFTraceByteBuffer buffer) throws TmfTraceException {
        ArrayList<String> eventFormats = new ArrayList<>();

        validate(buffer, BinaryFTraceHeaderElementSize.EVENT_COUNT);
        int numOfTraceEventFormats = buffer.getNextInt();

        for (int i = 0; i < numOfTraceEventFormats; i++) {
            validate(buffer, BinaryFTraceHeaderElementSize.EVENT_SIZE);
            long formatSize = buffer.getNextLong();

            validate(buffer, formatSize);
            eventFormats.add(buffer.getNextBytesAsString(formatSize));
        }

        return extractTraceEventsFormat(eventFormats);
    }

    private static List<BinaryFTraceEventSystem> parseEventSystemsAndFormats(BinaryFTraceByteBuffer buffer) throws TmfTraceException {
        HashMap<String, List<String>> eventSystemData = new HashMap<>();

        validate(buffer, BinaryFTraceHeaderElementSize.EVENT_SYSTEM_COUNT);
        int numOfEventSystems = buffer.getNextInt();

        for (int i = 0; i < numOfEventSystems; i++) {
            String eventSystemName = buffer.getNextString();

            validate(buffer, BinaryFTraceHeaderElementSize.EVENT_COUNT);
            int numOfEvents = buffer.getNextInt();

            ArrayList<String> lstEventFormat = new ArrayList<>();
            for (int j = 0; j < numOfEvents; j++) {
                validate(buffer, BinaryFTraceHeaderElementSize.EVENT_SIZE);
                long eventSize = buffer.getNextLong();

                validate(buffer, eventSize);
                lstEventFormat.add(buffer.getNextBytesAsString(eventSize));
            }

            eventSystemData.put(eventSystemName, lstEventFormat);
        }

        return extractEventSystemsAndFormats(eventSystemData);
    }

    private static Map<String, BinaryFTraceFunctionAddressNameMapping> parseFunctionMapping(BinaryFTraceByteBuffer buffer) throws TmfTraceException {
        validate(buffer, BinaryFTraceHeaderElementSize.SMALL_SECTION_SIZE);
        int dataSize = buffer.getNextInt();

        validate(buffer, dataSize);
        String strMappings = buffer.getNextBytesAsString(dataSize);
        return extractFunctionMappingContent(strMappings);
    }

    private static Map<String, String> parseTracePrintKInfo(BinaryFTraceByteBuffer buffer) throws TmfTraceException {
        validate(buffer, BinaryFTraceHeaderElementSize.SMALL_SECTION_SIZE);
        int dataSize = buffer.getNextInt();

        validate(buffer, dataSize);
        String strMappings = buffer.getNextBytesAsString(dataSize);
        return extractPrintKContent(strMappings);
    }

    private static Map<Integer, String> parseProcessToFunctionNameMapping(BinaryFTraceByteBuffer buffer) throws TmfTraceException {
        validate(buffer, BinaryFTraceHeaderElementSize.LARGE_SECTION_SIZE);
        long dataSize = buffer.getNextLong();

        validate(buffer, dataSize);
        String strMappings = buffer.getNextBytesAsString(dataSize);
        return extractFunctionNameMapping(strMappings);
    }

    private static List<BinaryFTraceFileCPU> parseFlyRecordSection(BinaryFTraceByteBuffer buffer, int cpuCount, int pageSize) throws TmfTraceException {
        // Validate that the size of the CPU information section is valid
        validate(buffer, (long) cpuCount * (BinaryFTraceHeaderElementSize.CPU_SECTION_OFFSET + BinaryFTraceHeaderElementSize.CPU_SECTION_SIZE));

        // Obtain the starting offset and the size of each CPU section
        long totalTraceSize = 0;

        long[] cpuSectionStartingOffset = new long[cpuCount];
        long[] cpuSectionSize = new long[cpuCount];
        for (int i = 0; i < cpuCount; i++) {
            cpuSectionStartingOffset[i] = buffer.getNextLong();
            cpuSectionSize[i] = buffer.getNextLong();

            totalTraceSize += cpuSectionSize[i];

            if (cpuSectionStartingOffset[i] + cpuSectionSize[i] > buffer.getFileSize()) {
                String errorMessage = getUnsecureFileErrorMessage(cpuSectionStartingOffset[i], buffer.getCurrentOffset(), cpuSectionSize[i]);
                throw new TmfTraceException(errorMessage);
            }
        }

        if (totalTraceSize == 0) {
            throw new TmfTraceException("Empty trace."); //$NON-NLS-1$
        }

        return parseCPUPageHeader(buffer, cpuSectionStartingOffset, cpuSectionSize, pageSize);
    }

    private static List<BinaryFTraceFileCPU> parseCPUPageHeader(BinaryFTraceByteBuffer buffer, long[] cpuSectionStartingOffset, long[] cpuSectionSize, int pageSize) throws TmfTraceException {
        Map<Integer, List<Long>> mapTimeStamp = new HashMap<>();
        Map<Integer, List<Long>> mapFlag = new HashMap<>();
        Map<Integer, List<Long>> mapStartingOffset = new HashMap<>();

        int cpuCount = cpuSectionStartingOffset.length;

        for (int cpuNumber = 0; cpuNumber < cpuCount; cpuNumber++) {
            // Parse the header information for each page
            List<Long> lstTimeStamp = new ArrayList<>();
            List<Long> lstFlag = new ArrayList<>();
            List<Long> lstStartingOffset = new ArrayList<>();

            long sectionStartingOffset = cpuSectionStartingOffset[cpuNumber];
            long pageStartingOffset = sectionStartingOffset;
            long endingOffset = sectionStartingOffset + cpuSectionSize[cpuNumber];
            while (pageStartingOffset < endingOffset) {
                buffer.movePointerToOffset(pageStartingOffset);

                // Make sure that we can read the page header at least
                validate(buffer, BinaryFTraceHeaderElementSize.PAGE_HEADER_SIZE);

                lstTimeStamp.add(buffer.getNextLong());
                lstFlag.add(buffer.getNextLong());
                lstStartingOffset.add(pageStartingOffset);
                pageStartingOffset = pageStartingOffset + pageSize;
            }

            mapTimeStamp.put(cpuNumber, lstTimeStamp);
            mapFlag.put(cpuNumber, lstFlag);
            mapStartingOffset.put(cpuNumber, lstStartingOffset);
        }

        return initializeCPUs(mapTimeStamp, mapFlag, cpuSectionStartingOffset, cpuSectionSize, pageSize);
    }

    private static List<BinaryFTraceOption> parseOptionsSection(BinaryFTraceByteBuffer buffer) throws TmfTraceException {
        ArrayList<Short> optionTypes = new ArrayList<>();
        ArrayList<String> optionData = new ArrayList<>();

        short optionType = 0;
        int optionSize = 0;

        do {
            validate(buffer, BinaryFTraceHeaderElementSize.OPTION_TYPE);
            optionType = buffer.getNextShort();

            if (optionType > 0) {
                validate(buffer, BinaryFTraceHeaderElementSize.OPTION_SIZE);
                optionSize = buffer.getNextInt();

                optionTypes.add(optionType);
                optionData.add(buffer.getNextBytesAsString(optionSize).trim());
            }
        } while (optionType > 0);

        return extractOptionsSection(optionTypes, optionData);
    }

    private static int parseCPUCount(BinaryFTraceByteBuffer buffer) throws TmfTraceException {
        validate(buffer, BinaryFTraceHeaderElementSize.SMALL_SECTION_SIZE);
        return buffer.getNextInt();
    }

    private static String getUnsecureFileErrorMessage(long offset, long fileSize, long bytesToRead) {
        String errorMessage = UNSECURE_TRACE_ERROR_MESSAGE +
                ". Requested %d from %d, but file is %d bytes long."; //$NON-NLS-1$
        return String.format(errorMessage, bytesToRead, offset, fileSize);
    }
}
