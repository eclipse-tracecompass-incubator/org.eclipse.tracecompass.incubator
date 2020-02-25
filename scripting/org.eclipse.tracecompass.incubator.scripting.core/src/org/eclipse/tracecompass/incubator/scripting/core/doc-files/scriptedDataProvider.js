/*******************************************************************************
 * Copyright (c) 2019 Geneviève Bastien
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * scriptedDataProvider.js
 *******************************************************************************/

// load Trace Compass modules
loadModule('/TraceCompass/Analysis');
loadModule('/TraceCompass/DataProvider');
loadModule('/TraceCompass/View');
loadModule('/TraceCompass/Utils');

// Create an analysis named mpiring.js.
var analysis = getAnalysis("mpiring.js");

if (analysis == null) {
	print("Trace is null");
	exit();
} 

// Get the analysis's state system so we can fill it, false indicates to create a new state system even if one already exists, true would re-use an existing state system
var ss = analysis.getStateSystem(false);

// Variable to save the arrow information
var arrows = [];
// The analysis itself is in this function
function runAnalysis() {
	// Get the event iterator for the trace
	var iter = analysis.getEventIterator();
	
	var event = null;
	// Associate a TID with an mpi resource
	var tidToResMap = {};
	// Save information on the pending arrows
	var pendingArrows = {};
	// Parse all events
	while (iter.hasNext()) {
		
		event = iter.next();
		
		// Do something when the event if it is one we are interested in
		name = event.getName();
		if (name == "mpi:mpi_init_exit") {
			// This function is a wrapper to get the value of field CPU in the event, or return null if the field is not present
			resourceId = getFieldValue(event, "res");
			tid = getFieldValue(event, "tid");
			if ((resourceId != null) && (tid != null)) {
				// Save the association between tid and resource
				tidToResMap[tid] = resourceId;
			}
		} else if (name == "mpi:mpi_recv_entry") {
			// First get the current resource from its tid
			tid = getFieldValue(event, "tid");
			if (tid != null) {
				resourceId = tidToResMap[tid];
				if (resourceId != null) {
					// Save the state of the resource as waiting for reception
					quark = ss.getQuarkAbsoluteAndAdd(resourceId);
					ss.modifyAttribute(event.getTimestamp().toNanos(), "Waiting for reception", quark);
				}
			}
		} else if (name == "mpi:mpi_recv_exit") {
			tid = getFieldValue(event, "tid");
			if (tid != null) {
				resourceId = tidToResMap[tid];
				if (resourceId != null) {
					// Close the receiving state of the resource
					quark = ss.getQuarkAbsoluteAndAdd(resourceId);
					ss.removeAttribute(event.getTimestamp().toNanos(), quark);
				}
				// We received a message, see if we can close a pending arrow
				source = getFieldValue(event, "source");
				
				if (source != null) {
					pending = pendingArrows[resourceId];
					if (pending != null) {
						// There is a pending arrow (ie send) for this message
						pendingArrows[resourceId] = null;
						pending["endTime"] = event.getTimestamp().toNanos();
						arrows.push(pending);
					}
				}
			}
			
		} else if (name == "mpi:mpi_send_entry") {
			tid = getFieldValue(event, "tid");
			if (tid != null) {
				resourceId = tidToResMap[tid];
				if (resourceId != null) {
					// Save the state of this resource as "Sending message"
					quark = ss.getQuarkAbsoluteAndAdd(resourceId);
					ss.modifyAttribute(event.getTimestamp().toNanos(), "Sending message", quark);
				}
				// Prepare the start of an arrow
				dest = getFieldValue(event, "dest");
				if (dest != null) {
					pendingArrows[dest] = {"time" : event.getTimestamp().toNanos(), "source" : resourceId, "dest" : dest};
				}
			}
		} else if (name == "mpi:mpi_send_exit") {
			tid = getFieldValue(event, "tid");
			if (tid != null) {
				resourceId = tidToResMap[tid];
				if (resourceId != null) {
					// Close the sending state of this resource
					quark = ss.getQuarkAbsoluteAndAdd(resourceId);
					ss.removeAttribute(event.getTimestamp().toNanos(), quark);
				}
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

// Get list wrappers from Trace Compass for the entries and arrows. The conversion between javascript list and java list is not direct, so we need a wrapper
var tgEntries = createListWrapper();
var tgArrows = createListWrapper();

// Prepare the time graph data, there is few enough entries and arrows that it can be done once and returned once
function prepareTimeGraph() {
	// Map the resource to an entry ID
	var mpiResToId = {};
	
	// Prepare the entries
	quarks = ss.getQuarks("*");
	// Prepare the entry names and sort them
	var mpiEntries = [];
	for (i = 0; i < quarks.size(); i++) {
		quark = quarks.get(i);
		mpiEntries.push(ss.getAttributeName(quark));
	}
	mpiEntries.sort(function(a,b){return Number(a) - Number(b)});

	// With the sorted entry name, actually create the entries
	var entries = [];
	for (i = 0; i < mpiEntries.length; i++) {
		// Get the mpi resource ID, and find its quark
		mpiResId = mpiEntries[i];
		quark = ss.getQuarkAbsolute(mpiResId);
		// Create an entry with the resource ID as name and the quark. The quark value will be used to populate the entry's data.
		entry = createEntry(mpiResId, {'quark' : quark});
		// Add the entry to the entry list
		tgEntries.getList().add(entry);
		
		// Map the resource ID with the entry ID. This information will be useful to create the arrows later
		mpiResToId[mpiResId] = entry.getId();
	}
	
	// Prepare the arrows
	for (i=0; i < arrows.length; i++) {
		arrow = arrows[i];
		
		// For each arrow, we get the source and destination entry ID from its mpi resource ID
		srcId = mpiResToId[arrow["source"]];
		dstId = mpiResToId[arrow["dest"]];
		// Get the start time and calculate the duration
		startTime = arrow["time"];
		duration = arrow["endTime"] - startTime;
		// Add the arrow to the arrows list
		tgArrows.getList().add(createArrow(srcId, dstId, startTime, duration, 1));
	}
}

// Call the preparation of the time graph data
prepareTimeGraph();

// A function used to return the entries to the data provider. It receives the filter in parameter, which contains the requested time range and any additional information
function getEntries(parameters) {
	// The list is static once built, return all entries
	return tgEntries.getList();
}

// A function used to return the arrows to the data provider. It receives the filter in parameter, which contains the requested time range and any additional information
function getArrows(parameters) {
	// Just return all the arrows, the view will take those in the range
	return tgArrows.getList();
}

// Create a scripted data provider for this analysis, using script functions to get the entries, row model data and arrows. Since the entries have a quark associated with them which contains the data to display, there is no need for a scripted getRowData function, so we send null
provider = createScriptedTimeGraphProvider(analysis, getEntries, null, getArrows);
if (provider != null) {
	// Open a time graph view displaying this provider
	openTimeGraphView(provider);
}
