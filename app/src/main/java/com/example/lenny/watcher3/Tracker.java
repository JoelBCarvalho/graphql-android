package com.example.lenny.watcher3;

class Tracker  implements Comparable<Tracker> {
    String name;
    String address;
    Integer value;

    public Tracker(String name, String address, Integer value) {
        this.name = name;
        this.address = address;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Tracker{" +
                "name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", value=" + value +
                '}';
    }

    @Override
    public int compareTo(Tracker tracker) {
        if(this.getValue() > tracker.getValue()) {
            return 1;
        } else if (this.getValue() < tracker.getValue()) {
            return -1;
        } else {
            return 0;
        }
    }
}
