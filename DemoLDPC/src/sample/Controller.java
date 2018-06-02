package sample;


import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import sample.ldpc.*;

import java.io.File;


public class Controller {

    private LDPCEncoder encoder;
    private LDPCSimpleDecoder decoder;
    private Noiser noiser;
    boolean[][] trueCodewords, trueEncodedCodewords, noisedCodewords, noisedEncodedCodewords, decodedCodewords;

    @FXML private Spinner<Integer> encoding, codeword;
    @FXML private Spinner<Float> noiseSpinner;
    @FXML private Slider noiseSlider;
    @FXML private Button open, noiseButton, decodeButton;
    @FXML private ImageView source, noised, decoded;
    @FXML private Pane sourcePane, noisedPane, decodedPane;
    @FXML private ProgressBar encodeProgress, noiseProgress, decodeProgress;
    @FXML private ChoiceBox<String> decoderSelect;

    @FXML
    private void initialize() {

        encoding.setValueFactory(new SpinnerValueFactory<>() {
            @Override
            public void decrement(int steps) {
                int newValue = getValue() - steps * 32;
                if (newValue < 32) newValue = 32;
                setValue(newValue);
                encoder = new LDPCEncoder(codeword.getValue(), newValue);
                updateDecoder();
                readAndEncode();
            }

            @Override
            public void increment(int steps) {
                int newValue = getValue() + steps * 32;
                if (newValue >= codeword.getValue() - 32) newValue = codeword.getValue() - 32;
                setValue(newValue);
                encoder = new LDPCEncoder(codeword.getValue(), newValue);
                updateDecoder();
                readAndEncode();
            }
        });

        codeword.setValueFactory(new SpinnerValueFactory<>() {
            @Override
            public void decrement(int steps) {
                int newValue = getValue() - steps * 32;
                if (newValue < 32) newValue = 32;
                if (newValue <= encoding.getValue() + 32) newValue = encoding.getValue() + 32;
                setValue(newValue);
                encoder = new LDPCEncoder(newValue, encoding.getValue());
                updateDecoder();
                readAndEncode();
            }

            @Override
            public void increment(int steps) {
                int newValue = getValue() + steps * 32;
                setValue(newValue);
                encoder = new LDPCEncoder(newValue, encoding.getValue());
                updateDecoder();
                readAndEncode();
            }
        });

        encoding.getValueFactory().setValue(32);
        codeword.getValueFactory().setValue(512);

        encoder = new LDPCEncoder(512, 32);
        decoder = new SPRDecoder(encoder.getParityCheckMatrix());
        noiser = new Noiser();
        noiser.setNoising(0);

        noiseSpinner.setValueFactory(new SpinnerValueFactory<>() {

            @Override
            public void decrement(int steps) {
                float newValue = getValue() - 0.001f * steps;
                newValue = Math.round(newValue*10000.0)/10000.0f;
                if (newValue <= 0.0f) newValue = 0.0f;
                setValue(newValue);
            }

            @Override
            public void increment(int steps) {
                float newValue = getValue() + 0.001f * steps;
                newValue = Math.round(newValue*10000.0)/10000.0f;
                if (newValue >= 1.0f) newValue = 1.0f;
                setValue(newValue);
            }
        });

        noiseSpinner.getValueFactory().setValue(0f);
        noiseSlider.setValue(0);

        decoderSelect.setItems(FXCollections.observableArrayList("SVR-decoder", "ADMM-decoder"));
        decoderSelect.setValue("SVR-decoder");

        decoderSelect.getSelectionModel().selectedIndexProperty().addListener((observableValue, number, number2) -> {
            if (number2.intValue() == 1) {
                ADMMDecoder d = new ADMMDecoder(encoder.getParityCheckMatrix());
                d.setErrorProbability(noiser.getNoising());
                decoder = d;
                decoderSelect.setValue("ADMM-decoder");
            }
            else {
                decoder = new SPRDecoder(encoder.getParityCheckMatrix());
                decoderSelect.setValue("SVR-decoder");
            }
        });

        source.fitWidthProperty().bind(sourcePane.widthProperty());
        source.fitHeightProperty().bind(sourcePane.heightProperty());
        noised.fitWidthProperty().bind(noisedPane.widthProperty());
        noised.fitHeightProperty().bind(noisedPane.heightProperty());
        decoded.fitWidthProperty().bind(decodedPane.widthProperty());
        decoded.fitHeightProperty().bind(decodedPane.heightProperty());

    }

    public void noisingSliderChanged() {
        float newValue = (float) noiseSlider.getValue() / 100;
        noiseSpinner.getValueFactory().setValue(newValue);
    }

    public void noisingSpinnerChanged() {
        float newValue = noiseSpinner.getValueFactory().getValue();
        noiseSlider.setValue(newValue * 100);
    }

    public void openFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Source File");
        Stage stage = (Stage) open.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file == null) return;
        Image image = new Image(file.toURI().toString());
        source.setImage(image);
        noised.setImage(image);
        readAndEncode();
    }

    public void readAndEncode() {
        if (source.getImage() == null) return;
        Thread thread = new Thread(() -> {
            disableUI(true);
            updateSource();
            encode();
            disableUI(false);
        });
        thread.start();
    }

    public void updateSource() {

        if (source.getImage() == null) return;

        PixelReader reader = source.getImage().getPixelReader();
        int width = (int) source.getImage().getWidth();
        int height = (int) source.getImage().getHeight();
        int pixels = width * height;
        int codewordSize = encoding.getValue();
        int codewordsAmount = pixels * 32 / codewordSize;
        int pixelsAtCodeword = encoding.getValue() / 32;

        trueCodewords = new boolean[codewordsAmount][];

        int pixelIndex = 0;
        for (int i = 0; i < codewordsAmount; i++) {
            trueCodewords[i] = new boolean[codewordSize];
            for (int j = 0; j < pixelsAtCodeword; j++) {
                Double x = pixelIndex % source.getImage().getWidth();
                Double y = pixelIndex / source.getImage().getWidth();
                Color color = reader.getColor(x.intValue(), y.intValue());
                Double r = color.getRed() * 255, g = color.getGreen() * 255, b = color.getBlue() * 255;
                boolean[] bits = getRGBBits(r.intValue(), g.intValue(), b.intValue());
                for (int k = 0; k < 32; k++)
                    trueCodewords[i][j * 32 + k] = bits[k];
                pixelIndex++;
            }
            encodeProgress.setProgress((float) i / codewordsAmount);
        }

    }

    public void updateDecoded() {

        if (source.getImage() == null) return;

        int width = (int) source.getImage().getWidth();
        int height = (int) source.getImage().getHeight();
        WritableImage image = new WritableImage(width, height);
        PixelWriter pixelWriter = image.getPixelWriter();

        int pixels = width * height;
        int codewordSize = encoding.getValue();
        int codewordsAmount = pixels * 32 / codewordSize;
        int pixelsAtCodeword = encoding.getValue() / 32;

        int pixelIndex = 0;
        for (int i = 0; i < codewordsAmount; i++) {
            for (int j = 0; j < pixelsAtCodeword; j++) {
                boolean[] bits = new boolean[32];
                for (int k = 0; k < 32; k++)
                    bits[k] = decodedCodewords[i][j * 32 + k];
                int[] rgb = getRGBFromBits(bits);
                Color color = new Color(rgb[0] / 255f, rgb[1] / 255f, rgb[2] / 255f, 1);
                pixelWriter.setColor(pixelIndex % width, pixelIndex / width, color);
                pixelIndex++;
            }
            decodeProgress.setProgress((float) i / codewordsAmount);
        }
        decoded.setImage(image);

    }

    public void encode() {
        trueEncodedCodewords = new boolean[trueCodewords.length][];
        for (int i = 0; i < trueCodewords.length; i++) {
            trueEncodedCodewords[i] = encoder.encode(trueCodewords[i]);
            encodeProgress.setProgress((float) i / trueCodewords.length);
        }
    }

    public void decode() {
        Thread thread = new Thread(() -> {
            disableUI(true);
            if ("ADMM-decoder".equals(decoderSelect.getValue()))
                ((ADMMDecoder) decoder).setErrorProbability(noiser.getNoising());
            decodedCodewords = new boolean[trueCodewords.length][];
            for (int i = 0; i < trueCodewords.length; i++) {
                decodedCodewords[i] = decoder.decode(noisedEncodedCodewords[i]);
                decodeProgress.setProgress((float) i / trueCodewords.length);
            }
            updateDecoded();
            disableUI(false);
        });
        thread.start();
    }

    public void noise() {
        Thread thread = new Thread(() -> {
            disableUI(true);
            noiser.setNoising(noiseSpinner.getValue());
            noisedCodewords = new boolean[trueCodewords.length][];
            noisedEncodedCodewords = new boolean[trueCodewords.length][];
            int encodeSize = encoding.getValue();
            for (int i = 0; i < trueCodewords.length; i++) {
                noisedEncodedCodewords[i] = noiser.noise(trueEncodedCodewords[i]);
                noisedCodewords[i] = new boolean[encodeSize];
                for (int j = 0; j < encodeSize; j++)
                    noisedCodewords[i][j] = noisedEncodedCodewords[i][j];
                noiseProgress.setProgress((float) i / trueCodewords.length);
            }
            updateNoised();
            disableUI(false);
        });
        thread.start();
    }

    public void updateNoised() {

        if (source.getImage() == null) return;

        int width = (int) source.getImage().getWidth();
        int height = (int) source.getImage().getHeight();
        WritableImage image = new WritableImage(width, height);
        PixelWriter pixelWriter = image.getPixelWriter();
        int pixels = width * height;
        int codewordSize = encoding.getValue();
        int codewordsAmount = pixels * 32 / codewordSize;
        int pixelsAtCodeword = encoding.getValue() / 32;

        int pixelIndex = 0;
        for (int i = 0; i < codewordsAmount; i++) {
            for (int j = 0; j < pixelsAtCodeword; j++) {
                boolean[] bits = new boolean[32];
                for (int k = 0; k < 32; k++)
                    bits[k] = noisedCodewords[i][j * 32 + k];
                int[] rgb = getRGBFromBits(bits);
                Color color = new Color(rgb[0] / 255f, rgb[1] / 255f, rgb[2] / 255f, 1);
                pixelWriter.setColor(pixelIndex % width, pixelIndex / width, color);
                pixelIndex++;
            }
            noiseProgress.setProgress((float) i / codewordsAmount);
        }
        noised.setImage(image);
    }

    private void disableUI(boolean disable) {
        decoderSelect.setDisable(disable);
        encoding.setDisable(disable);
        codeword.setDisable(disable);
        open.setDisable(disable);
        noiseSlider.setDisable(disable);
        noiseSpinner.setDisable(disable);
        noiseButton.setDisable(disable);
        decodeButton.setDisable(disable);
    }

    private int[] getRGBFromBits(boolean[] bits) {
        int[] rgb = new int[3];
        int power = 1;
        for (int i = 7; i >= 0; i--) {
            if (bits[i]) rgb[0] += power;
            power *= 2;
        }
        power = 1;
        for (int i = 15; i >= 8; i--) {
            if (bits[i]) rgb[1] += power;
            power *= 2;
        }
        power = 1;
        for (int i = 23; i >= 16; i--) {
            if (bits[i]) rgb[2] += power;
            power *= 2;
        }
        return rgb;
    }

    private boolean[] getRGBBits(int r, int g, int b) {
        boolean[] bits = new boolean[32];
        String s = Integer.toBinaryString(r);
        for(int i = s.length()-1; i >= 0; i--) {
            if (s.charAt(i) == '1') bits[8-s.length()+i] = true;
        }
        s = Integer.toBinaryString(g);
        for(int i = s.length()-1; i >= 0; i--) {
            if (s.charAt(i) == '1') bits[16-s.length()+i] = true;
        }
        s = Integer.toBinaryString(b);
        for(int i = s.length()-1; i >= 0; i--) {
            if (s.charAt(i) == '1') bits[24-s.length()+i] = true;
        }
        return bits;
    }

    private void updateDecoder() {
        if ("ADMM-decoder".equals(decoderSelect.getValue()))
            decoder = new ADMMDecoder(encoder.getParityCheckMatrix());
        else
            decoder = new SPRDecoder(encoder.getParityCheckMatrix());
    }

}
