package soot.jimple.infoflow.android.plugin.wear.generators;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import soot.ArrayType;
import soot.ByteType;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.StringConstant;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;

public class MessageEventSourceGenerator {

	private static MessageEventSourceGenerator instance = new MessageEventSourceGenerator();
	SootClass sclass;

	public static MessageEventSourceGenerator getInstance() {
		return instance;
	}

	private MessageEventSourceGenerator() {
		SootClass sclass = Scene.v().getSootClass(ExtraTypes.MESSAGE_EVENT);
		this.sclass = sclass;
	}

	public void setClass(SootClass sc) {
		this.sclass = sc;
	}

	public void generateMessageEventSources() {

		List<Type> params = new ArrayList<Type>();
		params.add(RefType.v(ExtraTypes.STRING_TYPE));
		Type returnType = ArrayType.v(ByteType.v(), 1);
		String name = "getDataSource";
		if (sclass.getMethodByNameUnsafe(name) == null) {
			SootMethod newSource = new SootMethod(name, params, returnType, Modifier.PUBLIC);
			JimpleBody body = Jimple.v().newBody(newSource);
			newSource.setActiveBody(body);
			sclass.addMethod(newSource);
		}
	}

	/**
	 * Generates a call to inject into the client component to a DataMap source with
	 * a specific data type.
	 * 
	 * @param path
	 * @param iexpr
	 * @return
	 */
	public InvokeStmt generateSourceCall(String path, InvokeExpr iexpr) {
		String methodName = iexpr.getMethod().getName().concat("Source");
		SootClass sClass = Scene.v().getSootClass(ExtraTypes.MESSAGE_EVENT);
		SootMethod toCall = sClass.getMethodByName(methodName);
		Value base = iexpr.getUseBoxes().get(0).getValue();
		List<Value> params = new ArrayList<Value>();
		params.add(StringConstant.v(path));
		InvokeExpr sourceExpr = Jimple.v().newVirtualInvokeExpr((Local) base, toCall.makeRef(), params);
		InvokeStmt newsourcekExpr = Jimple.v().newInvokeStmt(sourceExpr);
		return newsourcekExpr;
	}

}
