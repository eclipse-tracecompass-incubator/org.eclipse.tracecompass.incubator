################################################################################
# Copyright (c) 2019 Geneviève Bastien
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
################################################################################

# load Trace Compass modules
loadModule('/TraceCompass/Analysis')

# Create an analysis for this script
analysis = getAnalysis("activetid_python.js")

if analysis is None:
    print("Trace is null")
    exit()

eventCount = 0

# Get the event iterator for the trace
iter = analysis.getEventIterator()

# Parse all events
event = None
while iter.hasNext():

	event = iter.next();
	eventCount = eventCount + 1;

exit(eventCount);
