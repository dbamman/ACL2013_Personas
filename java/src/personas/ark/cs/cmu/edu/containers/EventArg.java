package personas.ark.cs.cmu.edu.containers;

import java.util.HashMap;

public class EventArg {
	// role,entity  are the core data here
	public EventRole role;
	public Entity entity;
	
	public int currentSample = -1;
	public HashMap<Integer, Integer> finalSamples;
	
	public EventTuple tuple; // the enclosing tuple, included only for convenience for display routines
	public String getVerb() { return tuple.getVerbString(); }
	
	public EventArg(EventRole role, Entity entity) {
		this.role = role;
		this.entity= entity;
		finalSamples=new HashMap<Integer, Integer>();
	}
	public void saveFinalSample() {
		int count=0;
		if (finalSamples.containsKey(currentSample)) {
			count=finalSamples.get(currentSample);
		}
		count++;
		finalSamples.put(currentSample, count);
	}
	
	public int[] getFinalSamples(int K) {
		int[] samples=new int[K];
		for (int key : finalSamples.keySet()) {
			samples[key]=finalSamples.get(key);
		}
		return samples;
	}

}
