package ch.ethz.matsim.run_tools.framework.run;

import java.util.Collection;

import org.apache.commons.vfs2.FileObject;

public interface RunEnvironment {
	void start(String id);

	void stop(String id);

	void setup(String id, RunDescription description);

	void remove(String id);

	boolean isRunning(String id);

	boolean exists(String id);

	FileObject getRootDirectory();

	FileObject getRunDirectory(String id);

	Collection<String> getAvailableIds();
}
