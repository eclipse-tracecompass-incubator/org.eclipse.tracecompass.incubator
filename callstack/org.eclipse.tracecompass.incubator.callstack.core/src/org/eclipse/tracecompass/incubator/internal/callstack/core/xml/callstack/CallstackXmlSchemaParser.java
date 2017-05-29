/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.core.xml.callstack;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.tmf.analysis.xml.core.module.ITmfXmlSchemaParser;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModuleHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Class that will parse the XML schema and extract the helpers for data-driven
 * callstack analyses
 *
 * @author Geneviève Bastien
 */
public class CallstackXmlSchemaParser implements ITmfXmlSchemaParser {

    @Override
    public Collection<? extends IAnalysisModuleHelper> getModuleHelpers(File xmlFile, Document doc) {
        List<IAnalysisModuleHelper> list = new ArrayList<>();
        NodeList callstackNodes = doc.getElementsByTagName(CallstackXmlStrings.CALLSTACK);
        for (int i = 0; i < callstackNodes.getLength(); i++) {
            Element node = NonNullUtils.checkNotNull((Element) callstackNodes.item(i));

            IAnalysisModuleHelper helper = new CallstackXmlModuleHelper(xmlFile, node);
            list.add(helper);
        }
        return list;
    }

}
