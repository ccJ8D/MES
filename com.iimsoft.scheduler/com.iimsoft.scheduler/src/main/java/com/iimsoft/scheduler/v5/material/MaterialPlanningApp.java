package com.iimsoft.scheduler.v5.material;

import com.iimsoft.scheduler.v5.material.model.*;
import com.iimsoft.scheduler.v5.model.*;
import com.iimsoft.scheduler.v5.optimization.*;
import com.iimsoft.scheduler.v5.scheduling.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Demo main application for in-memory material demand planning.
 * Uses TOC bottleneck identification plus NSGA-II multi-objective optimization
 * without requiring database connectivity.
 */
public class MaterialPlanningApp {
    
    public static void main(String[] args) {
        System.out.println("=== In-Memory Material Demand Planning Demo ===");
        
        try {
            // Create sample configuration
            MaterialPlanConfig config = createSampleConfiguration();
            
            System.out.println("Configuration: " + config);
            System.out.println();
            
            // Generate virtual orders and router steps
            VirtualOrderGenerator generator = new VirtualOrderGenerator();
            List<ShopOrder> virtualOrders = generator.generateVirtualOrders(config);
            Map<String, List<RouterStep>> routerSteps = generator.generateRouterSteps(config);
            
            System.out.printf("Generated %d virtual orders:%n", virtualOrders.size());
            for (ShopOrder order : virtualOrders) {
                System.out.printf("  %s: qty=%.2f, start=%s, due=%s%n", 
                    order.getShopOrder(), order.getQtyToBuild(), 
                    order.getPlannedStartDate(), order.getPlannedCompDate());
            }
            System.out.println();
            
            // Initialize components
            InMemoryLoader loader = new InMemoryLoader(routerSteps, virtualOrders);
            ResourceCalendar calendar = new ResourceCalendar();
            calendar.initShifts(loader.loadProductionShifts());
            
            // Identify bottleneck
            InMemoryBottleneckAnalyzer bottleneckAnalyzer = new InMemoryBottleneckAnalyzer(
                config, virtualOrders, calendar);
            String bottleneckResource = bottleneckAnalyzer.identifyBottleneck();
            System.out.println();
            
            // Initialize scheduling engine and objective calculator
            SchedulingEngine engine = new SchedulingEngine(calendar, (IRouterStepProvider) loader);
            InMemoryObjectiveCalculator objectiveCalculator = new InMemoryObjectiveCalculator(
                engine, bottleneckResource, config);
            
            // Run NSGA-II optimization
            System.out.println("=== Starting NSGA-II Optimization ===");
            NSGA2Optimizer nsga2 = new NSGA2Optimizer(
                objectiveCalculator, virtualOrders, 20, 30, 0.9, 0.1);
            
            List<Chromosome> paretoFront = nsga2.optimize();
            System.out.println();
            
            // Display results
            System.out.println("=== Pareto Front Results ===");
            System.out.printf("Found %d solutions in Pareto front:%n", paretoFront.size());
            System.out.println("Format: [Makespan(s), Total Lateness(s), Inventory Fluctuation]");
            System.out.println();
            
            for (int i = 0; i < paretoFront.size(); i++) {
                Chromosome solution = paretoFront.get(i);
                double[] objectives = solution.getObjectives();
                
                System.out.printf("Solution %d: [%.0f, %.0f, %.2f]%n", 
                    i + 1, objectives[0], objectives[1], objectives[2]);
                
                // Show first few orders in sequence
                List<ShopOrder> sequence = solution.getSequence();
                System.out.print("  Order sequence: ");
                for (int j = 0; j < Math.min(5, sequence.size()); j++) {
                    if (j > 0) System.out.print(" -> ");
                    System.out.print(sequence.get(j).getShopOrder());
                }
                if (sequence.size() > 5) {
                    System.out.print(" -> ... (" + sequence.size() + " total)");
                }
                System.out.println();
                System.out.println();
            }
            
            System.out.println("=== Demo Completed Successfully ===");
            
        } catch (Exception e) {
            System.err.println("Error during material planning: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static MaterialPlanConfig createSampleConfiguration() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(5);
        
        MaterialPlanConfig config = new MaterialPlanConfig(startDate, endDate);
        
        // Sample material demands
        List<MaterialDemand> demands = Arrays.asList(
            new MaterialDemand("WIDGET_A", startDate, 100.0),
            new MaterialDemand("WIDGET_A", startDate.plusDays(1), 150.0),
            new MaterialDemand("WIDGET_B", startDate.plusDays(1), 80.0),
            new MaterialDemand("WIDGET_C", startDate.plusDays(2), 120.0),
            new MaterialDemand("WIDGET_A", startDate.plusDays(3), 200.0),
            new MaterialDemand("WIDGET_B", startDate.plusDays(3), 90.0),
            new MaterialDemand("WIDGET_C", startDate.plusDays(4), 160.0)
        );
        
        // Material definitions (material -> resource mapping with rates)
        List<MaterialDefinition> definitions = Arrays.asList(
            new MaterialDefinition("WIDGET_A", "MACHINE_001", 50.0), // 50 units per hour
            new MaterialDefinition("WIDGET_B", "MACHINE_002", 40.0), // 40 units per hour
            new MaterialDefinition("WIDGET_C", "MACHINE_001", 30.0)  // 30 units per hour (shared resource)
        );
        
        // Resource capacities
        List<MaterialCapacity> capacities = new ArrayList<>();
        for (int day = 0; day < 6; day++) {
            LocalDate date = startDate.plusDays(day);
            capacities.add(new MaterialCapacity("MACHINE_001", date, 12.0)); // 12 hours available
            capacities.add(new MaterialCapacity("MACHINE_002", date, 10.0)); // 10 hours available  
        }
        
        // Initial inventory
        Map<String, Double> initialInventory = new HashMap<>();
        initialInventory.put("WIDGET_A", 50.0);
        initialInventory.put("WIDGET_B", 30.0);
        initialInventory.put("WIDGET_C", 20.0);
        
        config.setDemands(demands);
        config.setDefinitions(definitions);
        config.setCapacities(capacities);
        config.setInitialInventory(initialInventory);
        
        return config;
    }
}