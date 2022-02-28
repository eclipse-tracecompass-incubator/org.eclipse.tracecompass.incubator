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
    /**
     * Constructor
     */
    private BinaryFTraceFileParser() {
        // Do nothing
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
        try (BinaryFTraceByteBuffer buffer = new BinaryFTraceByteBuffer(path)) {
            return getFtraceVersionHeader(buffer);
        } catch (IOException e) {
            throw new TmfTraceException("Cannot open trace file", e); //$NON-NLS-1$
        }
    }

    private static BinaryFTraceVersionHeader getFtraceVersionHeader(BinaryFTraceByteBuffer buffer) throws TmfTraceException {
        try {
            byte[] bytes = buffer.getNextBytes(10);
            String strVersion = buffer.getNextString().trim();
            return getMagicValuesAndFtraceVersion(bytes, strVersion);
        } catch (IOException e) {
            throw new TmfTraceException("FTrace Version Header not readible", e); //$NON-NLS-1$
        }
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
        try (BinaryFTraceByteBuffer buffer = new BinaryFTraceByteBuffer(path)) {

            BinaryFTraceHeaderInfoBuilder builder = new BinaryFTraceHeaderInfoBuilder();
            builder.filePath(path);

            // Parse initial data section
            BinaryFTraceVersionHeader versionHeader = getFtraceVersionHeader(buffer);
            builder.version(versionHeader.getFTraceVersion());

            ByteOrder endianess = getFileEndianess(buffer);
            builder.endianess(endianess);
            buffer.setByteOrder(endianess);

            // Now all numbers are in file endianess
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
            return builder.build();
        } catch (IOException ex) {
            throw new TmfTraceException(ex.getMessage(), ex);
        }
    }

    private static List<BinaryFTraceFormatField> parseHeaderPage(BinaryFTraceByteBuffer buffer) throws IOException {
        buffer.getNextBytesAsString(12); // Skipping the section name
        long headerPageSize = buffer.getNextLong();
        String headerPageContent = buffer.getNextBytesAsString(headerPageSize);
        return extractHeaderPageContent(headerPageContent);
    }

    private static BinaryFTraceHeaderEvent parseHeaderEvent(BinaryFTraceByteBuffer buffer) throws IOException {
        buffer.getNextBytesAsString(13); // Skipping the section header

        long headerEventSize = buffer.getNextLong();
        String strHeaderEventInfo = buffer.getNextBytesAsString(headerEventSize);

        return extractHeaderEventContent(strHeaderEventInfo);
    }

    private static Map<Integer, BinaryFTraceEventFormat> parseTraceEventsFormat(BinaryFTraceByteBuffer buffer) throws IOException {
        ArrayList<String> eventFormats = new ArrayList<>();

        int numOfTraceEventFormats = buffer.getNextInt();

        for (int i = 0; i < numOfTraceEventFormats; i++) {
            long formatSize = buffer.getNextLong();
            eventFormats.add(buffer.getNextBytesAsString(formatSize));
        }

        return extractTraceEventsFormat(eventFormats);
    }

    private static List<BinaryFTraceEventSystem> parseEventSystemsAndFormats(BinaryFTraceByteBuffer buffer) throws IOException {
        HashMap<String, List<String>> eventSystemData = new HashMap<>();

        int numOfEventSystems = buffer.getNextInt();

        for (int i = 0; i < numOfEventSystems; i++) {
            String eventSystemName = buffer.getNextString();
            int numOfEvents = buffer.getNextInt();

            ArrayList<String> lstEventFormat = new ArrayList<>();
            for (int j = 0; j < numOfEvents; j++) {
                long fileSize = buffer.getNextLong();
                lstEventFormat.add(buffer.getNextBytesAsString(fileSize));
            }

            eventSystemData.put(eventSystemName, lstEventFormat);
        }

        return extractEventSystemsAndFormats(eventSystemData);
    }

    private static Map<String, BinaryFTraceFunctionAddressNameMapping> parseFunctionMapping(BinaryFTraceByteBuffer buffer) throws IOException {
        int dataSize = buffer.getNextInt();
        String strMappings = buffer.getNextBytesAsString(dataSize);

        return extractFunctionMappingContent(strMappings);
    }

    private static Map<String, String> parseTracePrintKInfo(BinaryFTraceByteBuffer buffer) throws IOException {
        int dataSize = buffer.getNextInt();
        String strMappings = buffer.getNextBytesAsString(dataSize);
        return extractPrintKContent(strMappings);
    }

    private static Map<Integer, String> parseProcessToFunctionNameMapping(BinaryFTraceByteBuffer buffer) throws IOException {
        long dataSize = buffer.getNextLong();
        String strMappings = buffer.getNextBytesAsString(dataSize);

        return extractFunctionNameMapping(strMappings);
    }

    private static List<BinaryFTraceFileCPU> parseFlyRecordSection(BinaryFTraceByteBuffer buffer, int cpuCount, int pageSize) throws IOException {
        // Obtain the starting offset and the size of each CPU section
        long[] cpuSectionStartingOffset = new long[cpuCount];
        long[] cpuSectionSize = new long[cpuCount];
        for (int i = 0; i < cpuCount; i++) {
            cpuSectionStartingOffset[i] = buffer.getNextLong();
            cpuSectionSize[i] = buffer.getNextLong();
        }

        return parseCPUPageHeader(buffer, cpuSectionStartingOffset, cpuSectionSize, pageSize);
    }

    private static List<BinaryFTraceFileCPU> parseCPUPageHeader(BinaryFTraceByteBuffer buffer, long[] cpuSectionStartingOffset, long[] cpuSectionSize, int pageSize) throws IOException {
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

    /**
     * Parse the options section of a binary ftrace trace. The option section
     * are just some metadata to provide additional information about the trace.
     * The information are just regular text strings and is independent from the
     * parsing of the trace.
     *
     * @param buffer
     *            The buffer that currently reading the trace file
     * @return A list of {@link BinaryFTraceOption} that contains the option
     *         strings
     * @throws IOException
     *             if an error occurred while reading the option strings
     */
    private static List<BinaryFTraceOption> parseOptionsSection(BinaryFTraceByteBuffer buffer) throws IOException {
        ArrayList<Short> optionTypes = new ArrayList<>();
        ArrayList<String> optionData = new ArrayList<>();

        short optionType = 0;
        int optionSize = 0;

        do {
            optionType = buffer.getNextShort();

            if (optionType > 0) {
                optionSize = buffer.getNextInt();

                optionTypes.add(optionType);
                optionData.add(buffer.getNextBytesAsString(optionSize).trim());
            }
        } while (optionType > 0);

        return extractOptionsSection(optionTypes, optionData);
    }

    private static int parseCPUCount(BinaryFTraceByteBuffer buffer) throws IOException {
        return buffer.getNextInt();
    }
}
