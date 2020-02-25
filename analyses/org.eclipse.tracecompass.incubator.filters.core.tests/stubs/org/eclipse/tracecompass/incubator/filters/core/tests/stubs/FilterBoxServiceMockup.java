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
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.ColorInformation;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Use this class to store data from FilterBoxServiceStub Add
 * attributes/functions if necessary
 *
 * {@link FilterBoxServiceStub}
 *
 * @author Maxime Thibault
 *
 */
public class FilterBoxServiceMockup {
    public String fInputReceived = null;
    public int fCursor = -1;
    public CompletableFuture<List<ColorInformation>> fColorsReceived = null;
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> fCompletionsReceived = null;
}
