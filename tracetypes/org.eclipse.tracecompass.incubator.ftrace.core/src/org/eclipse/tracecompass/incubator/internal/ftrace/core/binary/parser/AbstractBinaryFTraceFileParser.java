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

import static org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.event.BinaryFTraceConstants.FUNCTION_ADDRESS_NAME_SEPARATOR;
import static org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.event.BinaryFTraceConstants.HEADER_EVENT_BIT_VALUE_UNIT_SEPARATOR;
import static org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.event.BinaryFTraceConstants.HEADER_EVENT_INFO_DATA_MAX_TYPE_LENGTH_LABEL;
import static org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.event.BinaryFTraceConstants.HEADER_EVENT_INFO_EXTENDED_TIMESTAMP_LABEL;
import static org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.event.BinaryFTraceConstants.HEADER_EVENT_INFO_PADDING_LABEL;
import static org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.event.BinaryFTraceConstants.HEADER_EVENT_INFO_TIMESTAMP_LABEL;
import static org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.event.BinaryFTraceConstants.HEADER_EVENT_INFO_TIMESTAMP_SIZE_LABEL;
import static org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.event.BinaryFTraceConstants.HEADER_EVENT_INFO_TYPE_LENGTH_SIZE_LABEL;
import static org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.event.BinaryFTraceConstants.HEADER_EVENT_LABEL_VALUE_SEPARATOR;
import static org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.event.BinaryFTraceConstants.HEADER_EVENT_TYPE_LENGTH_VALUE_SEPARATOR;
import static org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.event.BinaryFTraceConstants.NEW_LINE;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceCPUDataPage;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceCPUDataPage.BinaryFTraceCPUDataPageBuilder;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceEventFormat;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceEventSystem;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceFileCPU;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceFormatField;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceFunctionAddressNameMapping;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceFunctionType;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceHeaderEvent;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceHeaderEvent.BinaryFTraceHeaderEventBuilder;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceOption;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceVersion;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceVersionHeader;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;

/**
 * A base class for Binary FTrace Parsers. It provides functions that parses
 * each section of the file.
 *
 * @author Hoang Thuan Pham
 */
public abstract class AbstractBinaryFTraceFileParser {
    /**
     * Constructor
     */
    protected AbstractBinaryFTraceFileParser() {
        // Do nothing
    }

    /**
     * Get the magic values and the ftrace version from the binary FTrace header
     *
     * @param magicValues
     *            A byte array that contains the magic values
     * @param strVersion
     *            The FTrace version as a string
     *
     * @return A {@link BinaryFTraceVersionHeader} containing the parsed
     *         information of the version header
     * @throws TmfTraceException
     *             If an error occurred while parsing the version header
     */
    protected static BinaryFTraceVersionHeader getMagicValuesAndFtraceVersion(byte[] magicValues, String strVersion) throws TmfTraceException {
        try {
            int ftraceVersionInt = Integer.parseInt(strVersion.trim());
            BinaryFTraceVersion ftraceVersionEnum = BinaryFTraceVersion.getVersionAsEnum(ftraceVersionInt);
            return new BinaryFTraceVersionHeader(magicValues, ftraceVersionEnum);
        } catch (NumberFormatException e) {
            throw new TmfTraceException("Cannot parse the magic values and FTrace version. Make sure you use trace-cmd v.2.9 and above. strVersion=" + strVersion, e); //$NON-NLS-1$
        }
    }

    /**
     * Parse the endianess of the trace file
     *
     * @param buffer
     *            The buffer that is currently pointing to the endianess value
     *            of the trace file
     * @return The endianess of the file as a {@link ByteOrder} value
     */
    protected static ByteOrder getFileEndianess(BinaryFTraceByteBuffer buffer) {
        int endianess = buffer.getNextBytes(1)[0];
        ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
        if (endianess == 0) {
            byteOrder = ByteOrder.LITTLE_ENDIAN;
        }

        return byteOrder;
    }

    /**
     * Parse the long value size of the binary FTrace file
     *
     * @param buffer
     *            The buffer that is currently pointing to the long value size
     *            of the trace file
     * @return The long value size of the trace file (either 4 or 8)
     * @throws TmfTraceException
     *             If an error occur while validating the long value size
     */
    protected static int getLongValueSize(BinaryFTraceByteBuffer buffer) throws TmfTraceException {
        int longValueSize = buffer.getNextBytes(1)[0];

        if (longValueSize != 4 && longValueSize != 8) {
            throw new TmfTraceException("Invalid size for long value, must be either 4 or 8 bytes, got " + longValueSize); //$NON-NLS-1$
        }

        return longValueSize;
    }

    /**
     * Get the host page size in bytes
     *
     * @param buffer
     *            The buffer that is currently pointing to the host page size
     *            value in the trace file
     * @return The host page size value of the trace file
     */
    protected static int getHostPageSize(BinaryFTraceByteBuffer buffer) {
        return buffer.getNextInt();
    }

    /**
     * Extract information from the Header Page content section.
     *
     * @param content
     *            A string that contains the information of the Header Page
     *            content section
     * @return A list of {@link BinaryFTraceFormatField} that contains the
     *         parsed information
     */
    protected static List<BinaryFTraceFormatField> extractHeaderPageContent(String content) {
        String[] strFields = content.split(NEW_LINE);
        return BinaryFTraceParsingUtils.parseEventFormatFields(strFields);
    }

    /**
     * Extract information from the Header Event section.
     *
     * @param content
     *            A string that contains the information of the Header Event
     *            content section
     * @return The parsed information of the Header Event section
     */
    protected static BinaryFTraceHeaderEvent extractHeaderEventContent(String content) {
        BinaryFTraceHeaderEventBuilder builder = new BinaryFTraceHeaderEventBuilder();

        String[] arrInfo = content.split(NEW_LINE);

        for (String info : arrInfo) {
            if (info.contains(HEADER_EVENT_TYPE_LENGTH_VALUE_SEPARATOR)) {
                String[] keyValuePair = info.split(HEADER_EVENT_TYPE_LENGTH_VALUE_SEPARATOR);
                String key = keyValuePair[0];
                int value = Integer.parseInt(keyValuePair[1].trim());

                if (key.contains(HEADER_EVENT_INFO_PADDING_LABEL)) {
                    builder.paddingTypeLen(value);
                } else if (key.contains(HEADER_EVENT_INFO_EXTENDED_TIMESTAMP_LABEL)) {
                    builder.timeExtendedTypeLen(value);
                } else if (key.contains(HEADER_EVENT_INFO_TIMESTAMP_LABEL)) {
                    builder.timeStampTypeLen(value);
                } else if (key.contains(HEADER_EVENT_INFO_DATA_MAX_TYPE_LENGTH_LABEL)) {
                    builder.dataMaxTypeLen(value);
                }
            } else if (info.contains(HEADER_EVENT_LABEL_VALUE_SEPARATOR)) {
                String[] keyValuePair = info.split(HEADER_EVENT_LABEL_VALUE_SEPARATOR);
                String key = keyValuePair[0].trim();
                String value = keyValuePair[1].trim();
                int numOfBits = Integer.parseInt(value.split(HEADER_EVENT_BIT_VALUE_UNIT_SEPARATOR)[0]);

                if (key.contains(HEADER_EVENT_INFO_TYPE_LENGTH_SIZE_LABEL)) {
                    builder.typeLen(numOfBits);
                } else if (key.contains(HEADER_EVENT_INFO_TIMESTAMP_SIZE_LABEL)) {
                    builder.timeDelta(numOfBits);
                }
            }
        }

        return builder.build();
    }

    /**
     * Extract function address to function name mapping from the KALLSYMS
     * section.
     *
     * @param content
     *            The string that represents the content of the KALLSYMS section
     * @return A Map that contains the mapped function address-function name(s).
     *         The key is the function address as a string.
     */
    protected static Map<String, BinaryFTraceFunctionAddressNameMapping> extractFunctionMappingContent(String content) {
        Map<String, BinaryFTraceFunctionAddressNameMapping> hashMap = new HashMap<>();
        String[] mappings = content.split(NEW_LINE);

        for (String mapping : mappings) {
            String[] values = mapping.split(FUNCTION_ADDRESS_NAME_SEPARATOR);

            String functionAddress = values[0];
            String functionName =  values[2];
            BinaryFTraceFunctionAddressNameMapping objMapping = new BinaryFTraceFunctionAddressNameMapping(
                    functionAddress,
                    // Temporary value, will be improved in the future
                    BinaryFTraceFunctionType.UNKNOWN,
                    functionName);

            hashMap.put(functionAddress, objMapping);
        }

        return hashMap;
    }

    /**
     * Extract printk format from the print_k section
     *
     * @param content
     *            The string that represents the content of the print_k section
     * @return A map that contains the print_k mappings
     */
    protected static Map<String, String> extractPrintKContent(String content) {
        Map<String, String> addressStringMapping = new HashMap<>();
        String[] arrMappings = content.split(NEW_LINE);

        for (String mapping : arrMappings) {
            String[] keyValuePair = mapping.split(HEADER_EVENT_LABEL_VALUE_SEPARATOR);
            String address = keyValuePair[0].trim();
            keyValuePair[1] = keyValuePair[1].trim();
            // Strip the quotation mark
            String mappedString = keyValuePair[1].substring(1, keyValuePair[1].length() - 1);
            addressStringMapping.put(address, mappedString);
        }

        return addressStringMapping;
    }

    /**
     * Extract printk format from the print_k section
     *
     * @param content
     *            The string that represents the content of the print_k section
     * @return A map that contains the print_k mappings
     **/
    protected static Map<Integer, String> extractFunctionNameMapping(String content) {
        /*
         * Max value of PID is 2^22 for 64 bits system so theoretically an
         * integer key is enough
         */
        Map<Integer, String> hashMap = new HashMap<>();

        if (!content.trim().isEmpty()) {
            String[] arrMappings = content.trim().split(NEW_LINE);
            for (String mapping : arrMappings) {
                String[] keyValuePair = mapping.split(FUNCTION_ADDRESS_NAME_SEPARATOR);
                hashMap.put(Integer.parseInt(keyValuePair[0]), keyValuePair[1]);
            }
        }

        return hashMap;
    }

    /**
     * Parse the options section of a binary ftrace trace. The option section
     * are just some metadata to provide additional information about the trace.
     * The information are just regular text strings and is independent from the
     * parsing of the trace
     *
     * @param optionTypes
     *            A list that contains the option types. The option order and
     *            length of this array should match the the optionData list
     * @param optionData
     *            A list that contains the data for each option. The option
     *            order and length of this array should match the optionTypes
     *            list
     *
     * @return A list of {@link BinaryFTraceOption} that contains the option
     *         strings
     */
    protected static List<BinaryFTraceOption> extractOptionsSection(List<Short> optionTypes, List<String> optionData) {
        List<BinaryFTraceOption> listOptions = new ArrayList<>();

        if (optionTypes.size() != optionData.size()) {
            return listOptions;
        }

        for (int i = 0; i < optionTypes.size(); i++) {
            BinaryFTraceOption newOption = new BinaryFTraceOption(optionTypes.get(i), optionData.get(i));
            listOptions.add(newOption);
        }

        return listOptions;
    }

    /**
     * Convert a String list of event formats to a map of
     * {@link BinaryFTraceEventFormat}
     *
     * @param eventFormatData
     *            A list of String that contains the raw event format
     * @return A Map of BinaryFTraceEventFormats that represent the processed
     *         event formats
     */
    protected static Map<Integer, BinaryFTraceEventFormat> extractTraceEventsFormat(List<String> eventFormatData) {
        Map<Integer, BinaryFTraceEventFormat> listEventFormats = new HashMap<>();

        for (int i = 0; i < eventFormatData.size(); i++) {
            BinaryFTraceEventFormat eventFormat = BinaryFTraceParsingUtils.parseEventFormat(eventFormatData.get(i));
            listEventFormats.put(eventFormat.getEventFormatID(), eventFormat);
        }

        return listEventFormats;
    }

    /**
     * Construct a list of {@link BinaryFTraceEventSystem} that represents event
     * systems in the header of a binary FTrace trace
     *
     * @param data
     *            A Map that contains the names of different event systems as
     *            the key, and the raw event formats under those systems as the
     *            value.
     * @return A list of {@link BinaryFTraceEventSystem} that represents the
     *         processed information
     */
    protected static List<BinaryFTraceEventSystem> extractEventSystemsAndFormats(Map<String, List<String>> data) {
        List<BinaryFTraceEventSystem> listEventSystems = new ArrayList<>();

        for (Entry<String, List<String>> systemData : data.entrySet()) {
            String systemName = systemData.getKey();
            Map<Integer, BinaryFTraceEventFormat> listEventFormat = extractTraceEventsFormat(systemData.getValue());
            BinaryFTraceEventSystem eventSystem = new BinaryFTraceEventSystem(systemName, listEventFormat);
            listEventSystems.add(eventSystem);
        }

        return listEventSystems;
    }

    /**
     * Parse the event format from a string that represent the event format
     *
     * @param content
     *            The string that contains the event format
     * @return A {@link BinaryFTraceEventFormat} that represents the parsed
     *         event
     */
    protected static BinaryFTraceEventFormat parseEventFormat(String content) {
        return BinaryFTraceParsingUtils.parseEventFormat(content);
    }

    /**
     * Initialize a list of pages of the CPU with the same properties as the
     * parameters.
     *
     * @param sectionStartingOffset
     *            The starting offset of the CPU section
     * @param listTimeStamp
     *            A list containing the timestamp of pages
     * @param listFlag
     *            A list containing the flags of pages
     * @param sectionSize
     *            The size of the CPU section
     * @param pageSize
     *            The page size
     * @param cpuNumber
     *            The CPU number
     * @return A list that contains all the pages of a CPU section identified by
     *         the parameter cpuNumber
     */
    protected static List<BinaryFTraceCPUDataPage> initializePages(List<Long> listTimeStamp, List<Long> listFlag, long sectionStartingOffset, long sectionSize, int pageSize, int cpuNumber) {
        int pageCount = listTimeStamp.size();

        BinaryFTraceCPUDataPage nextPage = null;
        BinaryFTraceCPUDataPageBuilder pageBuilder = new BinaryFTraceCPUDataPageBuilder();
        List<BinaryFTraceCPUDataPage> listPages = new ArrayList<>();
        long pageStartingOffset = sectionStartingOffset + sectionSize - pageSize;

        /*
         * Move backwards from the last page to avoid modifying the page object
         * to change the pointer to the next page
         */
        for (int i = (pageCount - 1); i >= 0; i--) {
            long timestamp = listTimeStamp.get(i);
            long flags = listFlag.get(i);
            long dataStartingOffset = pageStartingOffset + BinaryFTraceHeaderElementSize.PAGE_HEADER_SIZE;

            // Build the page
            BinaryFTraceCPUDataPage currentPage = pageBuilder.pageStartingOffset(pageStartingOffset)
                    .pageDataStartingOffset(dataStartingOffset)
                    .timeStamp(timestamp)
                    .flags(flags)
                    .cpu(cpuNumber)
                    .nextPage(nextPage)
                    .size(pageSize)
                    .build();

            // Add the page to the list and set the next page
            listPages.add(0, currentPage);
            nextPage = currentPage;
            pageStartingOffset = pageStartingOffset - pageSize;
        }

        return listPages;
    }

    /**
     * Initialize the CPU sections of a binary FTrace trace
     *
     * @param mapTimeStamp
     *            The timestamps of each CPU page of each CPU section. The key
     *            of the map is the CPU number
     * @param mapFlag
     *            The flag of each CPU page of each CPU section. The key of the
     *            map is the CPU number
     * @param listStartingOffset
     *            The offset of each CPU section. The index of the list is the
     *            CPU number
     * @param listSectionSize
     *            The size of each CPU section. The index of the list is the CPU
     *            number
     * @param pageSize
     *            The page size of the trace
     * @return A list of {@link BinaryFTraceFileCPU} representing the processed
     *         data
     */
    protected static List<BinaryFTraceFileCPU> initializeCPUs(Map<Integer, List<Long>> mapTimeStamp, Map<Integer, List<Long>> mapFlag, long[] listStartingOffset, long[] listSectionSize, int pageSize) {
        List<BinaryFTraceFileCPU> listCPU = new ArrayList<>();

        int cpuCount = listStartingOffset.length;

        for (int cpuNumber = 0; cpuNumber < cpuCount; cpuNumber++) {
            List<BinaryFTraceCPUDataPage> listPages = initializePages(mapTimeStamp.get(cpuNumber), mapFlag.get(cpuNumber), listStartingOffset[cpuNumber], listSectionSize[cpuNumber], pageSize, cpuNumber);
            BinaryFTraceFileCPU cpu = new BinaryFTraceFileCPU(listStartingOffset[cpuNumber], listSectionSize[cpuNumber], cpuNumber, listPages);
            listCPU.add(cpu);
        }

        return listCPU;
    }
}
