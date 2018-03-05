package ch.ethz.matsim.run_tools.examples;

import java.io.File;
import java.io.OutputStreamWriter;
import java.util.Arrays;

import org.apache.commons.vfs2.FileSystemException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.utils.io.UncheckedIOException;

import ch.ethz.matsim.run_tools.framework.run.LocalLinuxRunEnvironment;
import ch.ethz.matsim.run_tools.framework.run.RunDescription;
import ch.ethz.matsim.run_tools.framework.simulation.SimulationConfigurator;
import ch.ethz.matsim.run_tools.framework.simulation.SimulationDescription;
import ch.ethz.matsim.run_tools.framework.simulation.SimulationEnvironment;
import ch.ethz.matsim.run_tools.framework.simulation.SimulationHandle;

public class ExampleSimulationEnvironment {
	final private static String SIMULATION_ID = "example2";

	static public void main(String[] args) throws InterruptedException {
		String environmentPath = args[0];

		LocalLinuxRunEnvironment runEnvironment = new LocalLinuxRunEnvironment("run_example.json",
				new File(environmentPath));

		RunDescription runDescriptionTemplate = new RunDescription();
		runDescriptionTemplate.classPath = Arrays.asList("matsim-0.9.0/libs/*", "matsim-0.9.0/matsim-0.9.0.jar");
		runDescriptionTemplate.entryPoint = "org.matsim.run.Controler";
		runDescriptionTemplate.arguments = Arrays.asList("config.xml");

		SimulationConfigurator configurator = new ExampleSimulationConfigurator();

		SimulationEnvironment simulationEnvironment = new SimulationEnvironment(runDescriptionTemplate, runEnvironment,
				configurator);

		SimulationHandle handle = null;

		if (!simulationEnvironment.exists(SIMULATION_ID)) {
			handle = simulationEnvironment.setup(SIMULATION_ID, new ExampleSimulationDescription(-0.4));
		} else {
			handle = simulationEnvironment.recover(SIMULATION_ID);
		}

		if (!handle.isRunning()) {
			handle.clearOutput();
			System.out.println("Started simulation");
			handle.start();
		}

		while (handle.isRunning()) {
			System.out.println("Still running. Iteration: " + handle.getIteration());
			Thread.sleep(1000);

			if (handle.getIteration() > 10) {
				handle.stop();
			}
		}

		System.out.println("Simulation has stopped.");
	}

	static public class ExampleSimulationDescription implements SimulationDescription {
		final public double alphaCar;

		public ExampleSimulationDescription(double alphaCar) {
			this.alphaCar = alphaCar;
		}
	}

	static public class ExampleSimulationConfigurator implements SimulationConfigurator {
		@Override
		public void configureRunner(String id, SimulationDescription description, RunDescription runDescription) {
		}

		@Override
		public void configureSimulation(String id, SimulationDescription description, SimulationHandle handle) {
			try {
				Config config = ConfigUtils.createConfig();

				new ConfigReader(config).parse(
						handle.getDirectory().resolveFile("../matsim-0.9.0/examples/equil/config.xml").getContent().getInputStream());

				config.network().setInputFile("../matsim-0.9.0/examples/equil/network.xml");
				config.plans().setInputFile("../matsim-0.9.0/examples/equil/plans100.xml");
				config.facilities().setInputFile("../matsim-0.9.0/examples/equil/facilities.xml");

				ExampleSimulationDescription simulationDescription = (ExampleSimulationDescription) description;
				config.planCalcScore().getModes().get("car").setConstant(simulationDescription.alphaCar);

				new ConfigWriter(config).writeStream(new OutputStreamWriter(
						handle.getDirectory().resolveFile("config.xml").getContent().getOutputStream()));
			} catch (UncheckedIOException | FileSystemException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
