package com.biodex.recognition;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs a real forward pass through the committed classifier, when it is on the classpath. Skips
 * otherwise, so the suite stays green before the model is trained. Validates the whole pipeline:
 * ONNX session load, channels-last preprocessing and probability mapping.
 */
class OnnxRecognitionServiceTest {

    @Test
    void committedModelClassifiesAGeneratedPhoto() throws Exception {
        assumeTrue(OnnxRecognitionService.isModelAvailable(),
                "pest-classifier.onnx not on the classpath yet");

        OnnxRecognitionService service = new OnnxRecognitionService();

        BufferedImage photo = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < photo.getHeight(); y++) {
            for (int x = 0; x < photo.getWidth(); x++) {
                photo.setRGB(x, y, 0x6B8E23);
            }
        }
        File temp = File.createTempFile("biodex-photo", ".jpg");
        temp.deleteOnExit();
        ImageIO.write(photo, "jpg", temp);

        List<MatchCandidate> matches = service.identify(temp.toPath(), 3);
        assertFalse(matches.isEmpty(), "a readable photo must yield at least one candidate");
        for (MatchCandidate match : matches) {
            assertTrue(match.getConfidence() >= 0.0f && match.getConfidence() <= 1.0f);
            assertFalse(match.getCommonName().matches("\\d+\\s+.*"),
                    "Teachable Machine index prefixes must be stripped from labels");
        }
        // Labels are ordered most likely first, whatever the class count.
        for (int i = 1; i < matches.size(); i++) {
            assertTrue(matches.get(i - 1).getConfidence() >= matches.get(i).getConfidence());
        }
    }

    @Test
    void missingPhotoYieldsNoMatches() {
        assumeTrue(OnnxRecognitionService.isModelAvailable());
        OnnxRecognitionService service = new OnnxRecognitionService();
        List<MatchCandidate> matches = service.identify(Path.of("no-such-photo.jpg"), 3);
        assertTrue(matches.isEmpty());
    }
}