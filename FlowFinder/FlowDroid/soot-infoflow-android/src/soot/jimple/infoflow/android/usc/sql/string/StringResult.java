package soot.jimple.infoflow.android.usc.sql.string;

import java.util.ArrayList;
import java.util.List;

import soot.SootMethod;

public class StringResult {
	String methodName;
	String className;
	Integer line;
	Integer nthparam;
	String jimple;
	Integer offset;

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	List<String> values;

	public Integer getNthparam() {
		return nthparam;
	}

	public void setNthparam(Integer nthparam) {
		this.nthparam = nthparam;
	}

	public String getJimple() {
		return jimple;
	}

	public void setJimple(String jimple) {
		this.jimple = jimple;
	}

	public List<String> getValues() {
		return values;
	}

	public void setValues(List<String> values) {
		this.values = values;
	}

	public StringResult(SootMethod sm, String line, String nthParam, String jimple, String offset) {
		this.methodName = sm.getName();
		this.className = sm.getDeclaringClass().getName();
		this.line = Integer.parseInt(line);
		this.nthparam = Integer.parseInt(nthParam);
		this.offset = Integer.parseInt(offset);
		this.jimple = jimple;
		this.values = new ArrayList<String>();
	}

	public StringResult(SootMethod sm, int line, int nthParam, String jimple) {
		this.methodName = sm.getName();
		this.className = sm.getDeclaringClass().getName();
		this.line = line;
		this.nthparam = nthParam;
		this.jimple = jimple;
		this.values = new ArrayList<String>();
	}

	public StringResult(SootMethod sm, int line, int offset, int nthParam, String jimple) {
		this.methodName = sm.getName();
		this.className = sm.getDeclaringClass().getName();
		this.line = line;
		this.nthparam = nthParam;
		this.jimple = jimple;
		this.offset = offset;
		this.values = new ArrayList<String>();
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public Integer getLine() {
		return line;
	}

	public void setLine(Integer line) {
		this.line = line;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((className == null) ? 0 : className.hashCode());
		result = prime * result + ((jimple == null) ? 0 : jimple.hashCode());
		result = prime * result + ((line == null) ? 0 : line.hashCode());
		result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
		result = prime * result + ((nthparam == null) ? 0 : nthparam.hashCode());
		result = prime * result + offset;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StringResult other = (StringResult) obj;
		if (className == null || other.className == null) {
			return false;
		} else if (!className.equals(other.className))
			return false;
		if (jimple == null || other.jimple == null) {
			return false;
		} else if (!jimple.equals(other.jimple))
			return false;
		if (line != -1 && offset != -1) {
			if (!offset.equals(other.offset) || !line.equals(other.line))
				return false;
		}
		if (line != -1 && offset == -1) {
			if (!line.equals(other.line))
				return false;
		}
		if (line == -1 && offset != -1) {
			if (!offset.equals(other.offset))
				return false;
		}

		if (methodName == null || other.methodName == null) {
			return false;
		} else if (!methodName.equals(other.methodName))
			return false;
		if (nthparam == null || other.nthparam == null) {
			return false;
		} else if (!nthparam.equals(other.nthparam))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "StringResult \n[contextMethod=" + methodName + "\n className=" + className + "\n line=" + line
				+ "\noffset= " + offset + "\n nthparam=" + nthparam + "\n jimple=" + jimple + "\n values=" + values
				+ "]";
	}

}
