package personas.ark.cs.cmu.edu;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import personas.ark.cs.cmu.edu.HyperparameterOptimization;
import personas.ark.cs.cmu.edu.containers.Doc;
import personas.ark.cs.cmu.edu.containers.Entity;
import personas.ark.cs.cmu.edu.containers.EventArg;
import personas.ark.cs.cmu.edu.containers.EventRole;
import personas.ark.cs.cmu.edu.containers.EventTuple;
import personas.ark.cs.cmu.edu.util.DataReader;
import personas.ark.cs.cmu.edu.util.PrintUtil;
import cc.mallet.util.Randoms;

import com.google.common.collect.Maps;

import edu.stanford.math.primitivelib.autogen.matrix.DoubleSparseVector;
import edu.stanford.math.primitivelib.autogen.matrix.DoubleVectorEntry;
import edu.stanford.nlp.math.ArrayMath;

public class PersonaModel {

	public ArrayList<Doc> data;

	public double alpha = 1;

	// indirect linkers: persona x verbclass
	public int[] LAgentTotal;
	public int[][] LAgents;
	public int[] LPatientTotal;
	public int[][] LPatients;
	public int[] LModifierTotal;
	public int[][] LModifiers;
	// corresponding hyperparameters
	public double nuA = 1;
	public double nuP = 1;
	public double nuM = 1;

	public int A = 50;
	public boolean personaRegression;

	public double L2;

	public int totalEntities;

	public HashMap<String, String> genreIdToString;
	public int numFeatures;

	// number of personas
	public int K;
	// max vocabulary size
	public int V;

	// hyperparameter on phi
	public double gamma = 1;

	// verbclass x vocab multinomial
	public double[][] phis;

	// verbclass token total counts
	public double[] phiTotals;

	// regression
	public String weights;

	// map from vocab ID (index of array) to String
	public String[] reverseVocab;

	// trained logreg model
	public SparseMulticlassLogisticRegression.TrainedModel model;

	public double[][] finalPhis;
	public double[][] finalLAgents;
	public double[][] finalLPatients;
	public double[][] finalLMod;

	public HashMap<String, String> characterNames;
	public HashMap<String, HashSet<Integer>> characterFeatures;

	public double[] featureMeans;

	public HashMap<String, Integer> featureIds;
	public String[] reverseFeatureIds;

	// map from doc ids (1577389) to movie titles ("The Remains of the Day")
	public HashMap<String, String> movieTitles;

	// genre metadata for all the movies
	public HashMap<String, Set<String>> movieGenres;

	// for logistic regression: set of predictors, response counts (for each of
	// K classes)
	public double[][] responses;

	public SparseMulticlassLogisticRegression reg;

	public Randoms random = new Randoms();

	public PersonaModel() {
		featureIds = new HashMap<String, Integer>();
	}

	public void initialize() {

		LAgentTotal = new int[A];
		LAgents = new int[A][K];

		LPatientTotal = new int[A];
		LPatients = new int[A][K];

		LModifierTotal = new int[A];
		LModifiers = new int[A][K];

		phis = new double[K][V];
		phiTotals = new double[K];

		finalPhis = new double[K][V];
		finalLAgents = new double[A][K];
		finalLPatients = new double[A][K];
		finalLMod = new double[A][K];

		responses = new double[totalEntities][A];

		reg = new SparseMulticlassLogisticRegression(L2);
		model = reg.getDefault(A, numFeatures);

	}

	public void saveSamples() {

		for (int i = 0; i < data.size(); i++) {
			Doc doc = data.get(i);
			for (Entity e : doc.entities.values()) {
				if (e.getNumEvents() > 0) {
					responses[e.canonicalIndex][e.currentType]++;
					e.saveSample(e.currentType);
				}
			}
		}
	}

	public void saveFinalSamples() {

		for (int i = 0; i < data.size(); i++) {
			Doc doc = data.get(i);
			for (Entity e : doc.entities.values()) {
				int a = e.currentType;
				for (EventArg arg : e.agentArgs) {
					arg.saveFinalSample();
					finalLAgents[a][arg.currentSample]++;
					finalPhis[arg.currentSample][arg.tuple.canonicalVerb]++;
				}
				for (EventArg arg : e.patientArgs) {
					arg.saveFinalSample();
					finalLPatients[a][arg.currentSample]++;
					finalPhis[arg.currentSample][arg.tuple.canonicalVerb]++;
				}
				for (EventArg arg : e.modifieeArgs) {
					arg.saveFinalSample();
					finalLMod[a][arg.currentSample]++;
					finalPhis[arg.currentSample][arg.tuple.canonicalVerb]++;
				}
				if (e.getNumEvents() > 0) {
					e.saveFinalSample(e.currentType);
				}
			}

		}
	}

	/*
	 * Run multiclass logistic regression using all samples in responses. Clear
	 * response counts at the end.
	 */
	public void regress() {

		ArrayList<double[]> goodResponses = new ArrayList<double[]>();
		ArrayList<DoubleSparseVector> regressionPredictors = new ArrayList<DoubleSparseVector>();

		HashMap<String, DoubleSparseVector> vectors = new HashMap<String, DoubleSparseVector>();
		HashMap<DoubleSparseVector, Integer> indices = new HashMap<DoubleSparseVector, Integer>();
		int maxInt = 0;

		for (int i = 0; i < data.size(); i++) {

			Doc doc = data.get(i);
			for (Entity e : doc.entities.values()) {
				if (e.getNumEvents() > 0) {
					int index = e.canonicalIndex;
					DoubleSparseVector predictor = new DoubleSparseVector(
							numFeatures);

					int sampleCount = 0;
					for (int j = 0; j < responses[index].length; j++) {
						sampleCount += responses[index][j];
					}
					if (sampleCount > 0) {
						if (doc.genres != null) {

							for (String genre : doc.genres) {
								int genreID = featureIds.get(genre);
								predictor.set(genreID, 1.0);

							}

							HashSet<Integer> characterFeatures = e
									.getCharacterFeatures();
							for (int feat : characterFeatures) {
								predictor.set(feat, 1.0);
							}

						}

						// Group everything by unique combinations of features
						String key = predictor.toString();
						if (vectors.containsKey(key)) {
							int sourceindex = indices.get(vectors.get(key));
							double[] keeper = goodResponses.get(sourceindex);

							for (int j = 0; j < A; j++) {
								keeper[j] += responses[index][j];
							}
							goodResponses.set(sourceindex, keeper);
						} else {

							regressionPredictors.add(predictor);
							vectors.put(predictor.toString(), predictor);
							indices.put(predictor, maxInt);
							maxInt++;
							goodResponses.add(responses[index]);
						}
					}

				}
			}
		}

		// convert arraylist of responses to double array
		double[][] regressionResponses = new double[goodResponses.size()][K];

		for (int i = 0; i < goodResponses.size(); i++) {
			regressionPredictors.get(i);
			regressionResponses[i] = goodResponses.get(i);
		}

		featureMeans = new double[numFeatures];

		for (int i = 0; i < regressionPredictors.size(); i++) {
			DoubleSparseVector vec = regressionPredictors.get(i);
			for (Iterator<DoubleVectorEntry> iterator = vec.iterator(); iterator
					.hasNext();) {
				DoubleVectorEntry entry = iterator.next();
				int key = entry.getIndex();

				featureMeans[key] += 1.0 / (double) totalEntities; // not
																	// regressionPredictors.size()
																	// since
																	// these are
																	// only
																	// unique

			}
		}

		model = reg.regress(regressionPredictors, regressionResponses,
				numFeatures);
		model.printWeightsToFile(weights, reverseFeatureIds, genreIdToString);

		responses = new double[totalEntities][A];
	}

	/*
	 * Once the model is trained, set the document priors (which don't change
	 * between logreg runs).
	 */
	public void setDocumentPriors() {

		for (int i = 0; i < data.size(); i++) {
			Doc doc = data.get(i);

			for (Entity e : doc.entities.values()) {
				if (e.getNumEvents() > 0) {

					DoubleSparseVector moviePredictor = new DoubleSparseVector(
							numFeatures);
					DoubleSparseVector characterPredictor = new DoubleSparseVector(
							numFeatures);
					if (doc.genres != null) {
						for (String genre : doc.genres) {
							int genreID = featureIds.get(genre);
							moviePredictor.set(genreID, 1.0);
							characterPredictor.set(genreID, 1.0);
						}

						HashSet<Integer> characterFeatures = e
								.getCharacterFeatures();
						for (int feat : characterFeatures) {
							characterPredictor.set(feat, 1.0);
						}
					}

					double[] movieDistribution = model.predict(moviePredictor);
					double[] characterDistribution = model
							.predict(characterPredictor);

					e.prior = characterDistribution;
					doc.prior = movieDistribution;

				}
			}

		}
	}

	/*
	 * Conditioning on an entity's persona mode, generate posteriors over the
	 * typed topics given the observed words. (These are not used anywhere, but
	 * could be interesting to analyze how a character's actions deviates from
	 * the most likely persona).
	 */
	public void generateConditionalPosterior() {
		int samples = 100;

		for (int i = 0; i < data.size(); i++) {
			Doc doc = data.get(i);
			for (Entity e : doc.entities.values()) {

				double[] posterior = e.posterior;
				int mode = ArrayMath.argmax(posterior);
				double[] conditionalSamples = new double[K];

				for (EventArg arg : e.agentArgs) {
					double[] prob = new double[K];
					Arrays.fill(prob, 1.0);
					for (int j = 0; j < K; j++) {
						prob[j] *= unaryLFactor(arg, j, mode);
						prob[j] *= unaryEmissionFactor(arg, j);
					}

					double sum = ArrayMath.sum(prob);
					for (int j = 0; j < samples; j++) {
						int new_z = random.nextDiscrete(prob, sum);
						conditionalSamples[new_z]++;
					}
				}

				if (ArrayMath.sum(conditionalSamples) > 0) {
					ArrayMath.normalize(conditionalSamples);
				}
				e.conditionalAgentPosterior = conditionalSamples;
				double[] conditionalPatientSamples = new double[K];

				for (EventArg arg : e.patientArgs) {
					double[] prob = new double[K];
					Arrays.fill(prob, 1.0);
					for (int j = 0; j < K; j++) {
						prob[j] *= unaryLFactor(arg, j, mode);
						prob[j] *= unaryEmissionFactor(arg, j);
					}

					double sum = ArrayMath.sum(prob);
					for (int j = 0; j < samples; j++) {
						int new_z = random.nextDiscrete(prob, sum);
						conditionalPatientSamples[new_z]++;
					}
				}
				if (ArrayMath.sum(conditionalPatientSamples) > 0) {
					ArrayMath.normalize(conditionalPatientSamples);
				}
				e.conditionalPatientPosterior = conditionalPatientSamples;

				double[] conditionalModSamples = new double[K];

				for (EventArg arg : e.modifieeArgs) {
					double[] prob = new double[K];
					Arrays.fill(prob, 1.0);
					for (int j = 0; j < K; j++) {
						prob[j] *= unaryLFactor(arg, j, mode);
						prob[j] *= unaryEmissionFactor(arg, j);
					}

					double sum = ArrayMath.sum(prob);
					for (int j = 0; j < samples; j++) {
						int new_z = random.nextDiscrete(prob, sum);
						conditionalModSamples[new_z]++;
					}
				}
				if (ArrayMath.sum(conditionalModSamples) > 0) {
					ArrayMath.normalize(conditionalModSamples);
				}
				e.conditionalModPosterior = conditionalModSamples;

			}
		}

	}

	/*
	 * Generate an entity's posterior distribution over personas from saved
	 * samples; and then wipe those saved samples for the next iteration.
	 */
	public void generatePosteriors() {

		for (int i = 0; i < data.size(); i++) {
			Doc doc = data.get(i);
			for (Entity e : doc.entities.values()) {
				e.posterior = e.getSamplePosterior();
				e.posteriorSamples = new double[A];
			}

		}

	}

	public static class IterInfo {
		public double runningLL = 0;
		public int numClassChanges = 0;
		public int numFrameChanges = 0;
		public double frameRunningLL = 0;
		public double runningTotalLL = 0;

		public String toString() {
			return String.format("runningTotalLL %.3f numchanges %d",
					runningTotalLL, numClassChanges);
		}
	}

	// ///////////////////// z-level sampling inference stuff here
	// //////////////////////////

	public void incrementClassInfo(int delta, EventArg arg, int cur_z,
			int currentType) {
		incrementUnaries(delta, arg.entity, cur_z,
				arg.tuple.getCanonicalVerb(), currentType, arg.role);
	}

	public void incrementUnaries(int delta, Entity e, int currentZ, int wordId,
			int currentType, EventRole role) {

		phis[currentZ][wordId] += delta;
		phiTotals[currentZ] += delta;

		if (role == EventRole.AGENT) {
			LAgentTotal[currentType] += delta;
			LAgents[currentType][currentZ] += delta;

		} else if (role == EventRole.PATIENT) {
			LPatientTotal[currentType] += delta;
			LPatients[currentType][currentZ] += delta;
		} else if (role == EventRole.MODIFIEE) {
			LModifierTotal[currentType] += delta;
			LModifiers[currentType][currentZ] += delta;
		}
	}

	public double unaryLFactor(EventArg arg, int k, int type) {
		if (arg.role == EventRole.AGENT) {
			double norm = LAgentTotal[type] + (K * nuA);
			return (LAgents[type][k] + nuA) / norm;
		} else if (arg.role == EventRole.PATIENT) {
			double norm = LPatientTotal[type] + (K * nuP);
			return (LPatients[type][k] + nuP) / norm;
		} else if (arg.role == EventRole.MODIFIEE) {
			double norm = LModifierTotal[type] + (K * nuM);
			return (LModifiers[type][k] + nuM) / norm;
		}
		return 1;
	}

	/** p(w_unary | z=k) */
	public double unaryEmissionFactor(EventArg arg, int k) {
		int verb = arg.tuple.getCanonicalVerb();

		double norm = phiTotals[k] + (V * gamma);
		return (phis[k][verb] + gamma) / norm;
	}

	public void LDASamplePersonas(boolean first) {
		for (int i = 0; i < data.size(); i++) {

			Doc doc = data.get(i);

			for (Entity entity : doc.entities.values()) {
				if (!first) {
					doc.currentPersonaSamples[entity.currentType]--;
				}

				double[] regprobs = new double[A];
				Arrays.fill(regprobs, 1.0);

				for (int j = 0; j < A; j++) {
					regprobs[j] *= doc.currentPersonaSamples[j] + alpha;

					for (EventArg e : entity.agentArgs) {
						regprobs[j] *= unaryLFactor(e, e.currentSample, j);
					}
					for (EventArg e : entity.patientArgs) {
						regprobs[j] *= unaryLFactor(e, e.currentSample, j);
					}
					for (EventArg e : entity.modifieeArgs) {
						regprobs[j] *= unaryLFactor(e, e.currentSample, j);
					}
				}

				double sum = ArrayMath.sum(regprobs);
				int new_z = random.nextDiscrete(regprobs, sum);

				entity.lastType = entity.currentType;
				entity.currentType = new_z;
				doc.currentPersonaSamples[entity.currentType]++;
			}
		}

	}

	public double LogRegSample() {
		double ll = 0;
		for (int i = 0; i < data.size(); i++) {

			Doc doc = data.get(i);
			for (Entity entity : doc.entities.values()) {

				double[] characterPrior = entity.prior;

				double[] regprobs = new double[A];
				Arrays.fill(regprobs, 1.0);

				for (int j = 0; j < A; j++) {
					regprobs[j] *= characterPrior[j];
					for (EventArg e : entity.agentArgs) {
						regprobs[j] *= unaryLFactor(e, e.currentSample, j);
					}
					for (EventArg e : entity.patientArgs) {
						regprobs[j] *= unaryLFactor(e, e.currentSample, j);
					}
					for (EventArg e : entity.modifieeArgs) {
						regprobs[j] *= unaryLFactor(e, e.currentSample, j);
					}
				}

				double sum = ArrayMath.sum(regprobs);
				int new_z = random.nextDiscrete(regprobs, sum);
				ll += Math.log(characterPrior[new_z]);
				entity.lastType = entity.currentType;
				entity.currentType = new_z;
			}
		}

		return ll;
	}

	public IterInfo sample(boolean first, boolean doCompleteLL) {

		IterInfo info = new IterInfo();
		if (personaRegression) {
			info.runningTotalLL += LogRegSample();
		} else {
			LDASamplePersonas(first);
		}

		if (doCompleteLL) {
			info.runningTotalLL += HyperparameterOptimization.totalLL(this);
		}

		for (int i = 0; i < data.size(); i++) {

			Doc doc = data.get(i);

			for (EventTuple t : doc.eventTuples) {

				for (EventArg arg : t.arguments.values()) {

					int old_z = arg.currentSample;
					if (!first) {
						incrementClassInfo(-1, arg, old_z, arg.entity.lastType);
					}

					double[] regprobs = new double[K];
					Arrays.fill(regprobs, 1.0);

					for (int k = 0; k < K; k++) {

						regprobs[k] *= unaryLFactor(arg, k,
								arg.entity.currentType);

						regprobs[k] *= unaryEmissionFactor(arg, k);

					}

					double sum = ArrayMath.sum(regprobs);
					if (sum <= 0) {
						System.out.println("0 sum" + Arrays.toString(regprobs));
						System.exit(1);
					}
					int new_z = random.nextDiscrete(regprobs, sum);

					incrementClassInfo(+1, arg, new_z, arg.entity.currentType);
					arg.currentSample = new_z;

					assert regprobs[new_z] / sum >= 0;
					info.numClassChanges += old_z != new_z ? 1 : 0;
				}
			}
		}
		return info;

	}

	public static void main(String[] args) {

		String propertyFile = args[0];
		
		PersonaModel gibbs = new PersonaModel();

		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(propertyFile));

			int K = Integer.valueOf(properties.getProperty("K"));
			int V = Integer.valueOf(properties.getProperty("V"));
			int A = Integer.valueOf(properties.getProperty("A"));
			double alpha = Double.valueOf(properties.getProperty("alpha"));
			double gamma = Double.valueOf(properties.getProperty("gamma"));
			double L2 = Double.valueOf(properties.getProperty("L2"));

			int maxIterations = Integer.valueOf(properties
					.getProperty("maxIterations")) + 1;

			String dataFile = properties.getProperty("data");
			String movieMetadata = properties.getProperty("movieMetadata");
			String characterMetadata = properties
					.getProperty("characterMetadata");
			String characterPosteriorFile = properties
					.getProperty("characterPosteriorFile");
			boolean personaRegression = Boolean.valueOf(properties
					.getProperty("runPersonaRegressionModel"));
			String outPhiWeights = properties.getProperty("outPhiWeights");
			String featureMeans = properties.getProperty("featureMeans");
			String personaFile = properties.getProperty("personaFile");
			String characterConditionalPosteriorFile = properties
					.getProperty("characterConditionalPosteriorFile");

			String featureFile = properties.getProperty("featureFile");

			String finalLAgentsFile = properties
					.getProperty("finalLAgentsFile");
			String finalLPatientsFile = properties
					.getProperty("finalLPatientsFile");
			String finalLModFile = properties.getProperty("finalLModFile");

			String weights = properties.getProperty("weights");

			gibbs.weights = weights;

			gibbs.K = K;
			gibbs.V = V;
			gibbs.A = A;
			gibbs.alpha = alpha;
			gibbs.personaRegression = personaRegression;
			gibbs.L2 = L2;

			gibbs.gamma = gamma;

			int numWordsToPrint = 20;

			DataReader.read(characterMetadata, movieMetadata, dataFile, gibbs);

			gibbs.initialize();

			// START TRAINING
			gibbs.sample(true, true);
			System.out.println("Iter 0: runningLL 0 numchanges 0");
			HyperparameterOptimization.resampleConcs(gibbs);

			int burnin = maxIterations;

			int doCompleteLLEvery = 100;
			int regressEvery = 100; // test=100; real = 1000
			for (int i = 1; i <= burnin; i++) {

				if (i > 0 && ((i < 500 && i % 20 == 0) || (i % 100 == 0))) {
					HyperparameterOptimization.resampleConcs(gibbs);
				}

				IterInfo info = gibbs.sample(false, i % doCompleteLLEvery == 0);

				if (i % doCompleteLLEvery == 0) {
					info.runningTotalLL += HyperparameterOptimization
							.totalLL(gibbs);
					System.out
							.println(String.format("Iter %3d: %s\n", i, info));
				}

				/*
				 * After 50 iterations, save every sample
				 */
				if (i % regressEvery > 50) {
					gibbs.saveSamples();
				}
				/*
				 * Train logreg every 100 iterations and update document priors.
				 */
				if (i % regressEvery == 0 && i > 0) {
					if (personaRegression) {
						gibbs.regress();
						gibbs.setDocumentPriors();
						PrintUtil.printFeatureMeans(featureMeans, gibbs);
					}

					// wipes current samples
					gibbs.generatePosteriors();
					gibbs.generateConditionalPosterior();

					PrintUtil.printPosteriorsToFile(characterPosteriorFile,
							characterConditionalPosteriorFile, featureFile,
							gibbs);

					System.out.println(PrintUtil.printMeanTop(
							gibbs.phis, gibbs.reverseVocab,
							"verbs ratiorank", numWordsToPrint));
					System.out.println(PrintUtil.printSimpleTop(
							gibbs.phis, gibbs.reverseVocab,
							"verbs freqrank2", numWordsToPrint));

				}

				if (i % 100 == 0)
					System.out.println(String.format("\t wordLL %.3f\n",
							HyperparameterOptimization.wordLL(gibbs.gamma,
									gibbs)));

			}

			// take 100 samples
			for (int i = 0; i < 1000; i++) {
				if (i % 10 == 0) {
					System.out.print(i + " ");
				}

				gibbs.sample(false, false);
				if (i % 10 == 0) {
					// save current a for all entities and z for all entityargs
					gibbs.saveFinalSamples();
				}
			}
			System.out.println();

			/*
			 * Write character posteriors, conditional posteriors, and
			 * class/feature associations to file
			 */
			PrintUtil.printFinalPosteriorsToFile(characterPosteriorFile,
					characterConditionalPosteriorFile, featureFile, gibbs);

			/*
			 * Write personas to file
			 */
			PrintUtil.printFinalPersonas(personaFile, gibbs);

			System.out.println(PrintUtil.printMeanTop(gibbs.finalPhis,
					gibbs.reverseVocab, "final ratiorank", numWordsToPrint));
			System.out.println(PrintUtil.printSimpleTop(gibbs.finalPhis,
					gibbs.reverseVocab, "final freqrank", numWordsToPrint));

			String[] names = new String[K];
			for (int i = 0; i < K; i++) {
				names[i] = String.valueOf(i);
			}

			/**
			 * Write distributions to file
			 */
			PrintUtil.printWeights(gibbs.finalPhis, gibbs.reverseVocab,
					outPhiWeights);
			PrintUtil.printWeights(gibbs.finalLAgents, names, finalLAgentsFile);
			PrintUtil.printWeights(gibbs.finalLPatients, names,
					finalLPatientsFile);
			PrintUtil.printWeights(gibbs.finalLMod, names, finalLModFile);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
