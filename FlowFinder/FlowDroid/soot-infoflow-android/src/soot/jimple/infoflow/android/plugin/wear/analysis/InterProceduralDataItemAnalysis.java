package soot.jimple.infoflow.android.plugin.wear.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.NewExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.StringConstant;
import soot.jimple.infoflow.android.plugin.wear.transformers.WearSceneTransformer;
import soot.jimple.infoflow.android.usc.sql.string.StringResult;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;
import soot.jimple.infoflow.plugin.wear.extras.Keys;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.UnitValueBoxPair;

public class InterProceduralDataItemAnalysis {

	protected Body body;
	protected SootMethod initialMethod;
	protected Unit initialUnit;
	protected static SootClass component;
	protected static boolean fullComponent;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private static List<SootMethod> methodStack = new ArrayList<SootMethod>();
	protected static List<String> relevantTypes;
	protected AnalysisUtil utilInstance;
	protected String packageNameStart;
	private LinkedHashSet<SootMethod> contextMethods = new LinkedHashSet<SootMethod>();
	List<DataItemSearchResult> searchResults = new ArrayList<DataItemSearchResult>();
	String pathString = null;
	SootMethod parentContext = null;
	Local parentLocal = null;
	Local auxLocal = null;
	Unit parentUnit = null;

	public InterProceduralDataItemAnalysis(Body body, Unit unit) {
		this.body = body;
		initialUnit = unit;
		initialMethod = body.getMethod();
		component = body.getMethod().getDeclaringClass();
		fullComponent = existClassVariable(component);
		relevantTypes = new ArrayList<String>(Arrays.asList(ExtraTypes.fullAnalysisTypes));
		utilInstance = AnalysisUtil.getInstance();
		packageNameStart = WearSceneTransformer.packageNameStart;
	}

	/**
	 * Generates a call stack from the ICFG of the component and run the analysis
	 * trying to find the parameters of the function calls to sink or source method.
	 * Only consider methods defined in this component to expand the
	 * inter-procedural CFG.
	 * 
	 * @param iexp
	 */
	public void runInterProceduralAnalysis(InvokeExpr iexp, int argPosition) {

		logger.info("Init Interprocedural Analysis\n" + initialUnit.toString());
		try {
			Value param = iexp.getUseBoxes().get(argPosition).getValue();
			if (param instanceof Local) {
				Local local = (Local) param;
				runFastAnalysis(local, initialUnit);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void runFastAnalysis(Local request, Unit InitialUnit) {
		logger.info("Init backTrack Analysis\n" + InitialUnit.toString());
		IntermediateResult result = searchChannelDefinition(request, initialUnit, initialMethod);
		if (result == null) {
			logger.info("Channel definition not found" + initialMethod.getSignature());
			return;
		}
		Unit unit = result.getCurrentUnit();

		// ********** Check the String *************

		int line = utilInstance.getLineNumber(unit);
		int offset = utilInstance.getOffset(unit);

		int nthParam = 1;
		StringResult resultPath = new StringResult(result.getCurrentMethod(), line, offset, nthParam, unit.toString());
		pathString = utilInstance.getStringValue(resultPath);
		if (pathString == null || pathString.contains("Unknown@FIELD")) {
			InvokeExpr iexp = utilInstance.getInvokeExpr(unit);
			Value rv = iexp.getArg(0);
			if (rv instanceof StringConstant)
				pathString = rv.toString().replace("\"", "");
			else
				pathString = Keys.GENERAL_PATH;
		}

		// ********** Start Forward Analysis ************* //

		Local dataMapRegister = null;
		List<PairResult<Unit, InvokeExpr>> targets = null;
		SootMethod context = null;
		Local register = (parentLocal == null ? result.getRegister() : parentLocal);
		unit = (parentUnit == null ? result.getCurrentUnit() : parentUnit);
		context = (parentContext == null ? result.getCurrentMethod() : parentContext);

		// search within the currentMethod
		targets = searchVariableUses(unit, register, context, ExtraTypes.PUT_DATA_MAP_REQUEST, "getDataMap");

		if (targets.size() > 0) {
			// logger.info("TARGETS FOUND");

			for (PairResult<Unit, InvokeExpr> target : targets) {
				AssignStmt asigStmt = (AssignStmt) target.getUnit();
				Value leftOp = asigStmt.getLeftOp();
				dataMapRegister = (Local) leftOp;
				searchDataMapUses(target.getUnit(), dataMapRegister, context, ExtraTypes.DATA_MAP, "put");

			}
		} else {
			// logger.info("TARGETS NOT FOUND");
			List<ContextResult> targetsContext = searchDataMapDefinition(context, register);
			// logger.info("TARGETS CONTEXT: " + targetsContext.toString());
			for (ContextResult ctargets : targetsContext) {
				// logger.info("CONTEXT RESULT: " +ctargets.getContext().toString());
				for (PairResult<Unit, InvokeExpr> target : ctargets.getTarget()) {
					AssignStmt asigStmt = (AssignStmt) target.getUnit();
					Value leftOp = asigStmt.getLeftOp();
					dataMapRegister = (Local) leftOp;
					// logger.info("TARGET UNIT: " + asigStmt.toString() +  " DMREGISTER: " + dataMapRegister.toString());
					searchDataMapUses(target.getUnit(), dataMapRegister, ctargets.getContext(), ExtraTypes.DATA_MAP,
							"put");
				}
			}
		}
	}

	private List<ContextResult> searchDataMapDefinition(SootMethod context, Local register) {
		logger.info("SDMD context: " + context.toString() + " register: " + register.toString());
		List<PairResult<Unit, InvokeExpr>> target = null;
		List<ContextResult> targets = new ArrayList<>();

		boolean keepSearching = true;
		int iteration = 0;
		int maxIterations = 3;
		LinkedHashSet<SootMethod> newContexts = new LinkedHashSet<>();
		newContexts.add(context);
		List<SootMethod> processed = new ArrayList<>();

		while (keepSearching && iteration < maxIterations) {
			// logger.info("ITERATION: {}", iteration);
			iteration++;
			List<SootMethod> backwards = expandOneLevelBackward(newContexts, processed, register, ExtraTypes.DATA_MAP);
			for (SootMethod sm : backwards) {
				// logger.info("BACKWARDS METHOD: " +sm.toString());
				target = searchVariableUses(null, null, sm, ExtraTypes.PUT_DATA_MAP_REQUEST, "getDataMap");
				if (target != null && !target.isEmpty()) {
					// logger.info("TARGET FOUND 1: " + target.toString());
					ContextResult iresult = new ContextResult(sm, target);
					targets.add(iresult);
					keepSearching = false;
					break;
				}
			}
			newContexts.addAll(backwards);

			if (keepSearching) {

				List<SootMethod> forwards = expandOneLevelForward(newContexts, processed, register,
						ExtraTypes.DATA_MAP);
				for (SootMethod sm : forwards) {
					target = searchVariableUses(null, null, sm, ExtraTypes.PUT_DATA_MAP_REQUEST, "getDataMap");
					if (target != null && !target.isEmpty()) {
						// logger.info("TARGET FOUND 2");
						ContextResult iresult = new ContextResult(sm, target);
						targets.add(iresult);
						keepSearching = false;
						break;
					}
				}
				contextMethods.addAll(forwards);
			}
		} // while searching

		return targets;
	}

	private void searchDataMapUses(Unit unit, Local dataMapRegister, SootMethod currentMethod, String dataMap,
			String targetFunction) {
		UnitGraph cfg = new ExceptionalUnitGraph(currentMethod.retrieveActiveBody());
		SimpleLocalDefs slocalDef = new SimpleLocalDefs(cfg);
		SimpleLocalUses localUses = new SimpleLocalUses(cfg, slocalDef);
		for (UnitValueBoxPair uses : localUses.getUsesOf(unit)) {
			logger.debug(uses.toString());
			Unit tmpUnit = uses.getUnit();
			InvokeExpr iexpr = parseUnit(tmpUnit);
			if (iexpr == null)
				continue;

			if (iexpr instanceof SpecialInvokeExpr) {
				SootMethod tm = iexpr.getMethod();
				searchDataMapAll(null, null, null, tm);
			} else if (iexpr instanceof StaticInvokeExpr) {
				SootMethod tm = iexpr.getMethod();
				searchDataMapAll(null, null, null, tm);
			}

			if (iexpr.getMethod().getName().equals("putAll")) {

				Value paramDMap = iexpr.getArg(0);
				// the type of this local is DataMap
				Local local = (Local) paramDMap;
				for (Unit defUnit : slocalDef.getDefsOfAt(local, tmpUnit)) {
					logger.debug(defUnit.toString());
					if (AssignStmt.class.isAssignableFrom(defUnit.getClass())) {
						AssignStmt asigStmt = (AssignStmt) defUnit;
						Value rightOp = asigStmt.getRightOp();

						if (NewExpr.class.isAssignableFrom(rightOp.getClass())) {
							List<SootMethod> expandedMethods = new ArrayList<SootMethod>();
							expandedMethods.add(currentMethod);
							if (component.getSuperclass().getName().contains("Thread")) {
								expandedMethods = getReferenceFromOuterClass(currentMethod);
								for (SootMethod smethod : expandedMethods)
									searchDataMapAll(null, null, defUnit, smethod);

							}
							searchDataMapAll(local, localUses, defUnit, currentMethod);

						} else if (InstanceFieldRef.class.isAssignableFrom(rightOp.getClass())) {
							logger.debug(rightOp.getType().toString());
							FieldRef field = (FieldRef) rightOp;

							if (component.getSuperclass().getName().contains("Thread")) {
								List<SootMethod> expandedMethods = getReferenceFromOuterClass(currentMethod);
								searchFieldReferences(field, expandedMethods);

							} else {

								searchFieldReferences(field, null);
							}
						}
						// else if (return from function value) $r4 =
					} else if (IdentityStmt.class.isAssignableFrom(defUnit.getClass())) {
						if (defUnit.toString().contains("parameter")) {
							logger.debug("Interprocedural analysis needed");
							List<SootMethod> processed = new ArrayList<>();
							LinkedHashSet<SootMethod> contexts = new LinkedHashSet<>();
							contexts.add(currentMethod);
							List<SootMethod> backwards = expandOneLevelBackward(contexts, processed, null,
									ExtraTypes.DATA_MAP);
							for (SootMethod sm : backwards) {
								searchDataMapAll(null, null, null, sm);

								// boolean keepSearching = true;
								// int iteration = 0;
								// int maxIterations = 2;
								//
								// while (keepSearching && iteration < maxIterations) {
								// iteration++;
								// insert here searchDataMapAll
							}
							// contexts.addAll(backwards);
							// }
						}
					}
				}

			} else if (iexpr.getMethod().getName().contains("put")) {

				DataItemSearchResult tmp = new DataItemSearchResult();
				tmp.setPath(pathString);
				tmp.setContextMethod(currentMethod);
				tmp.setUnit(tmpUnit);
				tmp.setRegister(iexpr.getArgBox(1));
				tmp.setSinkMethod(iexpr.getMethod().getName());

				int line = utilInstance.getLineNumber(tmpUnit);
				int offset = utilInstance.getOffset(tmpUnit);
				int nthParam = 1;
				StringResult resultKey = new StringResult(currentMethod, line, offset, nthParam, tmpUnit.toString());
				String keyString = utilInstance.getStringValue(resultKey);
				if (keyString == null || keyString.contains("Unknown@FIELD")) {
					Value rv = iexpr.getArg(0);
					if (rv instanceof StringConstant)
						keyString = rv.toString().replace("\"", "");
					else
						keyString = Keys.GENERAL_KEY;
				}
				tmp.setKey(keyString);

				searchResults.add(tmp);

			}
		}
	}

	private List<SootMethod> getReferenceFromOuterClass(SootMethod currentMethod) {
		List<SootMethod> sources = new ArrayList<SootMethod>();

		if (currentMethod.getName().equals("run")) {
			CallGraph cg = Scene.v().getCallGraph();
			Iterator<Edge> it = cg.edgesInto(currentMethod);
			while (it.hasNext()) {
				Edge edge = it.next();
				SootMethod tmp = (SootMethod) edge.getSrc();
				if (tmp.getDeclaringClass() != currentMethod.getDeclaringClass())
					sources.add(tmp);

			}

		}
		return sources;
	}

	private HashMap<SootMethod, Value> searchFieldReferences(FieldRef fr, List<SootMethod> expandedMethods) {

		HashMap<SootMethod, Value> referenceTable = new HashMap<SootMethod, Value>();
		boolean checkExpanded = (expandedMethods == null ? false : true);
		List<SootMethod> contextToSearch = new ArrayList<SootMethod>();
		contextToSearch.addAll(component.getMethods());
		if (component.getSuperclass().getName().contains("Thread")) {
			SootClass outer = component.getOuterClassUnsafe();
			if (outer != null)
				contextToSearch.addAll(outer.getMethods());
		}

		for (SootMethod sm : contextToSearch) {

			if (isRelevant(sm, ExtraTypes.DATA_MAP)) {
				if (checkExpanded && expandedMethods.contains(sm))
					checkLocal(sm, ExtraTypes.DATA_MAP);
				for (Unit u : sm.getActiveBody().getUnits()) {
					if (u instanceof AssignStmt) {
						AssignStmt fassign = (AssignStmt) u;
						Value l = fassign.getRightOp();
						if (l instanceof FieldRef) {
							FieldRef ffr = (FieldRef) l;
							if (ffr.getField().getSignature().toString().equals(fr.getField().getSignature())) {

								UnitGraph cfg = new ExceptionalUnitGraph(sm.retrieveActiveBody());
								SimpleLocalDefs slocalDef = new SimpleLocalDefs(cfg);
								SimpleLocalUses localUses = new SimpleLocalUses(cfg, slocalDef);
								for (UnitValueBoxPair uses : localUses.getUsesOf(u)) {
									logger.debug(uses.toString());
									Unit tmpUnit = uses.getUnit();
									InvokeExpr iexpr = parseUnit(tmpUnit);
									if (iexpr == null)
										continue;
									if (iexpr.getMethod().getName().contains("put")
											&& !iexpr.getMethod().getName().equals("putAll")) {
										DataItemSearchResult tmp = new DataItemSearchResult();
										tmp.setContextMethod(sm);
										tmp.setUnit(tmpUnit);
										tmp.setRegister(iexpr.getArgBox(1));
										tmp.setSinkMethod(iexpr.getMethod().getName());

										int line = utilInstance.getLineNumber(tmpUnit);
										int offset = utilInstance.getOffset(tmpUnit);
										int nthParam = 1;
										StringResult resultKey = new StringResult(sm, line, offset, nthParam,
												tmpUnit.toString());
										String keyString = utilInstance.getStringValue(resultKey);
										if (keyString == null || keyString.contains("Unknown@FIELD")) {
											Value rv = iexpr.getArg(0);
											if (rv instanceof StringConstant)
												keyString = rv.toString().replace("\"", "");
											else
												keyString = Keys.GENERAL_KEY;
										}
										tmp.setKey(keyString);
										tmp.setPath(pathString);

										searchResults.add(tmp);
									}

									break;
								}
							}
						}
					}
				}
			}
		}
		return referenceTable;
	}

	private void checkLocal(SootMethod sm, String dataMap) {
		for (Unit tmpUnit : sm.retrieveActiveBody().getUnits()) {
			logger.debug(tmpUnit.toString());
			InvokeExpr iexpr = parseUnit(tmpUnit);
			if (iexpr == null)
				continue;
			if (iexpr.getMethod().getName().contains("put") && !iexpr.getMethod().getName().equals("putAll")) {
				DataItemSearchResult tmp = new DataItemSearchResult();
				tmp.setContextMethod(sm);
				tmp.setUnit(tmpUnit);
				tmp.setRegister(iexpr.getArgBox(1));
				tmp.setSinkMethod(iexpr.getMethod().getName());

				int line = utilInstance.getLineNumber(tmpUnit);
				int offset = utilInstance.getOffset(tmpUnit);
				int nthParam = 1;
				StringResult resultKey = new StringResult(sm, line, offset, nthParam, tmpUnit.toString());
				String keyString = utilInstance.getStringValue(resultKey);
				if (keyString == null || keyString.contains("Unknown@FIELD")) {
					Value rv = iexpr.getArg(0);
					if (rv instanceof StringConstant)
						keyString = rv.toString().replace("\"", "");
					else
						keyString = Keys.GENERAL_KEY;
				}
				tmp.setKey(keyString);
				tmp.setPath(pathString);

				searchResults.add(tmp);
			}
		}
	}

	private void searchDataMapAll(Local local, SimpleLocalUses localUses, Unit unit, SootMethod currentMethod) {
		if (local != null) {
			for (UnitValueBoxPair uses : localUses.getUsesOf(unit)) {
				logger.debug(uses.toString());
				Unit tmpUnit = uses.getUnit();
				if (InvokeStmt.class.isAssignableFrom(tmpUnit.getClass())) {
					InvokeExpr iexpr = ((InvokeStmt) tmpUnit).getInvokeExpr();
					if (iexpr != null && iexpr.getMethod().getName().contains("put")
							&& !iexpr.getMethod().getName().equals("putAll")) {
						DataItemSearchResult tmp = new DataItemSearchResult();
						tmp.setContextMethod(currentMethod);
						tmp.setUnit(tmpUnit);
						tmp.setRegister(iexpr.getArgBox(1));
						tmp.setSinkMethod(iexpr.getMethod().getName());
						int line = utilInstance.getLineNumber(tmpUnit);
						int offset = utilInstance.getOffset(tmpUnit);
						int nthParam = 1;
						StringResult resultKey = new StringResult(currentMethod, line, offset, nthParam,
								tmpUnit.toString());
						String keyString = utilInstance.getStringValue(resultKey);
						if (keyString == null || keyString.contains("Unknown@FIELD")) {
							Value rv = iexpr.getArg(0);
							if (rv instanceof StringConstant)
								keyString = rv.toString().replace("\"", "");
							else
								keyString = Keys.GENERAL_KEY;
						}
						tmp.setPath(pathString);
						tmp.setKey(keyString);
						searchResults.add(tmp);
					}
				}
			}
		} else {
			for (Unit u : currentMethod.retrieveActiveBody().getUnits()) {
				logger.debug(u.toString());
				InvokeExpr iexpr = parseUnit(u);
				if (iexpr == null)
					continue;
				if (iexpr.getMethod().getName().contains("put") && !iexpr.getMethod().getName().equals("putAll")) {
					DataItemSearchResult tmp = new DataItemSearchResult();
					tmp.setContextMethod(currentMethod);
					tmp.setUnit(u);
					tmp.setRegister(iexpr.getArgBox(1));
					tmp.setSinkMethod(iexpr.getMethod().getName());
					int line = utilInstance.getLineNumber(u);
					int offset = utilInstance.getOffset(u);
					int nthParam = 1;
					StringResult resultKey = new StringResult(currentMethod, line, offset, nthParam, u.toString());
					String keyString = utilInstance.getStringValue(resultKey);
					if (keyString == null || keyString.contains("Unknown@FIELD")) {
						Value rv = iexpr.getArg(0);
						if (rv instanceof StringConstant)
							keyString = rv.toString().replace("\"", "");
						else
							keyString = Keys.GENERAL_KEY;
					}
					tmp.setPath(pathString);
					tmp.setKey(keyString);
					searchResults.add(tmp);
				}
			}

		}
	}

	/**
	 * function to check if this unit corresponds to static assigment of DataMap
	 * from PutDataMapRequest
	 * 
	 * @param u
	 * @return
	 */
	private boolean auxCheck(Unit unit) {
		if (AssignStmt.class.isAssignableFrom(unit.getClass())) {
			AssignStmt asigStmt = (AssignStmt) unit;
			Value rightOp = asigStmt.getRightOp();
			if (JInstanceFieldRef.class.isAssignableFrom(rightOp.getClass())) {
				if (unit.toString().contains("PutDataMapRequest"))
					return true;

			}
		}
		return false;
	}

	private List<PairResult<Unit, InvokeExpr>> searchVariableUses(Unit unit, Local register, SootMethod currentMethod,
			String dataType, String targetFunction) {
		List<PairResult<Unit, InvokeExpr>> allUses = new ArrayList<PairResult<Unit, InvokeExpr>>();
		PairResult<Unit, InvokeExpr> result = null;
		if (unit != null) {
			UnitGraph cfg = new ExceptionalUnitGraph(currentMethod.retrieveActiveBody());
			SimpleLocalDefs slocalDef = new SimpleLocalDefs(cfg);
			SimpleLocalUses localUses = new SimpleLocalUses(cfg, slocalDef);

			for (UnitValueBoxPair uses : localUses.getUsesOf(unit)) {
				logger.debug(uses.toString());
				Unit tmpUnit = uses.getUnit();
				InvokeExpr iexpr = parseUnit(tmpUnit);

				if (iexpr != null && iexpr.getMethod().getName().equals(targetFunction)) {
					if (targetFunction.equals("getDataMap")) {
						result = new PairResult<Unit, InvokeExpr>(tmpUnit, iexpr);
						allUses.add(result);
					}
				} else {
					if (tmpUnit.toString().contains("DataMap") && auxCheck(tmpUnit)) {
						auxLocal = (Local) getLeftValueFromAssig(unit);
						result = new PairResult<Unit, InvokeExpr>(tmpUnit, iexpr);
						allUses.add(result);
					}
				}

			}
		} else {
			// we are doing a inter-procedural search, no reference of the unit
			for (Unit tmpUnit : currentMethod.getActiveBody().getUnits()) {
				logger.debug(tmpUnit.toString());

				InvokeExpr iexpr = parseUnit(tmpUnit);
				if (iexpr == null)
					continue;
				if (iexpr.getMethod().getName().equals(targetFunction)) {
					if (targetFunction.equals("getDataMap")) {
						result = new PairResult<Unit, InvokeExpr>(tmpUnit, iexpr);
						allUses.add(result);
					}
				} else {
					if (tmpUnit.toString().contains("DataMap") && auxCheck(tmpUnit)) {
						auxLocal = (Local) getLeftValueFromAssig(unit);
						result = new PairResult<Unit, InvokeExpr>(tmpUnit, iexpr);
						allUses.add(result);
					}
				}
			}
		}
		return allUses;
	}

	private IntermediateResult searchChannelDefinition(Local putDataReqLocal, Unit initialUnit,
			SootMethod initialMethod) {
		// for temporal results
		PairResult<Unit, InvokeExpr> target = null;
		SootMethod contextMethod = initialMethod;
		Local channel = null;
		Unit refUnit = null;
		IntermediateResult result = null;
		List<SootMethod> processed = new ArrayList<SootMethod>();
		IntermediateResult contextResult = new IntermediateResult();

		target = searchVariableDefinition(initialUnit, putDataReqLocal, contextMethod, ExtraTypes.PUT_DATA_REQUEST,
				"asPutDataRequest", contextResult);
		if (target == null) {
			contextMethods.add(contextMethod);

			boolean keepSearching = true;
			int iteration = 0;
			int maxIterations = 10;

			while (keepSearching && iteration < maxIterations) {
				iteration++;
				List<SootMethod> backwards = expandOneLevelBackward(contextMethods, processed, putDataReqLocal,
						ExtraTypes.PUT_DATA_REQUEST);
				for (SootMethod sm : backwards) {
					target = searchVariableDefinition(null, putDataReqLocal, sm, ExtraTypes.PUT_DATA_REQUEST,
							"asPutDataRequest", contextResult);
					if (target != null) {
						contextMethod = sm;
						keepSearching = false;
						break;
					}
				}
				contextMethods.addAll(backwards);

				if (keepSearching) {

					List<SootMethod> forwards = expandOneLevelForward(contextMethods, processed, putDataReqLocal,
							ExtraTypes.PUT_DATA_REQUEST);
					for (SootMethod sm : forwards) {
						target = searchVariableDefinition(null, putDataReqLocal, sm, ExtraTypes.PUT_DATA_MAP_REQUEST,
								"asPutDataRequest", contextResult);
						if (target != null) {
							contextMethod = sm;
							keepSearching = false;
							break;
						}
					}
					contextMethods.addAll(forwards);
				}
			} // while searching

		}
		if (target == null)
			return null;
		// Found asPutDataMapRequest()!!
		// 0 is the index for this API call
		Local pdmrLocal = getBaseRegister(target.getExpr(), 0);

		/// ***** slit the functions ***** \\\\\\\\\\

		// First we search within the same method
		target = searchVariableDefinition(target.getUnit(), pdmrLocal, contextMethod, ExtraTypes.PUT_DATA_MAP_REQUEST,
				"create", contextResult);

		if (target != null) {
			AssignStmt asigStmt = (AssignStmt) target.getUnit();
			Value leftOp = asigStmt.getLeftOp();
			channel = (Local) leftOp;
			result = new IntermediateResult(contextMethods, channel);
			result.setCurrentMethod(contextMethod);
			result.setCurrentUnit(target.getUnit());
			return result;
		} else {
			// search within the new context method if there is a direct call with relevant
			// data type
			if (contextResult.getActive()) {
				target = searchVariableDefinition(null, contextResult.getRegister(), contextResult.getCurrentMethod(),
						ExtraTypes.PUT_DATA_MAP_REQUEST, "create", contextResult);
				if (target != null) {
					parentContext = contextMethod;
					parentUnit = contextResult.getCurrentUnit();
					contextMethod = contextResult.getCurrentMethod();
					parentLocal = contextResult.getRegister();
					refUnit = target.getUnit();
				}
			} else {
				// search within all the previously expanded methods
				for (SootMethod sm : contextMethods) {
					target = searchVariableDefinition(null, pdmrLocal, sm, ExtraTypes.PUT_DATA_MAP_REQUEST, "create",
							contextResult);
					if (target != null) {
						contextMethod = sm;
						refUnit = target.getUnit();
						break;
					}
				}
			}

			if (target == null) { // if we haven't found within the previous cases
				// We need to keep expanding the ContextMethods
				boolean keepSearching = true;
				int iteration = 0;
				int maxIterations = 10;

				while (keepSearching && iteration < maxIterations) {
					iteration++;
					List<SootMethod> backwards = expandOneLevelBackward(contextMethods, processed, pdmrLocal,
							ExtraTypes.PUT_DATA_MAP_REQUEST);
					for (SootMethod sm : backwards) {
						target = searchVariableDefinition(null, pdmrLocal, sm, ExtraTypes.PUT_DATA_MAP_REQUEST,
								"create", contextResult);
						if (target != null) {
							contextMethod = sm;
							refUnit = target.getUnit();
							keepSearching = false;
							break;
						}
					}
					contextMethods.addAll(backwards);// add initial to processed

					if (keepSearching) {

						List<SootMethod> forwards = expandOneLevelForward(contextMethods, processed, pdmrLocal,
								ExtraTypes.PUT_DATA_MAP_REQUEST);
						for (SootMethod sm : forwards) {
							target = searchVariableDefinition(null, pdmrLocal, sm, ExtraTypes.PUT_DATA_MAP_REQUEST,
									"create", contextResult);
							if (target != null) {
								contextMethod = sm;
								refUnit = target.getUnit();
								keepSearching = false;
								break;
							}
						}
						contextMethods.addAll(forwards);
					}
				}
			}

			if (target == null)
				return null;
			// found create Channel!
			AssignStmt asigStmt = (AssignStmt) target.getUnit();
			Value leftOp = asigStmt.getLeftOp();
			channel = (Local) leftOp;
			result = new IntermediateResult(contextMethods, channel);
			result.setCurrentMethod(contextMethod);
			result.setCurrentUnit(refUnit);

		}
		return result;

	}

	private List<SootMethod> expandOneLevelForward(LinkedHashSet<SootMethod> methodCallers, List<SootMethod> processed,
			Local putDataReq, String dataType) {

		List<SootMethod> newCallees = new ArrayList<SootMethod>();
		CallGraph cg = Scene.v().getCallGraph();
		for (SootMethod contextMethod : methodCallers) {
			Iterator<Edge> iter = cg.edgesOutOf(contextMethod);
			while (iter.hasNext()) {
				SootMethod callee = iter.next().tgt();
				if (!processed.contains(callee) && !isMain(callee) && isAppComponent(callee)) {
					if (fullComponent) // there is a relevant class variable. Need to add everything
						newCallees.add(callee);
					else if (isRelevant(callee, dataType)) // check if the context is relevant
						newCallees.add(callee);

					processed.add(callee);
				}
			}
		}
		return newCallees;
	}

	private List<SootMethod> expandOneLevelBackward(LinkedHashSet<SootMethod> contextMethods,
			List<SootMethod> discovered, Local localRegister, String dataType) {
		List<SootMethod> newCallers = new ArrayList<SootMethod>();
		CallGraph cg = Scene.v().getCallGraph();
		for (SootMethod contextMethod : contextMethods) {
			Iterator<Edge> iter = cg.edgesInto(contextMethod);
			while (iter.hasNext()) {
				SootMethod caller = iter.next().src();
				if (!discovered.contains(caller) && !isMain(caller) && isAppComponent(caller)) {
					if (fullComponent) // there is a relevant class variable. Need to add everything
						newCallers.add(caller);
					else if (isRelevant(caller, dataType)) // check if the context is relevant
						newCallers.add(caller);

					discovered.add(caller);
					;
				}
			}
		}
		return newCallers;
	}

	private boolean isRelevant(SootMethod method, String dataType) {
		if (method.hasActiveBody()) {
			for (Local local : method.getActiveBody().getLocals()) {
				if (dataType.equals(local.getType().toString()))
					return true;
			}
		}
		return false;
	}

	/**
	 * Check if the package name of the class that contains the method matches with
	 * the package name defined in the manifest
	 * 
	 * @param sm
	 * @return
	 */
	private boolean isAppComponent(SootMethod sm) {

		if (sm.getDeclaringClass().getPackageName().startsWith("com.google.android.gms")
				|| sm.getDeclaringClass().getPackageName().startsWith("android.support.wearable")
				|| sm.getDeclaringClass().isJavaLibraryClass() || utilInstance.isAndroidLibrary(sm.getDeclaringClass()))
			return false;

		return true;

	}

	private Local getBaseRegister(InvokeExpr iexpr, int index) {

		Local local = (Local) iexpr.getUseBoxes().get(index).getValue();
		return local;
	}

	/**
	 * Searches the definition of a variable within a method specified by a target
	 * function Return null is variable not found
	 * 
	 * @param unit
	 * @param variable
	 * @param contextMethod
	 * @param contextResult
	 */
	private PairResult<Unit, InvokeExpr> searchVariableDefinition(Unit unit, Local variable, SootMethod contextMethod,
			String type, String targetFunction, IntermediateResult contextResult) {
		PairResult<Unit, InvokeExpr> result = null;
		if (unit != null) {
			UnitGraph cfg = new ExceptionalUnitGraph(contextMethod.retrieveActiveBody());
			SimpleLocalDefs slocalDef = new SimpleLocalDefs(cfg);
			for (Unit defUnit : slocalDef.getDefsOfAt(variable, unit)) {
				logger.debug(defUnit.toString());
				InvokeExpr iexpr = parseUnit(defUnit);
				if (iexpr == null)
					continue;
				else if (iexpr.getMethod().getName().equals(targetFunction)) {
					result = new PairResult<Unit, InvokeExpr>(defUnit, iexpr);
					break;
				} // if specialIvoke and return local is relevant -> expand to that context
				else if (SpecialInvokeExpr.class.isAssignableFrom(iexpr.getClass())) {
					Value left = getLeftValueFromAssig(defUnit);
					if (left == null)
						continue;
					// when looking the create method if the left value is PDMR and the stmt is
					// calling another function we search within this function
					if (left.getType().toString().equals(ExtraTypes.PUT_DATA_MAP_REQUEST)
							&& targetFunction.contains("create")) {
						SootMethod newContext = iexpr.getMethod();
						contextResult.setActive(true);
						contextResult.setCurrentMethod(newContext);
						contextResult.setRegister((Local) left);
						contextResult.setCurrentUnit(defUnit);
						break;
					}
				}
			}
		} else { // we are doing a inter-procedural search, no reference of the unit, only the
					// local in the best case
			for (Unit tmpUnit : contextMethod.getActiveBody().getUnits()) {
				logger.debug(tmpUnit.toString());
				InvokeExpr iexpr = parseUnit(tmpUnit);
				if (iexpr == null)
					continue;
				if (iexpr.getMethod().getName().equals(targetFunction)) {
					// add if for each targetFunction, if variable == null?
					if (targetFunction.contains("create") && type.equals(ExtraTypes.PUT_DATA_MAP_REQUEST)) {
						result = new PairResult<Unit, InvokeExpr>(tmpUnit, iexpr);
						break;
					} else {
						int count = iexpr.getArgCount();
						Local base = (Local) iexpr.getUseBoxes().get(count).getValue();
						if (base.getType().toString().equals(type)) {
							result = new PairResult<Unit, InvokeExpr>(tmpUnit, iexpr);
							break;
						}
					}
				}
			}
		}
		return result;
	}

	private InvokeExpr parseUnit(Unit unit) {
		if (AssignStmt.class.isAssignableFrom(unit.getClass())) {
			AssignStmt asigStmt = (AssignStmt) unit;
			Value rightOp = asigStmt.getRightOp();
			if (InvokeExpr.class.isAssignableFrom(rightOp.getClass())) {
				InvokeExpr iexpr = (InvokeExpr) rightOp;
				return iexpr;
			}
		} else if (InvokeStmt.class.isAssignableFrom(unit.getClass())) {
			InvokeExpr iexpr = ((InvokeStmt) unit).getInvokeExpr();
			return iexpr;
		}

		return null;
	}

	private Value getLeftValueFromAssig(Unit unit) {
		Value left = null;
		if (AssignStmt.class.isAssignableFrom(unit.getClass())) {
			AssignStmt asigStmt = (AssignStmt) unit;
			left = asigStmt.getLeftOp();
		}
		return left;
	}

	/**
	 * Check if there is a field of some specific types in the class like
	 * PutDataMapRequest. In this cases we need consider the full sequence of
	 * methods call in the analysis
	 * 
	 * @param component
	 * @return
	 */
	private boolean existClassVariable(SootClass component) {
		for (SootField classField : component.getFields()) {
			if (classField.getType().toString().equals(ExtraTypes.PUT_DATA_MAP_REQUEST)
					|| classField.getType().toString().equals(ExtraTypes.PUT_DATA_REQUEST)
					|| classField.getType().toString().equals(ExtraTypes.DATA_MAP))
				return true;
		}
		return false;
	}

	private static boolean isMain(SootMethod sm) {
		if (sm.getName().contains("dummyMainMethod"))
			return true;
		if (sm.isPublic() && sm.getName().equals("init") && sm.getReturnType().toString().equals(ExtraTypes.VOID_TYPE))
			return true;

		return false;
	}

	public static List<SootMethod> getMethodStack() {
		return methodStack;
	}

	public static void setMethodStack(List<SootMethod> methodStack) {
		InterProceduralDataItemAnalysis.methodStack = methodStack;
	}

	public List<DataItemSearchResult> getResults() {
		return searchResults;
	}

	public void setResults(List<DataItemSearchResult> results) {
		this.searchResults = results;
	}

	class IntermediateResult {
		LinkedHashSet<SootMethod> contextMethods;
		Unit currentUnit;
		SootMethod currentMethod;
		Local register;
		boolean active;

		public IntermediateResult() {
			this.active = false;
		}

		public boolean getActive() {
			return this.active;
		}

		public void setActive(boolean value) {
			this.active = value;
		}

		public IntermediateResult(LinkedHashSet<SootMethod> contextMethods, Local register) {
			this.contextMethods = contextMethods;
			this.register = register;
			this.active = false;

		}

		public SootMethod getCurrentMethod() {
			return currentMethod;
		}

		public void setCurrentUnit(Unit unit) {
			currentUnit = unit;
		}

		public void setCurrentMethod(SootMethod sm) {
			this.currentMethod = sm;
		}

		public LinkedHashSet<SootMethod> getContextMethods() {
			return contextMethods;
		}

		public void setContextMethods(LinkedHashSet<SootMethod> contextMethods) {
			this.contextMethods = contextMethods;
		}

		public Unit getCurrentUnit() {
			return currentUnit;
		}

		public Local getRegister() {
			return register;
		}

		public void setRegister(Local register) {
			this.register = register;
		}

	}

}
