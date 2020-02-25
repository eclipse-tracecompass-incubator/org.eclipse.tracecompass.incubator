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

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfProjectElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfProjectRegistry;

/**
 * Checks if a file is within a project with the tracing nature
 *
 * @author Geneviève Bastien
 */
public class InTracingProjectTester extends PropertyTester {

    @Override
    public boolean test(@Nullable Object receiver, @Nullable String property, Object @Nullable [] args, @Nullable Object expectedValue) {
        if (receiver instanceof IFile) {
            IFile file = (IFile) receiver;
            IProject project = file.getProject();
            TmfProjectElement projectElement = TmfProjectRegistry.getProject(project);
            /*
             * Check whether the projects are the same. If they are not,
             * then the tracing project is a shadow project of another
             * project (e.g. C project)
             */
            if ((projectElement != null) && project.equals(projectElement.getResource())) {
                return true;
            }
        }
        return false;
    }
}
