/*******************************************************************************
 * Copyright (c) 2020 Geneviève Bastien
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

loadModule("/TraceCompassTest/Test")

base = 10;
limit = 100000
value = base;
while (base < limit) {
    if (value == 1) {
        base = base + 1
        value = base;
    }
    value = compute(value)
}
