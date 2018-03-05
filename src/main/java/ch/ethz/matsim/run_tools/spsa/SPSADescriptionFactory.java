package ch.ethz.matsim.run_tools.spsa;

import java.util.List;

import ch.ethz.matsim.run_tools.framework.simulation.SimulationDescription;

@FunctionalInterface
public interface SPSADescriptionFactory {
	SimulationDescription create(List<Double> candidate);
}
