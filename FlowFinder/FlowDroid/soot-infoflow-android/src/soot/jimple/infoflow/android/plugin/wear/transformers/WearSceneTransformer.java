package soot.jimple.infoflow.android.plugin.wear.transformers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Body;
import soot.Local;
import soot.Modifier;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.infoflow.android.plugin.wear.analysis.AnalysisUtil;
import soot.jimple.infoflow.android.plugin.wear.analysis.ChannelAnalysis;
import soot.jimple.infoflow.android.plugin.wear.analysis.DataItemAnalysis;
import soot.jimple.infoflow.android.plugin.wear.analysis.DataItemSearchResult;
import soot.jimple.infoflow.android.plugin.wear.analysis.DataMapAnalysis;
import soot.jimple.infoflow.android.plugin.wear.analysis.InterProceduralDataItemAnalysis;
import soot.jimple.infoflow.android.plugin.wear.analysis.MessageEventAnalysis;
import soot.jimple.infoflow.android.plugin.wear.exception.DataTypeNotFoundException;
import soot.jimple.infoflow.android.plugin.wear.generators.ChannelClientGenerator;
import soot.jimple.infoflow.android.plugin.wear.generators.DataItemSinkGenerator;
import soot.jimple.infoflow.android.plugin.wear.generators.DataMapSourceGenerator;
import soot.jimple.infoflow.android.plugin.wear.generators.MessageEventSourceGenerator;
import soot.jimple.infoflow.android.usc.sql.string.StringResult;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;
import soot.jimple.infoflow.plugin.wear.extras.Keys;

public class WearSceneTransformer extends SceneTransformer {

	private static HashMap<String, Set<String>> manifestServices;
	private SootClass tmp;
	private Unit exUnit;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private static DataItemSinkGenerator sinkGenerator;
	private static DataMapSourceGenerator sourcesGenerator;
	private static MessageEventSourceGenerator messageSourceGenerator;
	private static ChannelClientGenerator channelGenerator;
	private static List<StringResult> stringValues;
	private static AnalysisUtil utilInstance;
	public static String packageNameStart;
	private List<SootClass> toInstrument;

	public WearSceneTransformer(HashMap<String, Set<String>> manifest, List<StringResult> stringAnalysisResults,
			String appPackage, List<SootClass> componentsToInstrument, String apkPath) {
		toInstrument = componentsToInstrument;
		manifestServices = manifest;
		stringValues = stringAnalysisResults;
		sinkGenerator = DataItemSinkGenerator.getInstance();
		sourcesGenerator = DataMapSourceGenerator.getInstance();
		messageSourceGenerator = MessageEventSourceGenerator.getInstance();
		channelGenerator = ChannelClientGenerator.getInstance();
		utilInstance = AnalysisUtil.getInstance();

	}

	@Override
	public void internalTransform(String phaseName, Map<String, String> options) {

		logger.info("starting the instrumentation");
		try {
			instrumentBaseClasses();
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		int countIntrumented = 0;

		List<String> visitedClasses = new ArrayList<>();

		for (SootClass sClass : toInstrument) {
			boolean instrumented = false;

			if (!visitedClasses.contains(sClass.getName()))
				visitedClasses.add(sClass.getName());
			else
				continue;

			tmp = sClass;

			for (SootMethod sMethod : sClass.getMethods()) {
				if (sMethod.isConcrete() && utilInstance.instrumentationNeeded(sMethod)) {
					Body body = sMethod.retrieveActiveBody();
					for (Iterator<Unit> i = body.getUnits().snapshotIterator(); i.hasNext();) {
						try {
							Unit unit = i.next();
							exUnit = unit; // debugging information
							Stmt stmt = (Stmt) unit;
							if (stmt.containsInvokeExpr()) {
								InvokeExpr iexp = (InvokeExpr) stmt.getInvokeExpr();
								String declaringClass = iexp.getMethod().getDeclaringClass().toString();
								// logger.info("DECLARING CLASS");
								// logger.info(declaringClass);

								if (declaringClass.equals(ExtraTypes.DATA_CLIENT)
										|| declaringClass.equals(ExtraTypes.DATA_API)) {
									if (iexp.getMethod().getName().equals("putDataItem")) {

										instrumented = true;
										// logger.info("FOUND putDataItem");

										Local dataClientLocal = null;
										Boolean oldApi = isOldApi(declaringClass);
										int argPosition = 0;
										if (oldApi == true) {
											// logger.info("OLD API");
											argPosition = 1;
											dataClientLocal = utilInstance.addLocal(ExtraTypes.DATA_CLIENT, sMethod);

										}

										DataItemAnalysis analysis = new DataItemAnalysis(unit, sMethod, stringValues);
										analysis.runDataItemAnalysis(iexp, argPosition);

										List<DataItemSearchResult> results = analysis.getResults();
										if (results != null && results.size() > 0) {
											// logger.info("RESULTS FOUND");
											for (DataItemSearchResult res : results) {

												if (!res.getRegister().getValue().toString().contains("null")) {

													Local tmpLocal = utilInstance.addLocal(res.getRegister(), sMethod);
													res.setIntrumentedRegister(tmpLocal);
													AssignStmt astm = Jimple.v().newAssignStmt(tmpLocal,
															res.getRegister().getValue());
													body.getUnits().insertAfter(astm, res.getUnit());

													InvokeStmt newSinkExpr = sinkGenerator.generateSinkCall(res, iexp,
															oldApi, dataClientLocal, false);
													body.getUnits().insertAfter(newSinkExpr, unit);
												}
											}
										} else {
											// logger.info("RESULTS NOT FOUND");
											InterProceduralDataItemAnalysis interAnalysis = new InterProceduralDataItemAnalysis(
													body, unit);
											boolean differentContexts = false;
											interAnalysis.runInterProceduralAnalysis(iexp, argPosition);
											List<DataItemSearchResult> resultsICFG = interAnalysis.getResults();

											if (resultsICFG != null && resultsICFG.size() > 0) {
												// logger.info("RESULTS NOT EMPTY");
												for (DataItemSearchResult res : resultsICFG) {
													// logger.info("RESULT LOOP");
													if (!res.getRegister().getValue().toString().contains("null")) {
														// logger.info("NOT NULL");
														if (!sMethod.getSignature()
																.equals(res.getcontextMethod().getSignature())) {
															// logger.info("ADDING INSTRUMENTATION");

															differentContexts = true;

															SootField newClassField = utilInstance
																	.addGlobalVar(res.getRegister(), sClass);

															StaticFieldRef fieldRef = Jimple.v()
																	.newStaticFieldRef(newClassField.makeRef());
															sinkGenerator.instrumentFieldAssignment(res, fieldRef);
															// assign static reference with the newLocal
															Local newLocal = utilInstance.addLocal(res.getRegister(),
																	sMethod);
															AssignStmt newAssignStmt = Jimple.v()
																	.newAssignStmt(newLocal, fieldRef);
															res.setIntrumentedRegister(newLocal);
															body.getUnits().insertBefore(newAssignStmt, unit);
														}

														InvokeStmt newSinkExpr = sinkGenerator.generateSinkCall(res,
																iexp, oldApi, dataClientLocal, differentContexts);
														body.getUnits().insertAfter(newSinkExpr, unit);
													}
												}

											} else
												logger.info(
														"DataItem definiton could not be found in the interprocedural analysis. Ignoring the following call"
																+ unit.toString());
										}
									}
								}
								/*
								 * If the app is receiving a DataItem
								 */
								else if (declaringClass.equals(ExtraTypes.DATA_MAP)
										&& !iexp.getMethod().getName().equals("get")
										&& iexp.getMethod().getName().contains("get")) {
									instrumented = true;

									DataMapAnalysis analysis = new DataMapAnalysis(sMethod, unit, manifestServices);
									analysis.runDataMapAnalysis(iexp);

									String path = (analysis.getPathValue() == null ? Keys.GENERAL_PATH
											: analysis.getPathValue().toString());

									int line = utilInstance.getLineNumber(unit);
									int offset = utilInstance.getOffset(unit);

									String key = utilInstance.getStringValue(sMethod, unit, line, offset, 1);
									if (key == null || key.contains("Unknown")) {
										Value rv = iexp.getArg(0);
										if (rv instanceof StringConstant)
											key = rv.toString().replace("\"", "");
										else
											key = Keys.GENERAL_KEY;
									}
									InvokeStmt newSourcekStmt = sourcesGenerator.generateSourceCall(path, key, iexp);
									if (stmt.getDefBoxes().size() == 0)
										continue; // unused stmt
									Value targetRegister = stmt.getDefBoxes().get(0).getValue();
									AssignStmt newAssignStmt = Jimple.v().newAssignStmt(targetRegister,
											newSourcekStmt.getInvokeExpr());

									body.getUnits().insertAfter(newAssignStmt, unit);
									body.getUnits().remove(unit);
								}
								/*
								 * CASE: we receive a Message
								 */

								else if (declaringClass.equals(ExtraTypes.MESSAGE_EVENT)
										&& iexp.getMethod().getName().equals("getData")) {
									instrumented = true;

									MessageEventAnalysis messageAnalysis = new MessageEventAnalysis(sMethod, unit,
											manifestServices);
									messageAnalysis.runAnalysis(iexp);
									String path = (messageAnalysis.getPathValue() == null ? Keys.GENERAL_PATH
											: messageAnalysis.getPathValue());

									InvokeStmt newSourcekExpr = messageSourceGenerator.generateSourceCall(path, iexp);
									Value baseRegister = stmt.getDefBoxes().get(0).getValue();
									AssignStmt newAssignStmt = Jimple.v().newAssignStmt(baseRegister,
											newSourcekExpr.getInvokeExpr());
									body.getUnits().insertAfter(newAssignStmt, unit);
									body.getUnits().remove(unit); //

								}
								// in case we find a sendMessage
								else if ((declaringClass.equals(ExtraTypes.MESSAGE_CLIENT)
										|| (declaringClass.equals(ExtraTypes.MESSAGE_API)))
										&& iexp.getMethod().getName().equals("sendMessage")) {

									instrumented = true;

									Boolean oldApi = isOldApi(declaringClass);
									int argPosition = 2;
									int argExpr = 1;
									// index starts with 1
									if (oldApi) {
										argPosition = 2;
										argExpr = 2;
									}

									Value rv = iexp.getArg(argExpr);
									if (rv instanceof StringConstant) {
										StringConstant cons = (StringConstant) rv;
										iexp.setArg(argExpr, cons);
										continue;
									}

									int line = utilInstance.getLineNumber(unit);
									int offset = utilInstance.getOffset(unit);
									String path = utilInstance.getStringValue(sMethod, unit, line, offset, argPosition);
									if (path == null || path.contains("Unknown"))
										path = Keys.GENERAL_PATH;

									iexp.setArg(argExpr, StringConstant.v(path));

								}

								else if (declaringClass.equals(ExtraTypes.CHANNEL_CLIENT)) {
									// logger.info("METHOD");
									// logger.info(iexp.getMethod().getName());

									if (iexp.getMethod().getName().equals("getInputStream")
											|| iexp.getMethod().getName().equals("receiveFile")) {
										instrumented = true;

										ChannelAnalysis channelAnalysis = new ChannelAnalysis(sMethod, unit,
												manifestServices);
										channelAnalysis.runAnalysis(iexp, "source");
										String path = (channelAnalysis.getPathValue() == null ? Keys.GENERAL_PATH
												: channelAnalysis.getPathValue().toString());

										InvokeStmt newSourcekStmt = channelGenerator.generateSourceCall(path, iexp);
										Value uri = iexp.getArg(1);
										Value baseChannelClient = iexp.getUseBoxes().get(3).getValue();
										AssignStmt newAssign = channelGenerator.callTaintWrapper(uri, baseChannelClient,
												path);
										body.getUnits().insertAfter(newSourcekStmt, unit);
										body.getUnits().insertAfter(newAssign, newSourcekStmt);
										body.getUnits().remove(unit);

									} else if (iexp.getMethod().getName().equals("getOutputStream")
											|| iexp.getMethod().getName().equals("sendFile")) {

										instrumented = true;
										ChannelAnalysis channelAnalysis = new ChannelAnalysis(sMethod, unit,
												manifestServices);
										channelAnalysis.runAnalysis(iexp, "sink");
										String path = (channelAnalysis.getPathValue() == null ? Keys.GENERAL_PATH
												: channelAnalysis.getPathValue());
										InvokeStmt newSinkStmt = channelGenerator.generateSinkCall(path, iexp);

										body.getUnits().insertAfter(newSinkStmt, unit);
										body.getUnits().remove(unit);
									}
								}
							}
						} catch (Exception e) {
							logger.error("Error in class" + tmp.getName());
							logger.error("Unit: " + exUnit.toString());

							e.printStackTrace();
						}

					}
					try {
						body.validate();
					} catch (Exception e) {
						logger.error("Error in class" + tmp.getName());
						logger.error("Unit: " + exUnit.toString());

						e.printStackTrace();
					}
				} // end if instrumentationNeeded()

			} // end for methods in class

			if (instrumented) {
				logger.debug("Component Instrumented: " + sClass.toString());
				countIntrumented++;
			}

		}

		if (countIntrumented > 0)
			logger.info("Components Instrumented: " + countIntrumented);
		else {
			logger.info("Program terminated, no component instrumented from candidates");
			System.exit(0);
		}

	}

	private void instrumentBaseClasses() throws DataTypeNotFoundException {

		// Generate sinks for DataClients
		SootClass dataClientClass = Scene.v().getSootClass(ExtraTypes.DATA_CLIENT);
		if (dataClientClass != null && !dataClientClass.isPhantom()) {
			sinkGenerator.generateNewDataClientSinks();

		} else {
			SootClass sClass = new SootClass(ExtraTypes.DATA_CLIENT, Modifier.PUBLIC);
			sClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
			Scene.v().addClass(sClass);
			sinkGenerator.setClass(sClass);
			sinkGenerator.generateNewDataClientSinks();
		}

		// generate sources for DataMaps
		SootClass sClass = Scene.v().getSootClass(ExtraTypes.DATA_MAP);
		if (sClass != null && !sClass.isPhantom()) {
			sourcesGenerator.generateDataMapSources();

		} else {
			SootClass dmClass = new SootClass(ExtraTypes.DATA_MAP, Modifier.PUBLIC);
			dmClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
			Scene.v().addClass(dmClass);
			sourcesGenerator.setSclass(dmClass);
			sourcesGenerator.generateDataMapSources();
		}

		SootClass messageClass = Scene.v().getSootClass(ExtraTypes.MESSAGE_EVENT);
		if (messageClass != null && !messageClass.isPhantom()) {
			messageSourceGenerator.generateMessageEventSources();
		} else {
			SootClass mClass = new SootClass(ExtraTypes.MESSAGE_EVENT, Modifier.PUBLIC);
			mClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
			Scene.v().addClass(mClass);
			messageSourceGenerator.setClass(mClass);
			messageSourceGenerator.generateMessageEventSources();

		}

		SootClass channelClass = Scene.v().getSootClass(ExtraTypes.CHANNEL_CLIENT);
		if (channelClass != null && !channelClass.isPhantom()) {
			channelGenerator.generateChannelSources();
			channelGenerator.generateChannelSinks();
			channelGenerator.generateTaintWrapper();
		} else {
			SootClass cClass = new SootClass(ExtraTypes.CHANNEL_CLIENT, Modifier.PUBLIC);
			cClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
			Scene.v().addClass(cClass);
			channelGenerator.setClass(cClass);
			channelGenerator.generateChannelSources();
			channelGenerator.generateChannelSinks();
			channelGenerator.generateTaintWrapper();

		}

	}

	private boolean isOldApi(String className) {
		List<String> oldApis = new ArrayList<String>(Arrays.asList(ExtraTypes.oldApis));
		if (oldApis.contains(className))
			return true;
		else
			return false;
	}

}
