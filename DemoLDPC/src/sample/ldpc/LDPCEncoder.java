package sample.ldpc;

import sample.ldpc.math.SparseBooleanMatrix2D;

import java.util.Random;


public class LDPCEncoder {

    private float DEFAULT_SPARSE_RATE = 0.01f;

    private float sparseRate = DEFAULT_SPARSE_RATE;
    private SparseBooleanMatrix2D generatorMatrix, parityCheckMatrix;


    public LDPCEncoder(int n, int k) throws IllegalArgumentException {
        if (n == 0) throw new IllegalArgumentException("Parameter n should be a positive number");
        if (k == 0) throw new IllegalArgumentException("Parameter k should be a positive number");
        if (n <= k) throw new IllegalArgumentException("Parameter n should be greater than k");
        generatorMatrix = createGeneratorMatrix(n, k);
        parityCheckMatrix = calculateParityCheckMatrix(generatorMatrix);
    }


    public void setSparseRate(float sparseRate) {
        this.sparseRate = sparseRate;
    }


    public SparseBooleanMatrix2D getParityCheckMatrix() {
        return parityCheckMatrix;
    }


    public boolean[] encode(boolean[] data) throws IllegalArgumentException {
        if (data.length != generatorMatrix.getRowDimension())
            throw new IllegalArgumentException("This Encoder can encode only boolean vectors of size " +
                    generatorMatrix.getRowDimension() + ", size " + data.length + " given");
        SparseBooleanMatrix2D sparseData = new SparseBooleanMatrix2D(data);
        sparseData = sparseData.multiply(generatorMatrix);
        return sparseData.toBooleans()[0];
    }


    public SparseBooleanMatrix2D createGeneratorMatrix(int n, int k) {
        Random rand = new Random();
        SparseBooleanMatrix2D matrix = new SparseBooleanMatrix2D(k, n);
        for (int i = 0; i < k; i++)
            matrix.set(i, i, true);
        for (int i = k; i < n; i++)
            for (int j = 0; j < k; j++)
                if (rand.nextFloat() < sparseRate) matrix.set(j, i, true);
        for (int j = 0; j < k; j++) {
            matrix.set(j, k + rand.nextInt(n - k), true);
            matrix.set(j, k + rand.nextInt(n - k), true);
        }
        return matrix;
    }

    public SparseBooleanMatrix2D calculateParityCheckMatrix(SparseBooleanMatrix2D generatorMatrix) {
        int n = generatorMatrix.getColumnDimension();
        int k = generatorMatrix.getRowDimension();
        SparseBooleanMatrix2D matrix = new SparseBooleanMatrix2D(n - k, n);
        for (int i = 0; i < n - k; i++) {
            for (int j = 0; j < k; j++)
                matrix.set(i, j, generatorMatrix.get(j, i + k));
            matrix.set(i, k + i, true);
        }
        return matrix;
    }

}
