package sample.ldpc;

import sample.ldpc.math.SparseBooleanMatrix2D;

import java.util.ArrayList;

public class SPRDecoder extends LDPCSimpleDecoder {

    TannerGraph graph;

    public SPRDecoder(SparseBooleanMatrix2D parityCheckMatrix) {
        super(parityCheckMatrix);
        this.graph = new TannerGraph(this.parityCheckMatrix);
    }


    @Override
    public boolean[] tryToDecodeCorruptedData(boolean[] data) {

        boolean[] result = new boolean[vars];
        for (int i = 0; i < vars; i++) {
            result[i] = data[i];
        }

        int[] good = new int[vars];
        int[] bad = new int[vars];
        int oldBadChecks = checks;

        boolean stop = false;
        while (!stop) {

            int badChecks = 0;
            for (int i = 0; i < vars; i++) {
                bad[i] = 0; good[i] = 0;
            }

            for (int i = 0; i < checks; i++) {

                ArrayList<TannerGraph.Edge> check = graph.checks.get(i);
                boolean bit = false;
                for (int j = 0; j < check.size(); j++)
                    bit = bit != result[check.get(j).var];

                if (bit) badChecks++;

                for (int j = 0; j < check.size(); j++) {
                    int index = check.get(j).var;
                    if (bit) bad[index]++;
                    else good[index]++;
                }
            }


            int indexToChange = 0;
            int maxBad = 0;
            ArrayList<Integer> mostBads = new ArrayList<>();
            for (int i = 0; i < vars; i++) {
                if (maxBad > 0 && bad[i] == maxBad) mostBads.add(i);
                if (bad[i] > maxBad) {
                    mostBads.clear();
                    mostBads.add(i);
                    maxBad = bad[i];
                }
            }

            if (!mostBads.isEmpty()) {
                int lessGood = good[mostBads.get(0)];
                indexToChange = mostBads.get(0);
                for (Integer i : mostBads) {
                    if (good[i] < lessGood) {
                        lessGood = good[i];
                        indexToChange = i;
                    }
                }
            } else {
                stop = true;
            }

            if (!stop) result[indexToChange] = !result[indexToChange];
            if (badChecks >= oldBadChecks) stop = true;
            oldBadChecks = badChecks;
        }

        return cut(result, vars - checks);
    }

}
