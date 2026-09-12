package com.biodex.recognition;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the {@link RecognitionService} contract as implemented by the offline stand-in: never
 * null, ordered by confidence, capped at topK, and empty for unusable input.
 */
class FakeRecognitionServiceTest {

    private final FakeRecognitionService service = new FakeRecognitionService();
    private final Path anyPhoto = Path.of("sample-photo.jpg");

    @Test
    void identifyNeverReturnsNull() {
        assertNotNull(service.identify(anyPhoto, 3));
        assertNotNull(service.identify(null, 3));
        assertNotNull(service.identify(anyPhoto, 0));
    }

    @Test
    void identifyOrdersByConfidenceDescending() {
        List<MatchCandidate> matches = service.identify(anyPhoto, 3);
        assertFalse(matches.isEmpty());
        for (int i = 1; i < matches.size(); i++) {
            assertTrue(matches.get(i - 1).getConfidence() >= matches.get(i).getConfidence(),
                    "matches must be ordered most likely first");
        }
    }

    @Test
    void identifyHonoursTopK() {
        assertEquals(2, service.identify(anyPhoto, 2).size());
        assertTrue(service.identify(anyPhoto, 0).isEmpty());
    }

    @Test
    void missingPhotoYieldsNoMatches() {
        assertTrue(service.identify(null, 3).isEmpty());
    }

    @Test
    void confidenceIsBetweenZeroAndOne() {
        for (MatchCandidate match : service.identify(anyPhoto, 3)) {
            assertTrue(match.getConfidence() >= 0.0f && match.getConfidence() <= 1.0f);
        }
    }
}