package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis.core.data.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis.VMNativeComparisonAnalysis;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.AbstractTmfTraceDataProvider;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Time graph data provider for VM/Native analysis
 */
public class VMNativeTimeGraphDataProvider extends AbstractTmfTraceDataProvider
        implements ITimeGraphDataProvider<@NonNull ITimeGraphEntryModel> {

    public static final String ID = "org.eclipse.tracecompass.incubator.overhead.timegraph.dataprovider"; //$NON-NLS-1$

    private static final int IDLE_STATE = 0;
    private static final int NATIVE_STATE = 1;
    private static final int VM_STATE = 2;
    private static final int HOST_OVERHEAD_STATE = 3;

    private final VMNativeComparisonAnalysis fModule;
    private long fNextEntryId = 0;

    /**
     * @param trace
     * @param module
     */
    public VMNativeTimeGraphDataProvider(@NonNull ITmfTrace experiment,
            VMNativeComparisonAnalysis module) {
        super(experiment);
        fModule = module;
    }

    @Override
    public @NonNull String getId() {
        return ID;
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull TimeGraphModel> fetchRowModel(
            @NonNull Map<@NonNull String, @NonNull Object> fetchParameters,
            @Nullable IProgressMonitor monitor) {

        fModule.waitForInitialization();
        ITmfStateSystem fStateSystem = fModule.getStateSystem();

        if (fStateSystem == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED,
                    CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
        }

        SelectionTimeQueryFilter filter = createSelectionTimeQuery(fetchParameters);
        if (filter == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED,
                    CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
        }

        List<ITimeGraphRowModel> rows = new ArrayList<>();
        List<Integer> quarks = fStateSystem.getSubAttributes(-1, false);

        for (Integer quark : quarks) {
            String attributeName = fStateSystem.getAttributeName(quark);

            if (attributeName.equals("natif_flow") || attributeName.equals("vm_flow")) { //$NON-NLS-1$ //$NON-NLS-2$
                List<Integer> pidQuarks = fStateSystem.getSubAttributes(quark, false);

                for (Integer pidQuark : pidQuarks) {
                    List<ITimeGraphState> states = getStatesForProcess(pidQuark, filter, attributeName);
                    ITimeGraphRowModel rowModel = new TimeGraphRowModel(fNextEntryId++, states);
                    rows.add(rowModel);
                }
            }
        }

        TimeGraphModel model = new TimeGraphModel(rows);
        return new TmfModelResponse<>(model, ITmfResponse.Status.COMPLETED,
                CommonStatusMessage.COMPLETED);
    }

    private List<ITimeGraphState> getStatesForProcess(int pidQuark,
            SelectionTimeQueryFilter filter,
            String flowType) {

        List<ITimeGraphState> states = new ArrayList<>();

        fModule.waitForInitialization();
        ITmfStateSystem fStateSystem = fModule.getStateSystem();

        if (fStateSystem == null) {
            return Collections.EMPTY_LIST;
        }

        try {
            int eventQuark = fStateSystem.optQuarkRelative(pidQuark, "event"); //$NON-NLS-1$
            if (eventQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                return states;
            }

            long startTime = Math.max(filter.getStart(), fStateSystem.getStartTime());
            long endTime = Math.min(filter.getEnd(), fStateSystem.getCurrentEndTime());

            Iterable<ITmfStateInterval> intervals = fStateSystem.query2D(
                    Collections.singletonList(eventQuark), startTime, endTime);

            for (ITmfStateInterval interval : intervals) {
                Object value = interval.getValue();

                if (value != null) {
                    String eventName = value.toString();
                    int stateValue = determineStateFromEvent(eventName, flowType);

                    ITimeGraphState timeGraphState = new TimeGraphState(
                            interval.getStartTime(),
                            interval.getEndTime() - interval.getStartTime(),
                            stateValue,
                            eventName);
                    states.add(timeGraphState);
                }
            }

        } catch (StateSystemDisposedException e) {
            // Retourner la liste partielle
        }

        return states;
    }

    private static int determineStateFromEvent(String eventName, String flowType) {
        if (eventName.contains("kvm_x86_exit")) { //$NON-NLS-1$
            return HOST_OVERHEAD_STATE;
        } else if (eventName.contains("kvm_x86_entry")) { //$NON-NLS-1$
            return VM_STATE;
        } else if (flowType.equals("natif_flow")) { //$NON-NLS-1$
            return NATIVE_STATE;
        } else if (flowType.equals("vm_flow")) { //$NON-NLS-1$
            return VM_STATE;
        }

        return IDLE_STATE;
    }

    @SuppressWarnings("null")
    @Override
    public @NonNull TmfModelResponse<@NonNull TmfTreeModel<@NonNull ITimeGraphEntryModel>> fetchTree(
            @NonNull Map<@NonNull String, @NonNull Object> fetchParameters,
            @Nullable IProgressMonitor monitor) {

        List<ITimeGraphEntryModel> entryList = new ArrayList<>();

        fModule.waitForInitialization();
        ITmfStateSystem fStateSystem = fModule.getStateSystem();

        if (fStateSystem == null) {
            return new TmfModelResponse<>(new TmfTreeModel<>(Collections.emptyList(), entryList),
                    ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }

        // Entrée racine
        long rootId = fNextEntryId++;
        ITimeGraphEntryModel rootEntry = new TimeGraphEntryModel(rootId, -1,
                "VM/Native Analysis", //$NON-NLS-1$
                fStateSystem.getStartTime(),
                fStateSystem.getCurrentEndTime());
        entryList.add(rootEntry);

        // Entrées pour chaque processus
        List<Integer> quarks = fStateSystem.getSubAttributes(-1, false);

        for (Integer quark : quarks) {
            String attributeName = fStateSystem.getAttributeName(quark);

            if (attributeName.equals("natif_flow") || attributeName.equals("vm_flow")) { //$NON-NLS-1$ //$NON-NLS-2$
                List<Integer> pidQuarks = fStateSystem.getSubAttributes(quark, false);

                for (Integer pidQuark : pidQuarks) {
                    String pidStr = fStateSystem.getAttributeName(pidQuark);

                    try {
                        int pid = Integer.parseInt(pidStr);
                        long entryId = fNextEntryId++;

                        String label = attributeName.equals("natif_flow") ? //$NON-NLS-1$
                                "Native Process " + pid : "VM Process " + pid; //$NON-NLS-1$ //$NON-NLS-2$

                        ITimeGraphEntryModel entry = new TimeGraphEntryModel(
                                entryId, rootId, label,
                                fStateSystem.getStartTime(), fStateSystem.getCurrentEndTime());

                        entryList.add(entry);

                    } catch (NumberFormatException e) {
                        // Ignorer les attributs qui ne sont pas des PID
                    }
                }
            }
        }

        return new TmfModelResponse<>(new TmfTreeModel<>(Collections.emptyList(), entryList),
                ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull Map<@NonNull String, @NonNull String>> fetchTooltip(
            @NonNull Map<@NonNull String, @NonNull Object> fetchParameters,
            @Nullable IProgressMonitor monitor) {

        Map<String, String> tooltip = new HashMap<>();
        tooltip.put("Analysis", "VM/Native Comparison"); //$NON-NLS-1$ //$NON-NLS-2$

        return new TmfModelResponse<>(tooltip, ITmfResponse.Status.COMPLETED,
                CommonStatusMessage.COMPLETED);
    }

    private @Nullable static SelectionTimeQueryFilter createSelectionTimeQuery(
            @NonNull Map<@NonNull String, @NonNull Object> fetchParameters) {
        List<@NonNull Long> times = DataProviderParameterUtils.extractTimeRequested(fetchParameters);
        List<@NonNull Long> items = DataProviderParameterUtils.extractTimeRequested(fetchParameters);

        if (times != null && !times.isEmpty() && items != null) {
            long startTime = times.get(0);
            long endTime = times.get(times.size() - 1);
            return new SelectionTimeQueryFilter(startTime, endTime, 1, items);
        }

        return new SelectionTimeQueryFilter(0L, Long.MAX_VALUE, 1, Collections.emptyList());
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull List<@NonNull ITimeGraphArrow>> fetchArrows(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(Collections.emptyList(), ITmfResponse.Status.COMPLETED, "No arrows available"); //$NON-NLS-1$
    }
}