package org.example.simulator.workflow;

import org.example.simulator.vm.DataCenter;
import org.example.simulator.vm.Vm;
import org.example.simulator.vm.VmType;

import java.util.*;

public class Task {

    // id of a task in XML file
    private String id;

    // name of a task in XML file
    private String name;

    // length of a task in million instructions (MI)
    // length = runtime (second) * processing-speed (MIPS)
    private float length;

    // list of predecessor tasks in the workflow
    private List<Task> predecessors;

    // list of successor tasks in the workflow
    private List<Task> successors;

    // list of files required by this task
    private List<FileItem> requiredData;

    // map containing predecessor-id and size of data transferred by it
    private Map<String,Float> transferredDataSize;

    // constructor
    public Task(String id, String name, float length) {
        this.id = id;
        this.name = name;
        this.length = length;
        this.predecessors = new ArrayList<>();
        this.successors = new ArrayList<>();
        this.requiredData = new ArrayList<>();
        this.transferredDataSize = new HashMap<>();
    }

    // to compute execution time of a task running on a VM
    // in seconds
    public float executionTime(Vm v) {
        return (this.length / v.getProcessingSpeed());
    }

    // to compute average execution time of a task
    // in seconds
    public float averageExecutionTime(List<VmType> vmTypeList){
        return vmTypeList.stream().map((tau) -> {
                Vm v = new Vm(-1,tau);
                return this.executionTime(v);
            })
            .reduce(0.0f,Float::sum) / vmTypeList.size();
    }

    // to compute the time required to transfer data from predecessor task
    // to a task running on same/different VMs
    // in seconds
    public float transferTime(Vm v,Task tp,Vm v_tp) {
        float size;
        if(v!=null && v_tp!=null && v.getId()==v_tp.getId()){
            return 0.0f;
        }
        size = transferredDataSize.getOrDefault(tp.getId(), 0.0f);
        return (size / DataCenter.BANDWIDTH);
    }

    // to compute power consumed by a task running on a VM
    public float powerConsumed(Vm v) {
        return v.powerConsumption() * executionTime(v);
    }

    // to compute monetary cost of a task running on a VM
    // in USD
    public float cost(Vm v) {
        return ((float) Math.ceil(this.executionTime(v))) * v.getType().getCostPerSecond();
    }

    // minimum of costs of running a task on all VMs
    public float minimumCost(List<VmType> vmTypeList) {
        return vmTypeList.stream().map((tau) -> {
                Vm v = new Vm(-1,tau);
                return this.cost(v);
            }).min(Float::compareTo).get();
    }

    // maximum of costs of running a task on all VMs
    public float maximumCost(List<VmType> vmTypeList) {
        return vmTypeList.stream().map((tau) -> {
            Vm v = new Vm(-1,tau);
            return this.cost(v);
        }).max(Float::compareTo).get();
    }

    // from recursive equation-(2)
    public float earliestStartTime(Schedule schedule) {
        return this.predecessors.stream().map((tp) -> {
            Vm v_ti = schedule.getAssignedVm(this);
            Vm v_tp = schedule.getAssignedVm(tp);
            float est = tp.earliestStartTime(schedule);
            float et = tp.executionTime(v_tp);
            float tt = this.transferTime(v_ti, tp, v_tp);
            return est + et + tt;
        })
        .max(Float::compareTo).orElse(0.0f);
    }

    // from recursive equation-(3)
    public float earliestFinishTime(Schedule schedule) {
        Vm v_ti = schedule.getAssignedVm(this);
        return this.earliestStartTime(schedule) + this.executionTime(v_ti);
    }

    // from recursive equation-(4)
    public float latestStartTime(float estdMakespan, Schedule schedule){
        return this.successors.stream().map(ts -> {
            Vm v_ti = schedule.getAssignedVm(this);
            Vm v_ts = schedule.getAssignedVm(ts);
            float lst = ts.latestStartTime(estdMakespan, schedule);
            float tt = ts.transferTime(v_ts,this, v_ti);
            float et = this.executionTime(v_ti);
            return lst - tt - et;
        })
        .min(Float::compareTo).orElse(estdMakespan - this.executionTime(schedule.getAssignedVm(this)));
    }

    // from recursive equation-(5)
    public float latestFinishTime(float estdMakespan, Schedule schedule){
        Vm v_ti = schedule.getAssignedVm(this);
        return this.latestStartTime(estdMakespan, schedule) + this.executionTime(v_ti);
    }

    // from equation-(12)
    public float deadline(float alpha, float estdMakespan, Schedule schedule){
        return alpha * this.latestFinishTime(estdMakespan, schedule);
    }

    // from equation-(13)
    public float priority(List<VmType> vmTypeList, Schedule schedule){
        return this.averageExecutionTime(vmTypeList) + this.successors.stream().map((ts) -> {
                Vm v_ti = schedule.getAssignedVm(this);
                Vm v_ts = schedule.getAssignedVm(ts);
                float tt = ts.transferTime(v_ts, this, v_ti);
                float pr = ts.priority(vmTypeList, schedule);
                return tt + pr;
            }).max(Float::compareTo).orElse(0.0f);
    }

    // from equation-(14)
    public float budget(float surplusBudget, List<VmType> vmTypeList){
        float minCost = this.minimumCost(vmTypeList);
        return minCost + surplusBudget;
    }

    // for modified algorithm
    public float modifiedBudget(float surplusBudget, float beta, List<VmType> vmTypeList){
        float minCost = this.minimumCost(vmTypeList), maxCost = this.maximumCost(vmTypeList);
        return minCost + beta*(maxCost-minCost);
    }

    // from equation-(16)
    public float possibleStartTime(Schedule schedule){
        return this.predecessors.stream().map((tp) -> {
                Vm v_ti = schedule.getAssignedVm(this);
                Vm v_tp = schedule.getAssignedVm(tp);
                float ast = tp.actualStartTime(v_tp, schedule);
                float et = tp.executionTime(v_tp);
                float tt = this.transferTime(v_ti, tp, v_tp);
                return ast + et + tt;
            })
            .max(Float::compareTo).orElse(0.0f);
    }

    // from equation-(17)
    public float actualStartTime(Vm v, Schedule schedule){
        if(!schedule.hasTasksScheduledBefore(this, v)){
            return this.possibleStartTime(schedule);
        }
        Task tb = schedule.getTaskScheduledBefore(this,v);
        float pst_ti = this.possibleStartTime(schedule);
        float aft_tb = tb.actualFinishTime(v, schedule);
        return Math.max(pst_ti, aft_tb);
    }

    // from equation-(18)
    public float actualFinishTime(Vm v, Schedule schedule){
        return this.actualStartTime(v, schedule) + this.executionTime(v);
    }

    // from equation-(21)
    public float minimumNeededProcessingSpeed(float alpha, float estdMakespan, Schedule forDeadline, Schedule forPst){
        return this.length / (this.deadline(alpha,estdMakespan,forDeadline)-this.possibleStartTime(forPst));
    }

    // from equation-(22)
    public float extendedFinishTime(float actualMakespan, Schedule schedule){
        return this.successors.stream().map((ts) -> {
                Vm v_ti = schedule.getAssignedVm(this);
                Vm v_ts = schedule.getAssignedVm(ts);
                float ast = ts.actualStartTime(v_ts,schedule);
                float tt = ts.transferTime(v_ts,this,v_ti);
                return  ast - tt;
            })
            .min(Float::compareTo).orElse(actualMakespan);
    }

    // from equation-(23)
    public float possibleExtendedFinishTime(float actualMakespan, Vm v, Schedule schedule){
        if(!schedule.hasTasksScheduledAfter(this,v)){
            float exft = this.extendedFinishTime(actualMakespan, schedule);
            float ast = this.actualStartTime(v,schedule);
            float ratio = (float) Math.ceil(this.length / v.getType().getMaximumProcessingSpeed());
            return Math.min(exft, ast+ratio);
        }
        Task ta = schedule.getTaskScheduledAfter(this,v);
        float exft_ti = this.extendedFinishTime(actualMakespan, schedule);
        float ast_ti = ta.actualStartTime(v,schedule);
        float ast_ta = this.actualStartTime(v,schedule);
        float ratio = (float) Math.ceil(this.length / v.getType().getMaximumProcessingSpeed());
        return (exft_ti<ast_ta)? Math.min(exft_ti,ast_ti+ratio): Math.min(ast_ta,ast_ti+ratio);
    }

    // from equation-(24)
    public float minimumNeededProcessingSpeedWithinPossibleExtendedFinishTime(float actualMakespan, Vm v, Schedule schedule){
        float pexft = this.possibleExtendedFinishTime(actualMakespan,v,schedule);
        float ast = this.actualStartTime(v,schedule);
        return this.length / (pexft - ast);
    }

    // helper method: to add a predecessor in the list
    public void addPredecessor(Task theTask){
        predecessors.add(theTask);
    }

    // helper method: to add a successor in the list
    public void addSuccessor(Task theTask){
        successors.add(theTask);
    }

    // to compute the amount of data (in MB) transferred by
    // each of predecessor tasks
    public void computeTransferredDataSizes(){
        for(Task tempTask : predecessors){ // for each predecessor task
            String pid = tempTask.id; // get the task-id
            for(FileItem tempFile : requiredData){ // for each input file
                String tempFileName = tempFile.getName(); // get file name
                String[] tempArr = tempFileName.split("[_.]");                   // check if file name contains
                int idx = Arrays.binarySearch(tempArr,pid, Comparator.naturalOrder()); // predecessor's task-id
                if(idx>=0){
                    float tempFileSize = tempFile.getSize();  // if predecessor transferred the file
                    addTransferredDataSize(pid,tempFileSize); // then add entry predecessor's task-id and file size
                }
            }
        }
    }

    // to insert an entry in the map
    private void addTransferredDataSize(String id, float fileSizeInBytes) {
        float fileSizeInGb = fileSizeInBytes * 7.451e-9f;
        float tempSize = 0.0f;
        if(this.transferredDataSize.containsKey(id)){
            tempSize = transferredDataSize.get(id);       // add current file-size with
        }
        transferredDataSize.put(id, tempSize+fileSizeInGb);  // existing file-size
    }

    // getters, setters and tostring

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getLength() {
        return length;
    }

    public void setLength(float length) {
        this.length = length;
    }

    public List<FileItem> getRequiredData() {
        return requiredData;
    }

    public void setRequiredData(List<FileItem> requiredData) {
        this.requiredData = requiredData;
    }

    public List<Task> getPredecessors() {
        return predecessors;
    }

    public void setPredecessors(List<Task> predecessors) {
        this.predecessors = predecessors;
    }

    public List<Task> getSuccessors() {
        return successors;
    }

    public void setSuccessors(List<Task> successors) {
        this.successors = successors;
    }

    public Map<String, Float> getTransferredDataSize() {
        return transferredDataSize;
    }

    public void setTransferredDataSize(Map<String, Float> transferredDataSize) {
        this.transferredDataSize = transferredDataSize;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", name=" + name +
                ", length=" + length +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        Task t = (Task) obj;
        return this.id.equalsIgnoreCase(t.id);
    }

    public void log(){

        System.out.println("---------------------------------------------------------");
        System.out.println(this);

        System.out.println("Predecessors:");
        for(Task tempPredTask : this.getPredecessors()){
            String id = tempPredTask.getId();
            float sizeInGb = transferredDataSize.getOrDefault(id,0.0f);
            System.out.printf("(id: %s, transferred-data-size: %.2e Gb)\n",id,sizeInGb);
        }

        System.out.println("Successors:");
        for(Task tempSuccTask : this.getSuccessors()){
            System.out.println("(id:" + tempSuccTask.getId() + ")");
        }

    }

}
