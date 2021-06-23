package org.example.simulator;

import org.example.simulator.workflow.Schedule;
import org.example.simulator.workflow.Workflow;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class WorkflowSimulator {

     // DAX files

    // "CyberShake_30.xml", "Epigenomics_24.xml", "Inspiral_30.xml", "Montage_25.xml", "Sipht_30.xml"

    // "CyberShake_50.xml", "Epigenomics_46.xml", "Inspiral_50.xml", "Montage_50.xml", "Sipht_60.xml"

    // "CyberShake_100.xml", "Epigenomics_100.xml", "Inspiral_100.xml", "Montage_100.xml", "Sipht_100.xml"

    // "CyberShake_1000.xml", "Epigenomics_997.xml", "Inspiral_1000.xml", "Montage_1000.xml", "Sipht_1000.xml"

    public static void main(String[] args) {

        String daxPath = "src/main/resources/dax/";
        String daxFileName = "Sipht_1000.xml";

        Workflow workflow = new Workflow();
        workflow.create(daxPath+daxFileName);

        float alpha = 1.3f, beta = 0.6f;
        float deadline = workflow.deadline(alpha);
        float budget = workflow.budget(beta);

        Schedule schedule1 = workflow.computeESDWBSchedule(alpha, beta);
        float makespan1 = workflow.actualMakespan(schedule1);
        float cost1 = workflow.cost(schedule1);
        float energyConsumption1 = workflow.energyConsumption(schedule1);
        schedule1.log();

        Schedule schedule2 = workflow.computeModifiedESDWBSchedule(alpha, beta);
        float makespan2 = workflow.actualMakespan(schedule2);
        float cost2 = workflow.cost(schedule2);
        float energyConsumption2 = workflow.energyConsumption(schedule2);
        schedule2.log();

        float minimumEnergyConsumption = Math.min(energyConsumption1,energyConsumption2);

        File logFile = new File("src/main/resources/logs/results/" +
                "Results-" + workflow.getName() + "-" + workflow.getTaskList().size() + ".txt");

        try {
            if (!logFile.exists()) {
                logFile.createNewFile();
            }

            BufferedWriter br = new BufferedWriter(new FileWriter(logFile));

            System.out.println("\nWorkflow: " + workflow.getName());
            System.out.println("Size: " + workflow.getTaskList().size());
            System.out.println("Deadline: " + deadline);
            System.out.println("Budget: " + budget);
            System.out.println();

            br.write("Workflow: " + workflow.getName());
            br.newLine();
            br.write("Size: " + workflow.getTaskList().size());
            br.newLine();
            br.write("Deadline: " + deadline);
            br.newLine();
            br.write("Budget: " + budget);
            br.newLine();
            br.newLine();

            System.out.println("Normalized Makespan:");
            System.out.println("ESDWB:\t\t\t" + makespan1/deadline);
            System.out.println("Modified ESDWB:\t" + makespan2/deadline);
            System.out.println();

            br.write("Normalized Makespan:");
            br.newLine();
            br.write("ESDWB:\t\t\t" + makespan1/deadline);
            br.newLine();
            br.write("Modified ESDWB:\t" + makespan2/deadline);
            br.newLine();
            br.newLine();

            System.out.println("Deadline Violation:");
            System.out.println("ESDWB:\t\t\t" + ((makespan1<deadline)?0.0f:((makespan1-deadline)/deadline))*100.0f);
            System.out.println("Modified ESDWB:\t" + ((makespan2<deadline)?0.0f:((makespan2-deadline)/deadline))*100.0f);
            System.out.println();

            br.write("Deadline Violation:");
            br.newLine();
            br.write("ESDWB:\t\t\t" + ((makespan1<deadline)?0.0f:((makespan1-deadline)/deadline))*100.0f);
            br.newLine();
            br.write("Modified ESDWB:\t" + ((makespan2<deadline)?0.0f:((makespan2-deadline)/deadline))*100.0f);
            br.newLine();
            br.newLine();

            System.out.println("Normalized Cost:");
            System.out.println("ESDWB:\t\t\t" + cost1/budget);
            System.out.println("Modified ESDWB:\t" + cost2/budget);
            System.out.println();

            br.write("Normalized Cost:");
            br.newLine();
            br.write("ESDWB:\t\t\t" + cost1/budget);
            br.newLine();
            br.write("Modified ESDWB:\t" + cost2/budget);
            br.newLine();
            br.newLine();

            System.out.println("Normalized Energy Consumption:");
            System.out.println("ESDWB:\t\t\t" + energyConsumption1/minimumEnergyConsumption);
            System.out.println("Modified ESDWB:\t" + energyConsumption2/minimumEnergyConsumption);
            System.out.println();

            br.write("Normalized Cost:");
            br.newLine();
            br.write("ESDWB:\t\t\t" + energyConsumption1/minimumEnergyConsumption);
            br.newLine();
            br.write("Modified ESDWB:\t" + energyConsumption2/minimumEnergyConsumption);
            br.newLine();
            br.newLine();

            br.close();

        }
        catch (Exception exc){
            exc.printStackTrace();
        }

    }

}
