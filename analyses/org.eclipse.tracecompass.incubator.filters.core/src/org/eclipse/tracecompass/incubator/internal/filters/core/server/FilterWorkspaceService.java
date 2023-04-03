/*******************************************************************************
 * Copyright (c) 2019 Ecole Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.filters.core.server;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.services.WorkspaceService;

/**
 * Empty implementation of the LSP specification WorkspaceService. Not useful
 * for the filterBox.
 *
 * @author David-Alexandre Beaupre
 * @author Remi Croteau
 *
 */
public class FilterWorkspaceService implements WorkspaceService {

    @Override
    public void didChangeConfiguration(@Nullable DidChangeConfigurationParams params) {
        // Not used
        throw new UnsupportedOperationException();
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
        // Not used
        throw new UnsupportedOperationException();
    }

}
