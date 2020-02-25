################################################################################
# Copyright (c) 2019 Geneviève Bastien
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0
#
# SPDX-License-Identifier: EPL-2.0
#
# basicAnalysis.py
################################################################################

# load proper Trace Compass modules
loadModule('/TraceCompass/Analysis');
loadModule('/TraceCompass/View');
loadModule('/TraceCompass/DataProvider');

from py4j.java_gateway import JavaClass

# Create an analysis for this script
analysis = getAnalysis("activetid_python.js")

if analysis is None:
    print("Trace is null")
    exit()

# Get the analysis's state system so we can fill it, true indicates to re-use an existing state system, false would create a new state system even if one already exists
ss = analysis.getStateSystem(False)

# The state system methods require a vararg array. This puts the string in a vararg array to call those methods
def strToVarargs(str):
    object_class = java.lang.String
    object_array = gateway.new_array(object_class, 1)
    object_array[0] = str
    return object_array

# The analysis itself is in this function
def runAnalysis():
    # Get the event iterator for the trace
    iter = analysis.getEventIterator()
   
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
            cpu = getFieldValue(event, "CPU")
            tid = getFieldValue(event, "next_tid")
            if (not(cpu is None) and not(tid is None)):
                # Write the tid to the state system, for the attribute corresponding to the cpu
                quark = ss.getQuarkAbsoluteAndAdd(strToVarargs(str(cpu)))
                # modify the value, tid is a long, so "" + tid make sure it's a string for display purposes
                ss.modifyAttribute(event.getTimestamp().toNanos(), str(tid), quark)
       
    # Done parsing the events, close the state system at the time of the last event, it needs to be done manually otherwise the state system will still be waiting for values and will not be considered finished building
    if not(event is None):
        ss.closeHistory(event.getTimestamp().toNanos())

# This condition verifies if the state system is completed. For instance, if it had been built in a previous run of the script, it wouldn't run again.
if not(ss.waitUntilBuilt(0)):
    # State system not built, run the analysis
    runAnalysis()

# Get a time graph provider from this analysis, displaying all attributes (which are the cpus here)
provider = createTimeGraphProvider(analysis, {ENTRY_PATH : '*'});
if not(provider is None):
    # Open a time graph view displaying this provider
    openTimeGraphView(provider)
