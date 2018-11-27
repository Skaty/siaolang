import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

// Generates the basic blocks from IR3
class BasicBlock {
    // CFG structure
    public ArrayList<BasicBlock> previous = new ArrayList<>();
    public ArrayList<BasicBlock> next = new ArrayList<>();

    // Liveness information
    public ArrayList<String> liveIn = new ArrayList<>();
    public ArrayList<String> liveOut = new ArrayList<>();
    public HashSet<String> gen = new HashSet<>();
    public HashSet<String> kill = new HashSet<>();
    public boolean hasGenerated = false;

    // Stmts in Block
    public ArrayList<Stmt3> stmts = new ArrayList<>();

    // Bookkeeping
    public boolean recToggle = false;
    public boolean isFirst = false;

    /**
     * State of registers after running basic block
     */
    public CodeGen.BasicBlockState bbsOut;

    public BasicBlock() { }

    /**
     * Generates a Control Flow Graph from a CMtd3
     * Returns the first block of the CFG
     */
    public static BasicBlock fromCMtd3(CMtd3 mtd3) {
        BasicBlock firstBlock = null;
        BasicBlock curBlock = null;
        // Last blocks
        ArrayList<BasicBlock> blocks = new ArrayList<>();

        HashMap<String, BasicBlock> targetBlocks = new HashMap<>();
        HashMap<String, ArrayList<BasicBlock>> unconnectedBlocks = new HashMap<>();
        Stmt3 prev = null;

        for (Stmt3 stmt : mtd3.stmts) {
            if (BasicBlock.isLeader(prev, stmt)) {
                if (firstBlock == null) {
                    // First block creation
                    firstBlock = new BasicBlock();
                    firstBlock.isFirst = true;
                    curBlock = firstBlock;
                    targetBlocks.put(stmt.children.get(0).getLabelledValue(), firstBlock);
                } else {
                    if (prev.type == Stmt3.StmtType.IfGoto || prev.type == Stmt3.StmtType.Goto) {
                        String lbl = (prev.type == Stmt3.StmtType.IfGoto) ? prev.children.get(1).getLabelledValue() : prev.children.get(0).getLabelledValue();
                        if (targetBlocks.containsKey(lbl)) {
                            targetBlocks.get(lbl).previous.add(curBlock);
                            curBlock.next.add(targetBlocks.get(lbl));
                        } else {
                            ArrayList<BasicBlock> blockArray = unconnectedBlocks.getOrDefault(lbl, new ArrayList<>());
                            blockArray.add(curBlock);
                            unconnectedBlocks.putIfAbsent(lbl, blockArray);
                        }
                    } else if (prev.type == Stmt3.StmtType.ReturnT
                                || prev.type == Stmt3.StmtType.ReturnV) {
                        //
                    }

                    BasicBlock newBlock = new BasicBlock();

                    if (stmt.type == Stmt3.StmtType.Label) {
                        String lbl = stmt.children.get(0).getLabelledValue();
                        targetBlocks.put(lbl, newBlock);
                        for (BasicBlock blk : unconnectedBlocks.getOrDefault(lbl, new ArrayList<>())) {
                            blk.next.add(newBlock);
                            newBlock.previous.add(blk);
                        }
                        unconnectedBlocks.remove(lbl);
                    }

                    if (prev.type != Stmt3.StmtType.Goto
                        && prev.type != Stmt3.StmtType.ReturnT
                        && prev.type != Stmt3.StmtType.ReturnV) {
                        curBlock.next.add(newBlock);
                        newBlock.previous.add(curBlock);
                    }

                    curBlock = newBlock;
                }

                blocks.add(curBlock);
            }


            if (firstBlock == null) {
                firstBlock = new BasicBlock();
                curBlock = firstBlock;
                blocks.add(curBlock);
            }

            curBlock.stmts.add(stmt);

            prev = stmt;
        }

        //System.err.println(firstBlock);

        // Only keep last blocks
        blocks.removeIf(blk -> !blk.next.isEmpty());
        BasicBlock.analyseLiveness(blocks);

        return firstBlock;
    }

    public static void analyseLiveness(ArrayList<BasicBlock> lastBlocks) {
        ArrayDeque<BasicBlock> blockQueue = new ArrayDeque<>(lastBlocks);

        while (!blockQueue.isEmpty()) {
            BasicBlock curBlock = blockQueue.pollFirst();
            curBlock.genGenKill();
        }
    } 

    private static boolean isLeader(Stmt3 prev, Stmt3 stmt) {
        if (prev == null) {
            return (stmt.type == Stmt3.StmtType.Label);
        }

        return (prev.type == Stmt3.StmtType.IfGoto)
            || (prev.type == Stmt3.StmtType.Goto)
            || (prev.type == Stmt3.StmtType.ReturnT)
            || (prev.type == Stmt3.StmtType.ReturnV)
            || (stmt.type == Stmt3.StmtType.Label);
    }

    public void genGenKill() {
        for (Stmt3 stmt : stmts) {
            switch (stmt.type) {
                case IfGoto:
                case PrintLn:
                    // Only read cases

                case ReadLn:
                    // Only write cases

                case LAssign:
            }
        }
    }

    private String toString(boolean curTest) {
        if (recToggle != curTest) {
            return "recurse";
        }

        ArrayList<String> nextPrint = new ArrayList<>();
        recToggle = !recToggle;

        for (BasicBlock nxt : next) {
            nextPrint.add(nxt.toString(!recToggle));
        }

        return String.format("%s => {%s}",
            this.stmts,
            nextPrint
        );
    }

    public String toString() {
        return this.toString(this.recToggle);
    }
}