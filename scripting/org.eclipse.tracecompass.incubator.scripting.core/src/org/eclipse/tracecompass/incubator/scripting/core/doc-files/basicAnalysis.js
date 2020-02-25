/*******************************************************************************
 * Copyright (c) 2019 Geneviève Bastien
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * basicAnalysis.js
 *******************************************************************************/

// load Trace Compass modules
loadModule('/TraceCompass/Analysis');
loadModule('/TraceCompass/DataProvider');
loadModule('/TraceCompass/View');

// Create an analysis named activetid.js.
var analysis = getAnalysis("activetid.js");

if (analysis == null) {
	print("Trace is null");
	exit();
}

// Get the analysis's state system so we can fill it, false indicates to create a new state system even if one already exists, true would re-use an existing state system
var ss = analysis.getStateSystem(false);

// The analysis itself is in this function
function runAnalysis() {
	// Get the event iterator for the trace
	var iter = analysis.getEventIterator();
	
	var event = null;
	// Parse all events
	while (iter.hasNext()) {
		
		event = iter.next();
		
		// Do something when the event is a sched_switch
		if (event.getName() == "sched_switch") {
			// This function is a wrapper to get the value of field CPU in the event, or return null if the field is not present
			cpu = getFieldValue(event, "CPU");
			tid = getFieldValue(event, "next_tid");
			if ((cpu != null) && (tid != null)) {
				// Write the tid to the state system, for the attribute corresponding to the cpu
				quark = ss.getQuarkAbsoluteAndAdd(cpu);
				// modify the value, tid is a long, so "" + tid make sure it's a string for display purposes
				ss.modifyAttribute(event.getTimestamp().toNanos(), "" + tid, quark);
			}
		}
		
	}
	// Done parsing the events, close the state system at the time of the last event, it needs to be done manually otherwise the state system will still be waiting for values and will not be considered finished building
	if (event != null) {
		ss.closeHistory(event.getTimestamp().toNanos());
	}
}

// This condition verifies if the state system is completed. For instance, if it had been built in a previous run of the script, it wouldn't run again.
if (!ss.waitUntilBuilt(0)) {
	// State system not built, run the analysis
	runAnalysis();
}

// Get a time graph provider from this analysis, displaying all attributes (which are the cpus here)
// Create a map and fill it, because javascript map cannot use the EASE constants as keys
var map = new java.util.HashMap();
map.put(ENTRY_PATH, '*');
provider = createTimeGraphProvider(analysis, map);
if (provider != null) {
	// Open a time graph view displaying this provider
	openTimeGraphView(provider);
}
