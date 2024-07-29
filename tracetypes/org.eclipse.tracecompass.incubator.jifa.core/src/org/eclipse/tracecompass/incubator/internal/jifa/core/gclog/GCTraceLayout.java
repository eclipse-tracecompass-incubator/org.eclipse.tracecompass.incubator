/********************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.eclipse.tracecompass.incubator.internal.jifa.core.gclog;

public class GCTraceLayout {

	private final String fAlloc = "Allocation";
	private final String fGcId = "GC Id";
	private final String fCause = "Cause";
	private final String fCauseInterval = "Cause Interval";
	private final String fPause = "Pause";
	private final String fReclamation = "Reclamation";
	private final String fCpuTime = "Cpu time";
	private final String fDuration = "Duration";
	private final String fLevel = "Level";
	private final String fPromotion = "Promotion";
	private final String fEndTime = "End time";

	public GCTraceLayout() {
		// Do Nothing
	}

	/**
	 * @return the alloc
	 */
	public String getAlloc() {
		return fAlloc;
	}

	/**
	 * @return the gcId
	 */
	public String getGcId() {
		return fGcId;
	}

	/**
	 * @return the cause
	 */
	public String getCause() {
		return fCause;
	}

	/**
	 * @return the pause
	 */
	public String getPause() {
		return fPause;
	}

	/**
	 * @return the causeInterval
	 */
	public String getCauseInterval() {
		return fCauseInterval;
	}

	/**
	 * @return the reclamation
	 */
	public String getReclamation() {
		return fReclamation;
	}

	/**
	 * @return the cpuTime
	 */
	public String getCpuTime() {
		return fCpuTime;
	}

	/**
	 * @return the duration
	 */
	public String getDuration() {
		return fDuration;
	}

	/**
	 * @return the level
	 */
	public String getLevel() {
		return fLevel;
	}

	/**
	 * @return the promotion
	 */
	public String getPromotion() {
		return fPromotion;
	}

	/**
	 * @return the endTime
	 */
	public String getEndTime() {
		return fEndTime;
	}

	public String getMemName(String suffix) {
		return "mem" + suffix;
	}
}
