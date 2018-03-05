package ch.ethz.matsim.run_tools.spsa;

import ch.ethz.matsim.run_tools.framework.simulation.SimulationHandle;

public interface SPSAObjective {
	double getObjective(SimulationHandle handle);
	double getIntermediateObjective(SimulationHandle handle, int iteration);
}
