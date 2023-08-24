package soot.jimple.infoflow.android.plugin.wear.generators;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import soot.Local;
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
import soot.jimple.StringConstant;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;

public class ChannelClientGenerator {

	private static ChannelClientGenerator instance;
	private SootClass sclass;

	public static ChannelClientGenerator getInstance() {
		if (instance == null)
			instance = new ChannelClientGenerator();
		return instance;
	}

	public void setClass(SootClass sc) {
		this.sclass = sc;
	}

	private ChannelClientGenerator() {
		this.sclass = Scene.v().getSootClass(ExtraTypes.CHANNEL_CLIENT);
	}

	public void generateChannelSources() {

		// generate source for receiveFiles(ChannelClient.Channel channel, Uri uri,
		// boolean append)
		List<Type> params = new ArrayList<Type>();
		params.add(RefType.v(ExtraTypes.CHANNEL));
		params.add(RefType.v(ExtraTypes.URI));
		params.add(RefType.v(ExtraTypes.BOOLEAN_TYPE));
		params.add(RefType.v(ExtraTypes.STRING_TYPE));
		Type returnType = RefType.v(ExtraTypes.TASK);
		String name = "receiveFileSource";
		if (sclass.getMethodByNameUnsafe(name) == null) {
			SootMethod newSource = new SootMethod("receiveFileSource", params, returnType, Modifier.PUBLIC);
			JimpleBody body = Jimple.v().newBody(newSource);
			newSource.setActiveBody(body);
			sclass.addMethod(newSource);

			// generate source for getInputStream(ChannelClient.Channel channel)
			params.clear();
			params.add(RefType.v(ExtraTypes.CHANNEL));
			params.add(RefType.v(ExtraTypes.STRING_TYPE));
			SootMethod newSource2 = new SootMethod("getInputStreamSource", params, returnType, Modifier.PUBLIC);
			newSource2.setActiveBody(body);
			sclass.addMethod(newSource2);
		}

	}

	/**
	 * Generates a TaintWrapper for an Uri to be call after a callback receives data
	 * from a ChannelClient and write to a file. This method will simulate a taint
	 * to the Uri corresponding to the file where the data is written Ex: uri =
	 * ChannelClient.taintUri(uri);
	 */

	public void generateTaintWrapper() {
		List<Type> params = new ArrayList<Type>();
		params.add(RefType.v(ExtraTypes.URI));
		params.add(RefType.v(ExtraTypes.STRING_TYPE));

		Type returnType = RefType.v(ExtraTypes.URI);
		String name = "taintUri";
		if (sclass.getMethodByNameUnsafe(name) == null) {
			SootMethod newSource = new SootMethod(name, params, returnType, Modifier.PUBLIC);
			JimpleBody body = Jimple.v().newBody(newSource);
			newSource.setActiveBody(body);
			sclass.addMethod(newSource);
		}
	}

	public void generateChannelSinks() {
		List<Type> params = new ArrayList<Type>();
		params.add(RefType.v(ExtraTypes.CHANNEL));
		params.add(RefType.v(ExtraTypes.URI));
		params.add(RefType.v(ExtraTypes.STRING_TYPE));
		Type returnType = RefType.v(ExtraTypes.TASK);
		String name = "sendFileSink";
		if (sclass.getMethodByNameUnsafe(name) == null) {
			SootMethod newSource = new SootMethod(name, params, returnType, Modifier.PUBLIC);
			JimpleBody body = Jimple.v().newBody(newSource);
			newSource.setActiveBody(body);
			sclass.addMethod(newSource);

			params.clear();
			params.add(RefType.v(ExtraTypes.CHANNEL));
			params.add(RefType.v(ExtraTypes.URI));
			params.add(RefType.v(ExtraTypes.LONG_TYPE));
			params.add(RefType.v(ExtraTypes.LONG_TYPE));
			params.add(RefType.v(ExtraTypes.STRING_TYPE));
			// FIXME change this and make like DataItems. the name of the method
			SootMethod newSource2 = new SootMethod("sendFile2Sink", params, returnType, Modifier.PUBLIC);
			JimpleBody body2 = Jimple.v().newBody(newSource2);
			newSource2.setActiveBody(body2);
			sclass.addMethod(newSource2);

			params.clear();
			params.add(RefType.v(ExtraTypes.CHANNEL));
			params.add(RefType.v(ExtraTypes.STRING_TYPE));

			SootMethod newSource3 = new SootMethod("getOutputStreamSink", params, returnType, Modifier.PUBLIC);
			JimpleBody body3 = Jimple.v().newBody(newSource3);
			newSource3.setActiveBody(body3);
			sclass.addMethod(newSource3);
		}

	}

	public InvokeStmt generateSinkCall(String path, InvokeExpr iexpr) {
		String methodName = iexpr.getMethod().getName().concat("Sink");
		InvokeStmt newSinkExpr = null;
		Value base = iexpr.getUseBoxes().get(2).getValue();
		SootMethod toCall = sclass.getMethodByName(methodName);
		List<Value> params = new ArrayList<Value>();
		Value channelClient = null;
		Value uri = null;
		Value iniOffset = null;
		Value endOffset = null;

		// this cover the case of getInputStream(channel)
		channelClient = iexpr.getArg(0);
		params.add(channelClient);

		if (methodName.equals("sendFileSink")) {
			uri = iexpr.getArg(1);
			params.add(uri);
		} else if (methodName.equals("sendFile2Sink")) {
			uri = iexpr.getArg(1);
			params.add(uri);

			iniOffset = iexpr.getArg(2);
			params.add(iniOffset);

			endOffset = iexpr.getArg(3);
			params.add(endOffset);
		}

		params.add(StringConstant.v(path));
		InvokeExpr sourceExpr = Jimple.v().newVirtualInvokeExpr((Local) base, toCall.makeRef(), params);
		newSinkExpr = Jimple.v().newInvokeStmt(sourceExpr);

		return newSinkExpr;
	}

	public InvokeStmt generateSourceCall(String path, InvokeExpr iexpr) {

		String methodName = iexpr.getMethod().getName().concat("Source");
		InvokeStmt newsourcekExpr = null;
		Value base = iexpr.getUseBoxes().get(3).getValue();
		SootMethod toCall = sclass.getMethodByName(methodName);
		Value channel = iexpr.getArg(0);
		List<Value> params = new ArrayList<Value>();

		if (methodName.contains("receiveFile")) {

			Value uri = iexpr.getArg(1);
			Value append = iexpr.getArg(2);
			params.add(channel);
			params.add(uri);
			params.add(append);

		} else if (methodName.contains("getInputStream")) {
			params.add(channel);

		}
		params.add(StringConstant.v(path));

		InvokeExpr sourceExpr = Jimple.v().newVirtualInvokeExpr((Local) base, toCall.makeRef(), params);
		newsourcekExpr = Jimple.v().newInvokeStmt(sourceExpr);

		return newsourcekExpr;
	}

	// Ex: uri = ChannelClient.taintUri(uri);
	public AssignStmt callTaintWrapper(Value uri, Value base, String path) {
		SootMethod toCall = sclass.getMethodByName("taintUri");
		InvokeStmt newInvokeStmt = null;
		List<Value> params = new ArrayList<Value>();
		params.add(uri);
		params.add(StringConstant.v(path));
		InvokeExpr newExpr = Jimple.v().newVirtualInvokeExpr((Local) base, toCall.makeRef(), params);
		newInvokeStmt = Jimple.v().newInvokeStmt(newExpr);

		AssignStmt newAssignStmt = Jimple.v().newAssignStmt(uri, newInvokeStmt.getInvokeExpr());

		return newAssignStmt;
	}

}
