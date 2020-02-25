/*******************************************************************************
 * Copyright (c) 2019 Geneviève Bastien
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
