package ch.ethz.matsim.run_tools.examples;

import java.io.File;
import java.util.Arrays;

import ch.ethz.matsim.run_tools.framework.run.LocalLinuxRunEnvironment;
import ch.ethz.matsim.run_tools.framework.run.RunDescription;

/**
 * TODO: Introduction to this example. TODO: Working simulation example.
 */
public class ExampleRunEnvironment {
	final private static String RUN_ID = "example1";

	static public void main(String[] args) throws InterruptedException {
		// The argument of this script is the location where the run environment
		// is/should be located.
		String environmentPath = args[0];

		// First we need to create the run environment. This will set up the file that
		// contains the state of the environment. If the script is restarted and the
		// environment is already there, the state will be loaded so we know which runs
		// are registered in the environment (and potentially are still running).
		LocalLinuxRunEnvironment runEnvironment = new LocalLinuxRunEnvironment("run_example.json",
				new File(environmentPath));

		// Check if our run already exists
		if (!runEnvironment.exists(RUN_ID)) {
			System.out.println("Setting up simulation ...");

			// The run does not exist. We can set up a new run by constructing a
			// RunDescription object.
			RunDescription description = new RunDescription();

			// The class path, relative to the environment. So in this case we assume that
			// the jar is already located in our environment. You can copy the standard
			// stand-alone MATSim jar there manually. The class path can also be more
			// complex, just as you would define it for the java command line.
			description.classPath = Arrays.asList("matsim-0.9.0/libs/*", "matsim-0.9.0/matsim-0.9.0.jar");

			// The name of the class that contains the main method that should be called by
			// the runner.
			description.entryPoint = "org.matsim.run.Controler";

			// Arguments that should be passed to the main function. Additionally,
			// vmArguments can be defined to add memory restrictions and similar things.
			description.arguments = Arrays.asList("../matsim-0.9.0/examples/equil/config.xml");

			// Send the description to the environment to set up the run. This does not
			// start the run yet, it is in idle now. Every run is known by a specific ID,
			// here it is RUN_ID.
			runEnvironment.setup(RUN_ID, description);
		}

		boolean outputExists = new File(runEnvironment.getDirectory(RUN_ID), "output").exists();

		// Only if the simulation is NOT RUNNING and there is NO OUTPUT we start the
		// simulation. This makes sure that a finished simulation (= not running) will
		// not be restarted by this script.
		if (!outputExists && !runEnvironment.isRunning(RUN_ID)) {
			System.out.println("Starting simulation ...");

			// Start the run with the id RUN_ID
			runEnvironment.start(RUN_ID);
		}

		// Print a message as long as the simulation is running.
		while (runEnvironment.isRunning(RUN_ID)) {
			System.out.println("Simulation is running ...");
			Thread.sleep(1000);
		}

		System.out.println("Simulation has stopped!");

		// Final remark: Please note that this script is built in a way that it can be
		// called again and again. Only the first time it will create the run and only
		// if it notices that the run has not been started yet, it will do so. This
		// means you can first call this script to start the simulation, but any
		// subsequent call will either show you that the simulation is still running, or
		// it will say that it has finished. This is a crucial point of the run_tools:
		// Whenever a script like this is called it should recover its state from the
		// run environment and continue to do what it did before.
	}
}
