package ch.ethz.matsim.run_tools.spsa;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ethz.matsim.run_tools.framework.run.RunEnvironment;
import ch.ethz.matsim.run_tools.framework.simulation.SimulationDescription;
import ch.ethz.matsim.run_tools.framework.simulation.SimulationEnvironment;
import ch.ethz.matsim.run_tools.framework.simulation.SimulationHandle;
import ch.ethz.matsim.run_tools.spsa.projection.SPSAProjection;
import ch.ethz.matsim.run_tools.spsa.sampler.SPSASampler;

public class SPSAEnvironment {
	final private Logger logger;
	final private SimulationEnvironment simulationEnvironment;

	final private SPSAObjective objective;
	final private SPSASampler sampler;
	final private SPSAProjection projection;
	final private SPSADescriptionFactory descriptionFactory;
	final private SPSASequence sequence;

	final private State state;
	final private FileObject calibrationFile;
	final private String prefix;

	final private int numberOfDimensions;
	final private int numberOfSimulationIterations;
	final private int intermediateObjectiveInterval;
	final private List<Double> initialCandidate;

	public SPSAEnvironment(RunEnvironment runEnvironment, SimulationEnvironment simulationEnvironment,
			SPSAObjective objective, SPSASampler sampler, SPSAProjection projection,
			SPSADescriptionFactory descriptionFactory, SPSASequence sequence, String prefix,
			List<Double> initialCandidate, int numberOfSimulationIterations, int intermediateObjectiveInterval)
			throws JsonParseException, JsonMappingException, IOException {
		this.simulationEnvironment = simulationEnvironment;
		this.calibrationFile = runEnvironment.getRootDirectory().resolveFile(prefix + "_spsa.json");
		this.prefix = prefix;

		this.objective = objective;
		this.sampler = sampler;
		this.projection = projection;
		this.descriptionFactory = descriptionFactory;
		this.sequence = sequence;

		this.numberOfDimensions = initialCandidate.size();
		this.initialCandidate = initialCandidate;
		this.numberOfSimulationIterations = numberOfSimulationIterations;
		this.intermediateObjectiveInterval = intermediateObjectiveInterval;

		this.logger = Logger.getLogger("SPSA " + prefix);

		if (calibrationFile.exists()) {
			state = new ObjectMapper().readValue(calibrationFile.getContent().getInputStream(), State.class);
			logger.info("Initialized SPSA from " + calibrationFile.getName().getPath());
		} else {
			state = new State();
			logger.info("Initialized new SPSA at " + calibrationFile.getName().getPath());
		}
	}

	private String buildSimulationId(String id) {
		return prefix + "_" + id;
	}

	private void save() throws JsonGenerationException, JsonMappingException, FileSystemException, IOException {
		new ObjectMapper().writeValue(calibrationFile.getContent().getOutputStream(), state);
	}

	private List<Double> buildPerturbation() {
		List<Double> perturbation = new LinkedList<>();

		for (int i = 0; i < numberOfDimensions; i++) {
			perturbation.add(sampler.sample());
		}

		return perturbation;
	}

	private List<List<Double>> buildGradientCandidates(double c, List<Double> perturbation,
			List<Double> objectiveCandidate) {
		List<Double> projectionCandidate = projection.projectGradientCandidate(c, perturbation, objectiveCandidate);

		List<Double> firstGradientCandidate = new LinkedList<>();
		List<Double> secondGradientCandidate = new LinkedList<>();

		for (int i = 0; i < numberOfDimensions; i++) {
			firstGradientCandidate.add(projectionCandidate.get(i) + c * perturbation.get(i));
			secondGradientCandidate.add(projectionCandidate.get(i) - c * perturbation.get(i));
		}

		logger.info("New gradient candidates:");
		logger.info("  - First gradient candidate: " + firstGradientCandidate);
		logger.info("  - Second gradient candidate: " + secondGradientCandidate);

		if (projectionCandidate.equals(objectiveCandidate)) {
			logger.info("  - No projection has been performed.");
		} else {

			logger.info("  - Projection has been performed: " + projectionCandidate);
		}

		return Arrays.asList(firstGradientCandidate, secondGradientCandidate);
	}

	private void startIteration(List<Double> candidate)
			throws JsonGenerationException, JsonMappingException, FileSystemException, IOException {
		int n = state.iterations.size() + 1;
		double c = sequence.getPerturbationFactor(n);

		List<Double> perturbation = buildPerturbation();
		List<List<Double>> gradientCandidates = buildGradientCandidates(c, perturbation, candidate);

		Evaluation objectiveEvaluation = new Evaluation();
		objectiveEvaluation.description = descriptionFactory.create(candidate);
		objectiveEvaluation.simulationId = buildSimulationId("objective_" + n);

		Evaluation firstGradientEvaluation = new Evaluation();
		firstGradientEvaluation.description = descriptionFactory.create(gradientCandidates.get(0));
		firstGradientEvaluation.simulationId = buildSimulationId("first_gradient_" + n);

		Evaluation secondGradientEvaluation = new Evaluation();
		secondGradientEvaluation.description = descriptionFactory.create(gradientCandidates.get(1));
		secondGradientEvaluation.simulationId = buildSimulationId("second_gradient_" + n);

		SPSAIteration iteration = new SPSAIteration();
		iteration.objectiveEvaluation = objectiveEvaluation;
		iteration.firstGradientEvaluation = firstGradientEvaluation;
		iteration.secondGradientEvaluation = secondGradientEvaluation;
		iteration.perturbation = perturbation;
		iteration.candidate = candidate;

		state.iterations.add(iteration);
		state.lastObjectiveIteration = -1;
		state.lastFirstGradientIteration = -1;
		state.lastSecondGradientIteration = -1;

		SimulationHandle objectiveHandle = simulationEnvironment.setup(objectiveEvaluation.simulationId,
				objectiveEvaluation.description);
		SimulationHandle firstGradientHandle = simulationEnvironment.setup(firstGradientEvaluation.simulationId,
				firstGradientEvaluation.description);
		SimulationHandle secondGradientHandle = simulationEnvironment.setup(secondGradientEvaluation.simulationId,
				secondGradientEvaluation.description);

		objectiveHandle.start();
		firstGradientHandle.start();
		secondGradientHandle.start();

		save();
	}

	private boolean stopIfFinished(Evaluation evaluation, SimulationHandle handle, int iteration, int lastIteration) {
		if (handle.isRunning()) {
			for (int intermediateIteration = 0; intermediateIteration <= numberOfSimulationIterations; intermediateIteration += intermediateObjectiveInterval) {
				if (intermediateIteration < iteration
						&& intermediateIteration > evaluation.lastIntermediateObjectiveIteration) {
					evaluation.intermediateObjectives
							.add(objective.getIntermediateObjective(handle, intermediateIteration));
					evaluation.lastIntermediateObjectiveIteration = intermediateIteration;
				}
			}

			if (iteration > numberOfSimulationIterations) {
				handle.stop();
				evaluation.objective = objective.getObjective(handle);
				handle.clearIterations();
				return true;
			} else {
				return false;
			}
		} else {
			if (lastIteration <= numberOfSimulationIterations) {
				throw new IllegalStateException("Simulation died before finishing");
			}

			return true;
		}
	}

	private boolean updateIterations(int objectiveIteration, int firstGradientIteration, int secondGradientIteration)
			throws JsonGenerationException, JsonMappingException, FileSystemException, IOException {
		if ((objectiveIteration > state.lastObjectiveIteration)
				|| (firstGradientIteration > state.lastFirstGradientIteration)
				|| (secondGradientIteration > state.lastSecondGradientIteration)) {
			state.lastObjectiveIteration = objectiveIteration;
			state.lastFirstGradientIteration = firstGradientIteration;
			state.lastSecondGradientIteration = secondGradientIteration;
			save();

			return true;
		}

		return false;
	}

	private List<Double> buildNextCandidate(SPSAIteration iteration) {
		int n = state.iterations.size();

		double a = sequence.getGradientFactor(n);
		double c = sequence.getPerturbationFactor(n);

		List<Double> gradient = new LinkedList<>();
		double nominator = iteration.firstGradientEvaluation.objective - iteration.secondGradientEvaluation.objective;

		for (int i = 0; i < numberOfDimensions; i++) {
			double denominator = 2.0 * c * iteration.perturbation.get(i);
			gradient.add(nominator / denominator);
		}

		List<Double> candidate = new LinkedList<>();

		for (int i = 0; i < numberOfDimensions; i++) {
			candidate.add(iteration.candidate.get(i) - a * gradient.get(i));
		}

		List<Double> projectionCandidate = projection.projectObjectiveCandidate(candidate);

		logger.info("New objective candidate:");
		logger.info("  - Gradient: " + gradient);
		logger.info("  - Candidate: " + projectionCandidate);

		if (projectionCandidate.equals(candidate)) {
			logger.info("  - No projection has been performed");
		} else {
			logger.info("  - Projection has been performed from " + candidate);
		}

		return projectionCandidate;
	}

	public void update() throws JsonGenerationException, JsonMappingException, FileSystemException, IOException {
		if (state.iterations.size() == 0) {
			startIteration(initialCandidate);
		} else {
			SPSAIteration spsaIteration = state.iterations.get(state.iterations.size() - 1);

			SimulationHandle objectiveHandle = simulationEnvironment
					.recover(spsaIteration.objectiveEvaluation.simulationId);
			SimulationHandle firstGradientHandle = simulationEnvironment
					.recover(spsaIteration.firstGradientEvaluation.simulationId);
			SimulationHandle secondGradientHandle = simulationEnvironment
					.recover(spsaIteration.secondGradientEvaluation.simulationId);

			int objectiveIteration = objectiveHandle.getIteration();
			int firstGradientIteration = firstGradientHandle.getIteration();
			int secondGradientIteration = secondGradientHandle.getIteration();

			boolean iterationFinished = true;

			iterationFinished &= stopIfFinished(spsaIteration.objectiveEvaluation, objectiveHandle, objectiveIteration,
					state.lastObjectiveIteration);
			iterationFinished &= stopIfFinished(spsaIteration.firstGradientEvaluation, firstGradientHandle,
					firstGradientIteration, state.lastFirstGradientIteration);
			iterationFinished &= stopIfFinished(spsaIteration.secondGradientEvaluation, secondGradientHandle,
					secondGradientIteration, state.lastSecondGradientIteration);

			if (iterationFinished) {
				logger.info("Iteration " + (state.iterations.size() + 1) + " has finished:");
				logger.info("  - Candidate objective: " + spsaIteration.objectiveEvaluation.objective);
				logger.info("  - First gradient objective: " + spsaIteration.firstGradientEvaluation.objective);
				logger.info("  - Second gradient objective: " + spsaIteration.secondGradientEvaluation.objective);

				startIteration(buildNextCandidate(spsaIteration));
			} else if (updateIterations(objectiveIteration, firstGradientIteration, secondGradientIteration)) {
				logger.info(String.format("Iteration %d running. [Objective: %d , 1st Gradient: %d , 2nd Gradient: %d]",
						state.iterations.size(), objectiveIteration, firstGradientIteration, secondGradientIteration));
			}
		}
	}

	private static class Evaluation {
		public String simulationId = null;
		public SimulationDescription description = null;
		public Double objective = null;

		public List<Double> intermediateObjectives = new LinkedList<>();
		public int lastIntermediateObjectiveIteration = -1;
	}

	private static class SPSAIteration {
		public List<Double> candidate;
		public List<Double> perturbation;

		public Evaluation firstGradientEvaluation;
		public Evaluation secondGradientEvaluation;
		public Evaluation objectiveEvaluation;
	}

	private static class State {
		public List<SPSAIteration> iterations = new LinkedList<>();

		int lastObjectiveIteration;
		int lastFirstGradientIteration;
		int lastSecondGradientIteration;
	}
}
