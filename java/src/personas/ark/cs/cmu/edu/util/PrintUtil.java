package personas.ark.cs.cmu.edu.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import personas.ark.cs.cmu.edu.PersonaModel;
import personas.ark.cs.cmu.edu.containers.Doc;
import personas.ark.cs.cmu.edu.containers.Entity;
import personas.ark.cs.cmu.edu.containers.EventArg;
import edu.stanford.nlp.math.ArrayMath;

/*
 * Utils for printing per-topic term probabilities. All methods require:
 * 
 * double[][] array : K topics x V vocab size array of counts
 * String[] reverseVocab : V vocab size array of term associated with each index
 * String type : phrase to be printed along with each K
 *  
 */

public class PrintUtil {

	public static void printWeights(double[][] array, String[] reverseVocab,
			String outFile) {
		int K = array.length;
		int V = array[0].length;

		OutputStreamWriter out;
		try {
			out = new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8");

			for (int j = 0; j < V; j++) {
				out.write(reverseVocab[j]);
				if (j < V - 1) {
					out.write(" ");
				}
			}
			out.write("\n");

			for (int i = 0; i < K; i++) {
				for (int j = 0; j < V; j++) {
					out.write(String.format("%.3f", array[i][j]));
					if (j < V - 1) {
						out.write(" ");
					} 
				}
				out.write("\n");
			}

			out.close();

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Sort by ratio to background distribution over all topics.
	 */
	public static String printMeanTop(double[][] array, String[] reverseVocab,
			String type, int numToPrint) {

		int K = array.length;
		int V = array[0].length;

		StringBuffer buffer = new StringBuffer();

		double[] mean = new double[V];
		for (int i = 0; i < K; i++) {
			double total = ArrayMath.sum(array[i]);
			for (int v = 0; v < V; v++) {
				mean[v] += (array[i][v] / total / K);
			}
		}
		for (int i = 0; i < K; i++) {
			buffer.append(type + ": " + i + "\t");
			HashMap<Integer, Double> thishash = new HashMap<Integer, Double>();
			double total = ArrayMath.sum(array[i]);
			for (int v = 0; v < V; v++) {
				if (mean[v] > 0) {
					double ratio = array[i][v] / total / mean[v];
					thishash.put(v, ratio);
				}
			}
			ArrayList<Object> sorted = Util.sortHashMapByValue(thishash);
			for (int k = 0; k < numToPrint & k < sorted.size(); k++) {
				Integer ko = (Integer) sorted.get(k);
				if (ko < reverseVocab.length) {
					String word = reverseVocab[ko];
					buffer.append(word + " ");
				}
			}
			buffer.append("\n");
		}
		return buffer.toString();
	}

	/*
	 * Just print the most frequent terms
	 */
	public static String printSimpleTop(double[][] array,
			String[] reverseVocab, String type, int numToPrint) {

		int K = array.length;
		int V = array[0].length;

		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < K; i++) {

			buffer.append(type + ": " + i + "\t");

			HashMap<Integer, Double> hash = new HashMap<Integer, Double>();

			for (int j = 0; j < V; j++) {
				hash.put(j, array[i][j]);
			}

			ArrayList<Object> sorted = Util.sortHashMapByValue(hash);
			for (int k = 0; k < numToPrint; k++) {
				Integer ko = (Integer) sorted.get(k);
				if (ko < reverseVocab.length && hash.get(ko) > 0) {
					String word = reverseVocab[ko];
					buffer.append(word + " ");
				}
			}
			buffer.append("\n");

		}
		return buffer.toString();
	}

	public static void printFeatureMeans(String weightFile, PersonaModel model) {

		try {
			OutputStreamWriter out = new OutputStreamWriter(
					new FileOutputStream(weightFile), "UTF-8");

			for (int i = 0; i < model.numFeatures; i++) {
				double mean = model.featureMeans[i];
				String value = model.reverseFeatureIds[i];
				if (model.genreIdToString.containsKey(value)) {
					value = model.genreIdToString.get(model.reverseFeatureIds[i]);
				}
				out.write(String.format("%s\t%s\n", mean, value));
			}

			out.close();

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void printFinalPosteriorsToFile(
			String characterPosteriorFile, String characterConditionalFile,
			String featureFile, PersonaModel model) {

		try {
			OutputStreamWriter out = new OutputStreamWriter(
					new FileOutputStream(characterPosteriorFile), "UTF-8");
			OutputStreamWriter outCond = new OutputStreamWriter(
					new FileOutputStream(characterConditionalFile), "UTF-8");
			DecimalFormat df = new DecimalFormat("0.00000");

			double[][] featureCounts = new double[model.A][model.numFeatures];

			for (int j = 0; j < model.data.size(); j++) {
				Doc doc = model.data.get(j);

				for (Entity e : doc.entities.values()) {

					double[] finalPosterior = e.finalSamples;
					ArrayMath.normalize(finalPosterior);

					HashSet<Integer> fs = new HashSet<Integer>();

					if (doc.genres != null) {
						for (String genre : doc.genres) {
							int genreID = model.featureIds.get(genre);
							fs.add(genreID);
						}
					}

					HashSet<Integer> characterFeatures = e
							.getCharacterFeatures();
					for (int feat : characterFeatures) {
						fs.add(feat);
					}

					// increment each feature value by its expectation under the
					// posterior for A.
					for (int i = 0; i < model.A; i++) {
						for (int f : fs) {
							featureCounts[i][f] += finalPosterior[i];
						}
					}

					String title = "???";
					if (doc.title != null) {
						title = doc.title;
					}
					String charName = "";
					String fullName = "";

					if (e.name != null) {
						charName = e.name;
					}
					if (e.fullname != null) {
						fullName = e.fullname;
					}

					out.write(e.id + "\t" + doc.id + "\t" + title + "\t"
							+ charName + "\t" + fullName + "\t"
							+ e.getNumEvents() + "\t"
							+ ArrayMath.argmax(finalPosterior) + "\t");
					for (int i = 0; i < finalPosterior.length; i++) {
						out.write(df.format(finalPosterior[i]));
						if (i < finalPosterior.length - 1) {
							out.write(" ");
						}
					}
					out.write("\n");

					int[] agentSamples = new int[model.K];
					int[] patientSamples = new int[model.K];
					int[] modSamples = new int[model.K];
					for (EventArg arg : e.agentArgs) {
						int[] samples = arg.getFinalSamples(model.K);
						for (int i = 0; i < model.K; i++) {
							agentSamples[i] += samples[i];
						}
					}
					for (EventArg arg : e.patientArgs) {
						int[] samples = arg.getFinalSamples(model.K);
						for (int i = 0; i < model.K; i++) {
							patientSamples[i] += samples[i];
						}
					}
					for (EventArg arg : e.modifieeArgs) {
						int[] samples = arg.getFinalSamples(model.K);
						for (int i = 0; i < model.K; i++) {
							modSamples[i] += samples[i];
						}
					}

					outCond.write("agent" + "\t" + e.id + "\t" + doc.id + "\t"
							+ title + "\t" + charName + "\t" + fullName + "\t"
							+ e.getNumEvents() + "\t"
							+ ArrayMath.argmax(agentSamples) + "\t");
					for (int i = 0; i < agentSamples.length; i++) {
						outCond.write(df.format(agentSamples[i]));
						if (i < agentSamples.length - 1) {
							outCond.write(" ");
						}
					}
					outCond.write("\n");

					outCond.write("patient" + "\t" + e.id + "\t" + doc.id
							+ "\t" + title + "\t" + charName + "\t" + fullName
							+ "\t" + e.getNumEvents() + "\t"
							+ ArrayMath.argmax(patientSamples) + "\t");
					for (int i = 0; i < patientSamples.length; i++) {
						outCond.write(df.format(patientSamples[i]));
						if (i < patientSamples.length - 1) {
							outCond.write(" ");
						}
					}
					outCond.write("\n");

					outCond.write("mod" + "\t" + e.id + "\t" + doc.id + "\t"
							+ title + "\t" + charName + "\t" + fullName + "\t"
							+ e.getNumEvents() + "\t"
							+ ArrayMath.argmax(modSamples) + "\t");
					for (int i = 0; i < modSamples.length; i++) {
						outCond.write(df.format(modSamples[i]));
						if (i < modSamples.length - 1) {
							outCond.write(" ");
						}
					}
					outCond.write("\n");

				}

			}

			out.close();
			outCond.close();

			OutputStreamWriter outFeat = new OutputStreamWriter(
					new FileOutputStream(featureFile), "UTF-8");

			outFeat.write("Labels\t");
			for (int i = 0; i < model.numFeatures; i++) {
				outFeat.write(model.reverseFeatureIds[i]);
				if (i < model.numFeatures - 1) {
					outFeat.write(" ");
				}
			}
			outFeat.write("\n");
			for (int i = 0; i < model.A; i++) {
				outFeat.write(i + "\t");
				for (int j = 0; j < featureCounts[i].length; j++) {
					outFeat.write(String.valueOf(featureCounts[i][j]));
					if (j < featureCounts[i].length - 1) {
						outFeat.write(" ");
					}
				}
				outFeat.write("\n");
			}
			outFeat.close();

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Print character posteriors to file
	 */
	public static void printPosteriorsToFile(String characterPosteriorFile,
			String characterConditionalFile, String featureFile,
			PersonaModel model) {

		try {
			OutputStreamWriter out = new OutputStreamWriter(
					new FileOutputStream(characterPosteriorFile), "UTF-8");
			OutputStreamWriter outCond = new OutputStreamWriter(
					new FileOutputStream(characterConditionalFile), "UTF-8");
			DecimalFormat df = new DecimalFormat("0.00000");

			double[][] featureCounts = new double[model.A][model.numFeatures];
			for (int j = 0; j < model.data.size(); j++) {
				Doc doc = model.data.get(j);

				for (Entity e : doc.entities.values()) {

					double[] posterior = e.posterior;

					HashSet<Integer> fs = new HashSet<Integer>();

					if (doc.genres != null) {
						for (String genre : doc.genres) {
							int genreID = model.featureIds.get(genre);
							fs.add(genreID);
						}
					}

					HashSet<Integer> characterFeatures = e
							.getCharacterFeatures();
					for (int feat : characterFeatures) {
						fs.add(feat);
					}

					// increment each feature value by its expectation under the
					// posterior for A.
					for (int i = 0; i < model.A; i++) {
						for (int f : fs) {
							featureCounts[i][f] += posterior[i];
						}
					}

					/*
					 * Only print out posteriors for the top characters
					 */
					String title = "???";
					if (doc.title != null) {
						title = doc.title;
					}
					String charName = "";
					String fullName = "";

					if (e.name != null) {
						charName = e.name;
					}
					if (e.fullname != null) {
						fullName = e.fullname;
					}

					out.write(e.id + "\t" + title + "\t" + charName + "\t"
							+ fullName + "\t" + e.getNumEvents() + "\t"
							+ ArrayMath.argmax(posterior) + "\t");
					for (int i = 0; i < posterior.length; i++) {
						out.write(df.format(posterior[i]));
						if (i < posterior.length - 1) {
							out.write(" ");
						}
					}
					out.write("\n");

					posterior = e.conditionalAgentPosterior;
					outCond.write("agent" + "\t" + e.id + "\t" + title + "\t"
							+ charName + "\t" + fullName + "\t"
							+ e.getNumEvents() + "\t"
							+ ArrayMath.argmax(posterior) + "\t");
					for (int i = 0; i < posterior.length; i++) {
						outCond.write(df.format(posterior[i]));
						if (i < posterior.length - 1) {
							outCond.write(" ");
						}
					}
					outCond.write("\n");

					posterior = e.conditionalPatientPosterior;
					outCond.write("patient" + "\t" + e.id + "\t" + title + "\t"
							+ charName + "\t" + fullName + "\t"
							+ e.getNumEvents() + "\t"
							+ ArrayMath.argmax(posterior) + "\t");
					for (int i = 0; i < posterior.length; i++) {
						outCond.write(df.format(posterior[i]));
						if (i < posterior.length - 1) {
							outCond.write(" ");
						}
					}
					outCond.write("\n");

					posterior = e.conditionalModPosterior;
					outCond.write("mod" + "\t" + e.id + "\t" + title + "\t"
							+ charName + "\t" + fullName + "\t"
							+ e.getNumEvents() + "\t"
							+ ArrayMath.argmax(posterior) + "\t");
					for (int i = 0; i < posterior.length; i++) {
						outCond.write(df.format(posterior[i]));
						if (i < posterior.length - 1) {
							outCond.write(" ");
						}
					}
					outCond.write("\n");

				}

			}

			out.close();
			outCond.close();

			OutputStreamWriter outFeat = new OutputStreamWriter(
					new FileOutputStream(featureFile), "UTF-8");

			outFeat.write("Labels\t");
			for (int i = 0; i < model.numFeatures; i++) {
				outFeat.write(model.reverseFeatureIds[i]);
				if (i < model.numFeatures - 1) {
					outFeat.write(" ");
				}
			}
			outFeat.write("\n");
			for (int i = 0; i < model.A; i++) {
				outFeat.write(i + "\t");
				for (int j = 0; j < featureCounts[i].length; j++) {
					outFeat.write(String.valueOf(featureCounts[i][j]));
					if (j < featureCounts[i].length - 1) {
						outFeat.write(" ");
					}
				}
				outFeat.write("\n");
			}
			outFeat.close();

			String[] tmp = new String[model.reverseFeatureIds.length];
			for (int i = 0; i < tmp.length; i++) {
				tmp[i] = model.reverseFeatureIds[i];
				if (model.genreIdToString.containsKey(tmp[i])) {
					tmp[i] = model.genreIdToString.get(tmp[i]);
				}
			}

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void printFinalPersonas(String outfile,
			PersonaModel model) {
		try {
			OutputStreamWriter out = new OutputStreamWriter(
					new FileOutputStream(outfile), "UTF-8");

			for (int i = 0; i < model.A; i++) {
				out.write(i + "\t");
				for (int j = 0; j < model.K; j++) {
					double val = (double) model.finalLAgents[i][j];
					out.write(String.valueOf(val));
					out.write(" ");
				}
				for (int j = 0; j < model.K; j++) {
					double val = (double) model.finalLPatients[i][j];
					out.write(String.valueOf(val));
					out.write(" ");
				}

				for (int j = 0; j < model.K; j++) {
					double val = (double) model.finalLMod[i][j];
					out.write(String.valueOf(val));
					if (j < model.K - 1) {
						out.write(" ");
					}
				}
				out.write("\n");
			}
			out.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
