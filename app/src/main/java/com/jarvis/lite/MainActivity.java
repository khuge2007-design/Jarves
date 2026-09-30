package com.jarvis.lite;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView status, output;
    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private final Handler handler = new Handler();

    private boolean continuousMode = false;
    private boolean isSpeaking = false;
    private boolean isListening = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = findViewById(R.id.status);
        output = findViewById(R.id.output);
        Button listenButton = findViewById(R.id.listenButton);

        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {

                int language = tts.setLanguage(
                        new Locale("hi", "IN")
                );

                tts.setSpeechRate(0.90f);
                tts.setPitch(1.0f);

                tts.setOnUtteranceProgressListener(
                        new UtteranceProgressListener() {

                    @Override
                    public void onStart(String utteranceId) {
                        isSpeaking = true;
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        isSpeaking = false;

                        if (continuousMode) {
                            handler.postDelayed(
                                    () -> startListening(),
                                    500
                            );
                        }
                    }

                    @Override
                    public void onError(String utteranceId) {
                        isSpeaking = false;

                        if (continuousMode) {
                            handler.postDelayed(
                                    () -> startListening(),
                                    700
                            );
                        }
                    }
                });
            }
        });

        setupSpeechRecognizer();

        listenButton.setOnClickListener(v -> {
            continuousMode = true;
            startListening();
        });

        if (android.os.Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(
                        Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    11
            );
        }
    }

    private void setupSpeechRecognizer() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.setText("Speech service उपलब्ध नहीं है");
            return;
        }

        speechRecognizer =
                SpeechRecognizer.createSpeechRecognizer(this);

        speechRecognizer.setRecognitionListener(
                new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                isListening = true;
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
                isListening = false;
                status.setText("🧠 समझ रहा हूँ...");
            }

            @Override
            public void onError(int error) {

                isListening = false;

                if (continuousMode && !isSpeaking) {
                    handler.postDelayed(
                            () -> startListening(),
                            1000
                    );
                }
            }

            @Override
            public void onResults(Bundle results) {

                isListening = false;

                ArrayList<String> matches =
                        results.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION
                        );

                if (matches != null &&
                        !matches.isEmpty()) {

                    handleCommand(matches.get(0));

                } else if (continuousMode) {

                    handler.postDelayed(
                            () -> startListening(),
                            700
                    );
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

    private void startListening() {

        if (!continuousMode) {
            return;
        }

        if (speechRecognizer == null) {
            setupSpeechRecognizer();
        }

        if (speechRecognizer == null ||
                isListening ||
                isSpeaking) {
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(
                        Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED) {

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

        try {
            speechRecognizer.startListening(intent);
        } catch (Exception e) {

            isListening = false;

            handler.postDelayed(
                    () -> startListening(),
                    1000
            );
        }
    }

    private void handleCommand(String command) {

        output.setText("आप: " + command);

        String c = command.toLowerCase(Locale.ROOT).trim();

        String reply = null;

        // -------------------------
        // GREETINGS
        // -------------------------

        if (contains(c,
                "नमस्ते",
                "नमस्कार",
                "hello",
                "हेलो",
                "hi",
                "हाय")) {

            reply = "नमस्ते। मैं JARVIS हूँ। मैं आपकी सहायता के लिए तैयार हूँ।";
        }

        // -------------------------
        // WHO ARE YOU
        // -------------------------

        else if (contains(c,
                "तुम कौन हो",
                "आप कौन हो",
                "तुम्हारा नाम",
                "आपका नाम")) {

            reply = "मेरा नाम JARVIS Lite है। मुझे KHUSHWANT के लिए बनाया गया है।";
        }

        // -------------------------
        // TIME
        // -------------------------

        else if (contains(c,
                "समय",
                "टाइम",
                "time",
                "कितने बजे")) {

            SimpleDateFormat format =
                    new SimpleDateFormat(
                            "hh:mm a",
                            Locale.getDefault()
                    );

            reply = "अभी समय " +
                    format.format(new Date()) +
                    " है।";
        }

        // -------------------------
        // DATE
        // -------------------------

        else if (contains(c,
                "आज कौन सा दिन",
                "आज की तारीख",
                "तारीख",
                "date",
                "दिन")) {

            SimpleDateFormat format =
                    new SimpleDateFormat(
                            "EEEE, dd MMMM yyyy",
                            new Locale("hi", "IN")
                    );

            reply = "आज " +
                    format.format(new Date()) +
                    " है।";
        }

        // -------------------------
        // BATTERY
        // -------------------------

        else if (contains(c,
                "बैटरी",
                "battery",
                "चार्ज")) {

            BatteryManager bm =
                    (BatteryManager)
                            getSystemService(
                                    BATTERY_SERVICE
                            );

            int percent =
                    bm.getIntProperty(
                            BatteryManager
                                    .BATTERY_PROPERTY_CAPACITY
                    );

            reply = "आपके फोन की बैटरी " +
                    percent +
                    " प्रतिशत है।";
        }

        // -------------------------
        // YOUTUBE
        // -------------------------

        else if (contains(c,
                "youtube",
                "यूट्यूब")) {

            openUrl(
                    "https://www.youtube.com/"
            );

            reply = "YouTube खोल रहा हूँ।";
        }

        // -------------------------
        // GOOGLE
        // -------------------------

        else if (contains(c,
                "google",
                "गूगल")) {

            openUrl(
                    "https://www.google.com/"
            );

            reply = "Google खोल रहा हूँ।";
        }

        // -------------------------
        // CALCULATOR
        // -------------------------

        else if (contains(c,
                "कितना होगा",
                "जोड़ो",
                "जोड़",
                "गुणा",
                "भाग",
                "multiply",
                "calculate",
                "calculator")) {

            reply = calculate(command);
        }

        // -------------------------
        // STOP
        // -------------------------

        else if (contains(c,
                "बंद हो जाओ",
                "चुप हो जाओ",
                "stop",
                "रुक जाओ")) {

            continuousMode = false;

            reply = "ठीक है। मैं सुनना बंद कर रहा हूँ।";
        }

        // -------------------------
        // UNKNOWN QUESTION
        // -------------------------

        else {

            reply = "मैंने सुना: " + command +
                    "। इस सवाल का पूरा उत्तर देने के लिए मुझे इंटरनेट आधारित AI ज्ञान से जोड़ना होगा।";
        }

        output.setText(reply);
        speak(reply);
    }

    private boolean contains(
            String text,
            String... words) {

        for (String word : words) {

            if (text.contains(
                    word.toLowerCase(Locale.ROOT)
            )) {
                return true;
            }
        }

        return false;
    }

    private String calculate(String command) {

        String c = command.toLowerCase(Locale.ROOT);

        try {

            if (c.contains("गुणा") ||
                    c.contains("multiply")) {

                String[] parts =
                        c.split("गुणा|multiply");

                if (parts.length >= 2) {

                    double a =
                            Double.parseDouble(
                                    parts[0]
                                            .replaceAll(
                                                    "[^0-9.]",
                                                    ""
                                            )
                            );

                    double b =
                            Double.parseDouble(
                                    parts[1]
                                            .replaceAll(
                                                    "[^0-9.]",
                                                    ""
                                            )
                            );

                    return "उत्तर " + (a * b) + " है।";
                }
            }

            if (c.contains("जोड़") ||
                    c.contains("जोड़ो")) {

                String[] numbers =
                        c.replaceAll(
                                "[^0-9.]+",
                                " "
                        ).trim().split(" ");

                if (numbers.length >= 2) {

                    double sum = 0;

                    for (String n : numbers) {
                        sum += Double.parseDouble(n);
                    }

                    return "उत्तर " + sum + " है।";
                }
            }

        } catch (Exception ignored) {
        }

        return "मैं इस calculation को अभी समझ नहीं पाया।";
    }

    private void openUrl(String url) {

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                    );

            startActivity(intent);

        } catch (Exception e) {

            speak("यह ऐप नहीं खुल पाया।");
        }
    }

    private void speak(String text) {

        if (tts == null) {
            return;
        }

        isSpeaking = true;

        tts.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "jarvis_reply"
        );
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
