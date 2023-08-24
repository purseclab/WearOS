package soot.jimple.infoflow.android.plugin.wear.deofucator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import soot.ArrayType;
import soot.ByteType;
import soot.Local;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;
import soot.util.Chain;

public class DeofuscatorUtil {

	public static DeofuscatorUtil instance = null;

	public static DeofuscatorUtil getInstance() {
		if (instance == null)
			return new DeofuscatorUtil();
		else
			return instance;
	}

	private DeofuscatorUtil() {

	}

	public List<SootMethod> searchMethodsByParams(SootClass sclass, List<Type> params) {
		List<SootMethod> methods = new ArrayList<SootMethod>();
		int paramsSize = params.size();
		for (SootMethod sm : sclass.getMethods()) {
			if (sm.getParameterCount() == paramsSize && paramsSize > 0) {
				boolean match = true;
				for (int i = 0; i < paramsSize; i++) {
					Type item1 = params.get(i);
					Type item2 = sm.getParameterType(i);
					if (!item1.equals(item2))
						match = false;
				}
				if (match == true)
					methods.add(sm);

			}
		}

		return methods;

	}

	/**
	 * Search methods using the return type and the params type as matching
	 * condition
	 * 
	 * @param sclass
	 * @param params
	 * @return
	 */
	public List<SootMethod> searchMethodsFull(SootClass sclass, List<Type> params, Type returnType) {
		List<SootMethod> result = new ArrayList<SootMethod>();

		List<SootMethod> tmp1 = searchMethodsByReturnType(sclass, returnType);
		List<SootMethod> tmp2 = searchMethodsByParams(sclass, params);
		Set<SootMethod> intersection = Sets.intersection(Sets.newHashSet(tmp1), Sets.newHashSet(tmp2));

		result.addAll(intersection);

		return result;
	}

	public List<SootMethod> searchMethodsByReturnType(SootClass sclass, Type type) {
		List<SootMethod> methods = new ArrayList<SootMethod>();
		for (SootMethod sm : sclass.getMethods()) {
			if (sm.getReturnType().equals(type))
				methods.add(sm);
		}
		return methods;
	}

	/**
	 * Return a method in the class with the return type. This method assumes that
	 * this exist and it is the only method with such ruturn type in the class
	 * 
	 * @param sclass
	 * @param type
	 * @return
	 */
	public List<SootMethod> searchMethodByReturnType(SootClass sclass, Type type) {
		List<SootMethod> methods = new ArrayList<SootMethod>();
		for (SootMethod sm : sclass.getMethods()) {
			if (sm.getReturnType().equals(type))
				methods.add(sm);
		}
		return methods;
	}

	/**
	 * Search local of specific type in the method
	 * 
	 * @param sm
	 * @param type
	 * @return
	 */
	public boolean checkLocalsType(SootMethod sm, Type type) {
		return checkParamsType(sm.getActiveBody().getLocals(), type.toString());
	}

	public boolean checkParamsType(Chain<Local> locals, String type) {
		for (Local l : locals) {
			if (l.getType().toString().contentEquals(type))
				return true;
		}

		return false;
	}

	public boolean matchSignature(String methodName, SootMethod sm) {
		boolean match = false;
		switch (methodName) {
		case "sendMessage":
			if (sm.getDeclaringClass().toString().equals(ExtraTypes.MESSAGE_API))
				match = matchMApiSendM(sm);
			else if (sm.getDeclaringClass().toString().equals(ExtraTypes.MESSAGE_CLIENT))
				match = matchMClientSendM(sm);
			break;
		case "getData":
			if (sm.getDeclaringClass().toString().equals(ExtraTypes.MESSAGE_EVENT))
				match = matchGetDataMEvent(sm);
			break;
		case "getPath":
			if (sm.getDeclaringClass().toString().equals(ExtraTypes.MESSAGE_EVENT))
				match = matchGetPathMEvent(sm);
			break;
		}

		return match;
	}

	private boolean matchGetDataMEvent(SootMethod sm) {
		String byteArray = ArrayType.v(ByteType.v(), 1).toString();
		if (!sm.getReturnType().toString().equals(byteArray))
			return false;
		return true;
	}

	private boolean matchGetPathMEvent(SootMethod sm) {
		if (!sm.getReturnType().toString().equals(ExtraTypes.STRING_TYPE))
			return false;
		return true;
	}

	private boolean matchMApiSendM(SootMethod sm) {

		if (sm.getParameterCount() != 4)
			return false;
		if (!sm.getParameterType(1).toString().equals(ExtraTypes.STRING_TYPE))
			return false;
		if (!sm.getParameterType(2).toString().equals(ExtraTypes.STRING_TYPE))
			return false;

		return true;
	}

	private boolean matchMClientSendM(SootMethod sm) {

		if (sm.getParameterCount() != 3)
			return false;
		if (!sm.getParameterType(0).toString().equals(ExtraTypes.STRING_TYPE))
			return false;
		if (!sm.getParameterType(1).toString().equals(ExtraTypes.STRING_TYPE))
			return false;

		return true;
	}

}
