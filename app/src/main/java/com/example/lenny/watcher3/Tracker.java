package com.example.lenny.watcher3;

class Tracker  implements Comparable<Tracker> {
    String id;
    String name;
    String deviceName;
    String address;
    Integer value;

    public Tracker(String id, String name, String address) {
        this.id = id;
        this.name = name;
        this.address = address;
    }

    public Tracker(String deviceName, String address, Integer value) {
        this.deviceName = deviceName;
        this.address = address;
        this.value = value;
    }

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

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
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
                ", deviceName='" + deviceName + '\'' +
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
