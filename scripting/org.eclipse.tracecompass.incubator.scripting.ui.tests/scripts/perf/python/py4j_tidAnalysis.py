################################################################################
# Copyright (c) 2020 Geneviève Bastien
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0
#
# SPDX-License-Identifier: EPL-2.0
################################################################################

# load proper Trace Compass modules
loadModule('/TraceCompass/Analysis');
loadModule('/TraceCompass/Trace')
loadModule('/TraceCompass/TraceUI');
loadModule('/TraceCompass/Utils');

from py4j.java_gateway import JavaClass

trace = openTrace("Tracing", argv[0])

# Create an analysis for this script
analysis = createScriptedAnalysis(trace, "activetid_python.py")

if analysis is None:
    print("Trace is null")
    exit()

# Get the analysis's state system so we can fill it, true indicates to re-use an existing state system, false would create a new state system even if one already exists
ss = analysis.getStateSystem(False)

# The analysis itself is in this function
def runAnalysis():
    # Get the event iterator for the trace
    iter = analysis.getEventIterator()
    iter.addEvent("sched_switch")

    # Parse all events
    event = None
    while iter.hasNext():
        # The python java gateway keeps a reference to the Java objects it sends to python. To avoid OutOfMemoryException, they need to be explicitly detached from the gateway when not needed anymore
        if not(event is None):
            gateway.detach(event)

        event = iter.next();

        # Do something when the event is a sched_switch
        if event.getName() == "sched_switch":
            # This function is a wrapper to get the value of field CPU in the event, or return null if the field is not present
            cpu = getEventFieldValue(event, "CPU")
            tid = getEventFieldValue(event, "next_tid")
            if (not(cpu is None) and not(tid is None)):
                # Write the tid to the state system, for the attribute corresponding to the cpu
                quark = ss.getQuarkAbsoluteAndAdd(strToArray(str(cpu)))
                # modify the value, tid is a long, so "" + tid make sure it's a string for display purposes
                ss.modifyAttribute(event.getTimestamp().toNanos(), str(tid), quark)

    # Done parsing the events, close the state system at the time of the last event, it needs to be done manually otherwise the state system will still be waiting for values and will not be considered finished building
    if not(event is None):
        ss.closeHistory(event.getTimestamp().toNanos())

runAnalysis()
