package soot.jimple.infoflow.android.plugin.wear.analysis;

import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;

public class DataItemSearchResult {

	private String path;
	private String key;
	private ValueBox register;
	private Value intrumentedRegister;
	private String sinkMethod;
	private SootMethod contextMethod;
	private Unit unit;

	public DataItemSearchResult() {
	}

	public DataItemSearchResult(String path, String key, ValueBox register, SootMethod sootMethod) {
		this.path = path;
		this.key = key;
		this.register = register;
	}

	public SootMethod getcontextMethod() {
		return contextMethod;
	}

	public void setContextMethod(SootMethod sMethod) {
		this.contextMethod = sMethod;
	}

	public Unit getUnit() {
		return unit;
	}

	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public ValueBox getRegister() {
		return register;
	}

	public void setRegister(ValueBox register) {
		this.register = register;
	}

	public String getSinkMethod() {
		return sinkMethod;
	}

	public void setSinkMethod(String sinkMethod) {
		this.sinkMethod = sinkMethod;
	}

	public Value getIntrumentedRegister() {
		return intrumentedRegister;
	}

	public void setIntrumentedRegister(Value intrumentedRegister) {
		this.intrumentedRegister = intrumentedRegister;
	}

	@Override
	public String toString() {
		return "DataItemSearchResult [path=" + path + ", key=" + key + ", register=" + register
				+ ", intrumentedRegister=" + intrumentedRegister + ", sinkMethod=" + sinkMethod + ", contextMethod="
				+ contextMethod + ", unit=" + unit + "]";
	}

}
