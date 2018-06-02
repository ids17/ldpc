package sample.ldpc;


import sample.ldpc.math.SparseBooleanMatrix2D;

public class LDPCSimpleDecoder {

    protected SparseBooleanMatrix2D parityCheckMatrix;
    protected int checks, vars;


    public LDPCSimpleDecoder(SparseBooleanMatrix2D parityCheckMatrix) {
        if (parityCheckMatrix == null) throw new IllegalArgumentException();
        this.parityCheckMatrix = parityCheckMatrix.transpose();
        vars = parityCheckMatrix.getColumnDimension();
        checks = parityCheckMatrix.getRowDimension();
    }


    public boolean[] decode(boolean[] data) {
        SparseBooleanMatrix2D sparseData = new SparseBooleanMatrix2D(data);
        SparseBooleanMatrix2D check = sparseData.multiply(parityCheckMatrix);
        if (check.rate() == 0) {
            return cut(data, checks);
        }
        else {
            return tryToDecodeCorruptedData(data);
        }
    }


    //should be overridden by inheritors
    public boolean[] tryToDecodeCorruptedData(boolean[] data) {
        return cut(data, checks);
    }


    boolean[] cut(boolean[] data, int size) {
        if (size > data.length) throw new IllegalArgumentException();
        boolean[] result = new boolean[size];
        for (int i = 0; i < size; i++)
            result[i] = data[i];
        return result;
    }

}
