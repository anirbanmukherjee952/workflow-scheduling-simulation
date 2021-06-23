package org.example.simulator.vm;

import java.util.List;
import java.util.stream.Collectors;

public class VmType {

    private int id;

    private float costPerSecond; // in USD per Second

    private List<Float> voltageLevelsInVolt; // in Volt

    private List<Float> frequenciesInGHz;  // in Giga Hertz

    private List<Float> processingSpeedsInMips; // in Million Instructions per Second


    public VmType(int id, float costPerHour, List<Float> voltageLevelsInVolt, List<Float> frequenciesInGHz) {
        this.id = id;
        this.costPerSecond = costPerHour / 3600.0f;
        this.voltageLevelsInVolt = voltageLevelsInVolt;
        this.frequenciesInGHz = frequenciesInGHz;
        this.processingSpeedsInMips = this.frequenciesInGHz.stream()
                .map((freqInGHz) -> freqInGHz*1.0e3f)
                .collect(Collectors.toList());
    }

    public float getMaximumProcessingSpeed(){
        return this.processingSpeedsInMips.stream().max(Float::compareTo).get();
    }

    public float ratioOfComputingCostToMaxProcessingSpeed(){
        return this.costPerSecond / this.processingSpeedsInMips.stream().max(Float::compareTo).get();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public float getCostPerSecond() {
        return costPerSecond;
    }

    public void setCostPerSecond(float costPerSecond) {
        this.costPerSecond = costPerSecond;
    }

    public List<Float> getVoltageLevelsInVolt() {
        return voltageLevelsInVolt;
    }

    public void setVoltageLevelsInVolt(List<Float> voltageLevelsInVolt) {
        this.voltageLevelsInVolt = voltageLevelsInVolt;
    }

    public List<Float> getFrequenciesInGHz() {
        return frequenciesInGHz;
    }

    public void setFrequenciesInGHz(List<Float> frequenciesInGHz) {
        this.frequenciesInGHz = frequenciesInGHz;
    }

    public List<Float> getProcessingSpeedsInMips() {
        return processingSpeedsInMips;
    }

    public void setProcessingSpeedsInMips(List<Float> processingSpeedsInMips) {
        this.processingSpeedsInMips = processingSpeedsInMips;
    }

    @Override
    public boolean equals(Object obj) {
        VmType tau = (VmType) obj;
        return (this.id == tau.id);
    }

    @Override
    public String toString() {
        return "VmType{" +
                "id=" + id +
                ", costPerSecond=" + costPerSecond +
                ", maxVoltageLevelsInVolt=" + voltageLevelsInVolt.stream().max(Float::compareTo) +
                ", maxFrequenciesInGHz=" + frequenciesInGHz.stream().max(Float::compareTo) +
                ", maxProcessingSpeedsInMips=" + processingSpeedsInMips.stream().max(Float::compareTo) +
                '}';
    }
}
