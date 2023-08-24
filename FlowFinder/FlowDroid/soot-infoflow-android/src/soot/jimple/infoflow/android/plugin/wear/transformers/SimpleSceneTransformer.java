package soot.jimple.infoflow.android.plugin.wear.transformers;

import java.util.Map;
import java.util.logging.Logger;

import soot.Body;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.Jimple;
import soot.jimple.StringConstant;

public class SimpleSceneTransformer extends SceneTransformer {

	private final static Logger LOG = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	public SimpleSceneTransformer() {
	};

	@Override
	public void internalTransform(String phaseName, Map<String, String> options) {
		LOG.info("Intrumenting app: SimpleInstrumenter");
		SootClass sClass = Scene.v().getSootClass("de.ecspride.MainActivity");
		for (SootMethod sm:sClass.getMethods()) {
			sm.getName();
		}
		
		Body body = sClass.getMethodByName("onCreate").getActiveBody();
		Local arg = Jimple.v().newLocal("l1", RefType.v("java.lang.String"));
		body.getLocals().add(arg);
		AssignStmt astm = Jimple.v().newAssignStmt(arg, StringConstant.v("crash!"));
		Unit first = body.getUnits().getFirst();
		body.getUnits().insertBefore(astm, first);
	}

}
