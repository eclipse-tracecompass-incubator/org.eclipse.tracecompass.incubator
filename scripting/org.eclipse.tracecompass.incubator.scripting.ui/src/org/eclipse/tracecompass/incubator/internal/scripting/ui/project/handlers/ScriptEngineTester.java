/*******************************************************************************
 * Copyright (c) 2019 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.scripting.ui.project.handlers;

import java.util.Iterator;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.ease.ui.propertytester.EngineTester;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceElement;

/**
 * Checks if a script has as an engine for installed.
 *
 * @author Bernd Hufmann
 *
 */
@SuppressWarnings("restriction")
public class ScriptEngineTester extends PropertyTester {
    // Engine tester from EASE project
    private EngineTester fDelegateTester = new EngineTester();

    @Override
    public boolean test(@Nullable Object receiver, @Nullable String property, Object @Nullable [] args, @Nullable Object expectedValue) {
        if (receiver instanceof TreeSelection) {
            TreeSelection selection = (TreeSelection) receiver;
            Iterator<Object> iterator = selection.iterator();
            while (iterator.hasNext()) {
                Object element = iterator.next();
                if (element instanceof TmfTraceElement) {
                    IResource resource = ((TmfTraceElement) element).getResource();
                    if (resource instanceof IFile) {
                        return fDelegateTester.test(resource, property, args, expectedValue);
                    }
                } else if (element instanceof IFile) {
                    return fDelegateTester.test(element, property, args, expectedValue);
                }
                break;
            }
        }
        return false;
    }
}
