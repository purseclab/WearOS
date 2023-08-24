package soot.jimple.infoflow.plugin.wear.extras;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import soot.Body;
import soot.Printer;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Unit;
import soot.jimple.Stmt;
import soot.options.Options;

public class Extras {

	private final static Logger LOG = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	/**
	 * This method prints the class given as parameter to a file in the sootOutput
	 * directory in a Jimple format
	 * 
	 * @param className
	 */
	public static void printJimpleToFile(SootClass sClass) {
		try {
			String fileName = SourceLocator.v().getFileNameFor(sClass, Options.output_format_jimple);
			OutputStream streamOut;
			streamOut = new FileOutputStream(fileName);
			PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
			Printer.v().printTo(sClass, writerOut);
			writerOut.flush();
			streamOut.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void printClass(SootClass sClass) {
		LOG.info("Printing " + sClass.getName());
		for (SootMethod sMethod : sClass.getMethods()) {
			Body body = sMethod.retrieveActiveBody();
			for (Iterator<Unit> i = body.getUnits().snapshotIterator(); i.hasNext();) {
				Unit unit = i.next();
				Stmt stmt = (Stmt) unit;
				System.out.println(stmt);
			}
		}
	}

	public static void printClassByName(String className) {
		LOG.info("Printing " + className);
		SootClass sClass = Scene.v().getSootClass(className);
		for (SootMethod sMethod : sClass.getMethods()) {
			Body body = sMethod.retrieveActiveBody();
			for (Iterator<Unit> i = body.getUnits().snapshotIterator(); i.hasNext();) {
				Unit unit = i.next();
				Stmt stmt = (Stmt) unit;
				System.out.println(stmt);
			}
		}
	}

	public static void printMethod(String className, String methodName) {
		LOG.info("Printing " + className + " : " + methodName);
		SootClass sClass = Scene.v().getSootClass(className);
		SootMethod sMethod = sClass.getMethodByName(methodName);
		Body body = sMethod.retrieveActiveBody();
		for (Iterator<Unit> i = body.getUnits().snapshotIterator(); i.hasNext();) {
			Unit unit = i.next();
			Stmt stmt = (Stmt) unit;
			System.out.println(stmt);
		}
	}

	public static void printMethod(SootMethod sMethod) {
		LOG.info("Printing " + sMethod.getName());
		Body body = sMethod.retrieveActiveBody();
		for (Iterator<Unit> i = body.getUnits().snapshotIterator(); i.hasNext();) {
			Unit unit = i.next();
			Stmt stmt = (Stmt) unit;
			System.out.println(stmt);
		}
	}

	public static void printExpandedMethod(List<Unit> units) {
		LOG.info("Printing expanded unit");
		for (Iterator<Unit> i = units.iterator(); i.hasNext();) {
			Unit unit = i.next();
			Stmt stmt = (Stmt) unit;
			System.out.println(stmt);
		}
	}

}
