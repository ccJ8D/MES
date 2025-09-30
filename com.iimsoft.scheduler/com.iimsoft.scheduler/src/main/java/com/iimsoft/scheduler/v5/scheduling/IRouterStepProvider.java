package com.iimsoft.scheduler.v5.scheduling;

import com.iimsoft.scheduler.v5.model.RouterStep;
import java.util.List;

/**
 * Interface for providing router step data to the SchedulingEngine.
 * Allows both database-backed and in-memory implementations.
 */
public interface IRouterStepProvider {
    
    /**
     * Loads router steps for a given router BO.
     * 
     * @param routerBo The router business object identifier
     * @return List of router steps for this router
     */
    List<RouterStep> loadRouterSteps(String routerBo);
}