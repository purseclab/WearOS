package soot.jimple.infoflow.android.plugin.wear.deofucator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import soot.ArrayType;
import soot.ByteType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;

public class MessageEventDeobfuscator {

	private static MessageEventDeobfuscator instance = null;
	SootClass mEventClass;
	HashMap<String, String> obfuscationMap;

	public HashMap<String, String> getObfuscationMap() {
		return obfuscationMap;
	}

	public static MessageEventDeobfuscator getInstance() {
		if (instance == null)
			instance = new MessageEventDeobfuscator();
		return instance;
	}

	private MessageEventDeobfuscator() {
		this.mEventClass = Scene.v().getSootClass(ExtraTypes.MESSAGE_EVENT);
		obfuscationMap = new HashMap<String, String>();
	}

	public boolean deofuscateMessageEvent() {
		boolean obf = false;
		try {
			SootMethod sm = mEventClass.getMethodByNameUnsafe("getData");
			SootMethod sm2 = mEventClass.getMethodByNameUnsafe("getPath");

			if (sm == null && sm2 == null) {
				// first search for getPath Method
				List<SootMethod> stringMethods = getMethodsByReturnType(ExtraTypes.STRING_TYPE);
				if (stringMethods.size() > 0) {
					// we select the first method. This is a heuristic based on the fact that
					// there are 2 possible methods and the getPath is first in the original class
					SootMethod getPathMethod = stringMethods.get(0);
					obfuscationMap.put(getPathMethod.getSubSignature(), "getPath");
					getPathMethod.setName("getPath");
					obf = true;
					if (stringMethods.size() == 2) {
						SootMethod sourceNodeIdMethod = stringMethods.get(1);
						obfuscationMap.put(sourceNodeIdMethod.getSubSignature(), "getSourceNodeId");
						sourceNodeIdMethod.setName("getSourceNodeId");
					}

				}
				// then search getData method
				String byteArray = ArrayType.v(ByteType.v(), 1).toString();
				SootMethod getDataMethod = getMethodByReturnType(byteArray);
				if (getDataMethod != null) {
					obfuscationMap.put(getDataMethod.getSubSignature(), "getData");
					getDataMethod.setName("getData");
					obf = true;
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return obf;

	}

	private List<SootMethod> getMethodsByReturnType(String type) {
		List<SootMethod> methods = new ArrayList<SootMethod>();
		for (SootMethod sm : mEventClass.getMethods()) {
			if (sm.getReturnType().toString().equals(type))
				methods.add(sm);
		}
		return methods;
	}

	/**
	 * Return method by return type. Only use this is you are sure that there is
	 * only one method with the return type
	 * 
	 * @param type
	 * @return
	 */
	private SootMethod getMethodByReturnType(String type) {
		List<SootMethod> methods = new ArrayList<SootMethod>();
		for (SootMethod sm : mEventClass.getMethods()) {
			if (sm.getReturnType().toString().equals(type))
				methods.add(sm);
		}
		if (methods.size() != 1)
			return null;
		else
			return methods.get(0);
	}

}
