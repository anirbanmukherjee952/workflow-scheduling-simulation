package org.example.simulator;

import org.example.simulator.workflow.Schedule;
import org.example.simulator.workflow.Workflow;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MontageSimulator {

    public static void main(String[] args) {

        String daxPath = "src/main/resources/dax/";
        String[] daxFileNames = { "Montage_25.xml",
                                "Montage_50.xml",
                                "Montage_100.xml",
                                "Montage_1000.xml" };

        float[][] normMakespan = new float[2][4];
        float[][] normCost = new float[2][4];
        float[][] normEnergyConsumption = new float[2][4];

        // simulation params
        float alpha = 1.3f, beta = 0.6f;

        // loop through all the dax files
        for(int i=0; i<daxFileNames.length; i++){

            // create the workflow
            String daxFileName = daxFileNames[i];
            Workflow workflow = new Workflow();
            workflow.create(daxPath+daxFileName);

            // compute deadline & budget
            float deadline = workflow.deadline(alpha);
            float budget = workflow.budget(beta);

            // executing ESDWB
            Schedule schedule1 = workflow.computeESDWBSchedule(alpha, beta);
            float makespan1 = workflow.actualMakespan(schedule1);
            float cost1 = workflow.cost(schedule1);
            float energyConsumption1 = workflow.energyConsumption(schedule1);

            // executing modified ESDWB
            Schedule schedule2 = workflow.computeModifiedESDWBSchedule(alpha, beta);
            float makespan2 = workflow.actualMakespan(schedule2);
            float cost2 = workflow.cost(schedule2);
            float energyConsumption2 = workflow.energyConsumption(schedule2);

            // normalized makespan
            normMakespan[0][i] = makespan1 / deadline;
            normMakespan[1][i] = makespan2 / deadline;

            // normalized cost
            normCost[0][i] = cost1 / budget;
            normCost[1][i] = cost2 / budget;

            // normalized energy consumption
            float minimumEnergyConsumption = Math.min(energyConsumption1,energyConsumption2);
            normEnergyConsumption[0][i] = energyConsumption1 / minimumEnergyConsumption;
            normEnergyConsumption[1][i] = energyConsumption2 / minimumEnergyConsumption;

        }

        // create CSV files
        createNormMakespanFile(normMakespan);
        createNormCostFile(normCost);
        createNormEnergyConsumptionFile(normEnergyConsumption);

    }

    static void createNormMakespanFile(float[][] normMakespan){
        File file = new File("src/main/resources/reports/Montage/norm-makespan.csv");
        BufferedWriter bw = null;
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            bw = new BufferedWriter(new FileWriter(file));
            bw.write("No. of Tasks,30,50,100,1000");
            bw.newLine();
            bw.write("ESDWB," + normMakespan[0][0] + "," + normMakespan[0][1] + "," + normMakespan[0][2] + "," + normMakespan[0][3]);
            bw.newLine();
            bw.write("Modified ESDWB," + normMakespan[1][0] + "," + normMakespan[1][1] + "," + normMakespan[1][2] + "," + normMakespan[1][3]);
            bw.close();
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    static void createNormCostFile(float[][] normCost){
        File file = new File("src/main/resources/reports/Montage/norm-cost.csv");
        BufferedWriter bw = null;
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            bw = new BufferedWriter(new FileWriter(file));
            bw.write("No. of Tasks,30,50,100,1000");
            bw.newLine();
            bw.write("ESDWB," + normCost[0][0] + "," + normCost[0][1] + "," + normCost[0][2] + "," + normCost[0][3]);
            bw.newLine();
            bw.write("Modified ESDWB," + normCost[1][0] + "," + normCost[1][1] + "," + normCost[1][2] + "," + normCost[1][3]);
            bw.close();
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    static void createNormEnergyConsumptionFile(float[][] normEnergyConsumption){
        File file = new File("src/main/resources/reports/Montage/norm-energy-consumption.csv");
        BufferedWriter bw = null;
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            bw = new BufferedWriter(new FileWriter(file));
            bw.write("No. of Tasks,30,50,100,1000");
            bw.newLine();
            bw.write("ESDWB," + normEnergyConsumption[0][0] + "," + normEnergyConsumption[0][1] + "," + normEnergyConsumption[0][2] + "," + normEnergyConsumption[0][3]);
            bw.newLine();
            bw.write("Modified ESDWB," + normEnergyConsumption[1][0] + "," + normEnergyConsumption[1][1] + "," + normEnergyConsumption[1][2] + "," + normEnergyConsumption[1][3]);
            bw.close();
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

}
