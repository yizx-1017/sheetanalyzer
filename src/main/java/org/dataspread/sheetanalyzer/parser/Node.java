package org.dataspread.sheetanalyzer.parser;

import java.util.List;

public abstract class Node {
}
class OperatorNode extends Node{
    public String value;
    public List<Node> children;
    public boolean isLeafNode = false;
    OperatorNode(String val, List<Node> c){
        this.value = val;
        this.children = c;
    }
}
class OperandNode extends Node{
    public boolean isLeafNode = true;
}

class RefNode extends OperandNode {
    public int rowStart;
    public int colStart;
    public int rowEnd;
    public int colEnd;
    public boolean startRelative;
    public boolean endRelative;
    public boolean isRef = true;

    RefNode(int rowStart,
            int colStart,
            int rowEnd,
            int colEnd,
            boolean startRelative,
            boolean endRelative) {
        this.rowStart = rowStart;
        this.colStart = colStart;
        this.rowEnd = rowEnd;
        this.colEnd = colEnd;
        this.startRelative = startRelative;
        this.endRelative = endRelative;
    }
}

class LiteralNode extends OperandNode {
    public double value;
    public boolean isRef = false;
    LiteralNode(double value) {
        this.value = value;
    }
}
