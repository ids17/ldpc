package sample.ldpc;

import sample.ldpc.math.SparseBooleanMatrix2D;

import java.util.ArrayList;

public class TannerGraph {

    public ArrayList<ArrayList<Edge>> vars, checks;


    public TannerGraph(SparseBooleanMatrix2D matrix) {
        checks = new ArrayList<>();
        vars = new ArrayList<>();
        int rows = matrix.getRowDimension();
        int cols = matrix.getColumnDimension();
        for (int i = 0; i < rows; i++) vars.add(new ArrayList<>());
        for (int i = 0; i < cols; i++) checks.add(new ArrayList<>());

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (matrix.get(i, j)) {
                    Edge value = new Edge(0, j, i);
                    vars.get(i).add(value);
                    checks.get(j).add(value);
                }
            }
        }
    }


    public class Edge {
        public float value;
        public int var, check;

        public Edge(float value, int check, int var) {
            this.value = value;
            this.var = var;
            this.check = check;
        }
    }
}
