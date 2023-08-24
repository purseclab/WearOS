package soot.jimple.infoflow.android.plugin.wear.analysis;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Local;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.NewExpr;
import soot.jimple.StringConstant;
import soot.jimple.infoflow.android.plugin.wear.exception.NonImplementedException;
import soot.jimple.infoflow.android.plugin.wear.exception.StringAnalysisException;
import soot.jimple.infoflow.android.usc.sql.string.StringResult;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;
import soot.jimple.infoflow.plugin.wear.extras.Keys;
import soot.jimple.internal.JInstanceFieldRef;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.UnitValueBoxPair;

/**
 * Class that perform an intra-procedural data flow analysis to find the
 * definition of a DataItem and the call to the function that put information
 * into the DataItem
 * 
 * @author yduf149
 *
 */
public class DataItemAnalysis {

	protected Unit callUnit;
	protected SootMethod sMethod;
	protected List<DataItemSearchResult> results;
	protected SootClass sClass;
	protected UnitGraph cfg;
	protected SimpleLocalDefs slocalDef;
	protected List<Unit> processedForward;
	protected List<Unit> processedBackward;
	protected List<StringResult> stringValues;
	AnalysisUtil utilInstance;
	String createPath;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public DataItemAnalysis(Unit unit, SootMethod sm, List<StringResult> stringValues) {
		this.callUnit = unit;
		this.sMethod = sm;
		this.sClass = sm.getDeclaringClass();
		this.results = new ArrayList<DataItemSearchResult>();
		this.cfg = new ExceptionalUnitGraph(sMethod.retrieveActiveBody());
		this.slocalDef = new SimpleLocalDefs(cfg);
		this.processedBackward = new ArrayList<Unit>();
		this.processedForward = new ArrayList<Unit>();
		this.stringValues = stringValues;
		this.utilInstance = AnalysisUtil.getInstance();

	}

	// FIXME remove localdef from parameters of functions

	/**
	 * Search units where the DataMap is created and the uses of the DataMap where
	 * data is put into the DataMap
	 * 
	 * @param iexp
	 * @param argPosition
	 * @throws NonImplementedException
	 */
	public void runDataItemAnalysis(InvokeExpr iexp, int argPosition) throws NonImplementedException {
		Value param = iexp.getUseBoxes().get(argPosition).getValue();
		if (param instanceof Local) {
			Local local = (Local) param;
			//logger.info("Init backTrack Analysis\n" + callUnit.toString());
			try {
				doBackwardSearch(local, callUnit);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Search recursively definition and uses of a local in unit
	 * 
	 * @param local
	 * @param unit
	 * @param processed
	 * @throws NonImplementedException
	 * @throws StringAnalysisException
	 */
	private void doBackwardSearch(Local local, Unit unit) throws NonImplementedException, StringAnalysisException {
		processedBackward.add(unit);
		//logger.info("BACKWARD SEARCH LOCAL: " + local.toString() + " UNIT: " + unit.toString());
		for (Unit defUnit : slocalDef.getDefsOfAt(local, unit)) {
			//logger.info("BACKWARD SEARCH UNIT: " + defUnit.toString());
			if (processedBackward.contains(defUnit)){
				//logger.info("BREAKING");
				break;
			}
			if (AssignStmt.class.isAssignableFrom(defUnit.getClass())) {
				//logger.info("ASSINGMENT ANALYSIS");
				doAssignStmtAnalysis(defUnit, slocalDef);
			} else if (InvokeStmt.class.isAssignableFrom(defUnit.getClass())) {
				//logger.info("INVOKE STATEMENT");
				doInvokeStmtBackwardAnalysis((InvokeStmt) defUnit);
			} else {
				logger.info("Missing implementation" + defUnit.getClass());
			}
		}
	}

	private void doInvokeStmtBackwardAnalysis(InvokeStmt defUnit) throws NonImplementedException {
		// TODO implement a invoke statement parser for the backward analysis
		throw new NonImplementedException("Missing InvokeStatment Analysis implementation" + defUnit.getClass());

	}

	/**
	 * Parse one unit and call either a backward search or forward search depending
	 * on the result
	 * 
	 * @param unit
	 * @param slocalDef
	 * @param processed
	 * @throws NonImplementedException
	 * @throws StringAnalysisException
	 */

	private void doAssignStmtAnalysis(Unit unit, SimpleLocalDefs slocalDef)
			throws NonImplementedException, StringAnalysisException {
		AssignStmt asigStmt = (AssignStmt) unit;
		Value leftOp = asigStmt.getLeftOp();
		Value rightOp = asigStmt.getRightOp();
		if (InvokeExpr.class.isAssignableFrom(rightOp.getClass())) {
			//logger.info("ASSIGNABLE");
			InvokeExpr tmpIexpr = (InvokeExpr) rightOp;
			//logger.info("CLASS: " + tmpIexpr.getMethod().getDeclaringClass().toString());
			//logger.info("METHOD: " + tmpIexpr.getMethod().getName());
			if (tmpIexpr.getMethod().getDeclaringClass().toString().equals(ExtraTypes.PUT_DATA_MAP_REQUEST)
					&& tmpIexpr.getMethod().getName().equals("asPutDataRequest")) {
				ValueBox baseReg = tmpIexpr.getUseBoxes().get(0);
				Local base = (Local) baseReg.getValue();
				doBackwardSearch(base, unit);
			} else if (tmpIexpr.getMethod().getDeclaringClass().toString().equals(ExtraTypes.PUT_DATA_MAP_REQUEST)
					&& tmpIexpr.getMethod().getName().contains("create")) {

				// ********** Check the String *************
				int line = utilInstance.getLineNumber(unit);
				int offset = utilInstance.getOffset(unit);
				int nthParam = 1;
				StringResult tmp = new StringResult(sMethod, line, offset, nthParam, unit.toString());
				String value = utilInstance.getStringValue(tmp);
				if (value == null || value.contains("Unknown")) {
					Value rv = tmpIexpr.getArg(0);
					if (rv instanceof StringConstant)
						value = rv.toString().replace("\"", "");
					else
						value = Keys.GENERAL_PATH;

				}
				// ********** Check the String *************

				Local left = (Local) leftOp;
				logger.info("creation of DataItem found, path is: " + value);
				logger.info("Starting forward search for PUT_DATA_MAP_REQUEST");
				doForwardSearch(left, slocalDef, unit, value);

			} else if (tmpIexpr.getMethod().getDeclaringClass().toString().equals(ExtraTypes.PUT_DATA_REQUEST)
					&& tmpIexpr.getMethod().getName().contains("create")) {

				int line = utilInstance.getLineNumber(unit);
				int offset = utilInstance.getOffset(unit);
				int nthParam = 1;
				StringResult tmp = new StringResult(sMethod, line, offset, nthParam, unit.toString());
				String value = utilInstance.getStringValue(tmp);
				if (value == null || value.contains("Unknown")) {
					Value rv = tmpIexpr.getArg(0);
					if (rv instanceof StringConstant)
						value = rv.toString().replace("\"", "");
					else
						value = Keys.GENERAL_PATH;
				}
				Local left = (Local) leftOp;
				logger.info("creation of DataItem found, path is: " + value);
				logger.info("Starting forward search for PutDataRequest");
				doForwardSearch(left, slocalDef, unit, value);
			}

		}
	}

	/**
	 * Search uses of DataMaps. We are trying to find functions call which put data
	 * into a DataMap
	 * 
	 * @param local
	 * @param slocalDef
	 * @param unit
	 * @param path
	 * @throws StringAnalysisException
	 */
	private void doForwardSearch(Local local, SimpleLocalDefs slocalDef, Unit unit, String path)
			throws StringAnalysisException {
		SimpleLocalUses localUses = new SimpleLocalUses(cfg, slocalDef);
		if (!processedForward.contains(unit))
			processedForward.add(unit);

		for (UnitValueBoxPair uses : localUses.getUsesOf(unit)) {
			logger.debug(uses.toString());
			Unit tmpUnit = uses.getUnit();

			if (processedForward.contains(tmpUnit))
				break;
			else
				processedForward.add(tmpUnit);

			if (AssignStmt.class.isAssignableFrom(tmpUnit.getClass())) {
				doAssignStmtForwardAnalysis(tmpUnit, slocalDef, path);
			} else if (InvokeStmt.class.isAssignableFrom(tmpUnit.getClass())) {
				doInvokeStmtForwardAnalysis(tmpUnit, path, slocalDef);
			} else if (IdentityStmt.class.isAssignableFrom(tmpUnit.getClass())) {
				logger.info("Missing implementation" + unit.getClass());
			}

		}
	}

	private void doAssignStmtForwardAnalysis(Unit unit, SimpleLocalDefs slocalDef, String path)
			throws StringAnalysisException {
		AssignStmt asigStmt = (AssignStmt) unit;
		Value leftOp = asigStmt.getLeftOp();
		Value rightOp = asigStmt.getRightOp();
		if (InvokeExpr.class.isAssignableFrom(rightOp.getClass())) {
			InvokeExpr tmpIexpr = (InvokeExpr) rightOp;
			String declaringClass = tmpIexpr.getMethod().getDeclaringClass().toString();
			String methodName = tmpIexpr.getMethod().getName();
			if (declaringClass.equals(ExtraTypes.PUT_DATA_MAP_REQUEST) && methodName.equals("getDataMap")) {
				Local left = (Local) leftOp;
				doForwardSearch(left, slocalDef, unit, path);

			} else if (declaringClass.toString().equals(ExtraTypes.DATA_MAP) && methodName.contains("put")) {
				doInvokeStmtForwardAnalysis((Unit) tmpIexpr, path, slocalDef);
			} else if (declaringClass.equals(ExtraTypes.PUT_DATA_REQUEST) && methodName.equals("putAsset")) {
				doInvokeStmtForwardAnalysis((Unit) tmpIexpr, path, slocalDef);
			}

		} else if (JInstanceFieldRef.class.isAssignableFrom(rightOp.getClass())) {
			Local left = (Local) leftOp;
			doForwardSearch(left, slocalDef, unit, path);
		}
	}

	private void doInvokeStmtForwardAnalysis(Unit tmpUnit, String path, SimpleLocalDefs slocalDef)
			throws StringAnalysisException {
		InvokeExpr tmpIexpr = ((InvokeStmt) tmpUnit).getInvokeExpr();
		String dclass = tmpIexpr.getMethod().getDeclaringClass().toString();
		if (dclass.equals(ExtraTypes.DATA_MAP)) {

			if (tmpIexpr.getMethod().getName().equals("putAll")) {
				ValueBox paramDMap = tmpIexpr.getArgBox(0);
				// the type of this local is DataMap
				Local local = (Local) paramDMap.getValue();
				for (Unit defUnit : slocalDef.getDefsOfAt(local, tmpUnit)) {
					logger.debug(defUnit.toString());
					if (AssignStmt.class.isAssignableFrom(defUnit.getClass())) {
						AssignStmt asigStmt = (AssignStmt) defUnit;
						Value rightOp = asigStmt.getRightOp();
						if (NewExpr.class.isAssignableFrom(rightOp.getClass())) {
							doForwardSearch(local, slocalDef, defUnit, path);
							break;
						}

					} else if (IdentityStmt.class.isAssignableFrom(defUnit.getClass())) {
						if (defUnit.toString().contains("parameter"))
							logger.info("Interprocedural analysis needed");
					}
				}
			} else if (tmpIexpr.getMethod().getName().contains("put")) {
				ValueBox param2 = tmpIexpr.getUseBoxes().get(1);
				DataItemSearchResult result = new DataItemSearchResult();

				// ********** Check the String *************
				int line = utilInstance.getLineNumber(tmpUnit);
				int nthParam = 1;
				int offset = utilInstance.getOffset(tmpUnit);

				StringResult tmp = new StringResult(sMethod, line, offset, nthParam, tmpUnit.toString());
				String keyValue = utilInstance.getStringValue(tmp);
				if (keyValue == null || keyValue.contains("Unknown")) {
					Value rv = tmpIexpr.getArg(0);
					if (rv instanceof StringConstant)
						keyValue = rv.toString().replace("\"", "");
					else
						keyValue = Keys.GENERAL_PATH;
				}
				// ********** Check the String *************

				result.setPath(path);
				result.setKey(keyValue);
				result.setRegister(param2);
				result.setSinkMethod(tmpIexpr.getMethod().getName());
				result.setUnit(tmpUnit);
				this.results.add(result);
			}
		} else if (dclass.equals(ExtraTypes.PUT_DATA_REQUEST) && tmpIexpr.getMethod().getName().contains("putAsset")) {
			ValueBox reg = tmpIexpr.getUseBoxes().get(1);
			DataItemSearchResult result = new DataItemSearchResult();

			// ********** Check the String *************
			int line = utilInstance.getLineNumber(tmpUnit);
			int nthParam = 1;
			int offset = utilInstance.getOffset(tmpUnit);

			StringResult tmp = new StringResult(sMethod, line, offset, nthParam, tmpUnit.toString());
			String keyValue = utilInstance.getStringValue(tmp);
			if (keyValue == null || keyValue.contains("Unknown")) {
				Value rv = tmpIexpr.getArg(0);
				if (rv instanceof StringConstant)
					keyValue = rv.toString().replace("\"", "");
				else
					keyValue = Keys.GENERAL_PATH;
			}
			// ********** Check the String *************

			result.setPath(path);
			result.setKey(keyValue);
			result.setRegister(reg);
			result.setSinkMethod(tmpIexpr.getMethod().getName());
			result.setUnit(tmpUnit);
			this.results.add(result);
		}
	}

	/**
	 * Return results of {@link runDataItemAnalysis} These are the path and key of
	 * the sink method
	 * 
	 * @return
	 */
	public List<DataItemSearchResult> getResults() {
		return results;
	}

	public void setResults(List<DataItemSearchResult> results) {
		this.results = results;
	}

}
