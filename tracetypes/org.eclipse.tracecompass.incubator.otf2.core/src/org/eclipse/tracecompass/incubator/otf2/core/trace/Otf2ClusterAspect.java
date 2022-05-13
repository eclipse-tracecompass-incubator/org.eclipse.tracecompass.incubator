/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.otf2.core.trace;

/**
 * Aspect for a cluster
 *
 * @author Yoann Heitz
 */
public class Otf2ClusterAspect extends Otf2NodeAspect {

    @Override
    public String getName() {
        return Messages.getMessage(Messages.Otf2_ClusterAspectName);
    }

    @Override
    public String getHelpText() {
        return Messages.getMessage(Messages.Otf2_ClusterAspectHelp);
    }
}
