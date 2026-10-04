package com.biodex.util;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * A short fade-up used when an auth screen opens: occasional screens can afford a little motion
 * while everyday actions stay instant. Strong ease-out curve, 350ms.
 */
public final class Entrance {

    private static final Interpolator EASE_OUT = Interpolator.SPLINE(0.23, 1, 0.32, 1);
    private static final double DURATION_MILLIS = 350;
    private static final double RISE = 14;

    private Entrance() {
    }

    /** Fades the node in from slightly below its resting position. */
    public static void play(Node node) {
        if (node == null) {
            return;
        }
        node.setOpacity(0);
        node.setTranslateY(RISE);

        FadeTransition fade = new FadeTransition(Duration.millis(DURATION_MILLIS), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(EASE_OUT);

        TranslateTransition rise = new TranslateTransition(Duration.millis(DURATION_MILLIS), node);
        rise.setFromY(RISE);
        rise.setToY(0);
        rise.setInterpolator(EASE_OUT);

        fade.play();
        rise.play();
    }
}
