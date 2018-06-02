package sample.ldpc;

import sample.ldpc.math.SparseBooleanMatrix2D;

import java.util.logging.SimpleFormatter;

public class LPBDecoder extends LDPCSimpleDecoder {


    public LPBDecoder(SparseBooleanMatrix2D parityCheckMatrix) {
        super(parityCheckMatrix);
    }



}
