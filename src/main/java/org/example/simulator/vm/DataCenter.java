package org.example.simulator.vm;

import org.example.simulator.workflow.Schedule;
import org.example.simulator.workflow.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataCenter {

    public static final float BANDWIDTH = 1.0f; // in Gbps

    private List<VmType> vmTypeList;

    private List<Vm> vmList;

    public DataCenter(){
        this.vmTypeList = new ArrayList<>();
        this.vmList = new ArrayList<>();
        createVmTypes();
    }

    public void createVmTypes(){

        // VM type-0
        // AMD Turion MT-34
        // AWS EC2 t2.nano
        VmType type0 = new VmType(0, 0.0058f,
                Stream.of(1.20f, 1.15f, 1.10f, 1.05f, 1.00f, 0.90f).collect(Collectors.toList()),
                Stream.of(1.80f, 1.60f, 1.40f, 1.20f, 1.00f, 0.80f).collect(Collectors.toList()));
        vmTypeList.add(type0);

        // VM type-1
        // AMD Opteron 2218
        // AWS EC2 t2.micro
        VmType type1 = new VmType(1, 0.0116f,
                Stream.of(1.30f, 1.25f, 1.20f, 1.15f, 1.10f, 1.05f).collect(Collectors.toList()),
                Stream.of(2.60f, 2.40f, 2.20f, 2.00f, 1.80f, 1.00f).collect(Collectors.toList()));
        vmTypeList.add(type1);

        // VM type-2
        // Intel Xeon E5450
        // AWS EC2 t2.small
        VmType type2 = new VmType(2, 0.0230f,
                Stream.of(1.35f, 1.17f, 1.00f, 0.85f).collect(Collectors.toList()),
                Stream.of(3.00f, 2.67f, 2.33f, 2.00f).collect(Collectors.toList()));
        vmTypeList.add(type2);

    }

    public VmType findFastestVmType(){
        return vmTypeList.stream().max((tau1,tau2) -> {
                float ps1 = Collections.max(tau1.getProcessingSpeedsInMips());
                float ps2 = Collections.max(tau2.getProcessingSpeedsInMips());
                return Float.compare(ps1, ps2);
            }).get();
    }

    public VmType findCheapestVmType(){
        return vmTypeList.stream().min((tau1,tau2) ->{
            float r1 = tau1.ratioOfComputingCostToMaxProcessingSpeed();
            float r2 = tau2.ratioOfComputingCostToMaxProcessingSpeed();
            return Float.compare(r1, r2);
        }).get();
    }

    public VmType findCostliestVmType(){
        return vmTypeList.stream().max((tau1,tau2) ->{
            float r1 = tau1.ratioOfComputingCostToMaxProcessingSpeed();
            float r2 = tau2.ratioOfComputingCostToMaxProcessingSpeed();
            return Float.compare(r1, r2);
        }).get();
    }

    public Vm launchNewVm(VmType vmType){
        Vm vm = new Vm(vmList.size(),vmType);
        vmList.add(vm);
        return vm;
    }

    public Vm findIdleVm(Schedule schedule, Task t, VmType vmType){
        return schedule.getAssignment().entrySet().stream()
                .filter((e) -> e.getKey().getType().equals(vmType))
                .filter((e) -> {
                    Vm vm = e.getKey();
                    Task t0 = e.getValue().stream().max((t1,t2) -> {
                        float aft1 = t1.actualFinishTime(vm,schedule);
                        float aft2 = t2.actualFinishTime(vm,schedule);
                        return (aft1==aft2)? 0: (aft1<aft2)? -1: 1;
                    }).get();
                    return t0.actualFinishTime(vm,schedule) < t.actualStartTime(vm,schedule);
                }).map((e) -> e.getKey()).findAny().orElse(null);
    }

    public void reset(){
        this.vmList.clear();
    }

    public List<VmType> getVmTypeList() {
        return vmTypeList;
    }

    public void setVmTypeList(List<VmType> vmTypeList) {
        this.vmTypeList = vmTypeList;
    }

    public List<Vm> getVmList() {
        return vmList;
    }

    public void setVmList(List<Vm> vmList) {
        this.vmList = vmList;
    }

    public void log(){
        System.out.println("Data-center:\n");
        this.vmList.forEach(System.out::println);
    }

}
