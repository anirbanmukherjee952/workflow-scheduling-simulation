package org.example.simulator.workflow;

import org.example.simulator.vm.DataCenter;
import org.example.simulator.vm.Vm;
import org.example.simulator.vm.VmType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Workflow {

    private String name;

    private List<Task> taskList;

    public Workflow(){
        this.taskList = new ArrayList<>();
    }

    // according to Workflow Model

    // to compute estimated makespan of a workflow
    public float estimatedMakespan(Schedule schedule){
       return this.taskList.stream()
               .filter((ti) -> ti.getSuccessors().isEmpty())
               .map((ti) -> ti.earliestFinishTime(schedule))
               .max(Float::compareTo).get();
    }

    // to compute deadline of a workflow by equation-(6)
    public float deadline(float alpha){

        // create a data center
        DataCenter dataCenter = new DataCenter();

        // create a schedule by assigning tasks to cheapest VMs
        // from the data center
        VmType fastestType = dataCenter.findFastestVmType();
        Schedule schedule = this.computeNaiveSchedule(fastestType);
        float minEstdMakespan = this.estimatedMakespan(schedule);

        // compute and return the deadline
        return alpha * minEstdMakespan;

    }

    // according to Energy Model

    // to compute energy consumption by equation-(8)
    public float energyConsumption(Schedule schedule){
        return this.taskList.stream().map((ti) -> {
                Vm v_ti = schedule.getAssignedVm(ti);
                return ti.powerConsumed(v_ti);
            })
            .reduce(0.0f,Float::sum);
    }

    // according to Budget Model

    // to compute monetary cost of a workflow by equation-(10)
    public float cost(Schedule schedule){
        return this.taskList.stream().map((ti) -> {
                Vm v_ti = schedule.getAssignedVm(ti);
                return ti.cost(v_ti);
            })
            .reduce(0.0f,Float::sum);
    }

    // to compute budget of a workflow by equation-(11)
    public float budget(float beta){

        // create a data center
        DataCenter dataCenter = new DataCenter();

        // create a schedule by assigning tasks to cheapest VMs
        // from the data center
        VmType cheapestType = dataCenter.findCheapestVmType();
        Schedule cheapestSchedule = computeNaiveSchedule(cheapestType);
        float lowestCost = this.cost(cheapestSchedule);

        // create a schedule by assigning tasks to costliest VMs
        // from the data center
        VmType costliestType = dataCenter.findCostliestVmType();
        Schedule costliestSchedule = computeNaiveSchedule(costliestType);
        float highestCost = this.cost(costliestSchedule);

        // compute and return the budget
        return lowestCost + beta*(highestCost-lowestCost);

    }

    // to compute surplus budget of a workflow
    public float initialSurplusBudget(float beta){

        // create a data center
        DataCenter dataCenter = new DataCenter();

        // create a schedule by assigning tasks to cheapest VMs
        // from the data center
        VmType cheapestType = dataCenter.findCheapestVmType();
        Schedule cheapestSchedule = computeNaiveSchedule(cheapestType);
        float lowestCost = this.cost(cheapestSchedule);

        // create a schedule by assigning tasks to costliest VMs
        // from the data center
        VmType costliestType = dataCenter.findCostliestVmType();
        Schedule costliestSchedule = computeNaiveSchedule(costliestType);
        float highestCost = this.cost(costliestSchedule);

        // compute and return initial surplus budget
        return beta*(highestCost-lowestCost);

    }

    // to compute actual makespan of a workflow by equation-(19)
    public float actualMakespan(Schedule schedule){
        return this.taskList.stream()
                .filter((ti) -> ti.getSuccessors().isEmpty())
                .map((ti) -> ti.actualFinishTime(schedule.getAssignedVm(ti), schedule))
                .max(Float::compareTo).get();
    }

    // to parse DAX (DAG in XML) file to create a workflow
    public void create(String daxPath) {

        // a VM type object
        VmType vmType = new VmType(0, 0.0058f,
                Arrays.asList(1.20f, 1.15f, 1.10f, 1.05f, 1.00f, 0.90f),
                Arrays.asList(1.80f, 1.60f, 1.40f, 1.20f, 1.00f, 0.80f));

        // helper DS
         Map<String,Task> taskMap = new HashMap<>();

        // first: parse the XML file
        try{

            // create the document object and
            // get root element
            SAXBuilder builder = new SAXBuilder();
            File dax = new File(daxPath);

            // get workflow name
            this.name = Arrays.stream(dax.getName().split("_")).findFirst().get();

            Document doc = builder.build(dax);
            Element root = doc.getRootElement();

            // loop through all the child-nodes of root
            List<Element> nodeList = root.getChildren();
            for(Element node : nodeList){

                switch(node.getName().toLowerCase()){

                    // if it is 'job' node
                    case "job":

                        // get id, name of the job
                        String id = node.getAttributeValue("id");
                        String name = node.getAttributeValue("name");

                        // compute the length in MIPS
                        float runtime = Float.parseFloat(node.getAttributeValue("runtime")); // in seconds
                        float processingSpeed = vmType.getMaximumProcessingSpeed(); // in MIPS
                        float length = runtime * processingSpeed;

                        // loop through all the files and
                        // keep the input files
                        List<Element> fileNodeList = node.getChildren();
                        List<FileItem> inputFileList = new ArrayList<>();
                        for(Element fileNode : fileNodeList){
                            String fileType = fileNode.getAttributeValue("link"); // get the file-type
                            if(fileType.equalsIgnoreCase("input")) {
                                // if it is input file, get name and size of the file
                                // add it to inputFileList
                                String fileName = fileNode.getAttributeValue("file");
                                float fileSize = Float.parseFloat(fileNode.getAttributeValue("size"));
                                inputFileList.add(new FileItem(fileName, fileSize));

                            }
                        }

                        // crate a Task object with id, name, length and input-files
                        Task tempTask = new Task(id,name,length);
                        tempTask.setRequiredData(inputFileList);

                        // add it to workflow
                        taskList.add(tempTask);
                        taskMap.put(id,tempTask);
                        break;

                    // if it is 'child' node
                    case "child":

                        // get the child node id and
                        // corresponding parent nodes
                        String childNodeId = node.getAttributeValue("ref");
                        List<Element> parentNodeList = node.getChildren();

                        // if workflow has a task with that id
                        if(taskMap.containsKey(childNodeId)){

                            // get the task
                            Task childTask = taskMap.get(childNodeId);

                            // loop through the parent nodes
                            for(Element parentNode : parentNodeList){

                                // get parent node id
                                String parentNodeId = parentNode.getAttributeValue("ref");

                                // if workflow has a task with that id
                                if(taskMap.containsKey(parentNodeId)){

                                    // get the task
                                    Task parentTask = taskMap.get(parentNodeId);

                                    // add entries accordingly
                                    childTask.addPredecessor(parentTask);
                                    parentTask.addSuccessor(childTask);

                                }

                            }

                        }
                        break;
                }
            }

            // second: compute transferred data sizes from its predecessor
            // for each task in the workflow
            this.taskList.forEach(Task::computeTransferredDataSizes);

        }
        catch(Exception exc){
            exc.printStackTrace();
        }

    }

    // to create a naive-schedule by Breadth-First Traversal
    public Schedule computeNaiveSchedule(VmType vmType){

        // create a Data Center
        DataCenter dataCenter = new DataCenter();

        // to store the schedule
        Schedule naiveSchedule = new Schedule("Naive");

        // helper object and collection
        HashMap<String,Boolean> visited = new HashMap<>();

        // get the set of entry tasks
        List<Task> entryTasks = taskList.stream()
                .filter((t) -> t.getPredecessors().isEmpty())
                .collect(Collectors.toList());

        // initialize visited HashMap to false(s)
        taskList.forEach((ti) -> visited.put(ti.getId(),false));

        // loop through all the entry tasks
        for(Task entryTask : entryTasks){

            // for each entry task start a breadth-first traversal
            Queue<Task> taskQueue = new LinkedList<>();
            taskQueue.add(entryTask);
            visited.put(entryTask.getId(),true);

            // breadth-first traversal
            while(!taskQueue.isEmpty()){

                // pop a task
                Task theTask = taskQueue.remove();

                // assign to the VM
                // get a new VM from the Data Center
                Vm theVm = dataCenter.launchNewVm(vmType);
                naiveSchedule.assign(theTask,theVm);

                // push successor(s) into queue
                for(Task succTask : theTask.getSuccessors()){
                    if(!visited.get(succTask.getId())){
                        taskQueue.add(succTask);
                        visited.put(succTask.getId(),true);
                    }
                }

            }

        }
        return naiveSchedule;

    }

    public Schedule computeESDWBSchedule(float alpha, float beta) {

        System.out.println("Executing ESDWB...");

        // create a data center
        DataCenter dataCenter = new DataCenter();

        // computing initial naive schedule on fastest VMs
        VmType fastestType = dataCenter.findFastestVmType();
        Schedule fastestSchedule = this.computeNaiveSchedule(fastestType);

        // sort workflow-tasks in descending order of their priorities
        List<Task> sortedReversedByPriority = this.taskList.stream()
                .sorted((t1, t2) -> {
                    float pr1 = t1.priority(dataCenter.getVmTypeList(), fastestSchedule);
                    float pr2 = t2.priority(dataCenter.getVmTypeList(), fastestSchedule);
                    return (pr2==pr1)? 0: (pr2<pr1)? -1: 1;
                }).collect(Collectors.toList());

        // compute estimated makespan
        float estdMakespan = this.estimatedMakespan(fastestSchedule);

        // compute initial surplus budget
        float surplusBudget = this.initialSurplusBudget(beta);

        // create empty schedule
        Schedule efficientSchedule = new Schedule("ESDWB",this.name,this.taskList.size());

        // for logging
        File logFile = new File("src/main/resources/logs/" +
                "ESDWB-" + this.name + "-" + this.taskList.size() + ".csv");
        BufferedWriter br = null;
        try {
            if(!logFile.exists()){
                logFile.createNewFile();
            }
            br = new BufferedWriter(new FileWriter(logFile));
            br.write("Task,Surplus,Budget,Min. cost,Max. cost,Vm,Cost,Update\n");
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        // for progress bar
        int iteration = 0;
        int totalTasks = sortedReversedByPriority.size();

        // loop through all tasks in workflow
        for(Task t : sortedReversedByPriority){

            // for progress bar
            int progess = (int) (((float)iteration/totalTasks) * 100.0f);
            System.out.print("Processing: " + progess + "% " + "\r");
            iteration += 1;

            // for logging
            float surplusBudgetForThisRound = surplusBudget;

            // initialize VM to null
            Vm v = null;

            // compute budget of the task
            float taskBudget = t.budget(surplusBudget, dataCenter.getVmTypeList());

            // compute the deadline of the task
            float taskDeadline = t.deadline(alpha, estdMakespan, fastestSchedule);

            // if the task is not an entry-task
            if(!t.getPredecessors().isEmpty()){

                // sort predecessors in descending order of the data sizes transferred to the task
                Map<String,Float> transferredDataSizes = t.getTransferredDataSize();
                List<Task> sortedReversedByTransferredDataSize =  t.getPredecessors().stream().sorted((t1,t2) ->{
                                        float s1 = transferredDataSizes.getOrDefault(t1.getId(),0.0f);
                                        float s2 = transferredDataSizes.getOrDefault(t2.getId(),0.0f);
                                        return (s2==s1)? 0: (s2<s1)? -1: 1;
                                    }).collect(Collectors.toList());

                // loop through all predecessor tasks
                for(Task tp : sortedReversedByTransferredDataSize){

                    // get the VM where predTask was assigned
                    Vm v_tp = efficientSchedule.getAssignedVm(tp);

                    // check if actual finish time of the task stays within the deadline and
                    // cost of executing the task on v_tp stays within the budget
                    efficientSchedule.assign(t,v_tp);
                    if(t.actualFinishTime(v_tp,efficientSchedule)<=taskDeadline && t.cost(v_tp)<=taskBudget){
                        v = v_tp;
                        surplusBudget -= (t.cost(v) - t.minimumCost(dataCenter.getVmTypeList()));
                        break;
                    }

                    // dismiss the assignment
                    efficientSchedule.dismiss(t,v_tp);

                }

            }

            // if the task is an entry task or the is still not assigned
            if(t.getPredecessors().isEmpty() || v == null){

                // minimum required processing speed of the task
                float minimumNeededProcessingSpeed = t.minimumNeededProcessingSpeed(alpha, estdMakespan, fastestSchedule, efficientSchedule);

                // filter the VM-types having maximum processing speed more than the minNeededProcessingSpeed
                // then sort them in ascending order of maximum processing speed value
                Comparator<VmType> compareByMaximumProcessingSpeed = Comparator.comparing(VmType::getMaximumProcessingSpeed);
                List<VmType> sortedByMaximumProcessingSpeed = dataCenter.getVmTypeList().stream()
                        .filter((tau) -> tau.getMaximumProcessingSpeed()>=minimumNeededProcessingSpeed)
                        .sorted(compareByMaximumProcessingSpeed).collect(Collectors.toList());

                // loop through the list of filtered VM types
                for(VmType tau : sortedByMaximumProcessingSpeed){

                    // get a idle/new VM of type tau
                    Vm v_idle = dataCenter.findIdleVm(efficientSchedule,t,tau);
                    if(v_idle == null){
                        v_idle = dataCenter.launchNewVm(tau);
                    }

                    // check if cost of executing the task running on idleVm
                    // stays within its budget; if so happens then make an assignment and update surplus budget
                    if(v_idle!=null && t.cost(v_idle)<=taskBudget){
                        v = v_idle;
                        efficientSchedule.assign(t, v);
                        surplusBudget -= (t.cost(v) - t.minimumCost(dataCenter.getVmTypeList()));
                        break;
                    }

                }

                // if still no such VM has been found
                if(v == null){

                    VmType tau_b = null;
                    try {

                        // among the set of VM types that can schedule t within its budget
                        // find the type that has maximum processing speed
                        tau_b = dataCenter.getVmTypeList().stream()
                                .filter((tau) -> (t.getLength() / tau.getMaximumProcessingSpeed()) * tau.getCostPerSecond() <= taskBudget)
                                .max(compareByMaximumProcessingSpeed).get();

                        // get an idle/new VM of type tau_b
                        Vm v_idle = dataCenter.findIdleVm(efficientSchedule, t, tau_b);
                        if (v_idle == null) {
                            v_idle = dataCenter.launchNewVm(tau_b);
                        }

                        // assign to it
                        v = v_idle;
                        efficientSchedule.assign(t, v);
                        surplusBudget -= (t.cost(v) - t.minimumCost(dataCenter.getVmTypeList()));
                    }
                    catch (Exception exc){
                        System.out.println("No idle/new VM found!");
                        System.out.println(tau_b);
                        System.out.println(t);
                        exc.printStackTrace();
                        System.exit(0);
                    }
                }

            }

            // log Task-ID,Surplus Budget,Task's Budget,Task's Min. Cost,Task's Max. Cost,VM-ID,Task's Cost,Update
            try {
                br.write(t.getId() + "," +
                        surplusBudgetForThisRound + "," +
                        taskBudget + "," +
                        t.minimumCost(dataCenter.getVmTypeList()) + "," +
                        t.maximumCost(dataCenter.getVmTypeList()) + "," +
                        v.getId() + "," +
                        t.cost(v) + "," +
                        (t.cost(v) - t.minimumCost(dataCenter.getVmTypeList())) + "\n");
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }

        try {
            br.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        // reduce energy consumption by tasks and return the schedule
        float actualMakespan = this.actualMakespan(efficientSchedule);
        this.reduceEnergyConsumedByTasks(efficientSchedule, actualMakespan);
        System.out.println("Processing: Done!          ");

        return efficientSchedule;

    }

    public Schedule computeModifiedESDWBSchedule(float alpha, float beta) {

        System.out.println("Executing Modified-ESDWB...");

        // create a data center
        DataCenter dataCenter = new DataCenter();

        // computing initial naive schedule on fastest VMs
        VmType fastestType = dataCenter.findFastestVmType();
        Schedule fastestSchedule = this.computeNaiveSchedule(fastestType);

        // sort workflow-tasks in descending order of their priorities
        List<Task> sortedReversedByPriority = this.taskList.stream().sorted((t1, t2) -> {
            float pr1 = t1.priority(dataCenter.getVmTypeList(), fastestSchedule);
            float pr2 = t2.priority(dataCenter.getVmTypeList(), fastestSchedule);
            return (pr2==pr1)? 0: (pr2<pr1)? -1: 1;
        }).collect(Collectors.toList());

        // compute estimated makespan
        float estdMakespan = this.estimatedMakespan(fastestSchedule);

        // compute initial surplus budget
        float surplusBudget = 0.0f;

        // create empty schedule
        Schedule efficientSchedule = new Schedule("Modified-ESDWB",this.name,this.taskList.size());

        // for logging
        File logFile = new File("src/main/resources/logs/" +
                "Modified-ESDWB-" + this.name + "-" + this.taskList.size() + ".csv");
        BufferedWriter br = null;
        try {
            if(!logFile.exists()){
                logFile.createNewFile();
            }
            br = new BufferedWriter(new FileWriter(logFile));
            br.write("Task,Surplus,Budget,Min. cost,Max. cost,Vm,Cost,Update\n");
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        // for progress
        int iteration = 0;
        int totalTasks = sortedReversedByPriority.size();

        // loop through all tasks in workflow
        for(Task t : sortedReversedByPriority){

            // for progress
            int progress = (int) (((float)iteration/totalTasks) * 100.0f);
            System.out.print("Processing: " + progress + "% " + "\r");
            iteration += 1;

            // for logging
            float surplusBudgetForThisRound = surplusBudget;

            // initialize VM to null
            Vm v = null;

            // compute budget of the task
            float taskBudget = t.modifiedBudget(surplusBudget, beta, dataCenter.getVmTypeList());

            // compute the deadline of the task
            float taskDeadline = t.deadline(alpha, estdMakespan, fastestSchedule);

            // if the task is not an entry-task
            if(!t.getPredecessors().isEmpty()){

                // sort predecessors in descending order of the data sizes transferred to the task
                Map<String,Float> transferredDataSizes = t.getTransferredDataSize();
                List<Task> sortedReversedByTransferredDataSize =  t.getPredecessors().stream().sorted((t1,t2) ->{
                    float s1 = transferredDataSizes.getOrDefault(t1.getId(),0.0f);
                    float s2 = transferredDataSizes.getOrDefault(t2.getId(),0.0f);
                    return (s2==s1)? 0: (s2<s1)? -1: 1;
                }).collect(Collectors.toList());

                // loop through all predecessor tasks
                for(Task tp : sortedReversedByTransferredDataSize){

                    // get the VM where predTask was assigned
                    Vm v_tp = efficientSchedule.getAssignedVm(tp);

                    // check if actual finish time of the task stays within the deadline and
                    // cost of executing the task on v_tp stays within the budget
                    efficientSchedule.assign(t,v_tp);
                    if(t.actualFinishTime(v_tp,efficientSchedule)<=taskDeadline && t.cost(v_tp)<=taskBudget){
                        v = v_tp;
                        surplusBudget = (taskBudget - t.cost(v));
                        break;
                    }

                    // dismiss the assignment
                    efficientSchedule.dismiss(t,v_tp);

                }

            }

            // if the task is an entry task or the is still not assigned
            if(t.getPredecessors().isEmpty() || v == null){

                // minimum required processing speed of the task
                float minimumNeededProcessingSpeed = t.minimumNeededProcessingSpeed(alpha, estdMakespan, fastestSchedule, efficientSchedule);

                // filter the VM-types having maximum processing speed more than the minNeededProcessingSpeed
                // then sort them in ascending order of maximum processing speed value
                Comparator<VmType> compareByMaximumProcessingSpeed = Comparator.comparing(VmType::getMaximumProcessingSpeed).reversed();
                List<VmType> sortedByMaximumProcessingSpeed = dataCenter.getVmTypeList().stream()
                        .filter((tau) -> tau.getMaximumProcessingSpeed()>=minimumNeededProcessingSpeed)
                        .sorted(compareByMaximumProcessingSpeed).collect(Collectors.toList());

                // loop through the list of filtered VM types
                for(VmType tau : sortedByMaximumProcessingSpeed){

                    // get a idle/new VM of type tau
                    Vm v_idle = dataCenter.findIdleVm(efficientSchedule,t,tau);
                    if(v_idle == null){
                        v_idle = dataCenter.launchNewVm(tau);
                    }

                    // check if cost of executing the task running on idleVm
                    // stays within its budget; if so happens then make an assignment and update surplus budget
                    if(v_idle!=null && t.cost(v_idle)<=taskBudget){
                        v = v_idle;
                        efficientSchedule.assign(t, v);
                        surplusBudget = (taskBudget - t.cost(v));
                        break;
                    }

                }

                // if still no such VM has been found
                if(v == null){

                    VmType tau_b = null;
                    try {

                        // among the set of VM types that can schedule t within its budget
                        // find the type that has maximum processing speed
                        tau_b = dataCenter.getVmTypeList().stream()
                                .filter((tau) -> (t.getLength() / tau.getMaximumProcessingSpeed()) * tau.getCostPerSecond() <= taskBudget)
                                .max(compareByMaximumProcessingSpeed).get();

                        // get an idle/new VM of type tau_b
                        Vm v_idle = dataCenter.findIdleVm(efficientSchedule, t, tau_b);
                        if (v_idle == null) {
                            v_idle = dataCenter.launchNewVm(tau_b);
                        }

                        // assign to it
                        v = v_idle;
                        efficientSchedule.assign(t, v);
                        surplusBudget = (taskBudget - t.cost(v));
                    }
                    catch (Exception exc){
                        System.out.println("No idle/new VM found!");
                        System.out.println(t);
                        System.out.println(tau_b);
                        exc.printStackTrace();
                        System.exit(0);
                    }

                }

            }

            // log Task-ID,Surplus Budget,Task's Budget,Task's Min. Cost,Task's Max. Cost,VM-ID,Task's Cost,Update
            try {
                br.write(t.getId() + "," +
                        surplusBudgetForThisRound + "," +
                        taskBudget + "," +
                        t.minimumCost(dataCenter.getVmTypeList()) + "," +
                        t.maximumCost(dataCenter.getVmTypeList()) + "," +
                        v.getId() + "," +
                        t.cost(v) + "," +
                        (taskBudget - t.cost(v)) + "\n");
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }

        try {
            br.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        // reduce energy consumption by tasks and return the schedule
        float actualMakespan = this.actualMakespan(efficientSchedule);
        this.reduceEnergyConsumedByTasks(efficientSchedule, actualMakespan);
        System.out.println("Processing: Done!          ");

        return efficientSchedule;

    }

    public void reduceEnergyConsumedByTasks(Schedule schedule, float actualMakespan){

        // now, energy reduction of tasks
        // loop through all tasks
        for(Task t : this.taskList){

            // get the VM on which theTask is assigned
            Vm v = schedule.getAssignedVm(t);

            // compute extended finish time & actual finish time of the task running on theVm
            float extendedFinishTime = t.extendedFinishTime(actualMakespan,schedule);
            float actualFinishTime = t.actualFinishTime(v,schedule);

            // check if extended finish time is more than actual finish time
            if(extendedFinishTime > actualFinishTime){

                // compute minimum needed processing speed within possible extended finish time
                float minimumNeededProcessingSpeed =
                        t.minimumNeededProcessingSpeedWithinPossibleExtendedFinishTime(actualMakespan,v,schedule);

                // find the required processing speed for the task
                List<Float> processingSpeedList = v.getType().getProcessingSpeedsInMips();
                float newProcessingSpeed = processingSpeedList.stream()
                        .filter( (ps) -> ps >= minimumNeededProcessingSpeed )
                        .min(Float::compareTo).orElse(0.0f);

                // if required processing speed is less than maximum processing speed
                if(newProcessingSpeed!=0.0f && newProcessingSpeed<v.getType().getMaximumProcessingSpeed()){
                    int i = v.getType().getProcessingSpeedsInMips().indexOf(newProcessingSpeed);
                    float newFrequency = v.getType().getFrequenciesInGHz().get(i);
                    float newVoltageLevel = v.getType().getVoltageLevelsInVolt().get(i);
                    v.setFrequency(newFrequency);
                    v.setVoltageLevel(newVoltageLevel);
                    v.setProcessingSpeed(newProcessingSpeed);
                }

            }

        }

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Task> getTaskList() {
        return taskList;
    }

    public void setTaskList(List<Task> taskList) {
        this.taskList = taskList;
    }

    public void log(){
        System.out.println("Workflow:");
        this.taskList.stream().sorted(Comparator.comparing(Task::getId)).forEach(Task::log);
    }

}
