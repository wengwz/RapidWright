package com.xilinx.rapidwright.rapidpnr;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.nio.file.Path;

import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.edif.EDIFCell;
import com.xilinx.rapidwright.edif.EDIFCellInst;
import com.xilinx.rapidwright.edif.EDIFPort;
import com.xilinx.rapidwright.util.FileTools;

public class VivadoTclUtils {

    public static class TclCmdFile {

        ArrayList<String> cmdLines;

        public TclCmdFile() {
            cmdLines = new ArrayList<>();
        }

        public void addCmd(String cmd) {
            cmdLines.add(cmd);
        }

        public void addCmds(List<String> cmds) {
            for (String cmd : cmds) {
                cmdLines.add(cmd);
            }
        }

        public void writeToFile(Path filePath) {
            FileTools.writeLinesToTextFile(cmdLines, filePath.toString());
        }
    }

    public static class VivadoTclCmd  {

        public static class IncrImplDirective {
            public static final String RuntimeOpt = "RuntimeOptimized";
            public static final String TimingClosure = "TimingClosure";
            public static final String Quick = "Quick";
        }

        public static class PlacerDirective {
            public static final String Default = "Default";
            public static final String RuntimeOpt = "RuntimeOptimized";
            public static final String Quick = "Quick";
        }

        public static void drawPblock(Design design, String pblockName, String pblockRange) {
            design.addXDCConstraint(String.format("create_pblock %s", pblockName));
            design.addXDCConstraint(String.format("resize_pblock %s -add { %s }", pblockName, pblockRange));
        }
    
        public static void setPblockProperties(Design design, String pblockName, Boolean isSoft, Boolean excludePlace, Boolean containRouting) {
            if (!isSoft) {
                design.addXDCConstraint(String.format("set_property IS_SOFT FALSE [get_pblocks %s]", pblockName));
            }
            if (excludePlace) {
                design.addXDCConstraint(String.format("set_property EXCLUDE_PLACEMENT true [get_pblocks %s]", pblockName));
            }
            if (containRouting) {
                design.addXDCConstraint(String.format("set_property CONTAIN_ROUTING true [get_pblocks %s]", pblockName));
            }
        }
    
        public static void addCellToPblock(Design design, String pblockName, String cellName) {
            design.addXDCConstraint(String.format("add_cells_to_pblock %s [get_cells %s]", pblockName, cellName));
        }

        public static void addCellToPBlock(Design design, String pblockName) {
            design.addXDCConstraint(String.format("add_cells_to_pblock %s -top", pblockName));
        }
    
        public static void addStrictPblockConstr(Design design, EDIFCellInst cellInst, String pblockRange) {
            String pblockName = "pblock_" + cellInst.getName();
            drawPblock(design, pblockName, pblockRange);
            setPblockProperties(design, pblockName, false, true, true);
            //setPblockProperties(design, pblockName, false, false, true);
            addCellToPblock(design, pblockName, cellInst.getName());
        }

        public static void addStrictPblockConstr(Design design, String pblockRange) {
            String pblockName = "pblock_" + design.getName();
            drawPblock(design, pblockName, pblockRange);
            setPblockProperties(design, pblockName, false, true, true);
            addCellToPBlock(design, pblockName);
        }

        public static void addPblockConstr(Design design, EDIFCellInst cellInst, String pblockRange, Boolean isSoft, Boolean excludePlace, Boolean containRouting) {
            
            String pblockName = "pblock_";
            if (cellInst != null) {
                pblockName += cellInst.getName();
            } else {
                pblockName += design.getName();
            }
            drawPblock(design, pblockName, pblockRange);
            setPblockProperties(design, pblockName, isSoft, excludePlace, containRouting);

            if (cellInst != null) {
                addCellToPblock(design, pblockName, cellInst.getName());
            } else {
                addCellToPBlock(design, pblockName);
            }
        }

        public static String setClockGroups(boolean isAsync, Set<String> clkGroups) {
            String cmdStr = "set_clock_groups";
            if (isAsync) {
                cmdStr += " -asynchronous";
            }
            for (String clkGroup : clkGroups) {
                cmdStr += " -group " + clkGroup;
            }
            return cmdStr;
        }

        public static void setClockGroups(Design design, boolean isAsync, Set<String> clkGroups) {
            design.addXDCConstraint(setClockGroups(isAsync, clkGroups));
        }

        public static void setAsyncClockGroupsForEachClk(Design design, Collection<String> clkPortNames) {
            EDIFCell topCell = design.getNetlist().getTopCell();
            Set<String> validClkPortNames = new HashSet<>();

            for (String clkPortName : clkPortNames) {
                if (topCell.getPort(clkPortName) != null) {
                    validClkPortNames.add(clkPortName);
                }
            }
            if (clkPortNames.size() > 1) {
                design.addXDCConstraint(setClockGroups(true, validClkPortNames));
            }
        }

        public static String createClock(String clkPortName, Double period) {
            return String.format("create_clock -period %f -name %s [get_ports %s]", period, clkPortName, clkPortName);
        }

        public static void createClock(Design design, String clkPortName, Double period) {
            design.addXDCConstraint(createClock(clkPortName, period));
        }

        public static void createClocks(Design design, Map<String, Double> clk2PeriodMap) {
            for (String clkName : clk2PeriodMap.keySet()) {
                EDIFCell topCell = design.getNetlist().getTopCell();
                if (topCell.getPort(clkName) != null) {
                    createClock(design, clkName, clk2PeriodMap.get(clkName));
                }
            }
        }
    
        public static void addIODelayConstraint(Design design, EDIFPort port, String clkName, Double delay) {
            String commandStr = port.isInput() ? "set_input_delay" : "set_output_delay";
            String portName = port.getName();
            String constrStr = String.format("%s -clock %s %f %s", commandStr, clkName, delay, portName);
            design.addXDCConstraint(constrStr);
        }
        
        public static String readCheckpoint(String cellInstName, boolean autoIncr, boolean incr, String incrDirective, String dcpPath) {
            String cmdStr = "read_checkpoint";

            if (cellInstName != null) {
                cmdStr += " -cell " + cellInstName;
            }

            if (autoIncr) {
                cmdStr += " -auto_incremental";
            } else if (incr) {
                cmdStr += " -incremental";
                if (incrDirective != null) {
                    cmdStr += " -directive " + incrDirective;
                }
            }

            cmdStr += " " + dcpPath;
            return cmdStr;
        }

        public static String readCheckpoint(String cellInstName, String dcpPath) {
            return readCheckpoint(cellInstName, false, false, null, dcpPath);
        }

        public static String readCheckpoint(Map<String, Path> cellInst2DcpMap) {
            String cellInst2DcpStr = "";
            for (String cellInstName : cellInst2DcpMap.keySet()) {
                if (cellInst2DcpStr.length() > 0) {
                    cellInst2DcpStr += " ";
                }
                cellInst2DcpStr += cellInstName + " " + cellInst2DcpMap.get(cellInstName).toString();
            }
            return String.format("read_checkpoint -dcp_cell_list {%s}", cellInst2DcpStr);
        }
    
        public static String openCheckpoint(String dcpPath) {
            return String.format("open_checkpoint %s", dcpPath);
        }
    
        public static String writeCheckpoint(boolean force, String cellInstName, String dcpPath) {
            String cmdStr = "write_checkpoint";
            if (force) {
                cmdStr += " -force";
            }
            if (cellInstName != null) {
                cmdStr += " -cell " + cellInstName;
            }
            assert dcpPath != null;
            return cmdStr + " " + dcpPath;
        }

        public static String writeEDIF(boolean force, String cellInstName, String edifPath) {
            String cmdStr = "write_edif";
            if (force) {
                cmdStr += " -force";
            }
            if (cellInstName != null) {
                cmdStr += " -cell " + cellInstName;
            }
            assert edifPath != null;
            return cmdStr + " " + edifPath;
        }
    
        public static String placeDesign(String directive, boolean noPSIP) {
            String cmdString = "place_design";
            if (directive != null) {
                cmdString += " -directive " + directive;
            }

            if (noPSIP) {
                cmdString += " -no_psip";
            }
            
            return cmdString;
        }

        public static String placeDesign() {
            return placeDesign(null, false);
        }
    
        public static String routeDesign(String directive) {
            String cmdString = "route_design";
            if (directive != null) {
                cmdString += " -directive " + directive;
            }
            return cmdString;
        }
    
        public static String physOptDesign() {
            return "phys_opt_design";
        }

        public static List<String> conditionalPhysOptDesign() {
            List<String> cmds = new ArrayList<>();
            cmds.add("if {[get_property SLACK [get_timing_paths -max_paths 1 -nworst 1 -setup]] < 0} {");
            cmds.add("    phys_opt_design");
            cmds.add("}");
            return cmds;
        }
    
        public static String reportTimingSummary(int maxPathNum, String filePath) {
            if (maxPathNum <= 0) {
                return String.format("report_timing_summary -file %s", filePath);
            } else {
                return String.format("report_timing_summary -max %d -file %s", maxPathNum, filePath);
            }
        }
    
        public static String updateCellBlackbox(String cellInstName) {
            return String.format("update_design -cells [get_cells %s] -black_box", cellInstName);
        }
    
        public static String lockDesign(boolean unlock, String level, String cellInstName) {
            String cmdStr = "lock_design";
            if (unlock) {
                cmdStr += " -unlock";
            }
            if (level != null) {
                cmdStr += String.format(" -level %s", level);
            }
    
            if (cellInstName != null) {
                cmdStr += " " + cellInstName;
            }
            return cmdStr;
        }
    
        public static String setProperty(String propertyName, boolean value, String cellInstName) {
            String valStr = value ? "TRUE" : "FALSE";
            String targetName = "[current_design]";
            if (cellInstName != null) {
                targetName = String.format("[get_cells %s]", cellInstName);
            }
    
            return String.format("set_property %s %s %s", propertyName, valStr, targetName); 
        }
    
        public static String setPropertyHDReConfig(boolean val, String cellInstName) {
            return setProperty("HD.RECONFIGURABLE", val, cellInstName);
        }
    
        public static void setPropertyHDReConfig(Design design, EDIFCellInst cellInst) {
            design.addXDCConstraint(setPropertyHDReConfig(true, cellInst.getName()));
        }
    
        public static String setPropertyHDPartition(boolean val, String cellInstName) {
            return setProperty("HD.PARTITION", val, cellInstName);
        }
    
        public static void setPropertyHDPartition(Design design) {
            design.addXDCConstraint(setPropertyHDPartition(true, null));
        }
    
        public static void setPropertyHDPartition(Design design, EDIFCellInst cellInst) {
            design.addXDCConstraint(setPropertyHDPartition(true, cellInst.getName()));
        }

        public static void setPropertyDontTouch(Design design, String cellInstName) {
            // set DONT_TOUCH property TRUE for specific cell inst
            assert cellInstName != null;
            design.addXDCConstraint(setPropertyDontTouch(true, cellInstName));
        }
    
        public static String setPropertyDontTouch(boolean val, String cellInstName) {
            assert cellInstName != null;
            return setProperty("DONT_TOUCH", val, cellInstName);
        }
    
        public static String exitVivad() {
            return "exit";
        }
    
        public static String setMaxThread(int maxThreadNum) {
            return String.format("set_param general.maxThreads %d", maxThreadNum);
        }
    
    }
}
