package ch.ethz.matsim.run_tools.framework.simulation;

import ch.ethz.matsim.run_tools.framework.run.RunDescription;

public interface SimulationConfigurator {
	void configureRunner(String id, SimulationDescription description, RunDescription runDescription);

	void configureSimulation(String id, SimulationDescription description, SimulationHandle handle);
}
