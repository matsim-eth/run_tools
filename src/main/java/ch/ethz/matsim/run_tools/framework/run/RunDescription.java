package ch.ethz.matsim.run_tools.framework.run;

import java.util.LinkedList;
import java.util.List;

public class RunDescription {
	public Double memory = null;
	public List<String> classPath = new LinkedList<>();
	public String entryPoint = null;

	public List<String> arguments = new LinkedList<>();
	public List<String> vmArguments = new LinkedList<>();
}
