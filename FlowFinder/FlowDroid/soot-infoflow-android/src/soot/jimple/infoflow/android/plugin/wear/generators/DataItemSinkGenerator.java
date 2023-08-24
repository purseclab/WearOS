package soot.jimple.infoflow.android.plugin.wear.generators;

import java.util.ArrayList;
import java.util.List;

import soot.ArrayType;
import soot.Body;
import soot.BooleanType;
import soot.ByteType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.Modifier;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.VoidType;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.StaticFieldRef;
import soot.jimple.StringConstant;
import soot.jimple.infoflow.android.plugin.wear.analysis.DataItemSearchResult;
import soot.jimple.infoflow.android.plugin.wear.exception.DataTypeNotFoundException;
import soot.jimple.infoflow.plugin.wear.extras.DataMapPutFunctions;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;

public class DataItemSinkGenerator {

	private static DataItemSinkGenerator genDataItemSinks = new DataItemSinkGenerator();

	SootClass dataItemClass;

	public static DataItemSinkGenerator getInstance() {
		return genDataItemSinks;
	}

	private DataItemSinkGenerator() {
		SootClass sclass = Scene.v().getSootClass(ExtraTypes.DATA_CLIENT);
		this.dataItemClass = sclass;
	}

	public void setClass(SootClass sc) {
		dataItemClass = sc;
	}

	/**
	 * Generates explicit sink methods in the DataClient class for all data types
	 * that supports a DataItem
	 * 
	 * @throws DataTypeNotFoundException
	 */
	public SootClass generateNewDataClientSinks() throws DataTypeNotFoundException {
		for (DataMapPutFunctions func : DataMapPutFunctions.values()) {
			String name = func.name().toString();
			String dataType = name.substring(3);
			Type argType = getType(dataType);
			List<Type> paramsType = new ArrayList<Type>();
			paramsType.add(RefType.v(ExtraTypes.STRING_TYPE));
			paramsType.add(RefType.v(ExtraTypes.STRING_TYPE));
			paramsType.add(argType);
			String finalName = name + "Sink";
			if (dataItemClass.getMethodByNameUnsafe(finalName) == null) {
				SootMethod sm = new SootMethod(finalName, paramsType, VoidType.v(), Modifier.PUBLIC);
				dataItemClass.addMethod(sm);
				JimpleBody body = Jimple.v().newBody(sm);
				sm.setActiveBody(body);
			}
		}
		return dataItemClass;

	}

	/**
	 * Generates a call to inject into the client component to a DataClient sink
	 * with a specific data type.
	 * 
	 * @param result
	 * @param iexp
	 * @param dataClientLocal
	 * @param fieldRef
	 * @return
	 */
	public InvokeStmt generateSinkCall(DataItemSearchResult result, InvokeExpr iexp, boolean oldApi,
			Local dataClientLocal, boolean isInterProcedural) {

		// if the invoke used old Api, then use a dataClient to generate the call
		Local baseLocal = (oldApi ? dataClientLocal : (Local) iexp.getUseBoxes().get(1).getValue());

		SootClass sClass = Scene.v().getSootClass(ExtraTypes.DATA_CLIENT);
		SootMethod toCall = sClass.getMethodByName(result.getSinkMethod() + "Sink");
		List<Value> params = new ArrayList<Value>();
		params.add(StringConstant.v(result.getPath()));
		params.add(StringConstant.v(result.getKey()));
		if (isInterProcedural)
			params.add(result.getIntrumentedRegister());
		else
			params.add(result.getRegister().getValue());

		InvokeExpr sinkExpr = Jimple.v().newVirtualInvokeExpr(baseLocal, toCall.makeRef(), params);
		InvokeStmt newsinkExpr = Jimple.v().newInvokeStmt(sinkExpr);
		return newsinkExpr;

	}

	/**
	 * Instrument an assignment of the SootField with the register found in the
	 * DataItemSearchResult in the contextMethod of the result. This is to propagate
	 * the value of this register to another method
	 * 
	 * @param result
	 * @param fieldRef
	 */
	public void instrumentFieldAssignment(DataItemSearchResult result, StaticFieldRef fieldRef) {

		Body body = result.getcontextMethod().getActiveBody();
		AssignStmt newAssignStmt = Jimple.v().newAssignStmt(fieldRef, result.getRegister().getValue());
		body.getUnits().insertBefore(newAssignStmt, result.getUnit());
	}

	/**
	 * 
	 * @param name
	 * @return
	 * @throws DataTypeNotFoundException
	 */
	private Type getType(String name) throws DataTypeNotFoundException {
		Type type = null;
		switch (name) {
		case "String":
			type = RefType.v(ExtraTypes.STRING_TYPE);
			break;
		case "Int":
			type = IntType.v();
			break;
		case "Long":
			type = LongType.v();
			break;
		case "Double":
			type = DoubleType.v();
			break;
		case "Float":
			type = FloatType.v();
			break;
		case "Boolean":
			type = BooleanType.v();
			break;
		case "Byte":
			type = ByteType.v();
			break;
		case "ByteArray":
			type = ArrayType.v(ByteType.v(), 1);
			break;
		case "StringArray":
			type = ArrayType.v(RefType.v(ExtraTypes.STRING_TYPE), 1);
			break;
		case "LongArray":
			type = ArrayType.v(LongType.v(), 1);
			break;
		case "FloatArray":
			type = ArrayType.v(FloatType.v(), 1);
			break;
		case "Asset":
			type = RefType.v(ExtraTypes.ASSETS);
			break;
		case "DataMap":
			type = RefType.v(ExtraTypes.DATA_MAP);
			break;
		case "StringArrayList":
			type = ArrayType.v(RefType.v(ExtraTypes.STRING_TYPE), 1);
			break;
		case "DataMapArrayList":
			type = ArrayType.v(RefType.v(ExtraTypes.DATA_MAP), 1);
			break;
		default:
			throw new DataTypeNotFoundException(DataItemSinkGenerator.class + name + "Not found");

		}
		return type;

	}

}
