package soot.jimple.infoflow.android.plugin.wear.analysis;

import java.util.AbstractMap.SimpleEntry;
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
import soot.jimple.AssignStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.infoflow.android.plugin.wear.exception.BlockAnalysisException;
import soot.jimple.infoflow.android.plugin.wear.exception.MessageAnalysisException;
import soot.jimple.infoflow.android.plugin.wear.exception.NonImplementedException;
import soot.jimple.infoflow.android.plugin.wear.exception.StringAnalysisException;
import soot.jimple.infoflow.android.usc.sql.string.StringResult;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;
import soot.jimple.infoflow.plugin.wear.extras.Keys;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;

public class MessageEventAnalysis {
	protected Unit callUnit;
	protected SootMethod sMethod;
	protected SootClass sclass;
	protected UnitGraph cfg;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	protected String pathValue;
	protected String pathRegex;
	HashMap<String, Set<String>> manifest;
	SimpleLocalDefs slocalDef;
	SimpleLocalUses localUses;
	AnalysisUtil utilInstance;

	public MessageEventAnalysis(SootMethod sm, Unit unit, HashMap<String, Set<String>> manifestServices) {
		this.sMethod = sm;
		this.callUnit = unit;
		this.sclass = sm.getDeclaringClass();
		this.manifest = manifestServices;
		this.cfg = new ExceptionalUnitGraph(sMethod.retrieveActiveBody());
		this.slocalDef = new SimpleLocalDefs(cfg);
		this.localUses = new SimpleLocalUses(cfg, slocalDef);
		this.utilInstance = AnalysisUtil.getInstance();

	}

	/**
	 * Search the parameters used to extract the message. If parameter cannot be
	 * found or inferred, set the path to a wild-card path that matches everything
	 * 
	 * @param iexp
	 * @throws NonImplementedException
	 */
	public void runAnalysis(InvokeExpr iexp) throws NonImplementedException {

		Value param = iexp.getUseBoxes().get(0).getValue();
		if (param instanceof Local) {
			Local local = (Local) param;
			logger.debug("Init backTrack Analysis\n" + callUnit.toString());
			try {
				SimpleEntry<String, String> pathType = utilInstance.parseManifestResult(sclass, manifest);
				if (pathType != null) {
					String type = pathType.getKey();
					if (type.equals("path")) {
						pathValue = pathType.getValue();
						return;
					} else if (type.equals("pathPrefix")) {
						pathValue = pathType.getValue();
					} else {
						pathRegex = pathType.getValue();
					}
				}
				doBackwardSearch(local, callUnit);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void doBackwardSearch(Local local, Unit unit)
			throws BlockAnalysisException, MessageAnalysisException, StringAnalysisException {
		SimpleLocalDefs slocalDef = new SimpleLocalDefs(cfg);
		List<Unit> def = slocalDef.getDefsOfAt(local, unit);
		if (def.size() > 1)
			throw new MessageAnalysisException("Definition of more than one Message Event. This shouldn't happen");
		if (def.size() == 0)
			return;
		Unit definitionUnit = def.get(0);
		logger.debug(definitionUnit.toString());
		if (IdentityStmt.class.isAssignableFrom(definitionUnit.getClass())) {
			IdentityStmt iStmt = (IdentityStmt) definitionUnit;
			Local var = (Local) iStmt.getLeftOp();
			doForwardSearchFast(var, definitionUnit);
		}
	}

	private void doForwardSearchFast(Local var, Unit defUnit) {
		List<Unit> slice = utilInstance.getUnitsBetween(defUnit, callUnit, sMethod.getActiveBody());
		int index = getIndexUnit(callUnit);
		Value pathArgValue = null;
		InvokeExpr iexpr = null;
		Value base = null;
		int j = -1;
		boolean keepSearching = true;
		while (keepSearching) {

			if (index == 0)
				keepSearching = false;

			Unit unit = slice.get(--index);
			iexpr = utilInstance.getInvokeExpr(unit);
			// there is only one MessageEvent in the callback
			if (iexpr != null && iexpr.getMethod().getDeclaringClass().toString().equals(ExtraTypes.MESSAGE_EVENT)
					&& iexpr.getMethod().getName().equals("getPath")) {
				pathArgValue = ((AssignStmt) unit).getLeftOp();
				keepSearching = false;
				j = index;
			}
			if (index == 0)
				keepSearching = false;
		}

		if (j == -1) // nothing found, use wildcard value
			return;
		// after the register is found

		for (int i = index + 1; i < slice.size(); i++) {
			Unit sunit = slice.get(i);
			logger.debug(sunit.toString());
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

	private int getIndexUnit(Unit callUnit) {
		int index = -1;
		for (Unit unit : sMethod.getActiveBody().getUnits()) {
			index++;
			if (unit.equals(callUnit))
				break;
		}
		return index;

	}

	public String getPathValue() {
		return pathValue;
	}

	public void setPathValue(String pathValue) {
		this.pathValue = pathValue;
	}

}
