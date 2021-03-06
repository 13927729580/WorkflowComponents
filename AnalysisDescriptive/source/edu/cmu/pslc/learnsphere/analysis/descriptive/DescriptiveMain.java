package edu.cmu.pslc.learnsphere.analysis.Descriptive;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class DescriptiveMain extends AbstractComponent {

    /** Component option (model). */
    String modelName = null;

    public static void main(String[] args) {

        DescriptiveMain tool = new DescriptiveMain();
        tool.startComponent(args);
    }

    public DescriptiveMain() {
        super();
    }


    @Override
    protected void parseOptions() {

      

    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");
        // Add the column headers from our input file to this component's output metadata,
        // plus one extra column for Predicted Error Rate, if it doesn't already exist.

        // addMetaDataFromInput(String fileType, Integer inputNodeIndex, Integer outputNodeIndex, String name)
     
    }

    @Override
    protected void runComponent() {
        // Run the program and add the files it generates to the component output.
        File outputDirectory = this.runExternal();
        // Attach the output files to the component output with addOutputFile(..>)
        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
            File file0 = new File(outputDirectory.getAbsolutePath() + "/factor.html");
            File file1 = new File(outputDirectory.getAbsolutePath() + "/factorbyfactor.html");
            File file2 = new File(outputDirectory.getAbsolutePath() + "/histogramfff.png");
            File file3 = new File(outputDirectory.getAbsolutePath() + "/factorbyfactorbyfactor.html");
            File file4 = new File(outputDirectory.getAbsolutePath() + "/histogramff.png");
            File file5 = new File(outputDirectory.getAbsolutePath() + "/histogramf.png");

            if (file0 != null && file0.exists() && file1 != null && file1.exists() && file4 != null && file4.exists() && file5 != null && file5.exists()) {

                Integer nodeIndex0 = 0;
                Integer fileIndex0 = 0;
                String label0 = "html";
                this.addOutputFile(file0, nodeIndex0, fileIndex0, label0);

                Integer nodeIndex1 = 1;
                Integer fileIndex1 = 0;
                String label1 = "html";
                this.addOutputFile(file1, nodeIndex1, fileIndex1, label1);

                Integer nodeIndex2 = 2;
                Integer fileIndex2 = 0;
                String label2 = "image";
                this.addOutputFile(file2, nodeIndex2, fileIndex2, label2);
                
                Integer nodeIndex3 = 3;
                Integer fileIndex3 = 0;
                String label3 = "html";
                this.addOutputFile(file3, nodeIndex3, fileIndex3, label3);
                
                Integer nodeIndex4 = 4;
                Integer fileIndex4 = 0;
                String label4 = "image";
                this.addOutputFile(file4, nodeIndex4, fileIndex4, label4);
                
                 Integer nodeIndex5 = 5;
                Integer fileIndex5 = 0;
                String label5 = "image";
                this.addOutputFile(file5, nodeIndex5, fileIndex5, label5);
                
            } else {
                this.addErrorMessage("An unknown error has occurred with the Descriptive component.");
            }

        }

        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
    }

}
