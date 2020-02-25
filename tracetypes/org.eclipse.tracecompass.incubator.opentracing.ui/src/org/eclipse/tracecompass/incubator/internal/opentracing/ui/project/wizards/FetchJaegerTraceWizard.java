/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.ui.project.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

/**
 * Wizard to import traces from Jaeger
 *
 * @author Simon Delisle
 */
public class FetchJaegerTraceWizard extends Wizard implements IImportWizard {

//    private IStructuredSelection fSelection;
    private FetchJaegerTracesNotAvailableWizardPage fPage;

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
//        fSelection = selection;
        setWindowTitle(Messages.FetchJaegerTraceWizard_wizardTitle);
    }

    @Override
    public boolean performFinish() {
        return true;
//        return fPage.performFinish();
    }

    @Override
    public void addPages() {
        super.addPages();
        fPage = new FetchJaegerTracesNotAvailableWizardPage();
        addPage(fPage);
    }

}
