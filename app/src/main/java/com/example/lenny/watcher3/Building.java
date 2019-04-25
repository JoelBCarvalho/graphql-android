package com.example.lenny.watcher3;

class Building {
    String id;
    String name;
    String activationLink;

    public Building(String id, String name, String activationLink) {
        this.id = id;
        this.name = name;
        this.activationLink = activationLink;
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

    public String getActivationLink() {
        return activationLink;
    }

    public void setActivationLink(String activationLink) {
        this.activationLink = activationLink;
    }
}
