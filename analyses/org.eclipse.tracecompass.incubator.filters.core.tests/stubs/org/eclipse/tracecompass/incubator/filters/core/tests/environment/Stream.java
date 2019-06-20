/*******************************************************************************
 * Copyright (c) 2019 Ecole Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.filters.core.tests.environment;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Class that returns à PipedInputStream and a PipedOutputStream
 *
 * @author Maxime Thibault
 *
 */
public class Stream {

    /** Package-private so TestEnvironment can access them */
    PipedInputStream read;
    PipedOutputStream write;

    /**
     * Constructor
     *
     * @throws IOException
     *             Exception thrown by streams
     */
    public Stream() throws IOException {
        write = new PipedOutputStream();
        read = new PipedInputStream();

        write.connect(read);
    }
}