# Pest Recognition — Model Integration Guide

## Document Control

| Field | Value |
| --- | --- |
| Module | Identify a Pest → photo recognition (`com.biodex.recognition`) |
| Page Owner | Vinny |
| Approach | Teachable Machine → ONNX → local inference via ONNX Runtime (offline, no API keys) |
| Status | Implemented — model file pending |

---

## 1. How it works

`IdentifyPestController` hands the uploaded photo to a `RecognitionService` (obtained from
`ServiceFactory.recognitionService()`) on a background `Task`. Two implementations exist:

| Implementation | When it runs | Behaviour |
| --- | --- | --- |
| `OnnxRecognitionService` | Model files exist on the classpath | Runs the trained classifier locally through ONNX Runtime |
| `FakeRecognitionService` | Model files absent, or `ServiceFactory.OFFLINE = true` | Returns the hardcoded matches from the original mockup |

The factory checks `OnnxRecognitionService.isModelAvailable()` on every app start, so **dropping the
model files in is all it takes to switch the page from mockup data to real recognition**. No code
change, no configuration.

## 2. Training the model

1. Go to <https://teachablemachine.withgoogle.com/train/image>.
2. Create one class per species — use the same names your labels file will carry (e.g. `Cane toad`,
   `Red imported fire ant`, `Indian myna`, `European rabbit`, `Green tree frog`, `Not a pest`).
   Aim for 50–100 images per class, varied angles and lighting. Good sources: ALA occurrence images,
   Wikimedia Commons, iNaturalist research-grade photos (download once for training — the app
   itself never calls these services).
3. Click **Train Model** (defaults: MobileNet transfer learning is fine).
4. **Export Model → Download → Keras**. You get `keras_model.h5` and `labels.txt`.

## 3. Converting to ONNX (one-time, needs Python)

Requires **Python 3.11–3.12** (TensorFlow has no wheels for 3.13+ as of writing) and, because
Teachable Machine exports with Keras 2 while new TensorFlow bundles Keras 3, the legacy-Keras
compatibility package:

```bash
python -m venv .venv-convert
.venv-convert\Scripts\pip install tensorflow tf2onnx onnx tf-keras
set TF_USE_LEGACY_KERAS=1
.venv-convert\Scripts\python -m tf2onnx.convert --keras keras_model.h5 --output pest-classifier.onnx --opset 13
```

Without `tf-keras` + `TF_USE_LEGACY_KERAS=1` the load fails with
`Unrecognized keyword arguments passed to DepthwiseConv2D: {'groups': 1}`.

**A ready-made conversion venv is already set up in this repo at `.venv-convert/` (gitignored)** —
on Vinny's machine, just run the last command with the flag set.

## 4. Installing the model

1. Rename/extend `labels.txt` so each line is `Common name | Scientific name` (the scientific name
   is optional; a plain class name works too). A leading `N ` index prefix as Teachable Machine
   writes it (e.g. `0 Cane Toad`) is stripped automatically by the loader:

   ```text
   Cane Toad | Rhinella marina
   Red imported fire ant | Solenopsis invicta
   Indian myna | Acridotheres tristis
   ```

   **Line order must match the class order in the exported model** — that is the order the
   classifier's outputs map to. Teachable Machine's `labels.txt` is already in that order.
2. Copy both files into `src/main/resources/com/biodex/ml/`:

   ```text
   src/main/resources/com/biodex/ml/pest-classifier.onnx   (typically 5–20 MB)
   src/main/resources/com/biodex/ml/labels.txt
   ```

3. Commit and run. `mvn javafx:run` → Identify a pest → Browse files or drag a photo in.

### ⚠️ Single-class models are useless for confidence

The current placeholder model was trained with **one class** (`Cane Toad`), whose head is
`Dense(relu)` → `Dense(softmax)`. A softmax over one class is always 1.0, so the app will show
"Cane Toad 100%" for every photo and the 0.70 low-confidence prompt can never trigger. Train with
**at least two classes** (ideally the species set plus a `Not a pest` class) before demoing.

### Model contract (verified against the current export)

- Input: `[batch, 224, 224, 3]` float, channels-last (NHWC), values in `[-1, 1]`.
- Output: `[batch, N]`, one softmax score per class.

## 5. Input/output contract (what `OnnxRecognitionService` does)

- Input: photo decoded with `ImageIO`, resized bilinearly to 224×224, RGB channels scaled to
  `[-1, 1]` (Teachable Machine's MobileNet preprocessing), packed as a `[1, 3, 224, 224]` float
  tensor.
- Output: one score per label. If the exported model emits logits instead of probabilities they are
  softmaxed automatically, so confidence is always in [0, 1].
- The top 3 candidates are shown, most likely first, with a percentage and Select chips.
- If the best candidate is below **0.70** (the `identification_confidence_threshold` default in the
  settings schema), the card shows a "Low confidence" prompt asking the user to confirm manually.

## 6. Error behaviour (matches the SpeciesService contract)

`identify` never throws: unreadable files, corrupt images and inference failures all yield an empty
list, and the page shows an inline hint — never an error dialog. The service is blocking, so it is
only ever called from a `javafx.concurrent.Task`.
