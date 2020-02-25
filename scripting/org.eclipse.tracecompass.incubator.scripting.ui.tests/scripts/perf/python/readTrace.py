################################################################################
# Copyright (c) 2020 Geneviève Bastien
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0
#
# SPDX-License-Identifier: EPL-2.0
################################################################################

loadModule("/TraceCompass/Trace")

trace = openMinimalTrace("Tracing", argv[0])

eventIterator = getEventIterator(trace)
eventIterator.addEvent("sched_switch")
schedSwitchCnt = 0;
while eventIterator.hasNext():
    event = eventIterator.next()
    if event.getName() == "sched_switch":
        schedSwitchCnt = schedSwitchCnt + 1

print(schedSwitchCnt)
trace.dispose()