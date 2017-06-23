/*******************************************************************************
 * Copyright (c) 2022 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.inandout.core.analysis;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfBaseAspects;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Segment specifiers
 *
 * Design philosophy: stateless and serializable. This is a config
 *
 * TODO: replace with TmfXmlScenario
 *
 * @author Matthew Khouzam
 */
public class SegmentSpecifier {

    private static final String ALL = "all"; //$NON-NLS-1$

    private static final ITmfEventAspect<@NonNull Object> CONTENT_ASPECT = TmfBaseAspects.getContentsAspect();

    /**
     * Segment context.
     *
     * TODO: replace with TmfXmlPatternSegment
     */
    public static class SegmentContext {
        private String fLabel;
        private String fContext;
        @Nullable
        private Object fClassifier;

        /**
         * @return the label
         */
        public String getLabel() {
            return fLabel;
        }

        /**
         * @param label
         *            the label to set
         */
        public void setLabel(String label) {
            fLabel = label;
        }

        /**
         * @return the context
         */
        public String getContext() {
            return fContext;
        }

        /**
         * @param context
         *            the context to set
         */
        public void setContext(String context) {
            fContext = context;
        }

        /**
         * @return the classifier
         */
        public Object getClassifier() {
            return fClassifier;
        }

        /**
         * @param classifier
         *            the classifier to set
         */
        public void setClassifier(Object classifier) {
            fClassifier = classifier;
        }
    }

    @Expose
    @SerializedName(value = "label")
    private String fLabel;
    @Expose
    @SerializedName(value = "inRegex")
    private String fInRegex;
    @Expose
    @SerializedName(value = "outRegex")
    private String fOutRegex;
    @Expose
    @SerializedName(value = "contextInRegex")
    private String fContextInRegex;
    @Expose
    @SerializedName(value = "contextOutRegex")
    private String fContextOutRegex;
    @Expose
    @SerializedName(value = "classifier")
    private String fClassifier;

    private transient @Nullable Pattern fInRegexPattern;
    private transient @Nullable Pattern fOutRegexPattern;
    private transient @Nullable Pattern fContextInPattern;
    private transient @Nullable Pattern fContextOutPattern;

    /**
     * Default constructor for GSON
     */
    public SegmentSpecifier() {
        fLabel = ""; //$NON-NLS-1$
        fInRegex = ""; //$NON-NLS-1$
        fOutRegex = ""; //$NON-NLS-1$
        fContextInRegex = ""; //$NON-NLS-1$
        fContextOutRegex = ""; //$NON-NLS-1$
        fClassifier = ""; //$NON-NLS-1$
    }

    /**
     * Copy constructor
     *
     * @param other
     *            other item
     */
    public SegmentSpecifier(SegmentSpecifier other) {
        fLabel = other.fLabel;
        fInRegex = other.fInRegex;
        fOutRegex = other.fOutRegex;
        fContextInRegex = other.fContextInRegex;
        fContextOutRegex = other.fContextOutRegex;
        fClassifier = other.fClassifier;
    }

    /**
     * Expanded constructor
     *
     * @param label
     *            the label (format string)
     * @param inRegex
     *            input regex to parse name
     * @param outRegex
     *            output regex to parse name
     * @param contextInRegex
     *            context in regex, the regex to apply to in event to get a
     *            context ID (snowflake)
     * @param contextOutRegex
     *            context out regex, the regex to apply to out event to get a
     *            context ID (snowflake)
     * @param classifier
     *            Aspect name to classify by
     */
    public SegmentSpecifier(String label, String inRegex, String outRegex, String contextInRegex, String contextOutRegex, String classifier) {
        fLabel = label;
        fInRegex = inRegex;
        fOutRegex = outRegex;
        fContextInRegex = contextInRegex;
        fContextOutRegex = contextOutRegex;
        fClassifier = classifier;
    }

    /**
     * Get label
     *
     * @return label
     */
    public String getLabel() {
        return fLabel;
    }

    /**
     * Get input regex
     *
     * @return input regex
     */
    public String getInRegex() {
        return fInRegex;
    }

    /**
     * Get output regex
     *
     * @return output regex
     */
    public String getOutRegex() {
        return fOutRegex;
    }

    /**
     * regex to extract context from in event
     *
     * @return the regex to extract context from in event
     */
    public String getContextInRegex() {
        return fContextInRegex;
    }

    /**
     * regex to extract context from out event
     *
     * @return the regex to extract context from input event
     */
    public String getContextOutRegex() {
        return fContextOutRegex;
    }

    /**
     * Get the classifier type, an aspect name
     *
     * @return the classifier name
     */
    public String getClassifierType() {
        return fClassifier;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SegmentSpecifier other = (SegmentSpecifier) obj;
        return Objects.equals(fLabel, other.fLabel) &&
                Objects.equals(fInRegex, other.fInRegex) &&
                Objects.equals(fOutRegex, other.fOutRegex) &&
                Objects.equals(fContextInRegex, other.fContextInRegex) &&
                Objects.equals(fContextOutRegex, other.fContextOutRegex) &&
                Objects.equals(fClassifier, other.fClassifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                fLabel,
                fInRegex,
                fOutRegex,
                fContextInRegex,
                fContextOutRegex,
                fClassifier);
    }

    @Override
    public String toString() {
        if (fContextInRegex.trim().isEmpty()) {
            return String.format("%s: [InRegex=%s, OutRegex=%s, Classifier=%s]", fLabel, fInRegex, fOutRegex, fClassifier); //$NON-NLS-1$
        }
        return String.format("%s: [InRegex=%s, OutRegex=%s, ContextInRegex=%s, ContextOutRegex=%s, Classifier=%s]", fLabel, fInRegex, fOutRegex, fContextInRegex, fContextOutRegex, fClassifier); //$NON-NLS-1$
    }

    /**
     * Get a context from an input event
     *
     * @param event
     *            the event
     * @return the context or null
     */
    public @Nullable SegmentContext getSegmentContext(@NonNull ITmfEvent event) {
        if (getInPattern().matcher(event.getName()).matches()) {
            SegmentContext segmentContext = new SegmentContext();
            segmentContext.setLabel(getLabel(event, null));
            Object resolve = CONTENT_ASPECT.resolve(event);
            if (resolve != null && !getContextInRegex().trim().isEmpty()) {
                segmentContext.setContext(findInFields(event, getContextInPattern()));
            }
            Object value = getClassifier(event);
            if (value != null) {
                segmentContext.setClassifier(value);
            }
            return segmentContext;
        }
        return null;
    }

    /**
     * Get the resolved classifier from the event
     *
     * @param event
     *            the event
     * @return the resolved classifier
     */
    public Object getClassifier(ITmfEvent event) {
        Object value = null;
        String classifier = fClassifier;
        if (classifier != null && !classifier.trim().isEmpty()) {
            value = TmfTraceUtils.resolveAspectOfNameForEvent(event.getTrace(), classifier, event);
        }
        return value;
    }

    private static String removeAll(String value) {
        return value == null || ALL.equalsIgnoreCase(value) ? "" : value; //$NON-NLS-1$
    }

    private Pattern getContextInPattern() {
        Pattern p = fContextInPattern;
        if (p == null) {
            String input = removeAll(fContextInRegex);
            p = Pattern.compile(input);
            fContextInPattern = p;
        }
        return p;
    }

    private Pattern getContextOutPattern() {
        Pattern p = fContextOutPattern;
        if (p == null) {
            String input = removeAll(fContextOutRegex);
            p = Pattern.compile(input);
            fContextOutPattern = p;
        }
        return p;
    }

    private Pattern getInPattern() {
        Pattern p = fInRegexPattern;
        if (p == null) {
            String input = removeAll(fInRegex);
            p = Pattern.compile(input);
            fInRegexPattern = p;
        }
        return p;
    }

    /**
     * Does the event match a given output name?
     *
     * @param event
     *            the event
     * @return true if it matches
     */
    public boolean matchesOutName(ITmfEvent event) {
        return (getOutPattern().matcher(event.getName()).matches());
    }

    private Pattern getOutPattern() {
        Pattern p = fOutRegexPattern;
        if (p == null) {
            String input = removeAll(fOutRegex);
            p = Pattern.compile(input);
            fOutRegexPattern = p;
        }
        return p;
    }

    private String getLabel(@NonNull ITmfEvent event, @Nullable SegmentContext sc) {
        if (sc != null) {
            return sc.fLabel;
        }

        if (fLabel.equals("{0}")) { //$NON-NLS-1$
            String ret = findInName(event, getInPattern());
            if (ret != null) {
                return ret;
            }
        }
        if (fLabel.equals("{1}")) { //$NON-NLS-1$
            String ret = findInName(event, getOutPattern());
            if (ret != null) {
                return ret;
            }
        }
        if (fLabel.equals("{2}")) { //$NON-NLS-1$
            String ret = findInFields(event, getContextInPattern());
            if (ret != null) {
                return ret;
            }
        }
        if (fLabel.equals("{3}")) { //$NON-NLS-1$
            String ret = findInFields(event, getContextOutPattern());
            if (ret != null) {
                return ret;
            }
        }
        return fLabel;
    }

    private static String findInName(@NonNull ITmfEvent event, Pattern p) {
        String resolve = event.getName();
        Matcher matcher = p.matcher(resolve);
        return findIn(matcher);
    }

    private static String findInFields(@NonNull ITmfEvent event, Pattern p) {
        Object resolve = CONTENT_ASPECT.resolve(event);
        String matched = null;
        if (resolve != null) {
            Matcher matcher = p.matcher(resolve.toString());
            matched = findIn(matcher);
        }
        return matched;
    }

    private static String findIn(Matcher matcher) {
        if (matcher.find()) {
            if (matcher.groupCount() >= 1) {
                return matcher.group(1);
            }
            return matcher.group(0);
        }
        return null;
    }

    /**
     * Bulk setting for specifier
     *
     * @param label
     *            the label (format string)
     * @param inRegex
     *            input regex to parse name
     * @param outRegex
     *            output regex to parse name
     * @param contextInRegex
     *            context in regex, the regex to apply to in event to get a
     *            context ID (snowflake)
     * @param contextOutRegex
     *            context out regex, the regex to apply to out event to get a
     *            context ID (snowflake)
     * @param category
     *            the aspect name to resolve
     * @return true if it has changed
     */
    public boolean setIfNotNull(@Nullable String label, @Nullable String inRegex, @Nullable String outRegex, @Nullable String contextInRegex, @Nullable String contextOutRegex, @Nullable String category) {
        boolean[] updated = new boolean[1];
        setIfNotNull(label, value -> {
            fLabel = value;
            updated[0] = true;
        });
        setIfNotNull(inRegex, value -> {
            fInRegex = value;
            updated[0] = true;
        });
        setIfNotNull(outRegex, value -> {
            fOutRegex = value;
            updated[0] = true;
        });
        setIfNotNull(contextInRegex, value -> {
            fContextInRegex = value;
            updated[0] = true;
        });
        setIfNotNull(contextOutRegex, value -> {
            fContextOutRegex = value;
            updated[0] = true;
        });
        setIfNotNull(category, value -> {
            fClassifier = category;
            updated[0] = true;
        });
        return updated[0];
    }

    private static void setIfNotNull(@Nullable String newString, Consumer<String> consumer) {
        if (newString != null) {
            consumer.accept(newString);
        }
    }

    /**
     * Get out context (if applicable)
     *
     * @param event
     *            the event
     * @return the out context
     */
    public String getOutContext(@NonNull ITmfEvent event) {
        if (getOutRegex() == null || getContextOutRegex().trim().isEmpty()) {
            return null;
        }
        return findInFields(event, getContextOutPattern());
    }
}
