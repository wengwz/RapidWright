package com.xilinx.rapidwright.rapidpnr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.xilinx.rapidwright.edif.EDIFCell;
import com.xilinx.rapidwright.edif.EDIFCellInst;
import com.xilinx.rapidwright.edif.EDIFLibrary;
import com.xilinx.rapidwright.edif.EDIFNet;
import com.xilinx.rapidwright.edif.EDIFPort;
import com.xilinx.rapidwright.edif.EDIFPortInst;

public class NetlistUtils {
    // public static final HashSet<String> regCellTypeNames = new HashSet<>(Arrays.asList("FDSE", "FDRE", "FDCE", "FDPE", "SRL16E"));
    public static final HashSet<String> regCellTypeNames = new HashSet<>(Arrays.asList("FDSE", "FDRE", "FDCE", "FDPE"));
    public static final HashSet<String> ioBufCellTypeNames = new HashSet<>(Arrays.asList("OBUF", "IBUFCTRL", "INBUF", "IBUF", "BUFGCE"));
    public static final HashSet<String> lutCellTypeNames = new HashSet<>(Arrays.asList("LUT1", "LUT2", "LUT3", "LUT4", "LUT5", "LUT6"));
    public static final HashSet<String> srlCellTypeNames = new HashSet<>(Arrays.asList("SRL16E", "SRLC32E"));

    // public static final HashSet<String> resTypeNames = new HashSet<>(Arrays.asList("LUT", "FF", "CARRY", "DSP", "BRAM", "IO", "URAM", "MISCS"));
    public static final HashSet<String> resTypeNames = new HashSet<>(Arrays.asList("LUT", "LUTM", "FF", "CARRY", "DSP", "BRAM", "IO", "URAM", "MISCS"));

    public static final HashMap<String, String> cellType2ResTypeMap = new HashMap<String, String>() {
        {
            // LUT
            put("LUT1", "MISCS");
            
            put("LUT2", "LUT");
            put("LUT3", "LUT");
            put("LUT4", "LUT");
            put("LUT5", "LUT");
            put("LUT6", "LUT");
            
            // put("RAMD32", "LUT");
            // put("RAMS32", "LUT");
            // put("RAMD64E", "LUT");
            // put("SRL16E", "LUT");
            // put("SRLC32E", "LUT");
            // put("RAMS64E", "LUT");

            put("RAMD32",  "LUTM");
            put("RAMS32",  "LUTM");
            put("RAMD64E", "LUTM");
            put("SRL16E",  "LUTM");
            put("SRLC32E", "LUTM");
            put("RAMS64E", "LUTM");

            // CARRY
            put("CARRY8", "CARRY");

            // FF
            put("FDSE", "FF");
            put("FDRE", "FF");
            put("FDCE", "FF");
            put("FDPE", "FF");

            // BRAM
            put("RAMB36E2", "BRAM");
            put("RAMB18E2", "BRAM");

            // IO
            put("OBUF", "IO");
            put("INBUF", "IO");
            put("IBUFCTRL", "IO");
            put("BUFGCE", "IO");

            // MISCS
            put("MUXF7", "MISCS");
            put("MUXF8", "MISCS");
            put("INV", "MISCS");
            put("VCC", "MISCS");
            put("GND", "MISCS");

            // DSP
            put("DSP48E2", "DSP");
            
            // URAM
            put("URAM288", "URAM");
        }
    };

    public static HashSet<String> pseudoLeafCellNames = new HashSet<>(Arrays.asList("DSP48E2"));

    public static Map<String, Map<String, Integer>> nonPrimUnisimCellUtils;

    static {
        nonPrimUnisimCellUtils = new HashMap<>();
        nonPrimUnisimCellUtils.put("RAM32M", new HashMap<>() {
            {
                put("RAMD32", 3);
                put("RAMS32", 1);
            }
        });

        nonPrimUnisimCellUtils.put("RAM32M16", new HashMap<>() {
            {
                put("RAMD32", 7);
                put("RAMS32", 1);
            }
        });
    }

    public static Boolean isRegisterCellInst(EDIFCellInst cellInst) {
        return regCellTypeNames.contains(cellInst.getCellType().getName());
    }
    public static Boolean isLutCellInst(EDIFCellInst cellInst) {
        return lutCellTypeNames.contains(cellInst.getCellName());
    }
    public static Boolean isLutOneCellInst(EDIFCellInst cellInst) {
        return cellInst.getCellName().equals("LUT1");
    }
    public static Boolean isSRLCellInst(EDIFCellInst  cellInst) {
        return srlCellTypeNames.contains(cellInst.getCellType().getName());
    }
    public static Boolean isIOBufCellInst(EDIFCellInst cellInst) {
        return ioBufCellTypeNames.contains(cellInst.getCellType().getName());
    }

    public static List<EDIFPortInst> getSinkPortsOf(EDIFNet net) {
        List<EDIFPortInst> sinkPortInsts = new ArrayList<>();
        for (EDIFPortInst portInst : net.getPortInsts()) {
            if (portInst.getCellInst() == null) {
                if (portInst.isOutput()) { // top-level port
                    sinkPortInsts.add(portInst);
                }
            } else {
                if (portInst.isInput()) { // ports of internal cells
                    sinkPortInsts.add(portInst);
                }
            }
        }
        return sinkPortInsts;
    }

    public static List<EDIFPortInst> getOutPortInstsOf(EDIFCellInst cellInst) {
        List<EDIFPortInst> outputPortInsts = new ArrayList<>();
        for (EDIFPortInst portInst : cellInst.getPortInsts()) {
            if (portInst.isOutput()) {
                outputPortInsts.add(portInst);
            }
        }
        return outputPortInsts;
    }

    public static List<EDIFPortInst> getInPortInstsOf(EDIFCellInst cellInst) {
        List<EDIFPortInst> inputPortInsts = new ArrayList<>();
        for (EDIFPortInst portInst : cellInst.getPortInsts()) {
            if (portInst.isInput()) {
                inputPortInsts.add(portInst);
            }
        }
        return inputPortInsts;
    }

    public static void getLeafCellUtils(EDIFCell cell, Map<EDIFCell, Integer> leafCellUtilMap) {
        assert leafCellUtilMap != null;
        if (cell.isLeafCellOrBlackBox() || pseudoLeafCellNames.contains(cell.getName())) {
            if (leafCellUtilMap.containsKey(cell)) {
                Integer amount = leafCellUtilMap.get(cell);
                leafCellUtilMap.replace(cell, amount + 1);
            } else {
                leafCellUtilMap.put(cell, 1);
            }
        } else if (nonPrimUnisimCellUtils.containsKey(cell.getName())) {
            EDIFLibrary hdiPrimLibrary = cell.getLibrary().getNetlist().getHDIPrimitivesLibrary();
            
            Map<String, Integer> unisimCellUtilMap = nonPrimUnisimCellUtils.get(cell.getName());
            for (Map.Entry<String, Integer> entry : unisimCellUtilMap.entrySet()) {
                String primCellName = entry.getKey();
                Integer amount = entry.getValue();
                EDIFCell primCell = hdiPrimLibrary.getCell(primCellName);
                if (leafCellUtilMap.containsKey(primCell)) {
                    Integer originAmount = leafCellUtilMap.get(primCell);
                    leafCellUtilMap.replace(primCell, originAmount + amount);
                } else {
                    leafCellUtilMap.put(primCell, amount);
                }
            }

        } else {
            for (EDIFCellInst childCellInst : cell.getCellInsts()) {
                getLeafCellUtils(childCellInst.getCellType(), leafCellUtilMap);
            }
        }
    }

    public static void calibrateLUTUtils(EDIFCell topCell, Map<EDIFCell, Integer> leafCellUtilMap) {
        calibrateLUTUtils(new HashSet<>(topCell.getCellInsts()), leafCellUtilMap);
    }
    public static void calibrateLUTUtils(Set<EDIFCellInst> cellInstsRange, Map<EDIFCell, Integer> leafCellUtilMap) {
        Set<String> packedLUTCellTypeName = new HashSet<>(Arrays.asList("LUT2", "LUT3", "LUT4", "LUT5"));
        //Set<String> packedLUTCellTypeName = new HashSet<>(Arrays.asList("LUT4", "LUT5"));

        Set<EDIFCellInst> packedLUTCellInsts = new HashSet<>();

        for (EDIFCellInst cellInst : cellInstsRange) {
            String cellTypeName = cellInst.getCellType().getName();

            if (!packedLUTCellTypeName.contains(cellTypeName)) continue;
            if (packedLUTCellInsts.contains(cellInst)) continue;

            Map<EDIFCellInst, Integer> neighbor2CommonNetNum = new HashMap<>();
            
            List<EDIFPortInst> inputPorts = getInPortInstsOf(cellInst);
            for (EDIFPortInst portInst : inputPorts) {
                EDIFNet net = portInst.getNet();
                
                for (EDIFPortInst sinkPortInst : getSinkPortsOf(net)) {
                    EDIFCellInst sinkCellInst = sinkPortInst.getCellInst();
                    if (sinkCellInst == null || sinkCellInst == cellInst) continue;

                    if (sinkCellInst.getCellName().equals(cellTypeName)) {
                        if (neighbor2CommonNetNum.containsKey(sinkCellInst)) {
                            Integer netNum = neighbor2CommonNetNum.get(sinkCellInst);
                            neighbor2CommonNetNum.replace(sinkCellInst, netNum + 1);
                        } else {
                            neighbor2CommonNetNum.put(sinkCellInst, 1);
                        }
                    }
                }
            }

            List<Map.Entry<EDIFCellInst, Integer>> sortedNeighbors = neighbor2CommonNetNum.entrySet().stream()
            .sorted(Map.Entry.<EDIFCellInst, Integer>comparingByValue().reversed()).collect(Collectors.toList());

            //Set<Map.Entry<EDIFCellInst, Integer>> sortedNeighbors = neighbor2CommonNetNum.entrySet();
            for (Map.Entry<EDIFCellInst, Integer> neighbor : sortedNeighbors) {
                
                EDIFCellInst neighborCellInst = neighbor.getKey();
                Integer commonNetNum = neighbor.getValue();
                
                Integer packThreshold = inputPorts.size();
                if (cellTypeName.equals("LUT3")) {
                    packThreshold = 2;
                } else if (cellTypeName.equals("LUT2")) {
                    packThreshold = 1;
                }
                
                if (commonNetNum >= packThreshold) {
                    packedLUTCellInsts.add(neighborCellInst);
                    Integer amount = leafCellUtilMap.get(neighborCellInst.getCellType());
                    leafCellUtilMap.replace(neighborCellInst.getCellType(), amount - 1);
                    break;
                } 
            }
        }

        // for (EDIFCell cellType : leafCellUtilMap.keySet()) {
        //     Integer amount = leafCellUtilMap.get(cellType);
        //     if (cellType.getName().equals("LUT2")) {
        //         leafCellUtilMap.replace(cellType, (int) Math.ceil(amount / 3.0));
        //     } else if (cellType.getName().equals("LUT3")) {
        //         leafCellUtilMap.replace(cellType, (int) Math.ceil(amount / 2.0));
        //     }
        // }

    }

    public static Map<String, Integer> getResTypeUtils(EDIFCell cell) {
        Map<EDIFCell, Integer> leafCellUtilMap = new HashMap<>();
        getLeafCellUtils(cell, leafCellUtilMap);
        return getResTypeUtils(leafCellUtilMap);
    }

    public static Map<String, Integer> getResTypeUtils(Map<EDIFCell, Integer> primCellUtilMap) {
        Map<String, Integer> resTypeUtil = new HashMap<>();
        for (Map.Entry<EDIFCell, Integer> entry : primCellUtilMap.entrySet()) {
            EDIFCell primCell = entry.getKey();
            Integer amount = entry.getValue();

            assert cellType2ResTypeMap.containsKey(primCell.getName()): String.format("Cell type %s not found", primCell.getName());
            String resType = cellType2ResTypeMap.get(primCell.getName());
            if (resTypeUtil.containsKey(resType)) {
                Integer resTypeAmount = resTypeUtil.get(resType);
                resTypeUtil.replace(resType, resTypeAmount + amount);
            } else {
                resTypeUtil.put(resType, amount);
            }
        }
        return resTypeUtil;
    }

    public static List<EDIFCellInst> getCellInstsOfNet(EDIFNet net) {
        List<EDIFCellInst> cellInsts = new ArrayList<>();
        for (EDIFPortInst portInst : net.getPortInsts()) {
            EDIFCellInst cellInst = portInst.getCellInst();
            if (cellInst == null) continue;
            cellInsts.add(cellInst);
        }
        return cellInsts;
    }

    public static EDIFCellInst getSourceCellInstOfNet(EDIFNet net) {
        List<EDIFPortInst> srcPortInsts = net.getSourcePortInsts(true);
        assert srcPortInsts.size() == 1;
        return srcPortInsts.get(0).getCellInst();
    }

    public static List<String> getCellInstsNameOfNet(EDIFNet net) {
        List<String> cellInstNames = new ArrayList<>();
        for (EDIFPortInst portInst : net.getPortInsts()) {
            EDIFCellInst cellInst = portInst.getCellInst();
            if (cellInst == null) continue;
            cellInstNames.add(cellInst.getName());
        }
        return cellInstNames;
    }

    public static Boolean isRegFanoutNet(EDIFNet net) {
        List<EDIFPortInst> srcPortInsts = net.getSourcePortInsts(true);
        assert srcPortInsts.size() == 1: String.format("Net %s has %d source ports", net.getName(), srcPortInsts.size());
        EDIFPortInst srcPortInst = srcPortInsts.get(0);
        EDIFCellInst srcCellInst = srcPortInst.getCellInst();
        if (srcCellInst == null) return false;
        return isRegisterCellInst(srcCellInst);
    }

    public static EDIFCellInst registerReplication(EDIFCellInst originCellInst, String repCellInstName, List<EDIFPortInst> transferPortInsts) {
        // replicate register originCellInst and transfer fanout cell insts specified in transferCellInsts to new register
        assert isRegisterCellInst(originCellInst): "originCellInst must be register";
        EDIFCell originCellType = originCellInst.getCellType();
        EDIFCell parentCell = originCellInst.getParentCell();
        // check name confliction
        assert parentCell.getCellInst(repCellInstName) == null: String.format("Cell instance %s already exists", repCellInstName);
        assert parentCell.getNet(repCellInstName) == null: String.format("Net %s already exists", repCellInstName);

        EDIFCellInst repCellInst = parentCell.createChildCellInst(repCellInstName, originCellType);
        repCellInst.setPropertiesMap(originCellInst.createDuplicatePropertiesMap());

        // copy port connections
        for (EDIFPortInst portInst : originCellInst.getPortInsts()) {
            EDIFPort port = portInst.getPort();
            EDIFNet net = portInst.getNet();

            if (portInst.isInput()) { // copy connections of input ports
                net.createPortInst(port, repCellInst);
            } else { // transfer connections with specified output ports to new replicated cellInsts
                Set<EDIFPortInst> fanoutPortInsts = new HashSet<>();
                fanoutPortInsts.addAll(getSinkPortsOf(net));
                assert transferPortInsts.size() < fanoutPortInsts.size();

                EDIFNet newNet = parentCell.createNet(repCellInstName);
                newNet.createPortInst(port, repCellInst);
                for (EDIFPortInst transferPortInst : transferPortInsts) {
                    assert fanoutPortInsts.contains(transferPortInst):
                    String.format("Port %s is not incident to cell instance %s", transferPortInst.getName(), originCellInst.getName());

                    net.removePortInst(transferPortInst);
                    newNet.addPortInst(transferPortInst);
                }
            }
        }
        return repCellInst;

    }

    public boolean isCellHasIllegalNet(EDIFCell topCell) {
        boolean hasIllegalNet = false;
        for (EDIFNet net : topCell.getNets()) {
            int netDegree = net.getPortInsts().size();
            int srcPortInstNum = net.getSourcePortInsts(true).size();

            if (netDegree <= 1 || srcPortInstNum != 1) {
                hasIllegalNet = true;
                break;
            }
        }
        return hasIllegalNet;
    }
        
}