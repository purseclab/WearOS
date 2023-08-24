package soot.jimple.infoflow.android.plugin.wear.analysis;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Local;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.StringConstant;
import soot.jimple.infoflow.android.plugin.wear.exception.StringAnalysisException;
import soot.jimple.infoflow.android.usc.sql.string.StringResult;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;
import soot.jimple.infoflow.plugin.wear.extras.Keys;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.UnitValueBoxPair;

public class DataMapAnalysis {
	protected Unit callUnit;
	protected SootMethod sMethod;
	protected SootClass sclass;
	protected UnitGraph cfg;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	protected String pathValue;
	protected String pathRegex;
	HashMap<String, Set<String>> manifest;
	AnalysisUtil utilInstance;
	List<Unit> slice;

	public DataMapAnalysis(SootMethod sm, Unit unit, HashMap<String, Set<String>> manifestServices) {
		this.sMethod = sm;
		this.callUnit = unit;
		this.sclass = sm.getDeclaringClass();
		this.manifest = manifestServices;
		this.cfg = new ExceptionalUnitGraph(sMethod.retrieveActiveBody());
		this.utilInstance = AnalysisUtil.getInstance();

	}

	public void runDataMapAnalysis(InvokeExpr iexp) {
		// aca considerar 2 tipos de apis
		int count = iexp.getArgCount();
		// consider api invocations with 1 and 2 param
		int index = (count == 1 ? 1 : 2);
		Value base = iexp.getUseBoxes().get(index).getValue();
		try {
			if (base instanceof Local) {
				Local local = (Local) base;
				List<Unit> processed = new ArrayList<Unit>();
				SimpleEntry<String, String> pathType = utilInstance.parseManifestResult(sclass, manifest);
				if (pathType != null) {
					String type = pathType.getKey();
					if (type.equals("path")) {
						pathValue = StringConstant.v(pathType.getValue()).toString();
						return;
					}
				}

				logger.debug("start backward analysis \n" + callUnit.toString());
				doBackTrack(local, callUnit, processed);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getPathRegex() {
		return pathRegex;
	}

	private void doBackTrack(Local local, Unit unit, List<Unit> processed) throws StringAnalysisException {
		SimpleLocalDefs slocalDef = new SimpleLocalDefs(cfg);
		processed.add(unit);
		for (Unit definitionUnit : slocalDef.getDefsOfAt(local, unit)) {
			logger.debug(definitionUnit.toString());
			if (processed.contains(definitionUnit))
				break;
			if (AssignStmt.class.isAssignableFrom(definitionUnit.getClass())) {
				doAssignStmtAnalysis(definitionUnit, slocalDef, processed);
			} else if (InvokeStmt.class.isAssignableFrom(definitionUnit.getClass())) {
				doInvokeStmtBackwardAnalysis((InvokeStmt) definitionUnit, processed);
			} else {
				logger.debug("Missing implementation" + definitionUnit.getClass());
			}
		}
	}

	private void doInvokeStmtBackwardAnalysis(InvokeStmt unit, List<Unit> processed) throws StringAnalysisException {
		InvokeExpr tmpIexpr = ((InvokeStmt) unit).getInvokeExpr();
		if (tmpIexpr.getMethod().getName().contains("fromDataItem")) {
			ValueBox param = tmpIexpr.getUseBoxes().get(0);
			Local localParam = (Local) param;
			doBackTrack(localParam, unit, processed);

		}
	}

	// here we need to consider all sequences which get a DataMap
	private void doAssignStmtAnalysis(Unit unit, SimpleLocalDefs slocalDef, List<Unit> processed)
			throws StringAnalysisException {
		AssignStmt asigStmt = (AssignStmt) unit;
		Value leftOp = asigStmt.getLeftOp();
		Value rightOp = asigStmt.getRightOp();
		if (InvokeExpr.class.isAssignableFrom(rightOp.getClass())) {
			InvokeExpr tmpIexpr = (InvokeExpr) rightOp;
			if ((tmpIexpr.getMethod().getDeclaringClass().toString().equals(ExtraTypes.DATA_MAP_ITEM)
					|| tmpIexpr.getMethod().getDeclaringClass().toString().equals(ExtraTypes.PUT_DATA_MAP_REQUEST))
					&& tmpIexpr.getMethod().getName().equals("getDataMap")) {
				ValueBox baseReg = tmpIexpr.getUseBoxes().get(0);
				Local base = (Local) baseReg.getValue();
				doBackTrack(base, unit, processed);
			} else if (tmpIexpr.getMethod().getDeclaringClass().toString().equals(ExtraTypes.DATA_MAP_ITEM)
					&& tmpIexpr.getMethod().getName().contains("fromDataItem")) {
				Value param = tmpIexpr.getUseBoxes().get(0).getValue();
				Local localParam = (Local) param;
				doBackTrack(localParam, unit, processed);
			} else if (tmpIexpr.getMethod().getDeclaringClass().toString().equals(ExtraTypes.PUT_DATA_MAP_REQUEST)
					&& tmpIexpr.getMethod().getName().contains("createFromDataMapItem")) {
				Value param = tmpIexpr.getUseBoxes().get(0).getValue();
				Local localParam = (Local) param;
				doBackTrack(localParam, unit, processed);
			} else if (tmpIexpr.getMethod().getDeclaringClass().toString().equals(ExtraTypes.DATA_EVENT)
					&& tmpIexpr.getMethod().getName().contains("getDataItem")) {
				Value param = tmpIexpr.getUseBoxes().get(0).getValue();
				Local localParam = (Local) param;
				doBackTrack(localParam, unit, processed);
			}
		} else if (rightOp.getType().toString().equals(ExtraTypes.DATA_EVENT)) {
			Local left = (Local) leftOp;
			logger.debug("Start foraward search");
			slice = utilInstance.getUnitsBetween(unit, callUnit, sMethod.getActiveBody());
			doForwardSearch(left, unit, slocalDef, processed);
		}
	}

	private void doForwardSearch(Local left, Unit unit, SimpleLocalDefs slocalDef, List<Unit> processed)
			throws StringAnalysisException {
		SimpleLocalUses localUses = new SimpleLocalUses(cfg, slocalDef);
		processed.add(unit);
		for (UnitValueBoxPair uses : localUses.getUsesOf(unit)) {
			logger.debug(uses.toString());
			Unit tmpUnit = uses.getUnit();
			if (tmpUnit.equals(callUnit) || !slice.contains(tmpUnit))
				break;
			if (AssignStmt.class.isAssignableFrom(tmpUnit.getClass())) {
				doAssignStmtForwardAnalysis(tmpUnit, slocalDef, processed);
			} else {
				logger.debug("Missing implementation" + unit.getClass());
			}
		}
	}

	private void doAssignStmtForwardAnalysis(Unit unit, SimpleLocalDefs slocalDef, List<Unit> processed)
			throws StringAnalysisException {
		AssignStmt asigStmt = (AssignStmt) unit;
		Value leftOp = asigStmt.getLeftOp();
		Value rightOp = asigStmt.getRightOp();
		if (InvokeExpr.class.isAssignableFrom(rightOp.getClass())) {
			InvokeExpr tmpIexpr = (InvokeExpr) rightOp;
			if (tmpIexpr.getMethod().getDeclaringClass().toString().equals(ExtraTypes.DATA_EVENT)
					&& tmpIexpr.getMethod().getName().contains("getDataItem")) {
				Local left = (Local) leftOp;
				doForwardSearch(left, unit, slocalDef, processed);
			} else if (tmpIexpr.getMethod().getDeclaringClass().toString().equals(ExtraTypes.DATA_ITEM)
					&& tmpIexpr.getMethod().getName().equals("getUri")) {
				Local left = (Local) leftOp;
				doForwardSearch(left, unit, slocalDef, processed);

			} else if (tmpIexpr.getMethod().getDeclaringClass().toString().equals(ExtraTypes.URI)
					&& tmpIexpr.getMethod().getName().equals("getPath")) {
				Local left = (Local) leftOp;
				processed.add(unit);
				logger.debug("Start infering value for path");
				searchPathValue(unit, left, slocalDef);
			}

		}
	}

	/**
	 * This function try to infer the value of the path. There is no direct
	 * reference to this value in the implementation so the method searches indirect
	 * references to the local which contains the path, e.g. search the method
	 * equals() and store the value of the string compared
	 * 
	 * @param unit
	 * @param left
	 * @param slocalDef
	 * @throws StringAnalysisException
	 */
	private void searchPathValue(Unit unit, Local left, SimpleLocalDefs slocalDef) throws StringAnalysisException {
		SimpleLocalUses localUses = new SimpleLocalUses(cfg, slocalDef);
		for (UnitValueBoxPair uses : localUses.getUsesOf(unit)) {
			logger.debug(uses.toString());
			Unit tmpUnit = uses.getUnit();
			if (!slice.contains(tmpUnit))
				continue;
			InvokeExpr tmpIexpr = null;
			if (AssignStmt.class.isAssignableFrom(tmpUnit.getClass())) {
				AssignStmt asigStmt = (AssignStmt) tmpUnit;
				if (asigStmt.containsInvokeExpr()) {
					Value rightOp = asigStmt.getRightOp();
					tmpIexpr = (InvokeExpr) rightOp;
				}
			} else if (InvokeStmt.class.isAssignableFrom(tmpUnit.getClass())) {
				tmpIexpr = ((InvokeStmt) tmpUnit).getInvokeExpr();

			}
			if (utilInstance.isComparison(tmpIexpr)) {

				Local ref = (Local) tmpIexpr.getUseBoxes().get(1).getValue();
				if (ref.equals(left)) {
					// the string is the parameter
					int line = utilInstance.getLineNumber(tmpUnit);
					int nthParam = 1;
					int offset = utilInstance.getOffset(tmpUnit);
					StringResult tmp = new StringResult(sMethod, line, offset, nthParam, tmpUnit.toString());
					String pathString = utilInstance.getStringValue(tmp);
					if (pathString == null || pathString.contains("Unknown"))
						pathString = Keys.GENERAL_PATH;

					pathValue = pathString;

				} else if (tmpIexpr.getArg(0).equals(left)) {
					// if the string is the base object
					// we need to do a Constant Value Propagation for the base
					// tmpIexpr.getUseBoxes().get(1).getValue()
					Local localRef = (Local) tmpIexpr.getUseBoxes().get(1).getValue();
					List<Unit> definition = slocalDef.getDefsOfAt(localRef, tmpUnit);
					if (definition.size() > 1) {
						pathValue = Keys.GENERAL_PATH;
					} else {
						AssignStmt asigStmt = (AssignStmt) definition.get(0);
						Value right = asigStmt.getRightOp();
						if (right instanceof Constant)
							pathValue = right.toString().replace("\"", "");
						else
							pathValue = Keys.GENERAL_PATH;

					}
				}
				break;
			}

		}

	}

	public String getPathValue() {
		return pathValue;
	}

}
