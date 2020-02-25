################################################################################
# Copyright (c) 2020 Geneviève Bastien
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0
#
# SPDX-License-Identifier: EPL-2.0
################################################################################

loadModule("/TraceCompassTest/Test")

class CallbackFunction(object):
    def apply(self, value):
        if value % 2 == 0:
            return value / 2
        return 3 * value + 1

    class Java:
        implements = ['java.util.function.Function']

callbackFunction = CallbackFunction()

doLoopWithCallback(callbackFunction)