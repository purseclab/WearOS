package soot.jimple.infoflow.android.plugin.wear.analysis;

import java.util.HashSet;
import java.util.Set;

import soot.Unit;

public class StackState {

	protected static StackState instance;
	protected static Set<Unit> mEventStack = new HashSet<Unit>();
	protected static Set<Unit> cEventStack = new HashSet<Unit>();

	public static StackState getInstance() {
		if (instance == null)
			instance = new StackState();
		return instance;
	}

	private StackState() {
	}

	public void putMessageStack(Unit unit) {
		mEventStack.add(unit);
	}

	public boolean containsMessageStack(Unit unit) {
		return mEventStack.contains(unit);
	}

	public void putChannelStack(Unit unit) {
		cEventStack.add(unit);
	}

	public boolean containsChannelStack(Unit unit) {
		return cEventStack.contains(unit);
	}

	public void clearStack() {
		mEventStack.clear();
		cEventStack.clear();
	}

}
