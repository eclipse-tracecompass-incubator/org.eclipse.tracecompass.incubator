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
