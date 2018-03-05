package ch.ethz.matsim.run_tools.framework.simulation;

import java.util.LinkedList;

import ch.ethz.matsim.run_tools.framework.run.RunDescription;
import ch.ethz.matsim.run_tools.framework.run.RunEnvironment;

public class SimulationEnvironment {
	final private RunEnvironment runEnvironment;
	final private SimulationConfigurator configurator;
	final private RunDescription runDescription;

	public SimulationEnvironment(RunDescription runDescription, RunEnvironment runEnvironment,
			SimulationConfigurator configurator) {
		this.runEnvironment = runEnvironment;
		this.configurator = configurator;
		this.runDescription = runDescription;
	}

	public SimulationHandle setup(String id, SimulationDescription simulationDescription) {
		if (runEnvironment.exists(id)) {
			throw new IllegalStateException("Simulation " + id + " already exists.");
		}

		RunDescription newRunDescription = new RunDescription();

		newRunDescription.entryPoint = runDescription.entryPoint;
		newRunDescription.classPath = runDescription.classPath;
		newRunDescription.memory = runDescription.memory;

		newRunDescription.arguments = new LinkedList<>(runDescription.arguments);
		newRunDescription.vmArguments = new LinkedList<>(runDescription.vmArguments);

		configurator.configureRunner(id, simulationDescription, newRunDescription);
		runEnvironment.setup(id, newRunDescription);

		SimulationHandle handle = new SimulationHandle(runEnvironment, id);
		configurator.configureSimulation(id, simulationDescription, handle);
		return handle;
	}

	public SimulationHandle recover(String id) {
		if (!runEnvironment.exists(id)) {
			throw new IllegalStateException("Simulation " + id + " does not exists.");
		}

		return new SimulationHandle(runEnvironment, id);
	}

	public boolean exists(String id) {
		return runEnvironment.exists(id);
	}
}
