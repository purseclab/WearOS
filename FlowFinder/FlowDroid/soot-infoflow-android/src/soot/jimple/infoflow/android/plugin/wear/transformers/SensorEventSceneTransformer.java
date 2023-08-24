package soot.jimple.infoflow.android.plugin.wear.transformers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.LookAndFeel;

import java.util.Collections;
import java.lang.reflect.Modifier;

import soot.*;
import soot.util.Chain;

import soot.jimple.Stmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.infoflow.android.plugin.wear.analysis.AnalysisUtil;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.InstanceFieldRef;
import soot.jimple.FieldRef;
import soot.jimple.AssignStmt;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.VirtualInvokeExpr;
import soot.jbco.util.*;


public class SensorEventSceneTransformer extends SceneTransformer {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static AnalysisUtil utilInstance = AnalysisUtil.getInstance();

    @Override
    protected void internalTransform(String phaseName, Map options) {
        logger.info("Frost SensorEventSceneTransformer");
	
		// addDummyGetImeiClassAndMethod();
		addFrostGetSensorDataClassAndMethod();

		for (SootClass sClass : Scene.v().getApplicationClasses()) {
            // Check if this method is an Android Framework method
            if(isAndroidMethod(sClass)) {
                continue;
            } else {
                // Detect onSensorChanged method
                if (addOnSensorChangedEdge(sClass)){
					logger.info("Added onSensorChanged edge to " + sClass.getName());
					// ExperimentalInstrumentSensorValueAccess(sClass);
					expandInstrumentSensorValueAccess(sClass);
					// instrumentGetImei(sClass);
				}
            }
        }

        // UnitPatchingChain units = b.getUnits();
        // List<Unit> generated = new ArrayList<>();
        // // Add a log message to show what method is calling incrementAndLogs
        // generated.addAll(InstrumentUtil.generateLogStmts(body, "Beginning of method " + b.getMethod().getSignature()));
        // // Call incrementAndLog method
        // generated.add(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(incNLogMethod.makeRef())));
        // units.insertBefore(generated, body.getFirstNonIdentityStmt());
        // b.validate();

        // Force resolve SensorEvent
		// Scene.v().forceResolve("android.hardware.SensorEvent", SootClass.BODIES);

		// imeiTransformation();
		// Scene.v().loadClass("com.frost.Imei", SootClass.BODIES);
		// Frost SensorEvent detection
		// addGetSensorDataMethod();
		// sensorEventDetection();
    
	}

	// SceneTransformer Version
    private boolean addOnSensorChangedEdge(SootClass sClass) {
        
		try {
			if (HierarchyUtils.getAllInterfacesOf(sClass).contains(Scene.v().getSootClass(ExtraTypes.SENSOR_EVENT_LISTENER))) {
				logger.info("Found SensorEventListener class: " + sClass.getName());
				// Get onSensorChanged method
				SootMethod onSensorChangeMethod = sClass.getMethodByName("onSensorChanged");
				// Get entryPoints
				SootMethod onCreateMethod = sClass.getMethodByName("onCreate");
				// Get onCreate method body
				Body b = onCreateMethod.retrieveActiveBody();
				JimpleBody body = (JimpleBody) b;

				// Add onSensorChanged invocation to onCreate method
				
				// Add SensorEvent field to class
				SootField sensorEventField = new SootField("mFrostSensorEvent", RefType.v("android.hardware.SensorEvent"), Modifier.PRIVATE);
				sClass.addField(sensorEventField);
				
				// Assign SensorEvent field to local
				// Add field local to main
				Local sensorEventLocal = Jimple.v().newLocal("mFrostSensorEventLocal", RefType.v("android.hardware.SensorEvent"));
				body.getLocals().add(sensorEventLocal);
				// Get this local reference
				Local mainThis = body.getThisLocal();
				// Get field reference
				FieldRef sensorEventFieldRef = Jimple.v().newInstanceFieldRef(mainThis, sensorEventField.makeRef());
				// Assign field to local
				AssignStmt sensorEventLocalAssignStmt = Jimple.v().newAssignStmt(sensorEventLocal, sensorEventFieldRef);

				// Invoke onSensorChanged method
				// Get onSensorChanged method reference
				SootMethodRef onSensorChangedMethodRef = onSensorChangeMethod.makeRef();
				// Create invoke expression
				VirtualInvokeExpr onSensorChangedInvokeExpr = Jimple.v().newVirtualInvokeExpr(mainThis, onSensorChangedMethodRef, sensorEventLocal);

				// Add invoke statement at start of onCreate method
				UnitPatchingChain units = b.getUnits();
				List<Unit> generated = new ArrayList<>();
				generated.add(sensorEventLocalAssignStmt);
				generated.add(Jimple.v().newInvokeStmt(onSensorChangedInvokeExpr));
				units.insertBefore(generated, body.getFirstNonIdentityStmt());
				b.validate();
				return true;
			}
		} catch (Exception e) {
			logger.error("Frost addOnSensorChangedEdge Error: " + e.getMessage());
		}
		return false;
    }

    private boolean isAndroidMethod(SootClass sClass) {
        if (sClass.getPackageName().startsWith("com.google.android") || sClass.getPackageName().startsWith("androidx")
        || sClass.getPackageName().startsWith("android.support.wearable") || sClass.isJavaLibraryClass()
        || utilInstance.isAndroidLibrary(sClass) || sClass.getPackageName().startsWith("kotlinx") || sClass.getPackageName().startsWith("kotlin")
        ) {
        return true; }
        return false;
    }

	// Current Implementation
	// Frost SensorEvent detection
	private void instrumentSensorValueAccess(SootClass sClass) {
		logger.info("Frost instrumentSensorValueAccess");
		SootMethod onSensorChangeMethod = sClass.getMethodByName("onSensorChanged");
		Body b = onSensorChangeMethod.retrieveActiveBody();
		// Get the list of statements
		Chain<Unit> units = b.getUnits();
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
					b.validate();
				}
			}
		}
	}


	// Experimental
	// Frost SensorEvent detection
	private void expandInstrumentSensorValueAccess(SootClass sClass) {
		logger.info("Frost instrumentSensorValueAccess");
		for(SootMethod sm : sClass.getMethods()) {
			if ( sm.getParameterTypes().contains(RefType.v("android.hardware.SensorEvent")) ) {
				logger.info("Frost instrumentSensorValueAccess: " + sm.getName());
				Body b = sm.retrieveActiveBody();
				// Get the list of statements
				Chain<Unit> units = b.getUnits();
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
							b.validate();
						}
					}
				}
			}
		}
	}


	// Experimental Implementation
	private void ExperimentalInstrumentSensorValueAccess(SootClass sClass) {
		logger.info("Frost ExperimentalInstrumentSensorValueAccess");
		SootMethod onSensorChangeMethod = sClass.getMethodByName("onSensorChanged");
		Body b = onSensorChangeMethod.retrieveActiveBody();
		// Get the list of statements
		Chain<Unit> units = b.getUnits();
		// Iterate through the list of statements
		int hits = 0;
		for (Unit unit : units) {
			// Get sensorEvent values access
			if (unit.toString().contains(".<android.hardware.SensorEvent: float[] values>")) {
				// Instrument Assignment Statement
				if (unit instanceof AssignStmt) {
					// Instrument 9th statement
					hits++;
					if (hits != 9) {		
						logger.info("Frost ExperimentalInstrumentSensorValueAccess hit: " + hits);
						continue;
					}
					logger.info("Frost Instrumenting ExperimentalInstrumentSensorValueAccess hit: " + hits);
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
					b.validate();
				}
			}
		}
	}


	// Current debug
	private void addFrostGetSensorDataClassAndMethod() {
		// Make Sensor Class
		SootClass sensorClass = new SootClass("com.frost.SensorEvent", Modifier.PUBLIC);
		sensorClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
		Scene.v().addClass(sensorClass);
		
		// Make field
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
		sensorClass.setApplicationClass();
	}

	private void imeiTransformation() {
		addDummyGetImeiClassAndMethod();
		// instrumentGetImei();
	}

	private void instrumentGetImei(SootClass sClass) {
		logger.info("Frost instrumentGetImei");
		SootMethod onSensorChangeMethod = sClass.getMethodByName("onSensorChanged");
		Body b = onSensorChangeMethod.retrieveActiveBody();
		// Get the list of statements
		Chain<Unit> units = b.getUnits();
		// Iterate through the list of statements
		for (Unit unit : units) {
			// Get getImei access
			if (unit.toString().contains(".<android.telephony.TelephonyManager: java.lang.String getImei()")) {
				// Instrument Assignment Statement
				if (unit instanceof AssignStmt) {
					// Get frost imei class
					SootClass frostImeiClass = Scene.v().getSootClass("com.frost.Imei");
					// Get getPhoneData method
					SootMethod getPhoneData = frostImeiClass.getMethodByName("getPhoneData");
					// Cast Unit to AssignStmt
					AssignStmt targetAssignStmt = (AssignStmt) unit;
					// Create a new statement to replace the old one
					StaticInvokeExpr newStaticInvoke = Jimple.v().newStaticInvokeExpr(getPhoneData.makeRef());
					// Replace rhs with invoke expression
					targetAssignStmt.setRightOp(newStaticInvoke);
					// break;
					b.validate();
				}
			}
		}
	}

	private void addDummyGetImeiClassAndMethod() {
		// Make Sensor Class
		SootClass sensorClass = new SootClass("com.frost.Imei", Modifier.PUBLIC);
		sensorClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
		Scene.v().addClass(sensorClass);

		// Make Field
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
		sensorClass.setApplicationClass();
	}

}
