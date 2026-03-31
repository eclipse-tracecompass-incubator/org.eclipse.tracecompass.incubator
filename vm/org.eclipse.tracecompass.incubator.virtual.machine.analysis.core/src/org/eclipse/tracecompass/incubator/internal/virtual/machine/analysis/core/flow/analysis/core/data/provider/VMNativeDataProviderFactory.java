package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis.core.data.provider;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis.VMNativeComparisonAnalysis;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Factory for VM/Native TimeGraph Data Provider
 */
public class VMNativeDataProviderFactory implements IDataProviderFactory {

    private static final IDataProviderDescriptor DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(TimeGraphDataProvider.ID)
            .setName("Overhead time graph data provider") //$NON-NLS-1$
            .setDescription("This is my example") //$NON-NLS-1$
            .setProviderType(ProviderType.TIME_GRAPH)
            .build();

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(ITmfTrace trace) {
        VMNativeComparisonAnalysis module = TmfTraceUtils.getAnalysisModuleOfClass(trace, VMNativeComparisonAnalysis.class, VMNativeComparisonAnalysis.ID);
        if (module != null) {
            module.schedule();
            return new VMNativeTimeGraphDataProvider(trace, module);
        }
        return null;
    }

    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(@NonNull ITmfTrace trace) {
        VMNativeComparisonAnalysis module = TmfTraceUtils.getAnalysisModuleOfClass(trace, VMNativeComparisonAnalysis.class, VMNativeComparisonAnalysis.ID);
        return module != null ? Collections.singletonList(DESCRIPTOR) : Collections.emptyList();
    }
}
