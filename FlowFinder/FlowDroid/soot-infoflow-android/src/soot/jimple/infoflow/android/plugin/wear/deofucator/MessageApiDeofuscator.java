package soot.jimple.infoflow.android.plugin.wear.deofucator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;

public class MessageApiDeofuscator {

	private static MessageApiDeofuscator instance = null;
	SootClass mApiClass;
	HashMap<String, String> obfuscationMap;
	HashMap<String, String> obfuscationMapCallback;
	DeofuscatorUtil deobUtil;

	public HashMap<String, String> getObfuscationMap() {
		return obfuscationMap;
	}

	public HashMap<String, String> getObfuscationCallbackMap() {
		return obfuscationMapCallback;
	}

	public static MessageApiDeofuscator getInstance() {
		if (instance == null)
			instance = new MessageApiDeofuscator();
		return instance;
	}

	private MessageApiDeofuscator() {
		this.mApiClass = Scene.v().getSootClass(ExtraTypes.MESSAGE_API);
		obfuscationMap = new HashMap<String, String>();
		obfuscationMapCallback = new HashMap<String, String>();
		deobUtil = DeofuscatorUtil.getInstance();

	}

	public boolean deofuscateMessageApi() {
		boolean obf = false;
		try {
			SootMethod sm = mApiClass.getMethodByNameUnsafe("sendMessage");
			if (sm == null) {
				SootMethod sendMessageObf = searchSendMessageMethod();
				if (sendMessageObf != null) {
					obf = true;
					obfuscationMap.put(sendMessageObf.getSubSignature(), "sendMessage");
					sendMessageObf.setName("sendMessage");
				}
				SootClass child = Scene.v().getSootClass(ExtraTypes.MESSAGE_LISTENER);
				if (child != null && !child.isPhantom()) {
					sm = null;
					sm = child.getMethodByNameUnsafe("onMessageReceived");
					if (sm == null) {
						List<Type> params = new ArrayList<>();
						params.add(Scene.v().getTypeUnsafe(ExtraTypes.MESSAGE_EVENT));
						List<SootMethod> result = deobUtil.searchMethodsByParams(child, params);
						if (result.size() == 1) {
							obf = true;
							obfuscationMapCallback.put(result.get(0).getSubSignature(), "onMessageReceived");
							result.get(0).setName("onMessageReceived");
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return obf;
	}

	private boolean checkParamsType(List<Type> types, String type) {
		for (Type t : types) {
			if (t.toString().contentEquals(type))
				return true;
		}

		return false;
	}

	private SootMethod searchSendMessageMethod() {
		for (SootMethod sm : mApiClass.getMethods()) {
			if (sm.getParameterCount() == 4) {
				List<Type> types = sm.getParameterTypes();
				if (checkParamsType(types, ExtraTypes.STRING_TYPE)) {
					return sm;
				}
			}
		}
		return null;
	}

}
