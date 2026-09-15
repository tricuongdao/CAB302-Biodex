package com.biodex.recognition;

import java.nio.file.Path;
import java.util.List;

/**
 * Hardcoded recognition results, shown when {@code ServiceFactory.OFFLINE} is true or when the
 * trained classifier has not been added to the project yet. Mirrors {@code FakeSpeciesService}:
 * the app stays usable, and the numbers match the original screen mockup.
 */
public class FakeRecognitionService implements RecognitionService {

    private static final List<MatchCandidate> MATCHES = List.of(
            new MatchCandidate("Cane toad", "Rhinella marina", 0.92f),
            new MatchCandidate("Eastern dwarf tree frog", "Litoria fallax", 0.77f),
            new MatchCandidate("Green tree frog", "Litoria caerulea", 0.12f));

    @Override
    public List<MatchCandidate> identify(Path photo, int topK) {
        if (photo == null || topK <= 0) {
            return List.of();
        }
        return MATCHES.stream().limit(topK).toList();
    }
}