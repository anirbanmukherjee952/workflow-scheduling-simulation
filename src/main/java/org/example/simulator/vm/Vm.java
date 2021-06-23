package org.example.simulator.vm;

import org.example.simulator.workflow.Task;

import java.util.List;

public class Vm {

    public static final float K = 1.0f;

    private int id;

    private VmType type;

    private float voltageLevel;

    private float frequency;

    private float processingSpeed;

    private List<Task> assignedTasks; // list of tasks assigned to this VM

    public Vm(int id, VmType type) {
        this.id = id;
        this.type = type;
        this.initializeVoltageLevel();
        this.initializeFrequency();
        this.initializeProcessingSpeed();
    }


    public float powerConsumption(){
        return  K * (voltageLevel*voltageLevel) * frequency;
    }

    public void initializeVoltageLevel(){
        this.voltageLevel = type.getVoltageLevelsInVolt().stream().max(Float::compareTo).get();
    }

    public void initializeFrequency(){
        this.frequency = type.getFrequenciesInGHz().stream().max(Float::compareTo).get();
    }

    public void initializeProcessingSpeed(){
        this.processingSpeed = type.getProcessingSpeedsInMips().stream().max(Float::compareTo).get();
    }

    // getters, setters and toString

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public VmType getType() {
        return type;
    }

    public void setType(VmType type) {
        this.type = type;
    }

    public float getVoltageLevel() {
        return voltageLevel;
    }

    public void setVoltageLevel(float voltageLevel) {
        this.voltageLevel = voltageLevel;
    }

    public float getFrequency() {
        return frequency;
    }

    public void setFrequency(float frequency) {
        this.frequency = frequency;
    }

    public float getProcessingSpeed() {
        return processingSpeed;
    }

    public void setProcessingSpeed(float processingSpeed) {
        this.processingSpeed = processingSpeed;
    }

    public List<Task> getAssignedTasks() {
        return assignedTasks;
    }

    public void setAssignedTasks(List<Task> assignedTasks) {
        this.assignedTasks = assignedTasks;
    }

    @Override
    public String toString() {
        return "Vm{" +
                "id=" + id +
                ", type-id=" + type.getId() +
                ", voltageLevel=" + voltageLevel +
                ", frequency=" + frequency +
                ", processingSpeed=" + processingSpeed +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        Vm v = (Vm) obj;
        return this.id == v.id;
    }

}
