/*******************************************************************************
 * Copyright (c) 2020 Geneviève Bastien
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

// load Trace Compass modules
loadModule('/TraceCompass/Analysis');
loadModule('/TraceCompass/Trace');
loadModule('/TraceCompass/TraceUI');

trace = openTrace("Tracing", argv[0])

// Create an analysis for this script
analysis = createScriptedAnalysis(trace, "activetid_python.js")

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
	iter.addEvent("sched_switch")

	var event = null;
	// Parse all events
	while (iter.hasNext()) {

		event = iter.next();

		// Do something when the event is a sched_switch
		if (event.getName() == "sched_switch") {
			// This function is a wrapper to get the value of field CPU in the event, or return null if the field is not present
			cpu = getEventFieldValue(event, "CPU");
			tid = getEventFieldValue(event, "next_tid");
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

runAnalysis();
