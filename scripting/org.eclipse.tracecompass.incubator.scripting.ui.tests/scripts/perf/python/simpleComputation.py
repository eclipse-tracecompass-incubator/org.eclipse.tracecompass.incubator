################################################################################
# Copyright (c) 2020 Geneviève Bastien
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
################################################################################

base = 10
limit = 300000
value = base;
while base < limit:
    if value == 1:
        base = base + 1
        value = base

    if value % 2 == 0:
        value = value / 2
    else:
        value = 3 * value + 1
