package org.example.simulator.workflow;

public class FileItem {

    // name of file
    private String name;

    // size of file in MB
    private float size;

    public FileItem(String name, float size) {
        this.name = name;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "FileItem{" +
                "name='" + name + '\'' +
                ", size=" + size +
                '}';
    }
}
