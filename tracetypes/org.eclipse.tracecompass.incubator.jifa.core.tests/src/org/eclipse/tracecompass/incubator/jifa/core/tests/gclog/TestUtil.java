/********************************************************************************
 * Copyright (c) 2022, 2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.eclipse.tracecompass.incubator.jifa.core.tests.gclog;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.Assert;

public class TestUtil {
    public static BufferedReader stringToBufferedReader(String source) {
        InputStream inputStream = new ByteArrayInputStream(source.getBytes());
        return new BufferedReader(new InputStreamReader(inputStream));
    }

    public static BufferedReader getGCLog(String name) throws FileNotFoundException {
        InputStream resourceAsStream = new FileInputStream(new File("src/org/eclipse/tracecompass/incubator/jifa/core/tests/gclog/res/" + name));
        InputStream is = Objects.requireNonNull(resourceAsStream);
        return new BufferedReader(new InputStreamReader(is));
    }

    public static List<String> generateShuffledGCLog(String name) {
        StringBuilder gclog = new StringBuilder("\n");
        try {
            BufferedReader bufferedReader = getGCLog(name);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                gclog.append(line);
                gclog.append('\n');
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        String regexForSplit = null;
        switch (name) {
        case "11CMSUpTime.log":
            regexForSplit = "(?=\n\\[[\\d-T:+.]+]\\[[0-9]+\\.[0-9]+s])";
            break;
        case "11G1Parser.log":
        case "11CMSGCParser.log":
        case "IncompleteGCLog.log":
            regexForSplit = "(?=\n\\[[0-9]+\\.[0-9]+s])";
            break;
        case "8CMSParser.log":
        case "8CMSPrintGC.log":
        case "8G1PrintGC.log":
        case "8ParallelGCParser.log":
            regexForSplit = "(?=\n[0-9]+\\.[0-9]+: \\[)";
            break;
        case "8G1GCParser.log":
        case "8G1GCParserAdaptiveSize.log":
        case "8ConcurrentPrintDateTimeStamp.log":
        case "8CMSCPUTime.log":
        case "8CMSPromotionFailed.log":
        case "8CMSScavengeBeforeRemark.log":
        case "8GenerationalGCInterleave.log":
            regexForSplit = "(?=\n[\\d-T:+.]+ \\d+\\.\\d+: \\[)";
            break;
        default:
            Assert.fail("can't find timestamp pattern for gc log " + name);
        }

        String originalLog = gclog.toString();
        List<String> shuffledLog = new ArrayList<>(Arrays.asList(originalLog.split(regexForSplit)));
        shuffledLog = shuffledLog.stream().map(str -> str.substring(1)).collect(Collectors.toList());
        Collections.shuffle(shuffledLog);
        return shuffledLog;
    }
}
