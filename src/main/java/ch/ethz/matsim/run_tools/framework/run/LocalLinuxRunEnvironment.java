package ch.ethz.matsim.run_tools.framework.run;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LocalLinuxRunEnvironment implements RunEnvironment {
	final private Logger logger = Logger.getLogger(LocalLinuxRunEnvironment.class);

	final private TypeReference<HashMap<String, RunInfo>> environmentTypeReference = new TypeReference<HashMap<String, RunInfo>>() {
	};

	final private File rootDirectory;
	final private File environmentFile;

	final private Map<String, RunInfo> environment;

	public LocalLinuxRunEnvironment(String stateFile, File rootDirectory) {
		this.rootDirectory = rootDirectory;

		try {
			this.environmentFile = new File(rootDirectory, stateFile);

			if (environmentFile.exists()) {
				environment = new ObjectMapper().readValue(environmentFile, environmentTypeReference);
			} else {
				environment = new HashMap<>();
				updateEnvironment();
				logger.info("Initialized environment at " + environmentFile);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		logger.info("Initialized environment with " + environment.size() + " runs.");
		cleanup();
	}

	private void cleanup() {
		Set<String> removeIds = new HashSet<>();

		for (String id : environment.keySet()) {
			if (!exists(id)) {
				removeIds.add(id);
				logger.info("Cleanup: Run " + id + " does not exist anymore.");
			}
		}

		environment.keySet().removeAll(removeIds);

		for (String id : environment.keySet()) {
			if (!isRunning(id, false)) {
				environment.get(id).pid = null;
				logger.info("Cleanup: Run " + id + " is not running anymore. Setting PID to null.");
			}
		}

		updateEnvironment();
	}

	private File getSimulationDirectory(String id) {
		return new File(rootDirectory, id);
	}

	private void checkExists(String id) {
		if (!environment.containsKey(id)) {
			throw new IllegalStateException("Run " + id + " does not exist");
		}
	}

	private void checkInitialized(String id) {
		if (!getSimulationDirectory(id).exists()) {
			throw new IllegalStateException("Run " + id + " is not initialized");
		}
	}

	private void updateEnvironment() {
		try {
			new ObjectMapper().writeValue(environmentFile, environment);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setup(String id, RunDescription description) {
		if (environment.containsKey(id)) {
			throw new IllegalStateException("Run " + id + " already exists");
		}

		if (getSimulationDirectory(id).exists()) {
			throw new IllegalStateException("Run " + id + " is already initialized");
		}

		if (description.classPath == null || description.classPath.isEmpty()) {
			throw new IllegalArgumentException("No classpath set for run " + id);
		}

		if (description.entryPoint == null) {
			throw new IllegalArgumentException("No entry point set for run " + id);
		}

		getSimulationDirectory(id).mkdir();

		RunInfo info = new RunInfo();
		info.description = description;

		environment.put(id, info);

		updateEnvironment();
		logger.info("Set up run " + id);
	}

	@Override
	public void remove(String id) {
		checkExists(id);
		checkInitialized(id);

		if (isRunning(id)) {
			throw new IllegalStateException();
		}

		try {
			FileUtils.deleteDirectory(getSimulationDirectory(id));
		} catch (IOException e) {
			throw new RuntimeException("Could not delete run " + id);
		}

		environment.remove(id);

		updateEnvironment();
		logger.info("Removed run " + id);
	}

	@Override
	public void start(String id) {
		checkExists(id);
		checkInitialized(id);

		if (isRunning(id)) {
			throw new IllegalStateException("Run " + id + " is already running");
		}

		RunInfo info = environment.get(id);
		String path = getSimulationDirectory(id).getAbsolutePath();

		List<String> command = new LinkedList<>();
		command.add("java");
		command.addAll(info.description.vmArguments);
		command.add("-cp");
		command.add(String.join(":", info.description.classPath.stream().map(s -> new File(rootDirectory, s).toString())
				.collect(Collectors.toList())));
		command.add(info.description.entryPoint);
		command.addAll(info.description.arguments);
		command.add("1>");
		command.add(new File(path, "run_output.log").toString());
		command.add("2>");
		command.add(new File(path, "run_error.log").toString());
		command.add("&");
		String javaCommand = String.join(" ", command);

		File pidPath = new File(path, "simulation.pid");
		File runScriptPath = new File(path, "runner.sh");

		String runScript = "cd " + path + "\n" + javaCommand + "\necho $! > " + pidPath;

		try {
			OutputStream outputStream = new FileOutputStream(runScriptPath);
			OutputStreamWriter streamWriter = new OutputStreamWriter(outputStream);
			streamWriter.write(runScript);
			streamWriter.flush();
			streamWriter.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Error while creating the run file for run " + id);
		} catch (IOException e) {
			throw new RuntimeException("Error while writing the run file for run " + id);
		}

		// Start.
		try {
			Runtime.getRuntime().exec("sh " + runScriptPath);
		} catch (IOException e) {
			throw new RuntimeException("Error while running the run script for run " + id);
		}

		long failTime = System.currentTimeMillis() + 10 * 1000;

		while (!pidPath.exists()) {
			if (System.currentTimeMillis() > failTime) {
				throw new RuntimeException("Unable to get PID for run " + id + ". Possibly still running.");
			}

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new RuntimeException("Interrupted while getting PID for run " + id + ". Possibly still running.");
			}
		}

		try {
			InputStream inputStream = new FileInputStream(pidPath);
			BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream));
			info.pid = Integer.parseInt(streamReader.readLine());
			streamReader.close();
		} catch (FileNotFoundException e) {
			throw new IllegalStateException();
		} catch (IOException e) {
			throw new RuntimeException("Error while reading PID for run " + id);
		}

		updateEnvironment();
		logger.info("Started run " + id + " with PID " + info.pid);
	}

	@Override
	public boolean isRunning(String id) {
		return isRunning(id, true);
	}

	private boolean isRunning(String id, boolean updateEnvironment) {
		checkExists(id);
		checkInitialized(id);

		RunInfo info = environment.get(id);

		if (info.pid == null) {
			return false;
		}

		try {
			Process process = Runtime.getRuntime().exec("ps " + info.pid);
			process.waitFor();

			if (process.exitValue() == 0) {
				return true;
			}
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Error while looking for PID " + info.pid + " (run " + id + ")");
		}

		if (updateEnvironment) {
			info.pid = null;

			updateEnvironment();
			logger.info("Run " + id + " is not running anymore. Cleaning up PID.");
		}

		return false;
	}

	@Override
	public void stop(String id) {
		checkExists(id);
		checkInitialized(id);

		if (!isRunning(id)) {
			throw new IllegalStateException("Run " + id + " is not running");
		}

		RunInfo info = environment.get(id);

		try {
			Runtime.getRuntime().exec("kill -9 " + info.pid);
		} catch (IOException e) {
			throw new RuntimeException("Error while killing process with pid " + info.pid + "(run " + id + ")");
		}

		long failTime = System.currentTimeMillis() + 10 * 1000;

		while (isRunning(id)) {
			if (System.currentTimeMillis() > failTime) {
				throw new RuntimeException("Unable to stop run " + id);
			}

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new RuntimeException("Interrupted while waiting for " + id + " to end.");
			}
		}

		info.pid = null;

		updateEnvironment();
		logger.info("Stopped run " + id);
	}

	@Override
	public boolean exists(String id) {
		return environment.containsKey(id) && getSimulationDirectory(id).exists();
	}

	public File getDirectory(String id) {
		return getSimulationDirectory(id);
	}

	static private class RunInfo {
		public RunDescription description = null;
		public Integer pid = null;
	}

	@Override
	public Collection<String> getAvailableIds() {
		return Collections.unmodifiableCollection(environment.keySet());
	}

	@Override
	public FileObject getRootDirectory() {
		try {
			return VFS.getManager().resolveFile(rootDirectory.getAbsolutePath());
		} catch (FileSystemException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public FileObject getRunDirectory(String id) {
		try {
			return VFS.getManager().resolveFile(getSimulationDirectory(id).getAbsolutePath());
		} catch (FileSystemException e) {
			throw new RuntimeException(e);
		}
	}
}
