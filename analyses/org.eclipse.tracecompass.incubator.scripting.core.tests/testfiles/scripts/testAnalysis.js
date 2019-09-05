/*******************************************************************************
 * Copyright (c) 2019 Geneviève Bastien
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

// load Trace Compass modules
loadModule('/TraceCompass/Analysis')

// Create an analysis named activetid.js.
var analysis = getAnalysis("activetid.js")

if (analysis == null) {
	print("Trace is null")
	exit();
}

var eventCount = 0

// Get the event iterator for the trace
var iter = analysis.getEventIterator();

// Parse all events
while (iter.hasNext()) {
	
	var event = iter.next()
	eventCount++	
}

exit(eventCount)
