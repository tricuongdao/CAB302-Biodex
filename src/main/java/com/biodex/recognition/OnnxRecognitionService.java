package com.biodex.recognition;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * The live recogniser: runs a MobileNet image classifier, exported from Google Teachable Machine to
 * ONNX, entirely on the local machine. No network, no API keys.
 *
 * <p>The model and its labels live on the classpath at {@link #MODEL_RESOURCE} and
 * {@link #LABELS_RESOURCE}. {@code ServiceFactory} checks {@link #isModelAvailable()} first, so this
 * class is only constructed when the files are actually present; see
 * {@code Technical Specs/pest-recognition-integration.md} for how to train and export them.
 *
 * <p>Preprocessing matches what Teachable Machine's MobileNet training pipeline expects: the photo
 * is squeezed to a square {@value #INPUT_SIZE}&times;{@value #INPUT_SIZE} image and each channel
 * scaled to the [-1, 1] range, packed channels-last as [batch, 224, 224, 3] — the layout the
 * exported model's input declares. Output is one score per label; logits are softmaxed when the
 * model does not already emit probabilities.
 */
public final class OnnxRecognitionService implements RecognitionService {

    /** Square input size the exported MobileNet expects. */
    public static final int INPUT_SIZE = 224;

    /** Classpath location of the model file. */
    static final String MODEL_RESOURCE = "/com/biodex/ml/pest-classifier.onnx";

    /** Classpath location of the label file: one class per line, "Common name | Scientific name". */
    static final String LABELS_RESOURCE = "/com/biodex/ml/labels.txt";

    private final OrtEnvironment environment;
    private final OrtSession session;
    private final String inputName;
    private final List<String> labels;

    /** Loads the model from the classpath. Throws when the files are missing or unusable. */
    public OnnxRecognitionService() {
        if (!isModelAvailable()) {
            throw new IllegalStateException(
                    "Pest classifier not found at " + MODEL_RESOURCE
                            + " - see Technical Specs/pest-recognition-integration.md");
        }
        try (InputStream model = OnnxRecognitionService.class.getResourceAsStream(MODEL_RESOURCE)) {
            environment = OrtEnvironment.getEnvironment();
            session = environment.createSession(model.readAllBytes(), new OrtSession.SessionOptions());
            inputName = session.getInputNames().iterator().next();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load the pest classifier model", e);
        }
        labels = readLabels();
    }

    /** True when the model and label files are on the classpath. */
    public static boolean isModelAvailable() {
        return OnnxRecognitionService.class.getResource(MODEL_RESOURCE) != null
                && OnnxRecognitionService.class.getResource(LABELS_RESOURCE) != null;
    }

    @Override
    public List<MatchCandidate> identify(Path photo, int topK) {
        if (photo == null || topK <= 0 || labels.isEmpty()) {
            return List.of();
        }
        BufferedImage image = read(photo);
        if (image == null) {
            return List.of();
        }
        float[] probabilities = classify(image);
        if (probabilities.length == 0) {
            return List.of();
        }

        List<MatchCandidate> candidates = new ArrayList<>();
        int count = Math.min(probabilities.length, labels.size());
        for (int i = 0; i < count; i++) {
            String label = labels.get(i);
            int splitAt = label.indexOf('|');
            String commonName = splitAt >= 0 ? label.substring(0, splitAt).trim() : label;
            String scientificName = null;
            if (splitAt >= 0) {
                String tail = label.substring(splitAt + 1).trim();
                scientificName = tail.isEmpty() ? null : tail;
            }
            candidates.add(new MatchCandidate(commonName, scientificName, probabilities[i]));
        }
        candidates.sort((a, b) -> Float.compare(b.getConfidence(), a.getConfidence()));
        return List.copyOf(candidates.subList(0, Math.min(topK, candidates.size())));
    }

    /** Decodes the photo; null when the file is missing, corrupt or not an image. */
    private static BufferedImage read(Path photo) {
        try {
            return ImageIO.read(photo.toFile());
        } catch (IOException e) {
            return null;
        }
    }

    /** Runs one forward pass and returns one probability per label, or nothing on failure. */
    private float[] classify(BufferedImage image) {
        try (OnnxTensor tensor = OnnxTensor.createTensor(environment, toTensor(prepare(image)));
             OrtSession.Result result = session.run(Map.of(inputName, tensor))) {
            float[][] raw = (float[][]) result.get(0).getValue();
            return normalise(raw[0]);
        } catch (Exception e) {
            return new float[0];
        }
    }

    /**
     * Returns the scores as-is when the model already emits probabilities, otherwise applies a
     * softmax so callers always see values in [0, 1] summing to one.
     */
    private static float[] normalise(float[] values) {
        double sum = 0;
        boolean alreadyProbabilities = values.length > 0;
        for (float value : values) {
            if (value < 0 || value > 1) {
                alreadyProbabilities = false;
                break;
            }
            sum += value;
        }
        if (alreadyProbabilities && Math.abs(sum - 1.0) <= 0.01) {
            return values;
        }
        return softmax(values);
    }

    private static float[] softmax(float[] logits) {
        float max = logits.length > 0 ? logits[0] : 0;
        for (float logit : logits) {
            max = Math.max(max, logit);
        }
        double total = 0;
        double[] exponentials = new double[logits.length];
        for (int i = 0; i < logits.length; i++) {
            exponentials[i] = Math.exp(logits[i] - max);
            total += exponentials[i];
        }
        float[] probabilities = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            probabilities[i] = (float) (exponentials[i] / total);
        }
        return probabilities;
    }

    /** Squeezes the photo into a square input image with bilinear interpolation. */
    private static BufferedImage prepare(BufferedImage image) {
        BufferedImage scaled = new BufferedImage(INPUT_SIZE, INPUT_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(image, 0, 0, INPUT_SIZE, INPUT_SIZE, null);
        graphics.dispose();
        return scaled;
    }

    /** Packs the pixels into the model's [1, 224, 224, 3] channels-last tensor scaled to [-1, 1]. */
    private static float[][][][] toTensor(BufferedImage image) {
        float[][][][] tensor = new float[1][INPUT_SIZE][INPUT_SIZE][3];
        for (int y = 0; y < INPUT_SIZE; y++) {
            for (int x = 0; x < INPUT_SIZE; x++) {
                int rgb = image.getRGB(x, y);
                tensor[0][y][x][0] = ((rgb >> 16) & 0xFF) / 127.5f - 1f;
                tensor[0][y][x][1] = ((rgb >> 8) & 0xFF) / 127.5f - 1f;
                tensor[0][y][x][2] = (rgb & 0xFF) / 127.5f - 1f;
            }
        }
        return tensor;
    }

    private static List<String> readLabels() {
        List<String> lines = new ArrayList<>();
        try (Scanner scanner = new Scanner(
                OnnxRecognitionService.class.getResourceAsStream(LABELS_RESOURCE),
                StandardCharsets.UTF_8)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                // Teachable Machine writes labels as "0 Cane Toad"; drop the leading index.
                line = line.replaceFirst("^\\d+\\s+", "");
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
        } catch (Exception e) {
            // Empty labels make identify() return empty results rather than crash the page.
        }
        return lines;
    }
}