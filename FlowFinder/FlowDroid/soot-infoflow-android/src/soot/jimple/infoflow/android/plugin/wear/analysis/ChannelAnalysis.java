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
import soot.jimple.CastExpr;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.infoflow.android.plugin.wear.exception.ChannelSearchException;
import soot.jimple.infoflow.android.plugin.wear.exception.NonImplementedException;
import soot.jimple.infoflow.android.plugin.wear.exception.StringAnalysisException;
import soot.jimple.infoflow.android.usc.sql.string.StringResult;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;
import soot.jimple.infoflow.plugin.wear.extras.Keys;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;

public class ChannelAnalysis {

	private Unit callUnit;
	private SootMethod sMethod;
	private SootClass sclass;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private String pathValue;
	private HashMap<String, Set<String>> manifest;
	private StackState stackInstance;
	private SimpleLocalDefs slocalDef;
	private UnitGraph cfg;
	private AnalysisUtil utilInstance;
	private List<Unit> processedBackward;
	private String analysisType;

	public ChannelAnalysis(SootMethod sm, Unit unit, HashMap<String, Set<String>> manifestServices) {
		this.sMethod = sm;
		this.callUnit = unit;
		this.sclass = sm.getDeclaringClass();
		this.manifest = manifestServices;
		this.cfg = new ExceptionalUnitGraph(sMethod.retrieveActiveBody());
		this.stackInstance = StackState.getInstance();
		this.utilInstance = AnalysisUtil.getInstance();
		this.processedBackward = new ArrayList<Unit>();
		this.slocalDef = new SimpleLocalDefs(cfg);

	}

	/**
	 * Search the parameters used to extract the message. If parameter cannot be
	 * found or inferred, set the path to a wild-card path that matches everything
	 * 
	 * @param iexp
	 * @throws NonImplementedException
	 */
	public void runAnalysis(InvokeExpr iexp, String sourceOrSink) throws NonImplementedException {

		Local local = null;
		logger.debug("Init backTrack Analysis\n" + callUnit.toString());
		try {

			if (sourceOrSink.equals("source")) {
				// we only run this part if we are receiving data, not when we send
				analysisType = "source";
				Value param = iexp.getArg(0);
				local = (Local) param;

				SimpleEntry<String, String> pathType = utilInstance.parseManifestResult(sclass, manifest);
				if (pathType != null) {
					String type = pathType.getKey();
					if (type.equals("path")) {
						pathValue = pathType.getValue();
						// we don't need to run the analysis because the path is constant
						return;
					} else if (type.equals("pathPrefix")) {

						pathValue = pathType.getValue();
					}
				}
			} else { // sourceOrSink = sink,
				// we replace the local from the base class to the 1st param
				local = (Local) iexp.getArg(0);
				analysisType = "sink";

			}
			doBackwardSearch(local, callUnit);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void doBackwardSearch(Local local, Unit unit) throws ChannelSearchException, StringAnalysisException {
		processedBackward.add(unit);
		List<Unit> definitions = slocalDef.getDefsOfAt(local, unit);
		if (analysisType.equals("source")) {
			if (definitions.size() > 1)
				throw new ChannelSearchException("Definition of more than one Channel Event. This shouldn't happen");
		}
		for (Unit defUnit : definitions) {
			logger.debug(defUnit.toString());
			if (processedBackward.contains(defUnit))
				break;
			if (AssignStmt.class.isAssignableFrom(defUnit.getClass())) {
				doAssignStmtAnalysis(defUnit, slocalDef);
			} else if (IdentityStmt.class.isAssignableFrom(defUnit.getClass())) {
				IdentityStmt iStmt = (IdentityStmt) defUnit;
				Local var = (Local) iStmt.getLeftOp();
				doForwardBlockSearch(var, defUnit);
			} else {
				logger.debug("Missing implementation" + defUnit.getClass());
			}
		}

	}

	private void doAssignStmtAnalysis(Unit unit, SimpleLocalDefs slocalDef2)
			throws ChannelSearchException, StringAnalysisException {
		AssignStmt asigStmt = (AssignStmt) unit;
		Value rightOp = asigStmt.getRightOp();
		if (InvokeExpr.class.isAssignableFrom(rightOp.getClass())) {
			InvokeExpr tmpIexpr = (InvokeExpr) rightOp;
			String methodName = tmpIexpr.getMethod().getName();
			String dclass = tmpIexpr.getMethod().getDeclaringClass().toString();
			if (dclass.equals(ExtraTypes.TASK) && methodName.equals("getResult")) {
				ValueBox baseReg = tmpIexpr.getUseBoxes().get(0);
				Local base = (Local) baseReg.getValue();
				doBackwardSearch(base, unit);
			} else if (dclass.equals(ExtraTypes.CHANNEL_CLIENT) && methodName.equals("openChannel")) {
				// ********** Check the String *************
				int line = utilInstance.getLineNumber(unit);
				int offset = utilInstance.getOffset(unit);
				int nthParam = 2;
				StringResult tmp = new StringResult(sMethod, line, offset, nthParam, unit.toString());
				String value = utilInstance.getStringValue(tmp);
				if (value == null || value.contains("Unknown"))
					value = Keys.GENERAL_PATH;
				pathValue = value;
				// ********** Check the String *************
			}

		} else if (CastExpr.class.isAssignableFrom(rightOp.getClass())) {
			Value localRef = rightOp.getUseBoxes().get(0).getValue();
			doBackwardSearch((Local) localRef, unit);

		}
	}

	/**
	 * Find a slice between a source stmt and definition of ChannelEvent and try to
	 * infer the path corresponding to the source stmt
	 * 
	 * @param var
	 * @param definitionUnit
	 * @throws ChannelSearchException
	 * @throws StringAnalysisException
	 */
	private void doForwardBlockSearch(Local var, Unit definitionUnit)
			throws ChannelSearchException, StringAnalysisException {
		List<Unit> slice = cfg.getExtendedBasicBlockPathBetween(definitionUnit, callUnit);// defUnit.getUnitBoxes();
		if (slice == null) {
			logger.debug("Slice not found, using the entire function");
			slice = utilInstance.getRelevantUnits(sMethod, definitionUnit, callUnit);
		}
		Value pathArgValue = null;
		Value base = null;
		int index = -1;
		InvokeExpr iexpr = null;
		for (int i = 1; i < slice.size(); i++) {
			Unit sunit = slice.get(i);
			logger.debug(sunit.toString());
			if (!stackInstance.containsChannelStack(sunit)) {
				iexpr = utilInstance.getInvokeExpr(sunit);
				stackInstance.putChannelStack(sunit);
				if (iexpr != null && iexpr.getMethod().getDeclaringClass().toString().equals(ExtraTypes.CHANNEL)
						&& iexpr.getMethod().getName().equals("getPath")) {
					pathArgValue = ((AssignStmt) sunit).getLeftOp();
					index = i;
					break;
				}
			}

		}

		if (index == -1)
			return;

		for (int i = index + 1; i < slice.size(); i++) {
			Unit sunit = slice.get(i);
			logger.debug(sunit.toString());
			if (!stackInstance.containsChannelStack(sunit)) {
				stackInstance.putChannelStack(sunit);
				iexpr = utilInstance.getInvokeExpr(sunit);
				if (iexpr != null && iexpr.getMethod().getDeclaringClass().toString().equals(ExtraTypes.STRING_TYPE)
						&& utilInstance.isComparison(iexpr)) {
					base = iexpr.getUseBoxes().get(1).getValue();
					if (base.equals(pathArgValue)) {
						int line = utilInstance.getLineNumber(sunit);
						int nthParam = 1;
						int offset = utilInstance.getOffset(sunit);
						StringResult tmp = new StringResult(sMethod, line, offset, nthParam, sunit.toString());
						String pathString = utilInstance.getStringValue(tmp);
						if (pathString == null || pathString.contains("Unknown"))
							pathString = Keys.GENERAL_PATH;
						pathValue = pathString;
						break;
					}
				}
			}
		}
	}

	public String getPathValue() {
		return this.pathValue;
	}

}
