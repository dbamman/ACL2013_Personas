package personas.ark.cs.cmu.edu;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import personas.ark.cs.cmu.edu.util.Util;
import edu.stanford.math.primitivelib.autogen.matrix.DoubleSparseVector;
import edu.stanford.math.primitivelib.autogen.matrix.DoubleVectorEntry;
import edu.stanford.nlp.optimization.DiffFunction;

/*
 * Multiclass logistic regression with sparse feature vectors.  Inputs are:
 * - sparse feature vectors
 * - counts for each of the K classes associated with each feature vector
 * - number of features in the predictors
 * - L2 regularization parameter
 * - (L1 can also be set)
 * 
 * Returns a trained model that can make predictions on new sparse vectors, inspect weights etc.
 * 
 */

public class SparseMulticlassLogisticRegression {
	
	// number of latent classes
	int K;
	int length;
	// number of training examples
	int numInstances;
	// number of features
	int numFeatures;
	
	double lambda;
	double threshold=1e-5;
	int memory=15;
	
	double L2Lambda=0;
	double L1Lambda=0;
	
	ArrayList<DoubleSparseVector> predictors;
	double[][] responses;
		
	double[][] current;
	
	double[] normalizers;
	double[] samples;
	
	DecimalFormat df=new DecimalFormat("0.000");
	
	public SparseMulticlassLogisticRegression(double initialL2Lambda) {
		L2Lambda=initialL2Lambda;
	}
	
	public void setBiasParameters(HashSet<Integer> biases) {
		OWLQN.biasParameters=biases;		
	}

	public TrainedModel getDefault(int K, int numFeatures) {
		return new TrainedModel(K, numFeatures);
	}

	public TrainedModel regress(ArrayList<DoubleSparseVector> predictors, double[][] responses, int numFeatures) {
		LogisticDiffFunction diff=new LogisticDiffFunction();
		
		// number of classes
		K=responses[0].length;
	
		// total samples for each feature vector type
		samples=new double[responses.length];
		for (int i=0; i<responses.length; i++) {
			for (int j=0; j<K; j++) {
				samples[i]+=responses[i][j];
			}
		}
		
		// number of training examples
		numInstances=predictors.size();
		
		// number of weights
		length=K*numFeatures;
		// number of features
		this.numFeatures=numFeatures;
	
		// features + bias terms
		double[] initial=new double[length+K];

		// Choose between Stanford QNMinimizer and OWL-QN
		OWLQN qn = new OWLQN();
		//qn.setQuiet(true);
		
		this.predictors=predictors;
		this.responses=responses;
		
		normalizers=new double[numInstances];
		
		// Don't regularize bias terms
		HashSet<Integer> biases=new HashSet<Integer>();
		for (int i=0; i<K; i++) {
			biases.add(length+i);
		}
		setBiasParameters(biases);
		
		System.out.println("SparseMulticlassLogisticRegression with " + numInstances + " data points and " + (length+K) + " " + K + " " + numFeatures + " features");
	
		current=new double[numInstances][K];

		double[] gradient=qn.minimize(diff,initial,L1Lambda,threshold,memory);
		
		TrainedModel model=new TrainedModel(gradient, K, numFeatures);

		System.out.println("OptLL: " + String.format("%.8f", diff.valueAt(gradient)));
		
		return model;
	}
	
	public class TrainedModel {
		double[][] classweights;  // K x numFeatures
		double[] biases;
		int K;
		
		// default model
		public TrainedModel(int K, int numFeatures) {
			classweights=new double[K][numFeatures];
			for (int i=0; i<K; i++) {
				Arrays.fill(classweights[i], 1.0);
			}
			biases=new double[K];
			this.K=K;
			
		}
		
		public TrainedModel(double[] weights, int K, int numFeatures) {
			classweights=new double[K][numFeatures];
			biases=new double[K];
			for (int j=0; j<K; j++) {
				for (int n=0; n<numFeatures; n++) {
					int index=j*numFeatures + n;
					classweights[j][n]=weights[index];
				}
				biases[j]=weights[K*numFeatures+j];
			}
			this.K=K;
		}
		
		public double[] getFeatureWeights(int featureId) {
			double[] weights=new double[K];
			for (int i=0; i<K; i++) {
				weights[i]=classweights[i][featureId];
			}
			return weights;
		}
		
		public ArrayList<Integer> getNBiggest(int n, int k, double[] featureMeans) {
			HashMap<Integer, Double> weights=new HashMap<Integer, Double>();
			for (int i=0; i<numFeatures; i++) {
				double value=featureMeans[i]*classweights[k][i];
				weights.put(i, value);
			}
			ArrayList<Object> sorted=Util.sortHashMapByValue(weights);
			ArrayList<Integer> toReturn=new ArrayList<Integer>();
			for (int i=0; i<n; i++) {
				Integer ko=(Integer)sorted.get(i);
				toReturn.add(ko);
			
			}
			return toReturn;
		}
		public double getWeight(int k, int i, double[] featureMeans) {
			if (k < K-1) {
				return classweights[k][i]*featureMeans[i]; 
			}
			return classweights[k][i];
		}
		public void printWeightsToFile(String weightFile, String[] reverseFeatureIds, HashMap<String, String> genreIdToString) {
			
			try {
				OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(weightFile),"UTF-8");
				
				for (int i=0; i<K; i++) {
					for (int j=0; j<numFeatures; j++) {
						String value=reverseFeatureIds[j];
						if (genreIdToString.containsKey(value)) {
							value=genreIdToString.get(reverseFeatureIds[j]);
						}
						double weight=classweights[i][j];
						out.write(String.format("%s\t%s\t%s\n", weight, i, value));
					}
					double weight=biases[i];
					out.write(String.format("%s\t%s\t%s\n", weight, i, "BIAS"));
				}
				
				
				out.close();
				
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
		
		public double[] predict(DoubleSparseVector vec) {
			double norm=0;
			for (int j=0; j<K; j++) {
				double dot=0;
				
				for (Iterator<DoubleVectorEntry> iterator = vec.iterator(); iterator.hasNext(); ) {
					DoubleVectorEntry entry=iterator.next();
					int key=entry.getIndex();
					double value = entry.getValue();

					dot += value*(classweights[j][key]); 

				}

				// bias term
				dot+=(biases[j]);
				norm+=Math.exp(dot);

			}
						
			double[] distribution=new double[K];
			
			for (int j=0; j<(K); j++) {
				double dot=0;
								
				for (Iterator<DoubleVectorEntry> iterator = vec.iterator(); iterator.hasNext(); ) {
					DoubleVectorEntry entry=iterator.next();
					int key=entry.getIndex();
					double value = entry.getValue();

					dot += value*(classweights[j][key]);

				}

				// bias term
				dot+=(biases[j]);
				
				double pred=Math.exp(dot)/norm;				
				distribution[j]=pred;
			}
			return distribution;
		}
	}
	
	/*
	 * Set inner product between weights and feature vector for each class given an input
	 * and save the normalizer over all of them.
	 */
	public void setCurrent(double[] weights) {
		
		for (int i=0; i<numInstances; i++) {
			
			DoubleSparseVector vec=predictors.get(i);
			
			double normalizer=0;
			for (int j=0; j<K; j++) { 
				double dot=0;
				
				for (Iterator<DoubleVectorEntry> iterator = vec.iterator(); iterator.hasNext(); ) {
					DoubleVectorEntry entry=iterator.next();
					int key=entry.getIndex();
					double value = entry.getValue();
					
					int index=j*numFeatures + key;
					dot += value*(weights[index]);

				}

				// bias term
				dot+=(weights[length+j]);

				current[i][j]=dot;
				normalizer+=Math.exp(dot);
			}
			normalizers[i]=normalizer;
		}
	}
	
	public class LogisticDiffFunction implements DiffFunction {

		public double[] derivativeAt(double[] arg0) {
			
			setCurrent(arg0);
						
			int arglength=arg0.length;
			double[] gradient=new double[arglength];
						
			for (int i=0; i<numInstances; i++) {
				
				DoubleSparseVector vec=predictors.get(i);

				for (int j=0; j<K-1; j++) {
					double dot=current[i][j];
					
					double cval=Math.exp(dot)/normalizers[i];
					
					double count=responses[i][j];
					double diff=1-cval;
					
					double negCount=samples[i]-count;
					double negdiff=0-cval;
					
					for (Iterator<DoubleVectorEntry> iterator = vec.iterator(); iterator.hasNext(); ) {
						DoubleVectorEntry entry=iterator.next();
						int key=entry.getIndex();
						double value = entry.getValue();
						
						int index=j*numFeatures + key;
						if (count > 0) {
							gradient[index]-=(count*diff*value);
						}
						if (negCount > 0) {
							gradient[index]-=(negCount*negdiff*value);
						}
						
					}

					// bias term
					if (count > 0) {
						gradient[length+j] -= (count*diff);
					}
					if (negCount > 0) {
						gradient[length+j] -= (negCount*negdiff);
					}
					
				}		
			}	
			
			if (L2Lambda > 0) {
				for (int i=0; i<gradient.length-K; i++) {
					gradient[i]+=L2Lambda*(arg0[i]);
				}					
			}

			return gradient;
		}

		public int domainDimension() {
			return length;
		}

		public double valueAt(double[] arg0) {
			double loss=0;
			setCurrent(arg0);
			
			for (int i=0; i<numInstances; i++) {
				
				for (int j=0; j<K; j++) {

					double count=responses[i][j];

					double dot=current[i][j];
					double cval=Math.exp(dot)/normalizers[i];
					if (count > 0) {						
						loss-=(count*Math.log(cval));
					}
				}
			}
			
			
			if (L2Lambda > 0) {
				for (int i=0; i<arg0.length-K; i++) {
					loss+=( 1/2.0 * L2Lambda*(arg0[i])*(arg0[i]));
				}
			}
			
			return loss;
		}

	}
}
