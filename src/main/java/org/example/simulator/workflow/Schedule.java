package org.example.simulator.workflow;

import org.example.simulator.vm.Vm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Schedule {

    private String algorithmName;

    private String workflowName;

    private int workflowSize;

    private Map<Vm,List<Task>> assignment;

    public Schedule(String algorithmName){
        this.algorithmName = algorithmName;
        this.assignment = new HashMap<>();
    }

    public Schedule(String algorithmName, String workflowName, int workflowSize){
        this.algorithmName = algorithmName;
        this.workflowName = workflowName;
        this.workflowSize = workflowSize;
        this.assignment = new HashMap<>();
    }

    public void assign(Task task, Vm vm){
        if(!assignment.containsKey(vm)){
            assignment.put(vm, new ArrayList<>());
        }
        assignment.get(vm).add(task);
    }

    public void dismiss(Task task, Vm vm){
        this.assignment.get(vm).remove(task);
    }

    public Vm getAssignedVm(Task task){
       return this.assignment.entrySet().stream()
                .filter((e) -> e.getValue().contains(task))
                .map((e) -> e.getKey())
                .findFirst().orElse(null);
    }

    public List<Task> getAssignedTasks(Vm vm){
        return this.assignment.get(vm);
    }

    public boolean hasVm(Vm vm){
        return this.assignment.containsKey(vm);
    }

    public boolean hasTasksScheduledBefore(Task task, Vm vm){
        List<Task> tasks = this.assignment.get(vm);
        int idx = tasks.indexOf(task);
        return idx>0 && idx<tasks.size();
    }

    public Task getTaskScheduledBefore(Task task, Vm vm){
        int idx = this.assignment.get(vm).indexOf(task);
        return this.assignment.get(vm).get(idx - 1);
    }

    public boolean hasTasksScheduledAfter(Task task, Vm vm){
        List<Task> taskList = this.assignment.get(vm);
        int idx = taskList.indexOf(task);
        return idx>0 && idx<taskList.size()-1;
    }

    public Task getTaskScheduledAfter(Task task, Vm vm){
        int idx = this.assignment.get(vm).indexOf(task);
        return this.assignment.get(vm).get(idx + 1);
    }

    public void log(){
        File logFile = new File("src/main/resources/logs/schedules/" +
                this.algorithmName + "-" + this.workflowName + "-" + this.workflowSize + ".txt");
        try {
            if(!logFile.exists()){
                logFile.createNewFile();
            }
            BufferedWriter br = new BufferedWriter(new FileWriter(logFile));
            TreeMap<Vm,List<Task>> sortedAssignment = new TreeMap<>((vm1,vm2) -> vm1.getId()-vm2.getId());
            sortedAssignment.putAll(assignment);
            for(Map.Entry<Vm,List<Task>> e : sortedAssignment.entrySet()){
                br.write(e.getKey() + "\n");
                for (Task t : e.getValue()) {
                    br.write(t + "\n");
                }
                br.write("\n\n");
            }
            br.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<Vm, List<Task>> getAssignment() {
        return assignment;
    }

}
