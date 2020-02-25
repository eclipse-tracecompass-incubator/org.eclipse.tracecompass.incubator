/*******************************************************************************
 * Copyright (c) 2019 Geneviève Bastien
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * statisticsDensityXY.js
 *******************************************************************************/

loadModule("/TraceCompass/Trace");
loadModule('/TraceCompass/Analysis');
loadModule('/TraceCompass/DataProvider');
loadModule('/TraceCompass/View');

// Get the currently active trace
var trace = getActiveTrace();

if (trace == null) {
	  print("Trace is null");
	  exit();
}

// Get the Statistics module (by name) for that trace
var analysis = getTraceAnalysis(trace, 'Statistics');
if (analysis == null) {
	  print("Statistics analysis not found");
	  exit();
}

// Prepare the parameters for the data provider:
// * path in the state system of the events we want to display (here, all events starting with sched)
// * Whether to show the absolute value (we would see an incremental line) or the delta from the previous value
// Create a map and fill it, because javascript map cannot use the EASE constants as keys
var map = new java.util.HashMap();
map.put(ENTRY_PATH, 'event_types/sched*');
map.put(ENTRY_DELTA, true);
// create a XY data provider
var provider = createXYProvider(analysis, map);

// Open an XY chart with this data provider
if (provider != null) {
	  openXYChartView(provider);
}
