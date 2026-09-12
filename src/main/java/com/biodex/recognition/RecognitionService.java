package com.biodex.recognition;

import java.nio.file.Path;
import java.util.List;

/**
 * Pest photo recognition for the Identify a pest page. This is the only recognition type a
 * controller should name — obtain one from {@code ServiceFactory#recognitionService()} and never
 * construct an implementation directly.
 *
 * <p><b>The method blocks.</b> It decodes the photo and runs the classifier on the calling thread,
 * so a controller must call it from a {@code javafx.concurrent.Task} and apply the result in
 * {@code setOnSucceeded}. Calling it on the FX Application Thread will freeze the window.
 *
 * <p><b>This method does not throw.</b> When the photo cannot be read or classification fails the
 * implementation returns an empty list. A page should show an inline hint, not an error dialog.
 */
public interface RecognitionService {

    /**
     * Ranks the species the classifier thinks the photo shows, most likely first.
     *
     * @param photo the uploaded photo, a jpg or png file on disk
     * @param topK  maximum number of candidates to return
     * @return candidates ordered by confidence, empty when the photo is unreadable or recognition
     *         failed; never null
     */
    List<MatchCandidate> identify(Path photo, int topK);
}