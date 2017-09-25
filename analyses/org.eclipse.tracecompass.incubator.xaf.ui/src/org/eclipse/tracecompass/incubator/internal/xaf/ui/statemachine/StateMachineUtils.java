/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.constraint.Operator;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.constraint.StateMachineConstraint;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.constraint.StateMachineConstraintAdaptive;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.constraint.ValueType;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.StateMachineVariable;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.base.Joiner;

/**
 * @author Raphaël Beamonte
 */
public final class StateMachineUtils {

    private static final @NonNull Pattern CONDITION_PATTERN = Pattern.compile("^(?<variable>[^ ]+)\\s+(?<operator><|<=|==|!=|>=|>|\\?\\?)\\s+(?<value>.+)$"); //$NON-NLS-1$
    private static final @NonNull Pattern ADAPTIVE_PATTERN = Pattern.compile("^\\?"); //$NON-NLS-1$
    private static final @NonNull Pattern UNIT_PATTERN = Pattern.compile(".*(ns|us|ms|s|m|h)$"); //$NON-NLS-1$
    private static final @NonNull Pattern TIMESTAMP_PATTERN = Pattern.compile("^(?<value>(?<int>[0-9]+)?(?:\\.(?<dec>[0-9]+))?|\\?)(?<unit>ns|us|ms|s|m|h|)$"); //$NON-NLS-1$

    /**
     * @param value The value to check
     * @return Whether the value is adaptive or not
     */
    public static boolean isValueAdaptive(String value) {
        return ADAPTIVE_PATTERN.matcher(value).find();
    }

    private static List<StateMachineTransition> getTransitionsFromXMLState(Element element, HashMap<String, StateMachineNode> nodeList) {
        List<StateMachineTransition> stateMachineTransitionList = new ArrayList<>();

        NodeList transitionList = element.getElementsByTagName("transition"); //$NON-NLS-1$
        for (int j = 0; j < transitionList.getLength(); j++) {
            Node transition = transitionList.item(j);

            // Get the needed attributes
            Node eventAttribute = transition.getAttributes().getNamedItem("event"); //$NON-NLS-1$
            Node targetAttribute = transition.getAttributes().getNamedItem("target"); //$NON-NLS-1$
            if (eventAttribute == null || targetAttribute == null) {
                continue;
            }

            // Split the event name from the potential added contexts
            String[] parts = eventAttribute.getNodeValue().split("\\|"); //$NON-NLS-1$

            // Get the triggering event name
            String eventName = parts[0];

            // Get the triggering event contexts
            Map<String, String> eventContexts = new HashMap<>();
            for (String context : parts) {
                String[] entry = context.split("=", 2); //$NON-NLS-1$
                if (entry.length > 1) {
                    eventContexts.put(entry[0], entry[1]);
                }
            }

            // Get the next node
            String targetNode = targetAttribute.getNodeValue();
            StateMachineNode nextNode = nodeList.get(targetNode);
            if (nextNode == null) {
                System.out.println("Transition to unknown node: "+targetNode); //$NON-NLS-1$
                continue;
            }

            StateMachineTransition stateMachineTransition = new StateMachineTransition(nextNode, eventName, eventContexts);

            Node conditionAttribute = transition.getAttributes().getNamedItem("cond"); //$NON-NLS-1$
            if (conditionAttribute != null) {
                String[] conditionList = conditionAttribute.getNodeValue().split("(?:;| and |&amp;&amp;)"); //$NON-NLS-1$

                for (int k = 0; k < conditionList.length; k++) {
                    String condition = conditionList[k].trim();

                    Matcher m = CONDITION_PATTERN.matcher(condition);
                    if (!m.find()) {
                        System.out.println("Unmatching condition: "+condition); //$NON-NLS-1$
                        continue;
                    }

                    String varName = m.group("variable"); //$NON-NLS-1$
                    String value = m.group("value"); //$NON-NLS-1$
                    Operator operator = Operator.getOperatorFromText(
                            m.group("operator")); //$NON-NLS-1$

                    StateMachineConstraint stateMachineConstraint;
                    if (operator.isAdaptive() || isValueAdaptive(value)) {
                        stateMachineConstraint = new StateMachineConstraintAdaptive(
                                varName, operator, value);
                    } else {
                        stateMachineConstraint = new StateMachineConstraint(
                                varName, operator, ValueType.CONSTANT, value);
                    }

                    stateMachineTransition.addConstraint(stateMachineConstraint);
                }
            }
            stateMachineTransitionList.add(stateMachineTransition);
        }

        return stateMachineTransitionList;
    }

    /**
     * Returns the initial transition for a state machine model built from a given SCXML file
     * @param xmlPath The path to the file
     * @return The initial transition
     * @throws FileNotFoundException If the file is not found
     * @throws SAXException If there is an error while parsing
     * @throws IOException If there is an IO exception
     * @throws ParserConfigurationException If there is a parser configuration exception
     */
    public static List<StateMachineTransition> getModelFromXML(String xmlPath) throws FileNotFoundException, SAXException, IOException, ParserConfigurationException {
        Document xml = null;
        try (FileInputStream fis = new FileInputStream(xmlPath)) {
            xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(fis);
        }

        NodeList scxmlList = xml.getElementsByTagName("scxml"); //$NON-NLS-1$
        Element scxml = (Element)scxmlList.item(0);

        HashMap<String, StateMachineNode> nodeList = new HashMap<>();

        /* Get the list of states.
         * Three tags possible for the states:
         *  - state
         *  - final
         *  - and initial
         */
        List<Element> statesList = new ArrayList<>();
        for (String tagName : new String[]{"state", "final", "initial"}) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            NodeList states = scxml.getElementsByTagName(tagName);
            for (int i = 0; i < states.getLength(); i++) {
                Node id = states.item(i).getAttributes().getNamedItem("id"); //$NON-NLS-1$
                if (id == null) {
                    // If the node has no ID, we don't use it
                    continue;
                }

                String name = id.getNodeValue();
                if (nodeList.containsKey(name)) {
                    System.out.println("Node "+name+" already exists"); //$NON-NLS-1$ //$NON-NLS-2$
                }

                nodeList.put(name, new StateMachineNode(name));
                statesList.add((Element)states.item(i));
            }
        }

        // For each node, create the variables and transitions
        for (Element s : statesList) {
            // Get the node in the HashMap
            String name = s.getAttributes().getNamedItem("id").getNodeValue(); //$NON-NLS-1$
            StateMachineNode stateMachineNode = NonNullUtils.checkNotNull(nodeList.get(name));

            // Variables
            NodeList onentryList = s.getElementsByTagName("onentry"); //$NON-NLS-1$
            for (int j = 0; j < onentryList.getLength(); j++) {
                Element onentry = (Element)onentryList.item(j);

                NodeList assignList = onentry.getElementsByTagName("assign"); //$NON-NLS-1$
                for (int k = 0; k < assignList.getLength(); k++) {
                    Node assign = assignList.item(k);

                    Node location = assign.getAttributes().getNamedItem("location"); //$NON-NLS-1$
                    Node expr = assign.getAttributes().getNamedItem("expr"); //$NON-NLS-1$
                    if (location == null || expr == null) {
                        continue;
                    }

                    String varName = location.getNodeValue();
                    String varValue = expr.getNodeValue();

                    Class<?> variableClass = StateMachineVariable.class;
                    String[] splitVar = varName.split("/", 2); //$NON-NLS-1$
                    if (splitVar.length > 1 && StateMachineVariable.VARIABLE_TYPES.containsKey(splitVar[0])) {
                        variableClass = NonNullUtils.checkNotNull(StateMachineVariable.VARIABLE_TYPES.get(splitVar[0]));
                    }

                    Constructor<?> variableConstructor = null;
                    StateMachineVariable stateMachineVariable = null;
                    try {
                        variableConstructor = variableClass.getConstructor(String.class, Comparable.class);
                        stateMachineVariable = (StateMachineVariable)variableConstructor.newInstance(varName, varValue);
                    } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        e.printStackTrace();
                    }

                    if (stateMachineVariable != null) {
                        stateMachineNode.addVariable(stateMachineVariable);
                    }
                }
            }

            // Transitions
            stateMachineNode.addTransitions(getTransitionsFromXMLState(s, nodeList));
        }

        // And now get the initial transition that allows to enter the state machine
        List<StateMachineTransition> initialTransitions = new ArrayList<>();
        NodeList initialList = scxml.getElementsByTagName("initial"); //$NON-NLS-1$
        for (int i = 0; i < initialList.getLength(); i++) {
            Element initial = (Element)initialList.item(i);
            initialTransitions.addAll(getTransitionsFromXMLState(initial, nodeList));

            // If we found an initial state with transitions, we can stop there
            if (!initialTransitions.isEmpty()) {
                break;
            }
        }

        // If we didn't find any "initial" state, just look at the scxml header to
        // verify if an initial state was declared
        if (initialTransitions.isEmpty()) {
            Node initial = scxml.getAttributes().getNamedItem("initial"); //$NON-NLS-1$
            if (initial != null && nodeList.containsKey(initial.getNodeValue())) {
                initialTransitions.addAll(NonNullUtils.checkNotNull(nodeList.get(initial.getNodeValue())).getTransitions());
            }
        }

        if (!initialTransitions.isEmpty()) {
            return initialTransitions;
        }

        // We didn't find any initial transition
        return null;
    }

    /**
     * Function that allows to return the XML version of the state machine in order to save it for
     * future usage.
     * @param initialTransitions The list of initial transitions of the state machine
     * @return The string representing the XML version of the state machine
     */
    public static String getXMLFromModel(List<StateMachineTransition> initialTransitions) {
        StringBuilder sb = new StringBuilder();

        Set<StateMachineNode> readNode = new HashSet<>();
        LinkedList<StateMachineNode> nodes = new LinkedList<>();

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"); //$NON-NLS-1$
        sb.append("<scxml xmlns=\"http://www.w3.org/2005/07/scxml\" version=\"1.0\">\n"); //$NON-NLS-1$

        sb.append("\t<initial>\n"); //$NON-NLS-1$
        for (StateMachineTransition transition : initialTransitions) {
            String fullEvent = transition.getEventName();
            for (Entry<String, String> entry : transition.getEventContexts().entrySet()) {
                fullEvent += String.format("|%s=%s", //$NON-NLS-1$
                        entry.getKey(),
                        entry.getValue());
            }
            sb.append(String.format("\t\t<transition event=\"%s\" target=\"%s\"/>\n", //$NON-NLS-1$
                    fullEvent,
                    transition.getNextNode().getName()));

            if (readNode.add(transition.getNextNode())) {
                nodes.add(transition.getNextNode());
            }
        }
        sb.append("\t</initial>\n\n"); //$NON-NLS-1$

        while (!nodes.isEmpty()) {
            StateMachineNode node = nodes.pop();

            sb.append(String.format("\t<state id=\"%s\">\n", //$NON-NLS-1$
                    node.getName()));
            if (!node.getVariables().isEmpty()) {
                sb.append("\t\t<onentry>\n"); //$NON-NLS-1$
                for (StateMachineVariable variable : node.getVariables()) {
                    sb.append(String.format("\t\t\t<assign location=\"%s\" expr=\"0\"/>\n", //$NON-NLS-1$
                            variable.getName()));
                }
                sb.append("\t\t</onentry>\n\n"); //$NON-NLS-1$
            }

            for (StateMachineTransition transition : node.getTransitions()) {
                String fullEvent = transition.getEventName();
                for (Entry<String, String> entry : transition.getEventContexts().entrySet()) {
                    fullEvent += String.format("|%s=%s", //$NON-NLS-1$
                            entry.getKey(),
                            entry.getValue());
                }

                sb.append(String.format("\t\t<transition event=\"%s\" target=\"%s\"%s/>\n", //$NON-NLS-1$
                        fullEvent,
                        transition.getNextNode().getName(),
                        (transition.getConstraints().isEmpty()) ?
                                "" //$NON-NLS-1$
                                : String.format(" cond=\"%s\"", //$NON-NLS-1$
                                        Joiner.on("; ").join( //$NON-NLS-1$
                                                transition.getConstraints()
                                                .stream()
                                                .map(a -> String.format("%s %s %s", //$NON-NLS-1$
                                                        a.getVarName(),
                                                        a.getOperator().toString()
                                                         .replace("<", "&lt;") //$NON-NLS-1$ //$NON-NLS-2$
                                                         .replace(">", "&gt;"), //$NON-NLS-1$ //$NON-NLS-2$
                                                        a.getValue()))
                                                .collect(Collectors.toList())
                                                )
                                              )
                        ));
            }

            sb.append("\t</state>\n\n"); //$NON-NLS-1$
        }

        sb.append("</scxml>"); //$NON-NLS-1$

        return sb.toString();
    }

    /**
     * Allows to convert a state machine in our internal Java class format
     * into a graphviz state machine in order to print it with dot -Tpdf -O /tmp/sm.dot
     * @author Raphaël Beamonte
     */
    public static class StateMachineToDot {

        private static class TransitionString {
            public final StateMachineNode from;
            public final StateMachineNode to;
            public final String event;
            public final List<String> constraints;

            public TransitionString(StateMachineNode from, StateMachineTransition transition) {
                this.from = from;
                this.to = transition.getNextNode();
                this.event = transition.getFullEvent();
                this.constraints = transition.getConstraints().stream().map(c -> c.toString()).collect(Collectors.toList());
            }
        }

        private Map<StateMachineNode, Integer> nodesToPrint = new HashMap<>();
        private List<TransitionString> transitionsToPrint = new ArrayList<>();

        private StateMachineToDot() {
            nodesToPrint.put(null, nodesToPrint.size());
        }

        /**
         * Uses the StateMachineToDot class to draw the state machine
         * from the initial transition
         * @param initialTransition the initial transition
         * @return the string representation of the state machine
         */
        public static String drawStateMachine(StateMachineTransition initialTransition) {
            StateMachineToDot smd = new StateMachineToDot();
            smd.prepareToDraw(null, initialTransition);
            return smd.toString();
        }

        /**
         * Uses the StateMachineToDot class to draw the state machine
         * from the initial transition
         * @param initialTransitions the list of the initial transitions
         * @return the string representation of the state machine
         */
        public static String drawStateMachine(List<StateMachineTransition> initialTransitions) {
            StateMachineToDot smd = new StateMachineToDot();
            for (StateMachineTransition t : initialTransitions) {
                smd.prepareToDraw(null, t);
            }
            return smd.toString();
        }

        private void prepareToDraw(StateMachineNode previousNode, StateMachineTransition transition) {
            // Add that transition to the list
            transitionsToPrint.add(new TransitionString(
                    previousNode,
                    transition
                ));

            // Add the node
            if (!nodesToPrint.containsKey(transition.getNextNode())) {
                nodesToPrint.put(transition.getNextNode(), nodesToPrint.size());

                // Then read the transitions inside the node
                for (StateMachineTransition t : transition.getNextNode().getTransitions()) {
                    prepareToDraw(transition.getNextNode(), t);
                }
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append("digraph SM {\n"); //$NON-NLS-1$
            //sb.append("\trankdir = LR;\n"); //$NON-NLS-1$
            sb.append("\tforcelabels=true;\n"); //$NON-NLS-1$
            sb.append("\tedge [arrowsize=0.4,fontsize=10]\n"); //$NON-NLS-1$
            sb.append("\t//------nodes and variables------\n"); //$NON-NLS-1$
            for (@NonNull Entry<StateMachineNode, Integer> entry : nodesToPrint.entrySet()) {
                sb.append(String.format("\tnode%d [label=\"%s\",style=filled,fillcolor=lightgrey,shape=circle%s];\n", //$NON-NLS-1$
                        entry.getValue(),
                        (entry.getKey() == null) ? "" : entry.getKey().getName(), //$NON-NLS-1$
                        (entry.getKey() == null) ? ",width=.1,height=.1" : "" //$NON-NLS-1$ //$NON-NLS-2$
                        ));
                if (entry.getKey() != null && entry.getKey().getVariables().size() > 0) {
                    sb.append(String.format("\tnode%d -> node%d [label=<<FONT COLOR=\"blue\">%s</FONT>>,weight=100,penwidth=0.0,arrowhead=none];\n", //$NON-NLS-1$
                            entry.getValue(),
                            entry.getValue(),
                            Joiner.on("<BR/>").join(entry.getKey().getVariables()) //$NON-NLS-1$
                            ));
                }
            }
            sb.append("\t//------transitions------\n"); //$NON-NLS-1$
            for (TransitionString ts : transitionsToPrint) {
                sb.append(String.format("\tnode%d -> node%d [label=<<I>%s</I>%s>,weight=3];\n", //$NON-NLS-1$
                        nodesToPrint.get(ts.from),
                        nodesToPrint.get(ts.to),
                        ts.event,
                        (ts.constraints.isEmpty()) ?
                                "" //$NON-NLS-1$
                                : String.format("<BR/><FONT POINT-SIZE=\"8\" COLOR=\"red\">%s</FONT>", //$NON-NLS-1$
                                        Joiner.on("<BR/>").join( //$NON-NLS-1$
                                                ts.constraints
                                                .stream()
                                                .map(a -> a.replace("<", "&lt;") //$NON-NLS-1$ //$NON-NLS-2$
                                                           .replace(">", "&gt;")) //$NON-NLS-1$ //$NON-NLS-2$
                                                .collect(Collectors.toList())
                                                )
                                              )
                        ));
            }
            sb.append("}\n"); //$NON-NLS-1$

            return sb.toString();
        }
    }

    /**
     * @param value the value
     * @param total the total
     * @return the numeric value to compare to
     */
    public static double getValueFromPercent(String value, double total) {
        double numval;
        if (value.endsWith("%")) { //$NON-NLS-1$
            numval = total *  Double.parseDouble(value.substring(0, value.length()-1)) / 100.0;
        } else if (UNIT_PATTERN.matcher(value).matches()) {
            ITmfTimestamp tv = strToTimestamp(value);
            numval = tv.normalize(0, ITmfTimestamp.NANOSECOND_SCALE).getValue();
        } else {
            numval = Double.parseDouble(value);
        }
        return numval;
    }

    /**
     * @param value A string value representing a timestamp
     * @return The timestamp generated from the string value
     */
    public static ITmfTimestamp strToTimestamp(String value) {
        Matcher m = TIMESTAMP_PATTERN.matcher(value);

        if (m.find()) {
            long numval = 0;
            double mult = 1;

            if (!m.group("value").equals("?")) { //$NON-NLS-1$ //$NON-NLS-2$
                numval = Long.parseLong(m.group("int")); //$NON-NLS-1$

                if (m.group("dec") != null) { //$NON-NLS-1$
                    mult = Math.pow(10, m.group("dec").length()); //$NON-NLS-1$
                    numval = numval * (long)mult + Long.parseLong(m.group("dec")); //$NON-NLS-1$
                }
            }

            int scale = ITmfTimestamp.NANOSECOND_SCALE;
            switch (m.group("unit")) { //$NON-NLS-1$
            case "ns": //$NON-NLS-1$
                break;
            case "us": //$NON-NLS-1$
                scale = ITmfTimestamp.MICROSECOND_SCALE;
                break;
            case "ms": //$NON-NLS-1$
                scale = ITmfTimestamp.MILLISECOND_SCALE;
                break;
            case "s": //$NON-NLS-1$
                scale = ITmfTimestamp.SECOND_SCALE;
                break;
            case "m": //$NON-NLS-1$
                scale = ITmfTimestamp.SECOND_SCALE;
                numval *= 60;
                break;
            case "h": //$NON-NLS-1$
                scale = ITmfTimestamp.SECOND_SCALE;
                numval *= 60*60;
                break;
            default:
                break;
            }

            if (mult > 1) {
                while (mult > 1 && scale != ITmfTimestamp.NANOSECOND_SCALE) {
                    mult /= 1000;
                    switch (scale) {
                    case ITmfTimestamp.MICROSECOND_SCALE:
                        scale = ITmfTimestamp.NANOSECOND_SCALE;
                        break;
                    case ITmfTimestamp.MILLISECOND_SCALE:
                        scale = ITmfTimestamp.MICROSECOND_SCALE;
                        break;
                    case ITmfTimestamp.SECOND_SCALE:
                        scale = ITmfTimestamp.MILLISECOND_SCALE;
                        break;
                    default:
                        break;
                    }
                }
                numval /= mult;
            }

            return TmfTimestamp.create(numval, scale).getDelta(TmfTimestamp.ZERO);
        }
        return null;
    }

    /**
     * To improve on TmfTimeRange by adding methods to get the duration of the
     * timestamp interval
     *
     * @author Raphaël Beamonte
     */
    public static class TimestampInterval extends TmfTimeRange implements Comparable<TimestampInterval> {
        /**
         * @param start The start timestamp as an ITmfTimestamp object
         * @param end The end timestamp as an ITmfTimestamp object
         */
        public TimestampInterval(ITmfTimestamp start, ITmfTimestamp end) {
            super(NonNullUtils.checkNotNull(start), NonNullUtils.checkNotNull(end));
        }

        /**
         * @param startTime The start timestamp as a long
         * @param endTime The end timestamp as a long
         */
        public TimestampInterval(long startTime, long endTime) {
            super(TmfTimestamp.create(NonNullUtils.checkNotNull(startTime), ITmfTimestamp.NANOSECOND_SCALE),
                    TmfTimestamp.create(NonNullUtils.checkNotNull(endTime), ITmfTimestamp.NANOSECOND_SCALE));
        }

        /**
         * @return The duration of the interval
         */
        public long getDuration() {
            return getEndTime().getDelta(getStartTime()).getValue();
        }

        @Override
        public String toString() {
            return String.format("[%s, %s]", //$NON-NLS-1$
                    getStartTime().toString(),
                    getEndTime().toString());
        }

        /**
         * @param tiCollection A collection of TimestampInterval
         * @return The tiniest interval grouping all the intervals being in the collection
         */
        public static TimestampInterval maxTsInterval(Collection<TimestampInterval> tiCollection) {
            ITmfTimestamp minTs = null, maxTs = null;
            for (TimestampInterval ti : tiCollection) {
                if (minTs == null || ti.getStartTime().compareTo(minTs) < 0) {
                    minTs = ti.getStartTime();
                }
                if (maxTs == null || ti.getEndTime().compareTo(maxTs) > 0) {
                    maxTs = ti.getEndTime();
                }
            }
            return new TimestampInterval(minTs, maxTs);
        }

        @Override
        public int compareTo(TimestampInterval ti) {
            int cmp = this.getStartTime().compareTo(ti.getStartTime());
            if (cmp == 0) {
                cmp = this.getEndTime().compareTo(ti.getEndTime());
            }
            return cmp;
        }
    }

}
