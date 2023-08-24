package soot.jimple.infoflow.android.plugin.wear.transformers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sound.midi.Instrument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Local;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.ValueBox;
import soot.Body;
import soot.jimple.Stmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.infoflow.android.plugin.wear.analysis.AnalysisUtil;
import soot.jimple.infoflow.android.plugin.wear.deofucator.CallbacksDeobfuscator;
import soot.jimple.infoflow.android.plugin.wear.deofucator.CapabilityApiDeobfuscator;
import soot.jimple.infoflow.android.plugin.wear.deofucator.CapabilityClientDeobfuscator;
import soot.jimple.infoflow.android.plugin.wear.deofucator.DataApiDeobfuscator;
import soot.jimple.infoflow.android.plugin.wear.deofucator.DataClientDeobfuscator;
import soot.jimple.infoflow.android.plugin.wear.deofucator.DataEventDeobfuscator;
import soot.jimple.infoflow.android.plugin.wear.deofucator.DataItemDeobfuscator;
import soot.jimple.infoflow.android.plugin.wear.deofucator.DataMapDeobfuscator;
import soot.jimple.infoflow.android.plugin.wear.deofucator.DataMapItemDeobfuscator;
import soot.jimple.infoflow.android.plugin.wear.deofucator.Deofuscator;
import soot.jimple.infoflow.android.plugin.wear.deofucator.MessageApiDeofuscator;
import soot.jimple.infoflow.android.plugin.wear.deofucator.MessageClientDeofuscator;
import soot.jimple.infoflow.android.plugin.wear.deofucator.MessageEventDeobfuscator;
import soot.jimple.infoflow.android.plugin.wear.deofucator.PutDMRDeobfuscator;
import soot.jimple.infoflow.android.plugin.wear.deofucator.PutDRDeobfuscator;
import soot.jimple.infoflow.android.plugin.wear.deofucator.WearableServiceObfuscator;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;
import soot.util.Chain;

import soot.Type;
import soot.ArrayType;
import soot.FloatType;
import java.util.Collections;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import java.lang.reflect.Modifier;
import soot.SootFieldRef;
import soot.jimple.InstanceFieldRef;
import soot.RefType;
import soot.jimple.FieldRef;
import soot.jimple.AssignStmt;
import soot.jimple.StaticInvokeExpr;

public class DeobfuscatorSceneTransformer extends SceneTransformer {

	private static AnalysisUtil utilInstance = AnalysisUtil.getInstance();
	private List<SootClass> relevantComponents;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	String apkFilePath;
	int nonFound = 0;

	public DeobfuscatorSceneTransformer(String targetAPKFile) {
		apkFilePath = targetAPKFile;
	}

	@Override
	protected void internalTransform(String phaseName, Map<String, String> options) {

		// Force resolve SensorEvent
		// Scene.v().forceResolve("android.hardware.SensorEvent", SootClass.BODIES);

		// imeiTransformation();
		// Scene.v().loadClass("com.frost.Imei", SootClass.BODIES);
		// Frost SensorEvent detection
		// addGetSensorDataMethod();
		// sensorEventDetection();

		relevantComponents = getRelevantComponents();
		logger.info("RELEVANT COMPONENTS");
		logger.info(relevantComponents.toString());
		boolean obf = deobfuscateLibraryClasses();
		if (obf == true) {
			deobfuscateComponents(relevantComponents);
		}

		if (relevantComponents.size() == 0) {
			logger.info("Program terminated, no components to instrument found");
			System.exit(0);
		}

	}

	public List<SootClass> getComponentsToInstrument() {
		return relevantComponents;

	}

	// Frost SensorEvent detection
	private void sensorEventDetection() {
		logger.info("Frost sensorEventDetection");
		for (SootClass sClass : Scene.v().getApplicationClasses()) {
			
			if (sClass.getPackageName().startsWith("com.google.android") || sClass.getPackageName().startsWith("androidx")
					|| sClass.getPackageName().startsWith("android.support.wearable") || sClass.isJavaLibraryClass()
					|| utilInstance.isAndroidLibrary(sClass) || sClass.getPackageName().startsWith("kotlinx") || sClass.getPackageName().startsWith("kotlin")
				) {
				continue; }

			if (sClass.implementsInterface(ExtraTypes.SENSOR_EVENT_LISTENER)) {
				SootMethod onSensorChangeMethod = sClass.getMethodByName("onSensorChanged");
		
				// Get the list of statements
				Chain<Unit> units = onSensorChangeMethod.retrieveActiveBody().getUnits();
				// Iterate through the list of statements
				for (Unit unit : units) {
					// Get sensorEvent values access
					if (unit.toString().contains(".<android.hardware.SensorEvent: float[] values>")) {
						// Instrument Assignment Statement
						if (unit instanceof AssignStmt) {
							// Get frost SensorEvent class
							SootClass frostSensorEventClass = Scene.v().getSootClass("com.frost.SensorEvent");
							// Get getSensorData method
							SootMethod getSensorDataMethod = frostSensorEventClass.getMethodByName("getSensorData");
							// Cast Unit to AssignStmt
							AssignStmt targetAssignStmt = (AssignStmt) unit;
							// Create a new statement to replace the old one
							StaticInvokeExpr newStaticInvoke = Jimple.v().newStaticInvokeExpr(getSensorDataMethod.makeRef());
							// Replace rhs with invoke expression
							targetAssignStmt.setRightOp(newStaticInvoke);
							// break;
						}
					}
				}

				// Iterator<Unit> statementIterator = onSensorChangeMethod.retrieveActiveBody().getUnits().iterator();
				// while(statementIterator.hasNext()) {
				// 	Unit statement = statementIterator.next();
				// 	if (statement.toString().contains(".<android.hardware.SensorEvent: float[] values>")) {
				// 		if (statement instanceof AssignStmt) {
				// 			// Instrument Assignment Statement
				// 			logger.info(statement.toString()  + " " + statement.getClass().toString());
				// 			logger.info("SensorEvent found in class: " + sClass.getName());
				// 		}
				// 	}
				// }
				
				// for (ValueBox vb : onSensorChangeMethod.retrieveActiveBody().getUseBoxes()) {
				// 	if (vb.getValue().toString().contains(".<android.hardware.SensorEvent: float[] values>")) {
				// 		logger.info("SensorEvent found in class: " + sClass.getName());
				// 		logger.info("SensorEvent access: " + vb.getValue().toString());
				// 	}
				// }
			}
		}
	}

	private void addGetSensorDataMethod() {
		// Make Sensor Class
		SootClass sensorClass = new SootClass("com.frost.SensorEvent", Modifier.PUBLIC);
		SootField myField = new SootField("values", ArrayType.v(FloatType.v(), 1), Modifier.PUBLIC | Modifier.FINAL);
		sensorClass.addField(myField);

		// Phantom Class
		// SootClass sensorClass = Scene.v().getSootClass(ExtraTypes.SENSOR_EVENT);
		// SootField myField = sensorClass.getFieldByName("values");

		// Create Dummy Method
		SootMethod myNewMethod = new SootMethod("getSensorData", new ArrayList<Type>(), ArrayType.v(FloatType.v(), 1), Modifier.PUBLIC | Modifier.STATIC);
		myNewMethod.setDeclaringClass(sensorClass);
		sensorClass.addMethod(myNewMethod);

		// Get Method from Class and set body
		JimpleBody body = Jimple.v().newBody(myNewMethod);
		myNewMethod.setActiveBody(body);

		// Add local variables
		Local thisLocal = Jimple.v().newLocal("this", sensorClass.getType());
		body.getLocals().add(thisLocal);
		Local fieldLocal = Jimple.v().newLocal("fieldLocal", myField.getType());
		body.getLocals().add(fieldLocal);

		// Add identity statements
		body.getUnits().add(Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(myNewMethod.getDeclaringClass().getType())));
		
		// Create a field reference to 'myField'
		FieldRef fieldRef = Jimple.v().newInstanceFieldRef(thisLocal, myField.makeRef());

		// Create an assignment statement to assign the value of 'myField' to 'fieldLocal'
		AssignStmt assignStmt = Jimple.v().newAssignStmt(fieldLocal, fieldRef);
		body.getUnits().add(assignStmt);

		// Return the value of 'fieldLocal'
		Unit returnStmt = Jimple.v().newReturnStmt(fieldLocal);
		body.getUnits().add(returnStmt);
	}

	private void imeiTransformation() {
		addDummyGetImeiMethod();
		instrumentGetImei();
	}

	private void instrumentGetImei() {
		logger.info("Frost instrumentGetImei");
		for (SootClass sClass : Scene.v().getApplicationClasses()) {
			
			if (sClass.getPackageName().startsWith("com.google.android") || sClass.getPackageName().startsWith("androidx")
					|| sClass.getPackageName().startsWith("android.support.wearable") || sClass.isJavaLibraryClass()
					|| utilInstance.isAndroidLibrary(sClass) || sClass.getPackageName().startsWith("kotlinx") || sClass.getPackageName().startsWith("kotlin")
				) {
				continue; }

			if (sClass.implementsInterface(ExtraTypes.SENSOR_EVENT_LISTENER)) {
				SootMethod onSensorChangeMethod = sClass.getMethodByName("onCreate");
		
				// Get the list of statements
				Chain<Unit> units = onSensorChangeMethod.retrieveActiveBody().getUnits();
				// Iterate through the list of statements
				for (Unit unit : units) {
					// Get sensorEvent values access
					if (unit.toString().contains(".<android.telephony.TelephonyManager: java.lang.String getImei()")) {
						// Instrument Assignment Statement
						if (unit instanceof AssignStmt) {
							// Get frost SensorEvent class
							SootClass frostSensorEventClass = Scene.v().getSootClass("com.frost.Imei");
							// Get getSensorData method
							SootMethod getSensorDataMethod = frostSensorEventClass.getMethodByName("getPhoneData");
							// Cast Unit to AssignStmt
							AssignStmt targetAssignStmt = (AssignStmt) unit;
							// Create a new statement to replace the old one
							StaticInvokeExpr newStaticInvoke = Jimple.v().newStaticInvokeExpr(getSensorDataMethod.makeRef());
							// Replace rhs with invoke expression
							targetAssignStmt.setRightOp(newStaticInvoke);
							// break;
						}
					}
				}
			}
		}
	}

	private void addDummyGetImeiMethod() {
		// Make Sensor Class
		SootClass sensorClass = new SootClass("com.frost.Imei", Modifier.PUBLIC);
		SootField myField = new SootField("values", RefType.v("java.lang.String"), Modifier.PUBLIC | Modifier.FINAL);
		sensorClass.addField(myField);

		// Phantom Class
		// SootClass sensorClass = Scene.v().getSootClass(ExtraTypes.SENSOR_EVENT);
		// SootField myField = sensorClass.getFieldByName("values");

		// Create Dummy Method
		SootMethod myNewMethod = new SootMethod("getPhoneData", new ArrayList<Type>(), RefType.v("java.lang.String"), Modifier.PUBLIC | Modifier.STATIC);
		myNewMethod.setDeclaringClass(sensorClass);
		sensorClass.addMethod(myNewMethod);

		// Get Method from Class and set body
		JimpleBody body = Jimple.v().newBody(myNewMethod);
		myNewMethod.setActiveBody(body);

		// Add local variables
		Local thisLocal = Jimple.v().newLocal("this", sensorClass.getType());
		body.getLocals().add(thisLocal);
		Local fieldLocal = Jimple.v().newLocal("fieldLocal", myField.getType());
		body.getLocals().add(fieldLocal);

		// Add identity statements
		body.getUnits().add(Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(myNewMethod.getDeclaringClass().getType())));
		
		// Create a field reference to 'myField'
		FieldRef fieldRef = Jimple.v().newInstanceFieldRef(thisLocal, myField.makeRef());

		// Create an assignment statement to assign the value of 'myField' to 'fieldLocal'
		AssignStmt assignStmt = Jimple.v().newAssignStmt(fieldLocal, fieldRef);
		body.getUnits().add(assignStmt);

		// Return the value of 'fieldLocal'
		Unit returnStmt = Jimple.v().newReturnStmt(fieldLocal);
		body.getUnits().add(returnStmt);
	}

	/**
	 * return true if the class extend from WearableListenerService or implements a
	 * Listener interface
	 * 
	 * @param sc
	 * @return
	 */
	private boolean hasCallbacks(SootClass sc) {
		if (sc.getSuperclass().toString().equals(ExtraTypes.WEARABLE_LISTENER_SERVICE))
			return true;
		if (sc.getInterfaceCount() > 0) {
			Chain<SootClass> interfaces = sc.getInterfaces();
			for (SootClass interf : interfaces) {
				switch (interf.getName()) {
				case ExtraTypes.DATA_LISTENER:
					return true;
				case ExtraTypes.ON_DATA_CHANGED_LISTENER:
					return true;
				case ExtraTypes.MESSAGE_LISTENER:
					return true;
				case ExtraTypes.ON_MESSAGE_RECEIVED_LISTENER:
					return true;
				case ExtraTypes.GOOGLE_API_CLIENT_CALLBACKS:
					return true;
				case ExtraTypes.CAPABILITY_API_LISTENER:
					return true;
				case ExtraTypes.CAPABILITY_CLIENT_ON_CHANGED_LISTENER:
					return true;
				default:
					return false;
				}
			}
		}

		return false;
	}

	private void deobfuscateComponents(List<SootClass> components) {
		Deofuscator deof = new Deofuscator();
		List<SootClass> processed = new ArrayList<>();
		for (int j = 0; j < components.size(); j++) {
			SootClass sc = components.get(j);

			if (processed.contains(sc))
				continue;
			else
				processed.add(sc);

			if (hasCallbacks(sc))
				deof.deobfuscateCallbacks(sc);

			List<SootMethod> processedMethods = new ArrayList<>();
			for (Iterator<SootMethod> it = sc.getMethods().listIterator(); it.hasNext();) {
				SootMethod sm = it.next();

				if (processedMethods.contains(sm))
					continue;
				else
					processedMethods.add(sm);

				if (sm.isConcrete() && utilInstance.instrumentationNeeded(sm)) {
					deof.deofucateMethod(sm);
				}
			}
		}
	}

	private boolean deobfuscateLibraryClasses() {

		boolean obfuscated = false;
		List<Boolean> obfChecker = new ArrayList<>();
		SootClass dataClientClass = Scene.v().getSootClass(ExtraTypes.DATA_CLIENT);
		if (dataClientClass != null && !dataClientClass.isPhantom()) {
			DataClientDeobfuscator dcDeob = DataClientDeobfuscator.getInstance();
			obfuscated = dcDeob.deobfuscateClass();
			obfChecker.add(obfuscated);
		} else
			nonFound++;

		SootClass dataClass = Scene.v().getSootClass(ExtraTypes.DATA_API);
		if (dataClass != null && !dataClass.isPhantom()) {
			DataApiDeobfuscator daDeob = DataApiDeobfuscator.getInstance();
			obfuscated = daDeob.deobfuscateClass();
			obfChecker.add(obfuscated);
		} else
			nonFound++;

		SootClass pdmrClass = Scene.v().getSootClass(ExtraTypes.PUT_DATA_MAP_REQUEST);
		if (pdmrClass != null && !pdmrClass.isPhantom()) {
			PutDMRDeobfuscator deob = PutDMRDeobfuscator.getInstance();
			obfuscated = deob.deobfuscateClass();
			obfChecker.add(obfuscated);
		} else
			nonFound++;

		SootClass pdrClass = Scene.v().getSootClass(ExtraTypes.PUT_DATA_REQUEST);
		if (pdrClass != null && !pdrClass.isPhantom()) {
			PutDRDeobfuscator deob = PutDRDeobfuscator.getInstance();
			obfuscated = deob.deobfuscateClass();
			obfChecker.add(obfuscated);
		} else
			nonFound++;

		// generate sources for DataMaps
		SootClass sClass = Scene.v().getSootClass(ExtraTypes.DATA_MAP);
		if (sClass != null && !sClass.isPhantom()) {
			DataMapDeobfuscator deob = DataMapDeobfuscator.getInstance();
			obfuscated = deob.deobfuscateDataMap();
			obfChecker.add(obfuscated);
		} else
			nonFound++;

		SootClass ditemClass = Scene.v().getSootClass(ExtraTypes.DATA_ITEM);
		if (ditemClass != null && !ditemClass.isPhantom()) {
			DataItemDeobfuscator dieobf = DataItemDeobfuscator.getInstance();
			obfuscated = dieobf.deobfuscateClass();
			obfChecker.add(obfuscated);
		} else
			nonFound++;

		SootClass dEventClass = Scene.v().getSootClass(ExtraTypes.DATA_EVENT);
		if (dEventClass != null && !dEventClass.isPhantom()) {
			DataEventDeobfuscator deobf = DataEventDeobfuscator.getInstance();
			obfuscated = deobf.deobfuscateClass();
			obfChecker.add(obfuscated);
		} else
			nonFound++;

		SootClass dmapitemClass = Scene.v().getSootClass(ExtraTypes.DATA_MAP_ITEM);
		if (dmapitemClass != null && !dmapitemClass.isPhantom()) {
			DataMapItemDeobfuscator dmapItemobf = DataMapItemDeobfuscator.getInstance();
			obfuscated = dmapItemobf.deobfuscateClass();
			obfChecker.add(obfuscated);
		} else
			nonFound++;

		SootClass mcClass = Scene.v().getSootClass(ExtraTypes.MESSAGE_CLIENT);
		if (mcClass != null && !mcClass.isPhantom()) {
			MessageClientDeofuscator messClientDeobf = MessageClientDeofuscator.getInstance();
			obfuscated = messClientDeobf.deofuscateMessageclient();
			obfChecker.add(obfuscated);
		} else
			nonFound++;

		SootClass maClass = Scene.v().getSootClass(ExtraTypes.MESSAGE_API);
		if (maClass != null && !maClass.isPhantom()) {
			MessageApiDeofuscator messApiDeof = MessageApiDeofuscator.getInstance();
			obfuscated = messApiDeof.deofuscateMessageApi();
			obfChecker.add(obfuscated);
		} else
			nonFound++;

		SootClass wsClass = Scene.v().getSootClass(ExtraTypes.WEARABLE_LISTENER_SERVICE);
		if (wsClass != null && !wsClass.isPhantom()) {
			WearableServiceObfuscator wsDeob = WearableServiceObfuscator.getInstance();
			obfuscated = wsDeob.deobfuscateClass();
			obfChecker.add(obfuscated);
		} else
			nonFound++;

		SootClass messageClass = Scene.v().getSootClass(ExtraTypes.MESSAGE_EVENT);
		if (messageClass != null && !messageClass.isPhantom()) {
			MessageEventDeobfuscator meDeobf = MessageEventDeobfuscator.getInstance();
			obfuscated = meDeobf.deofuscateMessageEvent();
			obfChecker.add(obfuscated);
		} else
			nonFound++;

		SootClass callbacks = Scene.v().getSootClass(ExtraTypes.GOOGLE_API_CLIENT_CALLBACKS);
		if (callbacks != null && !callbacks.isPhantom()) {
			CallbacksDeobfuscator cdeob = CallbacksDeobfuscator.getInstance();
			obfuscated = cdeob.deobfuscateGoogleApiClient();
			obfChecker.add(obfuscated);
		} else
			nonFound++;

		SootClass capaClient = Scene.v().getSootClass(ExtraTypes.CAPABILITY_CLIENT_ON_CHANGED_LISTENER);
		if (capaClient != null && !capaClient.isPhantom()) {
			CapabilityClientDeobfuscator ccdeob = CapabilityClientDeobfuscator.getInstance();
			obfuscated = ccdeob.deobfuscateClass();
			obfChecker.add(obfuscated);
		} else
			nonFound++;

		SootClass capi = Scene.v().getSootClass(ExtraTypes.CAPABILITY_API_LISTENER);
		if (capi != null && !capi.isPhantom()) {
			CapabilityApiDeobfuscator capideob = CapabilityApiDeobfuscator.getInstance();
			obfuscated = capideob.deobfuscateClass();
			obfChecker.add(obfuscated);
		} else
			nonFound++;

		obfuscated = false;
		for (Boolean b : obfChecker) {
			if (b == true)
				obfuscated = true;
		}
		return obfuscated;
	}

	private List<SootClass> getRelevantComponents() {

		List<SootClass> toInstrument = new ArrayList<SootClass>();
		List<String> relevantClasses = new ArrayList<String>(Arrays.asList(ExtraTypes.classesForInstrumentation));
		// logger.info("RELEVANT CLASSES");
		// logger.info(relevantClasses.toString());
		for (SootClass sClass : Scene.v().getApplicationClasses()) {
			if (sClass.getPackageName().startsWith("com.google.android.gms")
					|| sClass.getPackageName().startsWith("android.support.wearable") || sClass.isJavaLibraryClass()
					|| utilInstance.isAndroidLibrary(sClass))
				continue;

			Boolean searchMethods = true;
			// we only want to load classes defined by the user
			//logger.info("USER CLASS");
                        //logger.info(sClass.getPackageName());
			//logger.info("FIELDS");
			//logger.info(sClass.getFields().toString());
			//logger.info("METHODS");
			//logger.info(sClass.getMethods().toString());
			for (SootField f : sClass.getFields()) {
				if (relevantClasses.contains(f.getType().toString())) {
					toInstrument.add(sClass);
					searchMethods = false;
					//logger.info("MATCHED FIELD");
					//logger.info(f.getType().toString());
					break;
				}
			}
			if (searchMethods) {
				//logger.info("SEARCH METHODS");
				for (SootMethod sm : sClass.getMethods()) {
					if (sm.isConcrete()) {
						Chain<Local> locals = sm.retrieveActiveBody().getLocals();
						//logger.info("LOCALS");
						//logger.info(locals.toString());
						//logger.info("UNITS");
						//logger.info(sm.retrieveActiveBody().getUnits().toString());
						for (Local local : locals) {
							if (relevantClasses.contains(local.getType().toString())) {
								//logger.info("MATCHED");
								//logger.info(local.getType().toString());
								toInstrument.add(sClass);
								break;
							}
						}
						Body body = sm.retrieveActiveBody();
						for (Iterator<Unit> i = body.getUnits().snapshotIterator(); i.hasNext();) {
							try {
								Unit unit = i.next();
								//exUnit = unit; // debugging information
								Stmt stmt = (Stmt) unit;
								//logger.info("STMT");
								//logger.info(stmt.toString());
								if (stmt.containsInvokeExpr()) {
									//logger.info("IEXP FOUND");
									InvokeExpr iexp = (InvokeExpr) stmt.getInvokeExpr();
									String declaringClass = iexp.getMethod().getDeclaringClass().toString();
									//logger.info("IEXP CLASS");
									//logger.info(declaringClass);
									if (relevantClasses.contains(declaringClass)) {
                                                                		//logger.info("MATCHED");
                                                                		//logger.info(declaringClass);
                                                                		toInstrument.add(sClass);
                                                                		break;
                                                        		}
								}
							} catch (Exception e) {
								//logger.error("Error in class" + tmp.getName());
								//logger.error("Unit: " + exUnit.toString());

								e.printStackTrace();
							}
						}


					}
				}
			}

		}
		return (toInstrument);

	}

}
