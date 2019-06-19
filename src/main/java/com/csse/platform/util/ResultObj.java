package com.csse.platform.util;

import java.io.Serializable;

public class ResultObj implements Serializable{

    private static final long serialVersionUID = -2850139057059948138L;
    private String component;
    private Boolean health;

    public ResultObj(String component, Boolean health) {
        this.component = component;
        this.health = health;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public Boolean getHealth() {
        return health;
    }

    public void setHealth(Boolean health) {
        this.health = health;
    }
}

