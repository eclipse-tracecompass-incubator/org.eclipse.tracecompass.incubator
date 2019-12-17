/*******************************************************************************
 * Copyright (c) 2020 Geneviève Bastien
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

loadModule("/TraceCompass/Trace")

var trace = openMinimalTrace("Tracing", argv[0])

eventIterator = getEventIterator(trace);
schedSwitchCnt = 0;
while (eventIterator.hasNext()) {
    event = eventIterator.next();
    if (event.getName().equals("sched_switch")) {
        schedSwitchCnt = schedSwitchCnt + 1
    }
}
print("Sched switch " + schedSwitchCnt)
trace.dispose()
