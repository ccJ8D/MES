package com.iimsoft.scheduler.v5.model;


// 3. 工艺路线步骤 - model/RouterStep.java
public class RouterStep {
    private String handle;
    private int sequence;
    private String operation;
    private String resourceBo;
    
    public RouterStep(String handle, int sequence, String operation, String resourceBo) {
        this.handle = handle;
        this.sequence = sequence;
        this.operation = operation;
        this.resourceBo = resourceBo;
    }
    
    // Getters
    public String getHandle() { return handle; }
    public int getSequence() { return sequence; }
    public String getOperation() { return operation; }
    public String getResourceBo() { return resourceBo; }
}
