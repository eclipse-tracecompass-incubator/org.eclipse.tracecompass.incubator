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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.tracecompass.tmf.core.util.Pair;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;


/**
 * Base ContributionItem for the Run As -> <launch shortcut> or Debug As -> <launch shortcut>.
 *
 * @author Bernd Hufmann
 *
 */
public abstract class LaunchElementTypeContributionItem extends CompoundContributionItem {

    private static final String TYPE_PARAMETER = "org.eclipse.tracecompass.incubator.scripting.ui.commandparameter.launch_as_ease_script.type"; //$NON-NLS-1$
    private static final String MODE_PARAMETER = "org.eclipse.tracecompass.incubator.scripting.ui.commandparameter.launch_as_ease_script.mode"; //$NON-NLS-1$
    private static final String LAUNCH_AS_EASE_SCRIPT_COMMAND_ID = "org.eclipse.tracecompass.incubator.scripting.ui.command.launch_as_ease_script"; //$NON-NLS-1$
    private static final String LAUCH_DIALOG_STRING = "Configuration..."; //$NON-NLS-1$
    private static final String RUN_STRING = "Run"; //$NON-NLS-1$
    private static final String DEBUG_STRING = "Debug"; //$NON-NLS-1$
    private static final String LAUNCH_COMMAND_CONTRIBUTION_ITEM_ID = "org.eclipse.tracecompass.incubator.scripting.contribution.item.id"; //$NON-NLS-1$
    private static final Comparator<IContributionItem> ITEM_COMPARATOR = new ItemComparator();

    /** ID to identify the Open Run/Debug dialog menu item */
    public static final String LAUNCH_DIALOG_CONFIG_ID = "org.eclipse.tracecompass.incubator.scripting.ui.launch-dialog"; //$NON-NLS-1$

    private static final class ItemComparator implements Comparator<IContributionItem> {
        @Override
        public int compare(IContributionItem o1, IContributionItem o2) {
            CommandContributionItem c1 = (CommandContributionItem) o1;
            CommandContributionItem c2 = (CommandContributionItem) o2;
            if (c1.getData().label.equals(c2.getData().label)) {
                return 0;
            }
            if (c1.getData().label.endsWith(LAUCH_DIALOG_STRING)) {
                return 1;
            }
            if (c2.getData().label.endsWith(LAUCH_DIALOG_STRING)) {
                return -1;
            }
            return c1.getData().label.compareTo(c2.getData().label);
        }
    }

    @Override
    protected IContributionItem[] getContributionItems() {

        /*
         * Fill-in the available shortcuts
         */
        Set<Pair<String, String>> availableShortcuts = new HashSet<>(getParam());
        Pair<String, String> launchConfiguration = getLaunchConfigurationParam();
        if (launchConfiguration != null) {
            availableShortcuts.add(launchConfiguration);
        }

        List<IContributionItem> list = new LinkedList<>();
        for (Pair<String, String> item : availableShortcuts) {
            Map<String, String> params = new HashMap<>();
            params.put(TYPE_PARAMETER, item.getFirst());
            params.put(MODE_PARAMETER, getLaunchMode());
            CommandContributionItemParameter param = new CommandContributionItemParameter(
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow(),
                    LAUNCH_COMMAND_CONTRIBUTION_ITEM_ID,
                    getContributionItemCommandId(),
                    CommandContributionItem.STYLE_PUSH
            );
            param.parameters = params;
            param.label = item.getSecond();
            param.visibleEnabled = true;
            list.add(new CommandContributionItem(param));
        }
        Collections.sort(list, ITEM_COMPARATOR);
        return list.toArray(new IContributionItem[list.size()]);
    }

    /**
     * Returns the command id to use for contribution items
     *
     * @return the command id
     */
    protected String getContributionItemCommandId() {
        return LAUNCH_AS_EASE_SCRIPT_COMMAND_ID;
    }

    /**
     * Get the launch mode
     *
     * @return one of {@link ILaunchManager#RUN_MODE} or {@link ILaunchManager#DEBUG_MODE}
     */
    protected abstract String getLaunchMode();

    /**
     * Pairs of launch shortcut ID to shortcut label
     *
     * @return Pairs of launch shortcut ID to shortcut label
     */
    protected abstract Set<Pair<String, String>> getParam();

    @Nullable private Pair<String, String> getLaunchConfigurationParam() {
        if (getLaunchMode().equals(ILaunchManager.RUN_MODE)) {
            return new Pair<>(LAUNCH_DIALOG_CONFIG_ID, RUN_STRING + ' ' + LAUCH_DIALOG_STRING);
        }
        if (getLaunchMode().equals(ILaunchManager.DEBUG_MODE)) {
            return new Pair<>(LAUNCH_DIALOG_CONFIG_ID, DEBUG_STRING + ' ' + LAUCH_DIALOG_STRING);
        }
        return null;
    }

}
