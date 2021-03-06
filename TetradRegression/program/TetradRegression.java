/*
    CMU HCII 2017
    https://github.com/cmu-phil/tetrad
    Tetrad plugged into Tigris Workflows

    -Peter
*/

import java.io.BufferedReader;
import java.util.regex.Pattern;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import java.util.Vector;
import java.util.logging.*;
import java.util.regex.Pattern;
import cern.colt.Arrays;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.io.CharArrayWriter;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.regression.*;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.util.*;
import edu.cmu.tetradapp.model.*;
import edu.cmu.tetradapp.model.datamanip.*;
import edu.cmu.tetrad.regression.LogisticRegression.Result;


public class TetradRegression {
    private static final String FILENAME = "TetradComponentOutput.txt";
    private static final String ERROR_PREPEND = "ERROR: ";
    private static final String DEBUG_PREPEND = "DEBUG: ";
    private static boolean verbose = false;
    private static String outputDir = "";

    public TetradRegression () {}

    public static void main(String [] args) {
        PrintStream sysErr = System.err;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setErr(new PrintStream(baos));

        /* The new parameter syntax for files is -node m -fileIndex n <infile>. */
        Map<Integer, File> inFiles = new HashMap<Integer, File>();

        for ( int i = 0; i < args.length; i++) {	// Cursory parse to get the input files
            String arg = args[i];
            String nodeIndex = null;
            String fileIndex = null;
            String filePath = null;
            if (i < args.length - 4) {
                if (arg.equalsIgnoreCase("-node")) {
                    File inFile = null;
                    String[] fileParamsArray = { args[i] /* -node */, args[i + 1] /* node (index) */,
                                                 args[i + 2] /* -fileIndex */, args[i + 3] /* fileIndex */, args[i + 4] /* infile */
                                               };
                    String fileParamsString = Arrays.toString(fileParamsArray);
                    // Use regExp to get the file path
                    String regExp = "^\\[-node, ([0-9]+), -fileIndex, ([0-9]+), ([^\\]]+)\\]$";
                    Pattern pattern = Pattern.compile(regExp);
                    if (fileParamsString.matches(regExp)) {
                        // Get the third argument in parens from regExp
                        inFile = new File(fileParamsString.replaceAll(regExp, "$3"));
                    }
                    nodeIndex = args[i + 1];
                    Integer nodeIndexInt = Integer.parseInt(nodeIndex);
                    fileIndex = args[i + 3];
                    inFiles.put(nodeIndexInt, inFile);
                    // 5 arguments, but for loop still calls i++ after
                    i += 4;
                }
            }

        }

        //List<String> regressors = new ArrayList<String>();
        List<String> regressors = getMultiFileInputHeaders("regressors", args);

        HashMap<String, String> cmdParams = new HashMap<String, String>();
        for ( int i = 0; i < args.length; i++ ) {
            String s = args[i];
            if ( s.charAt(0) == '-' && i != args.length - 1) {
                String value = "";
                for (int j = i + 1; j < args.length; j++) {
                    if (args[j].charAt(0) == '-' && j > i + 1) {
                        break;
                    } else if (j != i + 1) {
                        value += " ";
                    }
                    value += args[j];
                }
                cmdParams.put(s, value);
                i++;
            }
        }

        if ( cmdParams.containsKey("-regression") == false ) {
            addToErrorMessages("No Regression Specified.");
            return;
        } else if ( cmdParams.containsKey("-target") == false ) {
            addToErrorMessages("No Target Specified.");
            return;
        } else if ( cmdParams.containsKey("-workingDir") == false ) {
            addToErrorMessages("No workingDir");
            return;
        } else if (!inFiles.containsKey(0)) {
            addToErrorMessages("No infile ");
            return;
        } else if ( cmdParams.containsKey("-regressors") == false ) {
            addToErrorMessages("No regressors specified.");
            return;
        } else if ( cmdParams.containsKey("-alpha") == false ) {
            addToErrorMessages("No alpha specified.");
            return;
        }

        String programDir = cmdParams.get("-programDir");
        String regrType = cmdParams.get("-regression");
        String target = cmdParams.get("-target");
        target = target.replaceAll(" ", "_");
        Double t1 = Double.parseDouble(cmdParams.get("-alpha"));
        double alpha = t1.doubleValue();
        String workingDir = cmdParams.get("-workingDir");
        outputDir = workingDir;

        //remove duplicates
        Set<String> hs = new HashSet<String>();
        hs.addAll(regressors);
        regressors.clear();
        regressors.addAll(hs);

        //replace spaces with underscores
        /*int size = regressors.size();
        List<String> oldRegressors = regressors;
        regressors = new ArrayList<String>();
        for (int i = 0; i < size; i++) {
          regressors.add(i, oldRegressors.get(i).replaceAll(" ","_"));
        }*/

        addToDebugMessages(target + "target");
        addToDebugMessages("regressors" + regressors.toString());

        File inputFile = inFiles.get(0);



        if (inputFile.exists() && inputFile.isFile() && inputFile.canRead() ) {

            String regressionTableFile = workingDir + "RegressionTable.txt";
            String regressionGraphFile = workingDir + "RegressionGraph.html";
            String regressionStatsFile = workingDir + "RegressionModelStatistics.txt";

            boolean multiLinRegr = false;
            if ( regrType.equals("Multiple_Linear_Regression") ) {
                multiLinRegr = true;
                addToDebugMessages("Regression Type: Multiple Linear Regression");
            } else {
                addToDebugMessages("Regression Type: Logistic Regression");
            }

            try {

                BufferedReader bReader = null;
                FileReader fReader = null;

                BufferedWriter bWriterTable = null;
                FileWriter fWriterTable = null;

                BufferedWriter bWriterGraph = null;
                FileWriter fWriterGraph = null;

                BufferedWriter bWriterStats = null;
                FileWriter fWriterStats = null;

                try {

                    fWriterTable = new FileWriter(regressionTableFile);
                    bWriterTable = new BufferedWriter(fWriterTable);

                    fWriterGraph = new FileWriter(regressionGraphFile);
                    bWriterGraph = new BufferedWriter(fWriterGraph);

                    fWriterStats = new FileWriter(regressionStatsFile);
                    bWriterStats = new BufferedWriter(fWriterStats);

                    char[] chars = fileToCharArray(inputFile);

                    DataReader reader = new DataReader();
                    reader.setMaxIntegralDiscrete(4);
                    reader.setDelimiter(DelimiterType.TAB);

                    DataSet data = reader.parseTabular(chars);

                    List<String> variableNames = data.getVariableNames();
                    addToDebugMessages(variableNames.toString());
                    if ( variableNames.contains(target) == false ) {
                        addToErrorMessages("Target specified was not in the DataSet.");
                    }

                    regressors.remove(target);

                    if ( variableNames.containsAll(regressors) == false ) {
                        addToErrorMessages("DataSet does not contain all of the specified regressors.");
                    }

                    //Node targ = new GraphNode(target);
                    Node targ = data.getVariable( target );
                    List<Node> regr = new ArrayList<Node>();
                    for ( int i = 0; i < regressors.size(); i++ ) {
                        regr.add( data.getVariable( regressors.get(i) ));
                    }


                    List<Node>  v = data.getVariables();
                    int _target = v.indexOf(targ);

                    String table;
                    Graph graph;
                    String modelStats = "";

                    if ( multiLinRegr ) {
                        Regression rd = new RegressionDataset(data);
                        rd.setAlpha( alpha );

                        RegressionResult result = rd.regress( targ, regr );

                        TextTable tt = result.getResultsTable();
                        tt.setTabDelimited( true );
                        table = tt.toString().replaceAll("\n[\\s]*\n", "\n");

                        graph = rd.getGraph();

                        modelStats = result.getPreamble();
                    } else {
                        LogisticRegression lr = new LogisticRegression( data );
                        lr.setAlpha( alpha );

                        Result result;
                        table = "";
                        try {
                            result = lr.regress( new DiscreteVariable(target, 2), regr );
                            table = logisticRegressionTable(result, alpha);
                            modelStats = result.toString();
                        } catch ( IllegalArgumentException e ) {
                            addToErrorMessages( "Exception thrown while in .regress(): " + e.toString() );
                        }

                        graph = new Dag();
                        Node noGraphForLR = new GraphNode("Log. Regr. Does Not Output a Graph");
                        graph.addNode(noGraphForLR);
                    }

                    bWriterTable.append( table );
                    bWriterTable.close();

                    bWriterStats.append(modelStats);
                    bWriterStats.close();

                    writeGraphToHtml(graph.toString(), programDir, bWriterGraph);

                } catch (IOException e) {
                    addToErrorMessages(e.toString());
                }


            } catch (Exception e) {
                addToErrorMessages(e.toString());
            }


        } else if (inputFile == null || !inputFile.exists()
                   || !inputFile.isFile()) {
            addToErrorMessages("Tab-delimited file does not exist.");

        } else if (!inputFile.canRead()) {
            addToErrorMessages("Tab-delimited file cannot be read.");
        }
        System.setErr(sysErr);

    }

    private static String logisticRegressionTable(Result result, double alpha) {
        String table = "";

        NumberFormat nf = new DecimalFormat("0.0000");

        table += "Var\tCoeff.\tStdErr\tprob.\tsig.";

        List<String> regressorNames = result.getRegressorNames();
        double[] coefs = result.getCoefs();
        double[] stdErrs = result.getStdErrs();
        double[] probs = result.getProbs();

        for (int i = 0; i < regressorNames.size(); i++) {
            table += "\n" + regressorNames.get(i) +
                    "\t" + nf.format(coefs[i + 1]) +
                    "\t" + nf.format(stdErrs[i + 1]) +
                    "\t" + nf.format(probs[i + 1]) +
                    "\t" + (probs[i + 1] < alpha ? "*" : "");
        }

        return table;
    }

    public static ArrayList<String> getMultiFileInputHeaders (String param, String [] args) {
        ArrayList<String> ret = new ArrayList<String>();
        String argName = "-" + param;
        for ( int i = 0; i < args.length; i++ ) {
            String s = args[i];
            if (argName.equals(s) && i != (args.length - 1)) {
                //ret.add(args[i+1]);
                String value = "";
                for (int j = i + 1; j < args.length; j++) {
                    if (args[j].charAt(0) == '-' && j > i + 1) {
                        break;
                    } else if (j != i + 1) {
                        value += " ";
                    }
                    value += args[j];
                }
                ret.add(value.replaceAll(" ", "_"));
                i++;
            }
        }

        return ret;
    }

    private static char[] fileToCharArray(File file) {
        try {
            FileReader reader = new FileReader(file);
            CharArrayWriter writer = new CharArrayWriter();
            int c;

            while ((c = reader.read()) != -1) {
                writer.write(c);
            }

            return writer.toCharArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static boolean addToErrorMessages(String message) {
        try {
            System.out.println(message);

            FileWriter fw = new FileWriter(outputDir + FILENAME, true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(ERROR_PREPEND + message + "\n");
            bw.flush();
            bw.close();
        } catch (IOException e) {
            addToErrorMessages("Unable to write to file: " + e.toString());
            return false;
        }
        return true;
    }

    /**
     *Save DEBUG message string from component to a file.
     */
    public static boolean addToDebugMessages(String message) {
        try {
            System.out.println(message);

            FileWriter fw = new FileWriter(outputDir + FILENAME, true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(DEBUG_PREPEND + message + "\n");
            bw.flush();
            bw.close();
        } catch (IOException e) {
            addToErrorMessages("Unable to write to file: " + e.toString());
            return false;
        }
        return true;
    }

    private static void writeGraphToHtml(String graphStr, String programDir, BufferedWriter bWriter) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(programDir + "/program/tetradGraph.html"));

            StringBuilder htmlStr = new StringBuilder();
            while (br.ready()) {
                htmlStr.append(br.readLine());
                if (br.ready()) {
                    htmlStr.append("\n");
                }
            }

            String s = htmlStr.toString();

            s = s.replaceAll("PutGraphDataHere", graphStr);

            bWriter.write(s);
            bWriter.close();
        } catch (IOException e) {
            addToErrorMessages("Could not write graph to file. " + e.toString());
        }
    }
}