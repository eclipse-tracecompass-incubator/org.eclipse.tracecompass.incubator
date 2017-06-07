/*******************************************************************************
 * Copyright (c) 2016-2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.ui.views.vresources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfNavigatorContentProvider;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfNavigatorLabelProvider;
import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;

/**
 * Dialog that allows to select the elements to highlight in the view
 *
 * @author Cédric Biancheri
 * @author Geneviève Bastien
 */
public class SelectMachineDialog extends CheckedTreeSelectionDialog {

    private static final TmfNavigatorContentProvider CONTENT_PROVIDER = new TmfNavigatorContentProvider() {

        @Override
        public Object getParent(Object element) {
            if (element instanceof Machine) {
                Machine host = ((Machine) element).getHost();
                if (host != null) {
                    return host;
                }
            } else if (element instanceof Processor) {
                return ((Processor) element).getMachine();
            }
            return null;
        }

        @Override
        public Object[] getElements(Object inputElement) {
            if (inputElement instanceof Collection<?>) {
                return ((Collection<?>) inputElement).toArray();
            }
            return Collections.singleton(inputElement).toArray();
        }

        @Override
        public synchronized Object[] getChildren(Object parentElement) {
            List<Object> children = new ArrayList<>();
            if (parentElement instanceof Collection<?>) {
                return ((Collection<?>) parentElement).toArray();
            } else if (parentElement instanceof Machine) {
                Machine m = (Machine) parentElement;
                children.addAll(m.getVirtualMachines());
                children.addAll(m.getContainers());
                // FIXME: Selecting CPUs does not work yet
//                children.addAll(m.getPhysicalCpus());
//                children.addAll(m.getCpus());
            }
            return children.toArray(new Object[children.size()]);
        }

        @Override
        public boolean hasChildren(Object element) {
            Object[] children = getChildren(element);
            return children != null && children.length > 0;
        }

    };
    private static final TmfNavigatorLabelProvider LABEL_PROVIDER = new TmfNavigatorLabelProvider() {
        @Override
        public String getText(Object arg0) {
            if (arg0 instanceof Processor) {
                return NLS.bind(Messages.SelectMachineDialog_CpuText, ((Processor) arg0).getNumber());
            } else if (arg0 instanceof Machine) {
                return ((Machine) arg0).getMachineName();
            }
            return arg0.toString();
        }
    };

    /**
     * Standard constructor
     *
     * @param parent
     *            The parent shell
     */
    public SelectMachineDialog(Shell parent) {
        super(parent, LABEL_PROVIDER, CONTENT_PROVIDER);
        setShellStyle(getShellStyle() | SWT.RESIZE);
        setTitle(Messages.SelectMachineDialog_Title);
        setMessage(Messages.SelectMachineDialog_SelectMachineMessage);
    }

}
