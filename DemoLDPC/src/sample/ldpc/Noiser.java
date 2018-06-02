package sample.ldpc;

import java.util.Random;

public class Noiser {

    private Random random;
    private float noising; //0.0 - 1.0 - bit change probability

    public Noiser() {
        noising = 0.1f;
        random = new Random();
    }

    public float getNoising() {
        return noising;
    }

    public void setNoising(float noising) {
        if (noising < 0) noising = 0;
        if (noising > 1) noising = 1;
        this.noising = noising;
    }

    public boolean[] noise(boolean[] data) {
        boolean[] noisedData = new boolean[data.length];
        for(int i = 0; i < data.length; i++)
            if (random.nextFloat() < noising) {
                noisedData[i] = random.nextBoolean();
            } else {
                noisedData[i] = data[i];
            }
        return noisedData;
    }


}
