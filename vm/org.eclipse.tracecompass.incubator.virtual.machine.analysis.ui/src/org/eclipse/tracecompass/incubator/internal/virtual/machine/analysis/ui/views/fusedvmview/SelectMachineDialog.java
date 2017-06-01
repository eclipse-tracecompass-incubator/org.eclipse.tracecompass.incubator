/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.ui.views.fusedvmview;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.data.Attributes;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfNavigatorContentProvider;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfNavigatorLabelProvider;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

/**
 * @author Cédric Biancheri
 */
public class SelectMachineDialog extends TitleAreaDialog {

    private final FusedVMViewPresentationProvider provider;
    private CheckboxTreeViewer fCheckboxTreeViewer;
    private TmfNavigatorContentProvider fContentProvider;
    private TmfNavigatorLabelProvider fLabelProvider;

    /**
     * Open the select machines window
     *
     * @param parent
     *            The parent shell
     * @param provider
     *            The presentation provider
     */
    public static void open(Shell parent, FusedVMViewPresentationProvider provider) {
        (new SelectMachineDialog(parent, provider)).open();
    }

    /**
     * Standard constructor
     *
     * @param parent
     *            The parent shell
     * @param provider
     *            The presentation provider
     */
    public SelectMachineDialog(Shell parent, FusedVMViewPresentationProvider provider) {
        super(parent);
        this.provider = provider;
        this.setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        if (provider == null) {
            return null;
        }
        createMachinesGroup(parent);

        setTitle(Messages.FusedVMView_SELECT_MACHINE);
        setDialogHelpAvailable(false);
        setHelpAvailable(false);

        return parent;
    }

    private void createMachinesGroup(Composite composite) {

        Map<String, Machine> machines = provider.getHighlightedMachines();

        new FilteredTree(composite, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER, new PatternFilter(), true) {
            @Override
            protected TreeViewer doCreateTreeViewer(Composite aparent, int style) {
                return SelectMachineDialog.this.doCreateTreeViewer(aparent, machines);
            }
        };
    }

    private TreeViewer doCreateTreeViewer(Composite parent, Map<String, Machine> machines) {
        fCheckboxTreeViewer = new CheckboxTreeViewer(parent, SWT.BORDER);

        fContentProvider = new TmfNavigatorContentProvider() {

            @Override
            public Object getParent(Object element) {
                if (element instanceof Machine) {
                    Machine host = ((Machine) element).getHost();
                    if (host != null) {
                        return host;
                    }
                    return element;
                } else if (element instanceof Processor) {
                    return ((Processor) element).getMachine();
                }
                return null;
            }

            @Override
            public Object[] getElements(Object inputElement) {
                return getChildren(inputElement);
            }

            @Override
            public synchronized Object[] getChildren(Object parentElement) {
                if (parentElement instanceof List) {
                    return ((List<?>) parentElement).toArray();
                } else if (parentElement instanceof Machine) {
                    Machine m = (Machine) parentElement;
                    Set<Processor> cpus = m.getCpus();
                    if (!cpus.isEmpty()) {
                        Object[] array = { cpus, m.getContainers() };
                        return array;
                    }
                } else if (parentElement instanceof Set) {
                    return ((Set<?>) parentElement).toArray();
                }
                return null;
            }

            @Override
            public boolean hasChildren(Object element) {
                Object[] children = getChildren(element);
                return children != null && children.length > 0;
            }

        };
        fCheckboxTreeViewer.setContentProvider(fContentProvider);
        fLabelProvider = new TmfNavigatorLabelProvider() {
            @Override
            public String getText(Object arg0) {
                if (arg0 instanceof Set<?>) {
                    Set<?> set = (Set<?>) arg0;
                    if (!set.isEmpty()) {
                        for (Object o : set) {
                            if (o instanceof Machine) {
                                return Attributes.CONTAINERS;
                            } else if (o instanceof Processor) {
                                return Attributes.CPUS;
                            }
                        }
                    }
                }
                return arg0.toString();
            }
        };
        fCheckboxTreeViewer.setLabelProvider(fLabelProvider);
        fCheckboxTreeViewer.setComparator(new ViewerComparator());

        final Tree tree = fCheckboxTreeViewer.getTree();
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        tree.setLayoutData(gd);
        tree.setHeaderVisible(true);

        final TreeViewerColumn column = new TreeViewerColumn(fCheckboxTreeViewer, SWT.NONE);
        column.getColumn().setText(Messages.FusedVMView_SelectMachineActionNameText);
        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof String) {
                    return (String) element;
                }
                return fLabelProvider.getText(element);
            }

            @Override
            public Image getImage(Object element) {
                return fLabelProvider.getImage(element);
            }
        });

        // Populate the list with the machines' names
        List<Machine> listMachines = new ArrayList<>(machines.values());
        fCheckboxTreeViewer.setInput(listMachines);
        column.getColumn().pack();

        fCheckboxTreeViewer.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                Object element = event.getElement();
                Object root = null;
                if (element instanceof Machine) {
                    Machine m = (Machine) element;
                    Machine host = m.getHost();
                    if (host == null) {
                        /* We are the root of a machine */
                        m.setHighlighted(event.getChecked());
                        Set<Processor> cpus = m.getCpus();
                        for (Processor cpu : cpus) {
                            cpu.setHighlighted(event.getChecked());
                        }
                        Set<Machine> containers = m.getContainers();
                        for (Machine container : containers) {
                            container.setHighlighted(event.getChecked());
                        }
                    } else {
                        /* We are at a container */
                        m.setHighlighted(event.getChecked());
                        host.setHighlighted(host.isChecked());
                    }
                } else if (element instanceof Processor) {
                    Boolean checked = event.getChecked();
                    ((Processor) element).setHighlighted(checked);
                    Object ancestor = fContentProvider.getParent(element);
                    if (ancestor instanceof Machine) {
                        Machine m = (Machine) ancestor;
                        m.setHighlighted(m.isChecked());
                    }
                } else if (element instanceof Set<?>) {
                    /* We are looking at a set of containers or cpus */
                    Set<?> set = (Set<?>) element;
                    if (!set.isEmpty()) {
                        for (Object o : set) {
                            if (o instanceof Machine) {
                                ((Machine) o).setHighlighted(event.getChecked());
                            } else if (o instanceof Processor) {
                                ((Processor) o).setHighlighted(event.getChecked());
                            }
                            root = fContentProvider.getParent(o);
                        }
                    }
                }
                if (root == null) {
                    root = fContentProvider.getParent(element);
                }
                ((Machine) root).setHighlighted(((Machine) root).isChecked());
                /* Update the view because the selection changed */
                updateCheckedNodes(root);
            }
        });

        /* Initial setting of the checked state when we open the dialog */
        for (Machine m : listMachines) {
            updateCheckedNodes(m);
        }

        return fCheckboxTreeViewer;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
                true);
    }

    /**
     * Method used to update the checked state of each node of the tree. Call
     * this if you modified the model under a specific node.
     *
     * @param node
     *            The node frome where the model has been changed.
     */
    private void updateCheckedNodes(Object node) {
        Boolean isExpanded = fCheckboxTreeViewer.getExpandedState(node);
        fCheckboxTreeViewer.expandToLevel(node, 1);
        if (node instanceof Machine) {
            Machine m = (Machine) node;
            Machine host = m.getHost();

            fCheckboxTreeViewer.setChecked(m, m.isHighlighted());
            fCheckboxTreeViewer.setGrayed(m, m.isGrayed());

            if (host == null) {
                /* We are the root of a machine */
                Set<Processor> cpus = m.getCpus();
                updateCheckedNodes(cpus);
                Set<Machine> containers = m.getContainers();
                updateCheckedNodes(containers);
                Boolean isSetExpanded = fCheckboxTreeViewer.getExpandedState(cpus);
                fCheckboxTreeViewer.expandToLevel(cpus, 1);
                fCheckboxTreeViewer.setChecked(cpus, m.isOneCpuHighlighted());
                fCheckboxTreeViewer.setGrayed(cpus, m.cpusNodeIsGrayed());
                updateCheckedNodes(cpus);
                if (!isSetExpanded) {
                    fCheckboxTreeViewer.collapseToLevel(cpus, 1);
                }

                isSetExpanded = fCheckboxTreeViewer.getExpandedState(containers);
                fCheckboxTreeViewer.expandToLevel(containers, 1);
                fCheckboxTreeViewer.setChecked(containers, m.isOneContainerHighlighted());
                fCheckboxTreeViewer.setGrayed(containers, m.containersNodeIsGrayed());
                updateCheckedNodes(containers);
                if (!isSetExpanded) {
                    fCheckboxTreeViewer.collapseToLevel(containers, 1);
                }

            }
        } else if (node instanceof Processor) {
            Processor p = (Processor) node;
            fCheckboxTreeViewer.setChecked(p, p.isHighlighted());
        } else if (node instanceof Set<?>) {
            Set<?> set = (Set<?>) node;
            if (!set.isEmpty()) {
                for (Object o : set) {
                    updateCheckedNodes(o);
                }
            }
        }
        if (!isExpanded) {
            fCheckboxTreeViewer.collapseToLevel(node, 1);
        }
    }

}
