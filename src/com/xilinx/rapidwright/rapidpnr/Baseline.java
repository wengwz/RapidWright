package com.xilinx.rapidwright.rapidpnr;

import java.nio.file.Path;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;

import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.rapidpnr.VivadoTclUtils.VivadoTclCmd;
import com.xilinx.rapidwright.util.Job;
import com.xilinx.rapidwright.util.JobQueue;
import com.xilinx.rapidwright.rapidpnr.VivadoTclUtils.TclCmdFile;

public class Baseline {

    public static void main(String[] args) {
        String jsonFilePath = "workspace/json/blue-rdma.json";

        Path paramsPath = Path.of(jsonFilePath).toAbsolutePath();
        DesignParams designParams = new DesignParams(paramsPath);
        DirectoryManager dirManager = new DirectoryManager(designParams.getWorkDir());

        HierarchicalLogger logger = new HierarchicalLogger("baseline");
        logger.setUseParentHandlers(false);
        Path logFilePath = dirManager.getRootDir().resolve("baseline.log");
        // Setup Logger
        try {
            FileHandler fileHandler = new FileHandler(logFilePath.toString(), false);
            fileHandler.setFormatter(new CustomFormatter());
            logger.addHandler(fileHandler);

            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new CustomFormatter());
            logger.addHandler(consoleHandler);
        } catch (Exception e) {
            System.out.println("Fail to open log file: " + logFilePath.toString());
        }
        logger.setLevel(Level.INFO);

        // prepare input design
        logger.info("Prepare input design");
        Design inputDesign = Design.readCheckpoint(designParams.getInputDcpPath().toString());
        VivadoTclCmd.addStrictPblockConstr(inputDesign, designParams.getDesignPblockRange());
        String clkPortName = designParams.getClkPortNames().get(0);
        double clkPeriod = designParams.getClkPeriod(clkPortName);
        VivadoTclCmd.addClockConstraint(inputDesign, clkPortName, clkPeriod);

        // prepare tcl command file
        logger.info("Prepare tcl command file");
        TclCmdFile tclCmdFile = new TclCmdFile();
        tclCmdFile.addCmd(VivadoTclCmd.setMaxThread(designParams.getVivadoMaxThreadNum()));
        tclCmdFile.addCmd(VivadoTclCmd.openCheckpoint(VivadoProject.INPUT_DCP_NAME));
        tclCmdFile.addCmd(VivadoTclCmd.placeDesign(null));
        tclCmdFile.addCmd(VivadoTclCmd.routeDesign(null));
        tclCmdFile.addCmds(VivadoTclCmd.conditionalPhysOptDesign());
        tclCmdFile.addCmd(VivadoTclCmd.reportTimingSummary(0, "timing.rpt"));
        tclCmdFile.addCmd(VivadoTclCmd.writeCheckpoint(true, null, VivadoProject.OUTPUT_DCP_NAME));

        // create Vivado project
        logger.info("Create Vivado project");
        Path workDir = dirManager.addSubDir("baseline");
        VivadoProject vivadoProject = new VivadoProject(inputDesign, workDir, "vivado", tclCmdFile);
        Job vivadoJob = vivadoProject.createVivadoJob();

        logger.info("Launch baseline flow");
        JobQueue jobQueue = new JobQueue();
        jobQueue.addJob(vivadoJob);

        long startTime = System.currentTimeMillis();
        boolean success = jobQueue.runAllToCompletion();
        long endTime = System.currentTimeMillis();

        if (success) {
            long duration = (endTime - startTime) / 1000;
            long durationMinute = duration / 60;
            long durationSecond = duration % 60;
            System.out.println("Baseline flow completed in " + durationMinute + ":" + durationSecond + " minutes");
        } else {
            System.out.println("Baseline flow failed");
        }
    }
}
