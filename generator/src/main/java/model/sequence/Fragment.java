package model.sequence;

import java.util.ArrayList;
import java.util.List;

public class Fragment {
    private String id;
    private String operator;          // "loop", "opt", "alt"
    private List<Operand> operands;

    public Fragment() {
        this.operands = new ArrayList<>();
    }

    public Fragment(String operator) {
        this.operator = operator;
        this.operands = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public List<Operand> getOperands() {
        return operands;
    }

    public void addOperand(Operand operand) {
        this.operands.add(operand);
    }

    @Override
    public String toString() {
        return "Fragment{" +
                "operator='" + operator + '\'' +
                ", operands=" + operands +
                '}';
    }
}