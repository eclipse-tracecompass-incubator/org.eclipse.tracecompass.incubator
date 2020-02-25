################################################################################
# Copyright (c) 2019 Geneviève Bastien
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0
#
# SPDX-License-Identifier: EPL-2.0
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
