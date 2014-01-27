package personas.ark.cs.cmu.edu.containers;

import java.util.Map;

import com.google.common.collect.Maps;

public class EventTuple {

	private String tupleID;
	public int canonicalVerb = -1;
	private String verbString;
	public Map<EventRole, EventArg> arguments;

	public int currentFrame = -1;

	public EventTuple(String tupleID, String verbString) {
		this.tupleID = tupleID;
		this.verbString = verbString;
		this.arguments = Maps.newHashMap();
	}

	public void numberize(Map<String, Integer> verbVocab) {
		assert verbVocab.containsKey(this.verbString);
		this.canonicalVerb = verbVocab.get(this.verbString);
	}

	public String toString() {
		String s = "";
		s += String.format("%s(%s)[\t", verbString, tupleID);
		for (EventRole r : EventRole.values()) {
			if (arguments.containsKey(r)) {
				s += String.format("\t%s=%s", r, getArg(r));
			}
		}
		s += "\t]";
		return s;
	}

	public Entity getArg(EventRole role) {
		if (arguments.containsKey(role))
			return arguments.get(role).entity;
		return null;
	}

	public void setCanonicalVerb(int canonicalVerb) {
		this.canonicalVerb = canonicalVerb;
	}

	public int getCanonicalVerb() {
		return canonicalVerb;
	}

	public String getTupleID() {
		return tupleID;
	}

	public void setTupleID(String tupleID) {
		if (this.tupleID != null)
			assert this.tupleID.equals(tupleID) : "bug in loading code";
		this.tupleID = tupleID;
	}

	public void setArg(EventArg arg) {
		arguments.put(arg.role, arg);
	}

	public int numArgs() {
		return arguments.size();
	}

	public void setVerbString(String verbString) {
		this.verbString = verbString;
	}

	public String getVerbString() {
		return verbString;
	}
}
