package com.jarvis.lite;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView status, output;
    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private boolean continuousMode = false;

    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = findViewById(R.id.status);
        output = findViewById(R.id.output);

        Button listenButton = findViewById(R.id.listenButton);

        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("hi", "IN"));
            }
        });

        if (SpeechRecognizer.isRecognitionAvailable(this)) {

            speechRecognizer =
                    SpeechRecognizer.createSpeechRecognizer(this);

            speechRecognizer.setRecognitionListener(
                    new RecognitionListener() {

                @Override
                public void onReadyForSpeech(Bundle params) {
                    status.setText("🎤 सुन रहा हूँ...");
                }

                @Override
                public void onBeginningOfSpeech() {
                    status.setText("🎤 बोलिए...");
                }

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {
                    status.setText("🧠 समझ रहा हूँ...");
                }

                @Override
                public void onError(int error) {

                    if (continuousMode) {
                        restartListening();
                    } else {
                        status.setText("Ready");
                    }
                }

                @Override
                public void onResults(Bundle results) {

                    ArrayList<String> matches =
                            results.getStringArrayList(
                                    SpeechRecognizer.RESULTS_RECOGNITION
                            );

                    if (matches != null && !matches.isEmpty()) {

                        handleCommand(matches.get(0));

                    } else if (continuousMode) {

                        restartListening();
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {}

                @Override
                public void onEvent(
                        int eventType,
                        Bundle params) {}
            });
        }

        listenButton.setOnClickListener(v -> {

            continuousMode = true;
            startListening();

        });

        if (android.os.Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(
                        Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    11
            );
        }
    }

    private void startListening() {

        if (speechRecognizer == null) {
            speak("Speech service उपलब्ध नहीं है।");
            return;
        }

        Intent intent = new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "hi-IN"
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                false
        );

        status.setText("🎤 सुन रहा हूँ...");

        try {
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            restartListening();
        }
    }

    private void restartListening() {

        if (!continuousMode) {
            return;
        }

        handler.postDelayed(() -> {

            if (continuousMode) {
                startListening();
            }

        }, 1200);
    }

    private void handleCommand(String command) {

        output.setText("आप: " + command);

        String c = command.toLowerCase(Locale.ROOT);

        String reply;

        if (c.contains("नमस्ते") ||
                c.contains("हेलो") ||
                c.contains("hello")) {

            reply = "नमस्ते। मैं सुन रहा हूँ।";

        } else if (c.contains("समय") ||
                c.contains("time")) {

            java.text.SimpleDateFormat format =
                    new java.text.SimpleDateFormat(
                            "hh:mm a",
                            Locale.getDefault()
                    );

            reply = "अभी समय " +
                    format.format(new java.util.Date()) +
                    " है।";

        } else if (c.contains("बैटरी") ||
                c.contains("battery")) {

            android.os.BatteryManager bm =
                    (android.os.BatteryManager)
                            getSystemService(BATTERY_SERVICE);

            int percent =
                    bm.getIntProperty(
                            android.os.BatteryManager
                                    .BATTERY_PROPERTY_CAPACITY
                    );

            reply = "बैटरी " +
                    percent +
                    " प्रतिशत है।";

        } else if (c.contains("youtube") ||
                c.contains("यूट्यूब")) {

            try {

                Intent intent = new Intent(
                        Intent.ACTION_VIEW,
                        android.net.Uri.parse(
                                "https://www.youtube.com/"
                        )
                );

                startActivity(intent);

                reply = "YouTube खोल रहा हूँ।";

            } catch (Exception e) {

                reply = "YouTube नहीं खुल पाया।";
            }

        } else {

            reply = "मैंने सुना: " + command;
        }

        output.setText(reply);

        speak(reply);
    }

    private void speak(String text) {

        if (tts != null) {

            tts.speak(
                    text,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "jarvis"
            );

            // जवाब खत्म होने के बाद फिर से सुनना
            handler.postDelayed(() -> {

                if (continuousMode) {
                    startListening();
                }

            }, 1800);
        }
    }

    @Override
    protected void onDestroy() {

        continuousMode = false;

        handler.removeCallbacksAndMessages(null);

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        super.onDestroy();
    }
}
