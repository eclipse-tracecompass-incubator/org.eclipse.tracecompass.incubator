/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.filters.core.tests.stubs;

import java.util.List;

import org.eclipse.lsp4j.Diagnostic;

/**
 * Use this class to save data from LSPClientStub Add attributes/functions if
 * necessary
 *
 * {@link LSPClientStub}
 *
 * @author Maxime Thibault
 */
public class LSPClientMockup {
    public String fInputReceived = null;
    public List<Diagnostic> fDiagnosticsReceived = null;
}
