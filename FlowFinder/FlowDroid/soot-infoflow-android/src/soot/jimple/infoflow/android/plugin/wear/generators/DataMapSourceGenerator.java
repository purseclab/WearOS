package soot.jimple.infoflow.android.plugin.wear.generators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import soot.Local;
import soot.Modifier;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.NewExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StringConstant;
import soot.jimple.infoflow.android.plugin.wear.exception.DataTypeNotFoundException;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;

public class DataMapSourceGenerator {

	private static DataMapSourceGenerator instance = new DataMapSourceGenerator();
	SootClass sclass;

	public static DataMapSourceGenerator getInstance() {
		return instance;
	}

	private DataMapSourceGenerator() {
		SootClass sclass = Scene.v().getSootClass(ExtraTypes.DATA_MAP);
		this.sclass = sclass;
	}

	public SootClass getSclass() {
		return sclass;
	}

	public void setSclass(SootClass sclass) {
		this.sclass = sclass;
	}

	/**
	 * Generates modified sources methods in the DataMap class for all data types
	 * that supports a DataMap
	 * 
	 * @throws DataTypeNotFoundException
	 */
	public SootClass generateDataMapSources() {
		String methodName;
		List<SootMethod> toInsert = new ArrayList<SootMethod>();
		Type returnType = null;
		for (Iterator<SootMethod> it = sclass.getMethods().iterator(); it.hasNext();) {
			SootMethod sm = it.next();
			if (sm.getName().contains("get") && !sm.getName().equals("get")) {
				if (sm.getParameterCount() == 1)
					methodName = sm.getName().concat("Source");
				else
					methodName = sm.getName().concat("Source2");
				List<Type> params = new ArrayList<Type>(sm.getParameterTypes());
				// we add one parameter to the method for the path
				params.add(RefType.v(ExtraTypes.STRING_TYPE));
				returnType = sm.getReturnType();
				if (sclass.getMethodByNameUnsafe(methodName) == null) {
					SootMethod newSource = new SootMethod(methodName, params, returnType, Modifier.PUBLIC);
					toInsert.add(newSource);
				}
			}
		}
		for (SootMethod sm : toInsert) {
			sclass.addMethod(sm);
			JimpleBody body = Jimple.v().newBody(sm);

			Local toReturn = Jimple.v().newLocal("l1", sm.getReturnType());
			body.getLocals().add(toReturn);
			NewExpr nExpr = Jimple.v().newNewExpr(RefType.v(sm.getReturnType().toString()));
			AssignStmt aStmt = Jimple.v().newAssignStmt(toReturn, nExpr);
			SootMethod objInit = Scene.v().getMethod("<java.lang.Object: void <init>()>");
			SpecialInvokeExpr sInvExpr = Jimple.v().newSpecialInvokeExpr(toReturn, objInit.makeRef());
			InvokeStmt iStmt = Jimple.v().newInvokeStmt(sInvExpr);
			body.getUnits().add(aStmt);
			body.getUnits().add(iStmt);

			ReturnStmt retStmt = Jimple.v().newReturnStmt(toReturn);
			body.getUnits().add(retStmt);

			sm.setActiveBody(body);

		}
		return sclass;
	}

	/**
	 * Generates a call to inject into the client component to a DataMap source with
	 * a specific data type.
	 * 
	 * @param path
	 * @param iexpr
	 * @return
	 */
	public InvokeStmt generateSourceCall(String path, String keyValue, InvokeExpr iexpr) {

		String methodName = null;
		int argCount = iexpr.getArgCount();
		int argIndex = 1;
		if (argCount == 1)
			methodName = iexpr.getMethod().getName().concat("Source");
		else {
			methodName = iexpr.getMethod().getName().concat("Source2");
			argIndex = 2;
		}
		SootClass sClass = Scene.v().getSootClass(ExtraTypes.DATA_MAP);
		SootMethod toCall = sClass.getMethodByName(methodName);

		Value base = iexpr.getUseBoxes().get(argIndex).getValue();
		List<Value> params = new ArrayList<Value>(argCount + 1);

		params.add(StringConstant.v(keyValue));
		if (argCount == 2) {
			Value defparam = iexpr.getUseBoxes().get(1).getValue();// if defparam.toString() == null
			params.add(defparam);
		}
		params.add(StringConstant.v(path));
		InvokeExpr sourceExpr = Jimple.v().newVirtualInvokeExpr((Local) base, toCall.makeRef(), params);
		InvokeStmt newsourcekExpr = Jimple.v().newInvokeStmt(sourceExpr);
		return newsourcekExpr;
	}

}
