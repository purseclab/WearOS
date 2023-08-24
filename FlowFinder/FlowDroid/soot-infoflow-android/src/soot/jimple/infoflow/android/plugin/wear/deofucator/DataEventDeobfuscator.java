package soot.jimple.infoflow.android.plugin.wear.deofucator;

import java.util.HashMap;
import java.util.List;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;

public class DataEventDeobfuscator {

	private static DataEventDeobfuscator instance = null;
	SootClass deClass;
	HashMap<String, String> obfuscationMap;
	DeofuscatorUtil deobUtil;

	public HashMap<String, String> getObfuscationMap() {
		return obfuscationMap;
	}

	public static DataEventDeobfuscator getInstance() {
		if (instance == null)
			instance = new DataEventDeobfuscator();
		return instance;
	}

	private DataEventDeobfuscator() {
		this.deClass = Scene.v().getSootClass(ExtraTypes.DATA_EVENT);
		obfuscationMap = new HashMap<String, String>();
		deobUtil = DeofuscatorUtil.getInstance();
	}

	public boolean deobfuscateClass() {
		boolean obf = false;
		try {
			SootMethod sm = deClass.getMethodByNameUnsafe("getDataItem");
			if (sm == null) {
				Type returnType = Scene.v().getTypeUnsafe(ExtraTypes.DATA_ITEM);
				List<SootMethod> methods = deobUtil.searchMethodByReturnType(deClass, returnType);
				if (methods.size() == 1) {
					obf = true;
					obfuscationMap.put(methods.get(0).getSubSignature(), "getDataItem");
					methods.get(0).setName("getDataItem");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return obf;
	}

}
