/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.ui.views.vresources;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.module.StateValues;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;

/**
 * @author Cedric Biancheri
 */
public class Machine {

    private String machineName;
    private Machine host = null;
    private Boolean highlighted;
    private float heightFactory;
    private Set<Processor> cpus = new HashSet<>();
    private Set<Processor> pcpus = new HashSet<>();
    private Set<Machine> containers = new HashSet<>();
    private Set<Machine> virtualMachines = new HashSet<>();
    private ITmfStateValue typeMachine;

//    public Machine(String name) {
//        machineName = name;
//        highlighted = true;
//    }

    public Machine(String name, @NonNull ITmfStateValue type) {
        machineName = name;
        highlighted = true;
        heightFactory = VirtualResourcePresentationProvider.FULL_HEIGHT;
        typeMachine = type;
    }

    public Machine(String name, Integer nbCPUs, @NonNull ITmfStateValue type) {
        machineName = name;
        highlighted = true;
        heightFactory = VirtualResourcePresentationProvider.FULL_HEIGHT;
        typeMachine = type;
        for (Integer i = 0; i < nbCPUs; i++) {
            cpus.add(new Processor(i.toString(), this));
        }
    }

    public Machine(String name, @NonNull ITmfStateValue type, List<String> pcpus) {
        machineName = name;
        highlighted = true;
        heightFactory = VirtualResourcePresentationProvider.FULL_HEIGHT;
        typeMachine = type;
        for (String pcpu : pcpus) {
            this.pcpus.add(new Processor(pcpu, this));
        }
    }

    public static Machine createContainer(String name, Machine host) {
        Machine container = new Machine(name, 0, StateValues.MACHINE_CONTAINER_VALUE);
        container.setHost(host);
        return container;
    }

    public String getMachineName() {
        return machineName;
    }

    public float getHeightFactory() {
        return heightFactory;
    }

    public ITmfStateValue getTypeMachine() {
        return typeMachine;
    }

    public void setHost(Machine h) {
        host = h;
    }

    public Machine getHost() {
        return host;
    }

    public Set<Machine> getContainers() {
        return containers;
    }

    public Set<Machine> getVirtualMachines() {
        return virtualMachines;
    }

    public void addCpu(String cpu) {
        cpus.add(new Processor(cpu, this));
    }

    public void addPCpu(String pcpu) {
        pcpus.add(new Processor(pcpu, this));
    }

    public void addContainer(Machine machine) {
        if ((machine.getTypeMachine().unboxInt() & StateValues.MACHINE_CONTAINER) != StateValues.MACHINE_CONTAINER) {
            return;
        }
        containers.add(machine);
    }

    public void addVirtualMachine(Machine machine) {
        if ((machine.getTypeMachine().unboxInt() & StateValues.MACHINE_GUEST) != StateValues.MACHINE_GUEST) {
            return;
        }
        virtualMachines.add(machine);
    }

    public Boolean isHighlighted() {
        return highlighted;
    }

    public void setHighlightedWithAllCpu(Boolean b) {
        highlighted = b;
        if (b) {
            heightFactory = VirtualResourcePresentationProvider.FULL_HEIGHT;
        } else {
            heightFactory = VirtualResourcePresentationProvider.REDUCED_HEIGHT;
        }
        for (Processor p : cpus) {
            p.setHighlighted(b);
        }
    }

    public void setHighlightedWithAllContainers(Boolean b) {
        highlighted = b;
        if (b){
            heightFactory = VirtualResourcePresentationProvider.FULL_HEIGHT;
        } else {
            heightFactory = VirtualResourcePresentationProvider.REDUCED_HEIGHT;
        }
        for (Machine c : containers) {
            c.setHighlighted(b);
        }
    }

    public void setHighlighted(Boolean b) {
        highlighted = b;
    }

    public void setHighlightedCpu(int cpu, Boolean b) {
        for (Processor p : getCpus()) {
            if (Integer.parseInt(p.getNumber()) == cpu) {
                p.setHighlighted(b);
                if (b) {
                    setHighlighted(b);
                } else {
                    setHighlighted(isOneCpuHighlighted());
                }
            }
        }
    }

    public Set<Processor> getCpus() {
        return cpus;
    }

    public Set<Processor> getPCpus() {
        return pcpus;
    }

    public Boolean isCpuHighlighted(String p) {
        for (Processor proc : cpus) {
            if (p.equals(proc.toString())) {
                return proc.isHighlighted();
            }
        }
        return false;
    }

    public Boolean isCpuHighlighted(int p) {
        for (Processor proc : cpus) {
            if (p == Integer.parseInt(proc.toString())) {
                return proc.isHighlighted();
            }
        }
        return false;
    }

    public Boolean areAllCpusHighlighted() {
        Boolean res = true;
        for (Processor p : getCpus()) {
            res &= p.isHighlighted();
        }
        return res;
    }

    public Boolean areAllCpusNotHighlighted() {
        Boolean res = true;
        for (Processor p : getCpus()) {
            res &= !p.isHighlighted();
        }
        return res;
    }

    public Boolean isOneCpuHighlighted() {
        Boolean res = false;
        for (Processor p : getCpus()) {
            if (p.isHighlighted()) {
                return true;
            }
        }
        return res;
    }

    public void setHighlightedContainer(String c, Boolean b) {
        for (Machine container : getContainers()) {
            if (container.getMachineName().equals(c)) {
                container.setHighlighted(b);
                if (b) {
                    setHighlighted(b);
                } else {
                    setHighlighted(isOneContainerHighlighted());
                }
            }
        }
    }

    public Boolean isContainerHighlighted(String c) {
        if (c == null) {
            return false;
        }
        for (Machine container : getContainers()) {
            if (c.equals(container.getMachineName())) {
                return container.isHighlighted();
            }
        }
        return false;
    }

    public Boolean isContainerHighlighted(long c) {
        for (Machine container : getContainers()) {
            if (c == Long.parseLong(container.getMachineName())) {
                return container.isHighlighted();
            }
        }
        return false;
    }

    public Boolean areAllContainersHighlighted() {
        Boolean res = true;
        for (Machine container : getContainers()) {
            res &= container.isHighlighted();
        }
        return res;
    }

    public Boolean areAllContainersNotHighlighted() {
        Boolean res = true;
        for (Machine container : getContainers()) {
            res &= !container.isHighlighted();
        }
        return res;
    }

    public Boolean isOneContainerHighlighted() {
        Boolean res = false;
        for (Machine container : getContainers()) {
            if (container.isHighlighted()) {
                return true;
            }
        }
        return res;
    }

    public Boolean cpusNodeIsGrayed() {
        return !areAllCpusHighlighted() && isOneCpuHighlighted();
    }

    public Boolean containersNodeIsGrayed() {
        return !areAllContainersHighlighted() && isOneContainerHighlighted();
    }

    public Boolean isGrayed() {
        return !(areAllCpusHighlighted() && areAllContainersHighlighted()) && (isOneCpuHighlighted() || isOneContainerHighlighted());
    }

    public Boolean isChecked() {
        return isOneContainerHighlighted() || isOneCpuHighlighted();
    }

    @Override
    public String toString() {
        return machineName;
    }

    public void displayMachine() {
        displayMachineRec(0);
    }

    private void displayMachineRec(int nbTab) {
        String tab = "";
        for (int i = 0; i < nbTab; i++) {
            tab += "\t";
        }
        System.out.println(tab + "Machine: " + this);
        System.out.println(tab + "\tVMs of " + this +  ":");
        for (Machine vm : getVirtualMachines()) {
            vm.displayMachineRec(nbTab + 2);
        }
        System.out.println(tab + "\tContainers of " + this +  ":");
        for (Machine container : getContainers()) {
            container.displayMachineRec(nbTab + 2);
        }
    }
}
