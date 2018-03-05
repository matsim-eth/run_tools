package ch.ethz.matsim.run_tools.framework.simulation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

import ch.ethz.matsim.run_tools.framework.run.RunEnvironment;

public class SimulationHandle {
	final private RunEnvironment environment;
	final private String id;

	public SimulationHandle(RunEnvironment environment, String id) {
		this.environment = environment;
		this.id = id;
	}

	public FileObject getDirectory() {
		return environment.getRunDirectory(id);
	}

	public void start() {
		environment.start(id);
	}

	public void stop() {
		environment.stop(id);
	}

	public boolean isRunning() {
		return environment.isRunning(id);
	}

	public int getIteration() {
		int iteration = 0;

		try {
			InputStream inputStream = getDirectory().resolveFile("output/scorestats.txt").getContent().getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("ITERATION")) {
					continue;
				}

				iteration = Integer.parseInt(line.split("\t")[0]);
			}

			reader.close();
		} catch (IOException | NumberFormatException e) {
			iteration = -1;
		}

		return iteration;
	}

	public boolean hasOutput() {
		try {
			return getDirectory().getChild("output").exists();
		} catch (FileSystemException e) {
			return false;
		}
	}

	public void clearOutput() {
		try {
			if (getDirectory().resolveFile("output").exists()) {
				getDirectory().resolveFile("output").deleteAll();
			}
		} catch (FileSystemException e) {
			e.printStackTrace();
		}
	}

	public void clearIterations() {
		try {
			if (getDirectory().resolveFile("output/ITERS").exists()) {
				getDirectory().resolveFile("output/ITERS").deleteAll();
			}
		} catch (FileSystemException e) {
			e.printStackTrace();
		}
	}
}
