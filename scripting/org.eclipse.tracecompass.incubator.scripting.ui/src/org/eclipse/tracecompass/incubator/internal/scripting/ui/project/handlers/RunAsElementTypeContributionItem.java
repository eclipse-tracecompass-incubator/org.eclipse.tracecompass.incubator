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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.tracecompass.tmf.core.util.Pair;

/**
 * ContributionItem for the Run As -> <launch shortcut>.
 *
 * @author Bernd Hufmann
 *
 */
public class RunAsElementTypeContributionItem extends LaunchElementTypeContributionItem {

    private static final String EASE_LAUNCH_SHORTCUT = "org.eclipse.ease.launchShortcut"; //$NON-NLS-1$

    @Override
    protected String getLaunchMode() {
        return Objects.requireNonNull(ILaunchManager.RUN_MODE);
    }

    @Override
    protected Set<Pair<String, String>> getParam() {
        Set<Pair<String, String>> selectedTraceTypes = new HashSet<>();
        selectedTraceTypes.add(new Pair<>(EASE_LAUNCH_SHORTCUT, Objects.requireNonNull(Messages.Scripting_RunAsScriptName)));
        return selectedTraceTypes;
    }
}
