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

package org.eclipse.tracecompass.incubator.ftrace.core.binary.parser;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.event.BinaryFTraceConstants;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceCPUDataPage;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceCPUDataPage.BinaryFTraceCPUDataPageBuilder;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceEventFormat;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceEventSystem;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceFileCPU;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceFileType;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceFormatField;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceFunctionAddressNameMapping;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceFunctionType;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceHeaderEvent;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceHeaderEvent.BinaryFTraceHeaderEventBuilder;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceHeaderInfo;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceHeaderInfo.BinaryFTraceHeaderInfoBuilder;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceOption;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceVersionHeader;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;

/**
 * A binary parser to parse binary FTrace files.
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceFileParser {
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
        try (BinaryFTraceByteBuffer buffer = new BinaryFTraceByteBuffer(path);) {
            return getMagicValuesAndFtraceVersion(buffer);
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
            BinaryFTraceVersionHeader versionHeader = getMagicValuesAndFtraceVersion(buffer);
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

    private static BinaryFTraceVersionHeader getMagicValuesAndFtraceVersion(BinaryFTraceByteBuffer buffer) throws TmfTraceException {
        try {
            byte[] bytes = buffer.getNextBytes(10);
            int ftraceVersion = Integer.parseInt(buffer.getNextString().trim());
            return new BinaryFTraceVersionHeader(bytes, ftraceVersion);
        } catch (IOException e) {
            throw new TmfTraceException("Cannot parse the magic values and FTrace version. Make sure you use trace-cmd v.2.9 and above.", e); //$NON-NLS-1$
        }
    }

    private static ByteOrder getFileEndianess(BinaryFTraceByteBuffer buffer) throws IOException {
        int endianess = buffer.getNextBytes(1)[0];
        ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
        if (endianess == 0) {
            byteOrder = ByteOrder.LITTLE_ENDIAN;
        }
        buffer.setByteOrder(byteOrder);

        return byteOrder;
    }

    private static int getLongValueSize(BinaryFTraceByteBuffer buffer) throws IOException {
        return buffer.getNextBytes(1)[0];
    }

    private static int getHostPageSize(BinaryFTraceByteBuffer buffer) throws IOException {
        return buffer.getNextInt();
    }

    private static List<BinaryFTraceFormatField> parseHeaderPage(BinaryFTraceByteBuffer buffer) throws IOException {
        buffer.getNextBytesAsString(12); // Skipping the section name
        long headerPageSize = buffer.getNextLong();
        String headerPageContent = buffer.getNextBytesAsString(headerPageSize);
        String[] strFields = headerPageContent.split(BinaryFTraceConstants.NEW_LINE);
        return BinaryFTraceParsingUtils.parseEventFormatFields(strFields);
    }

    private static BinaryFTraceHeaderEvent parseHeaderEvent(BinaryFTraceByteBuffer buffer) throws IOException {
        buffer.getNextBytesAsString(13); // Skipping the section header

        BinaryFTraceHeaderEventBuilder builder = new BinaryFTraceHeaderEventBuilder();
        long headerEventSize = buffer.getNextLong();
        String strHeaderEventInfo = buffer.getNextBytesAsString(headerEventSize);

        String[] arrInfo = strHeaderEventInfo.split(BinaryFTraceConstants.NEW_LINE);

        for (String info : arrInfo) {
            if (info.contains(BinaryFTraceConstants.HEADER_EVENT_TYPE_LENGTH_VALUE_SEPARATOR)) {
                String[] keyValuePair = info.split(BinaryFTraceConstants.HEADER_EVENT_TYPE_LENGTH_VALUE_SEPARATOR);
                String key = keyValuePair[0];
                int value = Integer.parseInt(keyValuePair[1].trim());

                if (key.contains(BinaryFTraceConstants.HEADER_EVENT_INFO_PADDING_LABEL)) {
                    builder.paddingTypeLen(value);
                } else if (key.contains(BinaryFTraceConstants.HEADER_EVENT_INFO_EXTENDED_TIMESTAMP_LABEL)) {
                    builder.timeExtendedTypeLen(value);
                } else if (key.contains(BinaryFTraceConstants.HEADER_EVENT_INFO_TIMESTAMP_LABEL)) {
                    builder.timeStampTypeLen(value);
                } else if (key.contains(BinaryFTraceConstants.HEADER_EVENT_INFO_DATA_MAX_TYPE_LENGTH_LABEL)) {
                    builder.dataMaxTypeLen(value);
                }
            } else if (info.contains(BinaryFTraceConstants.HEADER_EVENT_LABEL_VALUE_SEPARATOR)) {
                String[] keyValuePair = info.split(BinaryFTraceConstants.HEADER_EVENT_LABEL_VALUE_SEPARATOR);
                String key = keyValuePair[0].trim();
                String value = keyValuePair[1].trim();
                int numOfBits = Integer.parseInt(value.split(BinaryFTraceConstants.HEADER_EVENT_BIT_VALUE_UNIT_SEPARATOR)[0]);
                if (key.contains(BinaryFTraceConstants.HEADER_EVENT_INFO_TYPE_LENGTH_SIZE_LABEL)) {
                    builder.typeLen(numOfBits);
                } else if (key.contains(BinaryFTraceConstants.HEADER_EVENT_INFO_TIMESTAMP_SIZE_LABEL)) {
                    builder.timeDelta(numOfBits);
                }
            }
        }

        return builder.build();
    }

    private static Map<Integer, BinaryFTraceEventFormat> parseTraceEventsFormat(BinaryFTraceByteBuffer buffer) throws IOException {
        Map<Integer, BinaryFTraceEventFormat> lstEventFormats = new HashMap<>();

        int numOfTraceEventFormats = buffer.getNextInt();

        for (int i = 0; i < numOfTraceEventFormats; i++) {
            long formatFileSize = buffer.getNextLong();
            String strEventFormat = buffer.getNextBytesAsString(formatFileSize);
            BinaryFTraceEventFormat eventFormat = BinaryFTraceParsingUtils.parseEventFormat(strEventFormat);
            lstEventFormats.put(eventFormat.getEventFormatID(), eventFormat);
        }

        return lstEventFormats;
    }

    private static List<BinaryFTraceEventSystem> parseEventSystemsAndFormats(BinaryFTraceByteBuffer buffer) throws IOException {
        List<BinaryFTraceEventSystem> lstEventSystems = new ArrayList<>();
        int numOfEventSystems = buffer.getNextInt();

        for (int i = 0; i < numOfEventSystems; i++) {
            String eventSystemName = buffer.getNextString();
            int numOfEvents = buffer.getNextInt();

            Map<Integer, BinaryFTraceEventFormat> lstEventFormat = new HashMap<>();
            for (int j = 0; j < numOfEvents; j++) {
                long fileSize = buffer.getNextLong();
                String strEventFormat = buffer.getNextBytesAsString(fileSize);
                BinaryFTraceEventFormat eventFormat = BinaryFTraceParsingUtils.parseEventFormat(strEventFormat);
                lstEventFormat.put(eventFormat.getEventFormatID(), eventFormat);
            }

            BinaryFTraceEventSystem eventSystem = new BinaryFTraceEventSystem(eventSystemName, lstEventFormat);
            lstEventSystems.add(eventSystem);
        }

        return lstEventSystems;
    }

    private static Map<String, BinaryFTraceFunctionAddressNameMapping> parseFunctionMapping(BinaryFTraceByteBuffer buffer) throws IOException {
        Map<String, BinaryFTraceFunctionAddressNameMapping> hashMap = new HashMap<>();
        int dataSize = buffer.getNextInt();
        String strMappings = buffer.getNextBytesAsString(dataSize);
        String[] mappings = strMappings.split(BinaryFTraceConstants.NEW_LINE);

        for (String mapping : mappings) {
            String[] values = mapping.split(BinaryFTraceConstants.FUNCTION_ADDRESS_NAME_SEPARATOR);

            BinaryFTraceFunctionAddressNameMapping objMapping = new BinaryFTraceFunctionAddressNameMapping(
                    values[0], // Function memory address
                    BinaryFTraceFunctionType.UNKNOWN, // Temporary value, will
                                                      // be improve in the
                                                      // future
                    values[2]); // Function name
            hashMap.put(values[0], objMapping);
        }

        return hashMap;
    }

    private static Map<String, String> parseTracePrintKInfo(BinaryFTraceByteBuffer buffer) throws IOException {
        Map<String, String> addressStringMapping = new HashMap<>();
        int dataSize = buffer.getNextInt();
        String strMappings = buffer.getNextBytesAsString(dataSize);
        String[] arrMappings = strMappings.split(BinaryFTraceConstants.NEW_LINE);

        for (String mapping : arrMappings) {
            String[] keyValuePair = mapping.split(BinaryFTraceConstants.HEADER_EVENT_LABEL_VALUE_SEPARATOR);
            String address = keyValuePair[0].trim();
            keyValuePair[1] = keyValuePair[1].trim();
            String mappedString = keyValuePair[1].substring(1, keyValuePair[1].length() - 1); // Strip
                                                                                              // the
                                                                                              // quotation
                                                                                              // mark
            addressStringMapping.put(address, mappedString);
        }

        return addressStringMapping;
    }

    private static Map<Integer, String> parseProcessToFunctionNameMapping(BinaryFTraceByteBuffer buffer) throws IOException {
        // Max value of PID is 2^22 for 64 bits system so theoretically an
        // integer key
        // is enough
        Map<Integer, String> hashMap = new HashMap<>();

        long dataSize = buffer.getNextLong();
        String strMappings = buffer.getNextBytesAsString(dataSize);

        if (!strMappings.trim().isEmpty()) {
            String[] arrMappings = strMappings.split(BinaryFTraceConstants.NEW_LINE);
            for (String mapping : arrMappings) {
                String[] keyValuePair = mapping.split(BinaryFTraceConstants.FUNCTION_ADDRESS_NAME_SEPARATOR);
                hashMap.put(Integer.parseInt(keyValuePair[0]), keyValuePair[1]);
            }
        }

        return hashMap;
    }

    private static int parseCPUCount(BinaryFTraceByteBuffer buffer) throws IOException {
        return buffer.getNextInt();
    }

    private static List<BinaryFTraceFileCPU> parseFlyRecordSection(BinaryFTraceByteBuffer buffer, int cpuCount, int pageSize) throws IOException {
        List<BinaryFTraceFileCPU> lstCPU = new ArrayList<>();

        // Obtain the starting offset and the size of each CPU section
        long[] cpuSectionStartingOffset = new long[cpuCount];
        long[] cpuSectionSize = new long[cpuCount];
        for (int i = 0; i < cpuCount; i++) {
            cpuSectionStartingOffset[i] = buffer.getNextLong();
            cpuSectionSize[i] = buffer.getNextLong();
        }

        for (int i = 0; i < cpuCount; i++) {
            // First look for the starting offset and size of the CPU data
            // section (in bytes).
            long sectionStartingOffset = cpuSectionStartingOffset[i];
            long sectionSize = cpuSectionSize[i];
            int cpuNumber = i;

            /*
             * Look for the last page in the section first to prevent modifying
             * existing pages.
             */
            long pageStartingOffset = sectionStartingOffset + sectionSize - pageSize;
            BinaryFTraceCPUDataPage nextPage = null;
            BinaryFTraceCPUDataPageBuilder pageBuilder = new BinaryFTraceCPUDataPageBuilder();

            List<BinaryFTraceCPUDataPage> lstPages = new ArrayList<>();
            while (pageStartingOffset >= sectionStartingOffset) {
                buffer.movePointerToOffset(pageStartingOffset); // Move the
                                                                // pointer the
                                                                // the page
                                                                // starting
                                                                // offset
                long timestamp = buffer.getNextLong();
                long flags = buffer.getNextLong();
                long pageDataStartingOffset = pageStartingOffset + 16; // Can be
                                                                       // improved

                BinaryFTraceCPUDataPage currentPage = pageBuilder.pageStartingOffset(pageStartingOffset)
                        .pageDataStartingOffset(pageDataStartingOffset)
                        .timeStamp(timestamp)
                        .flags(flags)
                        .cpu(cpuNumber)
                        .nextPage(nextPage)
                        .size(pageSize)
                        .build();

                lstPages.add(0, currentPage);
                nextPage = currentPage;

                pageStartingOffset = pageStartingOffset - pageSize;
            }

            // This should be tested when there are multiple CPUs because
            // data is paged
            BinaryFTraceFileCPU cpu = new BinaryFTraceFileCPU(sectionStartingOffset, sectionSize, cpuNumber, lstPages);

            // Parse various CPUs information
            lstCPU.add(cpu);
        }

        return lstCPU;
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
     *             if an error occured while reading the option strings
     */
    private static List<BinaryFTraceOption> parseOptionsSection(BinaryFTraceByteBuffer buffer) throws IOException {
        List<BinaryFTraceOption> lstOptions = new ArrayList<>();

        short optionType = buffer.getNextShort();
        while (optionType > 0) {
            int optionSize = buffer.getNextInt();
            String optionData = buffer.getNextBytesAsString(optionSize).trim();

            BinaryFTraceOption newOption = new BinaryFTraceOption(optionType, optionData);
            lstOptions.add(newOption);

            // Read the next option
            optionType = buffer.getNextShort();
        }

        return lstOptions;
    }

}
