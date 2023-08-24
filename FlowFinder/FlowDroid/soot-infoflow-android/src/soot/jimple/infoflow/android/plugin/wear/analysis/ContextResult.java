package soot.jimple.infoflow.android.plugin.wear.analysis;

import java.util.List;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;

public class ContextResult {

	private SootMethod context;
	private List<PairResult<Unit, InvokeExpr>> target;

	public ContextResult(SootMethod context, List<PairResult<Unit, InvokeExpr>> target) {
		this.context = context;
		this.setTarget(target);
	}

	public SootMethod getContext() {
		return context;
	}

	public void setContext(SootMethod context) {
		this.context = context;
	}

	public List<PairResult<Unit, InvokeExpr>> getTarget() {
		return target;
	}

	public void setTarget(List<PairResult<Unit, InvokeExpr>> target) {
		this.target = target;
	}

}
