package sample.ldpc;

import sample.ldpc.math.SparseBooleanMatrix2D;

import java.util.Arrays;
import java.util.Collections;

public class ADMMDecoder extends LDPCSimpleDecoder {

    private final float DEFAULT_PENALTY = 2;
    private final float DEFAULT_ERROR_PROBABILITY = 0.2f;
    private final int DEFAULT_ITERATIONS = 50;

    private float zeroLikelihood, oneLikelihood;
    private int iterations = DEFAULT_ITERATIONS;
    private float penalty = DEFAULT_PENALTY;
    private TannerGraph messages;


    public ADMMDecoder(SparseBooleanMatrix2D parityCheckMatrix) {
        super(parityCheckMatrix);
        setErrorProbability(DEFAULT_ERROR_PROBABILITY);
        messages = new TannerGraph(this.parityCheckMatrix);
    }


    public void setIterations(int iterations) {
        this.iterations = iterations;
    }


    public void setPenalty(float penalty) {
        this.penalty = penalty;
    }


    public void setErrorProbability(float p) {
        zeroLikelihood = (float) Math.log((1 - p) / p);
        oneLikelihood = (float) Math.log(p / (1 - p));
    }

    @Override
    public boolean[] tryToDecodeCorruptedData(boolean[] data) {

        if (data.length != vars)
            throw new IllegalArgumentException("Vector for decoding must be " + vars + " size");

        float[] x = new float[vars];

        //generate lambdas
        float[][] l = new float[checks][];
        for (int j = 0; j < l.length; j++) {
            l[j] = new float[messages.checks.get(j).size()];
        }

        for (int count = 0; count < iterations; count++) {

            //for each var
            for (int i = 0; i < vars; i++) {
                float t = 0, s;
                for (TannerGraph.Edge edge: messages.vars.get(i)) t += edge.value;
                if (data[i]) t -= oneLikelihood;
                else t -= zeroLikelihood;
                s = t + sign(t) * penalty;
                int d = messages.vars.get(i).size();
                x[i] = s;
                if (d != 0) x[i] /= d;
                if (x[i] < -0.5f) x[i] = -0.5f;
                if (x[i] > 0.5f) x[i] = 0.5f;
            }

            //for each check
            for (int j = 0; j < checks; j++) {

                int d = l[j].length;
                float[] v = new float[d];
                for (int k = 0; k < d; k++) {
                    int index = messages.checks.get(j).get(k).var;
                    v[k] = x[index] + l[j][k];
                }

                float[] z = projectOntoPolytope(v);

                for (int k = 0; k < d; k++) {
                    l[j][k] = v[k] - z[k];
                    messages.checks.get(j).get(k).value = 2 * z[k] - v[k];
                }
            }
        }

        boolean[] result = new boolean[vars - checks];
        for (int i = 0; i < vars - checks; i++)
            result[i] = x[i] > 0;
        return result;
    }

    public float[] projectOntoPolytope(float[] vector) {

        int d = vector.length;
        boolean f[] = new boolean[d];
        float v[] = new float[d];
        float u[] = new float[d];
        boolean isEven = true;

        for (int i = 0; i < d; i++) {
            if (vector[i] >= 0) {
                f[i] = true;
                isEven = !isEven;
            }
        }

        if(isEven) {
            float min = Math.abs(vector[0]);
            int minIndex = 0;
            for (int i = 0; i < d; i++) {
                if (Math.abs(vector[i]) < min) {
                    min = Math.abs(vector[i]);
                    minIndex = i;
                }
            }
            f[minIndex] = !f[minIndex];
        }

        for (int i = 0; i < d; i++) {
            if (f[i]) v[i] = -vector[i];
            else v[i] = vector[i];
        }

        float[] us = simplexProjection(v);
        for (int i = 0; i < d; i++) {
            if (f[i]) u[i] = -us[i];
            else u[i] = us[i];
        }

        float[] vProjection = projectOntoCentredHypercube(0.5f, v);
        float check = 0;
        for (int i = 0; i < d; i++)
            check += vProjection[i];
        if (check >= 1 - d / 2f) return projectOntoCentredHypercube(0.5f, vector);
        else return u;

    }

    public float[] simplexProjection(float[] vector) {

        int d = vector.length;
        Float[] p = new Float[d];
        for (int i = 0; i < d; i++) p[i] = vector[i];
        Arrays.sort(p, Collections.reverseOrder());

        float u[] = new float[d];
        for (int i = 0; i < d; i++) {
            for (int j = 0; j <= i; j++)
                u[i] += p[j];
            u[i] = (u[i] - 1) / (i + 1);
        }

        int index = 0;
        for (int i = 0; i < d; i++)
            if (p[i] > u[i]) index = i;

        float[] w = new float[d];
        for (int i = 0; i < d; i++) {
            w[i] = vector[i] - u[index] - 0.5f;
            if (w[i] < -0.5f) w[i] = -0.5f;
        }

        return w;
    }

    public float[] projectOntoCentredHypercube(float bound, float[] vector) {
        float[] v = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            v[i] = vector[i];
            if (v[i] > bound) v[i] = bound;
            if (v[i] < -bound) v[i] = -bound;
        }
        return v;
    }

    private int sign(float x) {
        if (x > 0) return 1;
        if (x < 0) return -1;
        return 0;
    }

}
