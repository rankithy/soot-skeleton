package Analysis;

import soot.Scene;
import soot.options.Options;
import soot.SootClass;
import soot.*;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.options.Options;
import soot.toolkits.scalar.Pair;
import soot.util.dot.DotGraph;

import java.util.*;

public class ReachingDefAnalysis {
    Set<String> reachingResult;

    public static void main(String[] args) {
        ReachingDefAnalysis x = new ReachingDefAnalysis("test-resource", "DemoClass", true, true,
                true, true, false, true, true   , true);
    }

    public ReachingDefAnalysis(String classpath, String mainClass, boolean wholeProgram, boolean setApp,
                               boolean allowPhantomRef, boolean CGSafeNewInstance, boolean CGChaEnabled,
                               boolean CGSparkEnabled, boolean CGSparkVerbose, boolean CGSparkOnFlyCg) {
        reachingResult = new HashSet<String>();
        ReachingDefinitionAnalysis(classpath, mainClass, wholeProgram, setApp, allowPhantomRef, CGSafeNewInstance,
                CGChaEnabled, CGSparkEnabled, CGSparkVerbose, CGSparkOnFlyCg);
    }

    public void ReachingDefinitionAnalysis(String classpath, String mainClass, boolean wholeProgram, boolean setApp,
                                           boolean allowPhantomRef, boolean CGSafeNewInstance, boolean CGChaEnabled,
                                           boolean CGSparkEnabled, boolean CGSparkVerbose, boolean CGSparkOnFlyCg) {
        // Set Soot's internal classpath
        Options.v().set_soot_classpath(classpath + ":lib/rt.jar");

        // Enable whole-program mode
        Options.v().set_whole_program(wholeProgram);
        Options.v().set_app(setApp);

        // Call-graph options
        Options.v().setPhaseOption("cg", "safe-newinstance:" + CGSafeNewInstance);
        Options.v().setPhaseOption("cg.cha","enabled:" + CGChaEnabled);

        // Enable SPARK call-graph construction
        Options.v().setPhaseOption("cg.spark","enabled:" + CGSparkEnabled);
        Options.v().setPhaseOption("cg.spark","verbose:" + CGSparkVerbose);
        Options.v().setPhaseOption("cg.spark","on-fly-cg:" + CGSparkOnFlyCg);

        Options.v().set_allow_phantom_refs(allowPhantomRef);

        // Set the main class of the application to be analysed
        Options.v().set_main_class(mainClass);

        // Load the main class
        SootClass c = Scene.v().loadClass(mainClass, SootClass.BODIES);
        c.setApplicationClass();

        Scene.v().loadNecessaryClasses();

        // Load the "main" method of the main class and set it as a Soot entry point
        SootMethod entryPoint = c.getMethodByName("main");
        List<SootMethod> entryPoints = new ArrayList<SootMethod>();
        entryPoints.add(entryPoint);
        Scene.v().setEntryPoints(entryPoints);

        // Run the package
        final IFDSDataFlowTransformer transformer = new IFDSDataFlowTransformer();

        PackManager.v().getPack("wjtp").add(new Transform("wjtp.herosifds", new IFDSDataFlowTransformer()));
        PackManager.v().getPack("jtp").add(new Transform("jtp.myTransform", new BodyTransformer() {
            @Override
            protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
                String className = b.getMethod().getDeclaringClass().getName();
                for (Unit u : b.getUnits()) {
                    Stmt s = (Stmt) u;
                    if (s.containsInvokeExpr() && s.getInvokeExpr() instanceof InstanceInvokeExpr) {
                        InstanceInvokeExpr e = (InstanceInvokeExpr) s.getInvokeExpr();

                        // find all invokeExpress in given configuration
                        System.out.println(s.getInvokeExprBox().getValue().toString());
                        reachingResult.add(s.getInvokeExprBox().getValue().toString());

                        /*
                        Only for test purpose

                        if (e.getMethod().getName().equals("println") && className.equals("DemoClass")) {
                            boolean equals = false;
                            String constants = "[[other]]";
                            for (Pair result : transformer.getSolver().ifdsResultsAt(u)) {
                                if (result.getO1().equals(e.getArg(0))) {
                                    System.out.println(result.getO2().toString());
                                    String str = result.getO2().toString();
                                    if (str.equals(constants)) {
                                        equals = true;
                                    }
                                }
                            }
                        }
                         */
                    }
                }
            }
        }));
        PackManager.v().runPacks();
    }

    public Set<String> getReachingResult() {
        return reachingResult;
    }
}
