/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.ui.project.wizards;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * Temporary wizard page to tell users fetching jaeger traces is unavailable.
 * The original feature should come back when the javax.xml.bind version is
 * upgraded in orbit
 *
 * @author Geneviève Bastien
 */
public class FetchJaegerTracesNotAvailableWizardPage extends WizardPage {

    /**
     * Constructor
     */
    protected FetchJaegerTracesNotAvailableWizardPage() {
        super(Messages.FetchJaegerTracesWizardPage_wizardPageName, Messages.FetchJaegerTracesWizardPage_wizardPageName, null);
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(GridLayoutFactory.swtDefaults().create());
        composite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

        Label targetUrlLabel = new Label(composite, SWT.NONE);
        targetUrlLabel.setText(Messages.FetchJaegerTracesWizardPage_importNotAvailable);

        setControl(composite);
    }

}
