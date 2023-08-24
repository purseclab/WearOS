package soot.jimple.infoflow.android.plugin.wear.deofucator;

import java.util.HashMap;
import java.util.List;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;

public class DataMapItemDeobfuscator {

	private static DataMapItemDeobfuscator instance = null;
	SootClass dmiClass;
	HashMap<String, String> obfuscationMap;
	DeofuscatorUtil deobUtil;

	public HashMap<String, String> getObfuscationMap() {
		return obfuscationMap;
	}

	public static DataMapItemDeobfuscator getInstance() {
		if (instance == null)
			instance = new DataMapItemDeobfuscator();
		return instance;
	}

	private DataMapItemDeobfuscator() {
		this.dmiClass = Scene.v().getSootClass(ExtraTypes.DATA_MAP_ITEM);
		obfuscationMap = new HashMap<String, String>();
		deobUtil = DeofuscatorUtil.getInstance();
	}

	public boolean deobfuscateClass() {
		boolean obf = false;
		try {
			SootMethod sm = dmiClass.getMethodByNameUnsafe("getDataMap");
			if (sm == null) {
				Type returnType = Scene.v().getTypeUnsafe(ExtraTypes.DATA_MAP);
				List<SootMethod> methods = deobUtil.searchMethodByReturnType(dmiClass, returnType);
				if (methods.size() == 1) {
					obf = true;
					obfuscationMap.put(methods.get(0).getSubSignature(), "getDataMap");
					methods.get(0).setName("getDataMap");
				}

				methods.clear();
				Type type = Scene.v().getTypeUnsafe(ExtraTypes.DATA_MAP_ITEM);
				methods = deobUtil.searchMethodByReturnType(dmiClass, type);
				if (methods.size() == 1) {
					obf = true;
					obfuscationMap.put(methods.get(0).getSubSignature(), "fromDataItem");
					methods.get(0).setName("fromDataItem");
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return obf;
	}

}
