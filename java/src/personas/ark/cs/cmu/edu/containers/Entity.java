package personas.ark.cs.cmu.edu.containers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import com.google.common.collect.Lists;

import edu.stanford.nlp.math.ArrayMath;


public class Entity {

	public String id; 		// E1
	public String name;	// Batman
	public String fullname;	// Batman, who ...
	public ArrayList<EventArg> agentArgs;
	public ArrayList<EventArg> patientArgs;
	public ArrayList<EventArg> modifieeArgs;
	
	public int canonicalIndex;
	
	public int lastType;
	public int currentType;
	
	public int A;
	
	public double[] prior;
	public double[] posterior;
	
	public double[] posteriorSamples; 
	public double[] conditionalAgentPosterior;
	public double[] conditionalPatientPosterior;
	public double[] conditionalModPosterior;
			
	public HashSet<Integer> characterFeatures;

	public double[] finalSamples;
	public  int finalSampleSum=0;

	public void saveSample(int i) {
		posteriorSamples[i]++;
	}
	public void saveFinalSample(int i) {
		finalSamples[i]++;
		finalSampleSum++;
	}
	public double[] getSamplePosterior() {
		double[] post=new double[A];
		double sum=ArrayMath.sum(posteriorSamples);
		for (int i=0; i<A; i++) {
			post[i]=posteriorSamples[i]/sum;
		}
		
		return post;
	}
	
	public int getNumEvents() {
		return agentArgs.size() + patientArgs.size() + modifieeArgs.size();
	}
	
	public String getEventString() {
		StringBuffer buffer=new StringBuffer();
		buffer.append("A: ");
		for (EventArg e : agentArgs) {
			buffer.append(e.getVerb() + " ");
		}
		buffer.append("P: ");
		for (EventArg e : patientArgs) {
			buffer.append(e.getVerb() + " ");
		}
		return buffer.toString();
	}
	public Entity(String id, int A) {
		this.id=id;
		agentArgs = Lists.newArrayList();
		patientArgs=Lists.newArrayList();
		modifieeArgs=Lists.newArrayList();

		this.A=A;
		
		posteriorSamples=new double[A];
		finalSamples=new double[A];
		
		currentType=(int)(Math.random()*A);
		lastType=currentType;
		
		prior=new double[A];
		Arrays.fill(prior, 1./A);
		
	}
	
	public int getMax() {
		if (finalSampleSum > 0) {
			return ArrayMath.argmax(finalSamples);
		}
		return ArrayMath.argmax(posterior);
	}
	
	public HashSet<Integer> getCharacterFeatures() {
		if (characterFeatures == null) return new HashSet<Integer>();
		return characterFeatures;
	}
	
	public String toString(){
		return id + "\t" + name + "\t" + fullname;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getFullname() {
		return fullname;
	}
	public void setFullname(String fullname) {
		this.fullname = fullname;
	}
	
	
}
