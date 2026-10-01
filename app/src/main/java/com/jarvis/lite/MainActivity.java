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

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String AI_BACKEND_URL =
            "https://script.google.com/macros/s/AKfycby-Y4_7xC3QCs-EqA0FGfXRIFb8lwgCE9b96_133Tf8n9rAVNl9kod51NeqL65q6rMV/exec";

    private TextView status;
    private TextView output;

    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;

    private final Handler handler = new Handler();

    private boolean continuousMode = false;
    private boolean isSpeaking = false;
    private boolean isListening = false;
    private boolean ttsReady = false;
    private boolean aiBusy = false;

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
                tts.setSpeechRate(0.95f);
                tts.setPitch(1.0f);

                tts.setOnUtteranceProgressListener(
                        new UtteranceProgressListener() {

                            @Override
                            public void onStart(String id) {
                                isSpeaking = true;

                                runOnUiThread(() ->
                                        status.setText("🔊 बोल रहा हूँ...")
                                );
                            }

                            @Override
                            public void onDone(String id) {
                                isSpeaking = false;

                                runOnUiThread(() ->
                                        status.setText("● ONLINE")
                                );

                                if (continuousMode && !aiBusy) {
                                    handler.postDelayed(
                                            () -> startListening(),
                                            250
                                    );
                                }
                            }

                            @Override
                            public void onError(String id) {
                                isSpeaking = false;

                                if (continuousMode && !aiBusy) {
                                    handler.postDelayed(
                                            () -> startListening(),
                                            400
                                    );
                                }
                            }
                        }
                );

                ttsReady = true;
                status.setText("● ONLINE");
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
                    public void onRmsChanged(float rmsdB) {
                    }

                    @Override
                    public void onBufferReceived(byte[] buffer) {
                    }

                    @Override
                    public void onEndOfSpeech() {
                        isListening = false;
                        status.setText("🧠 समझ रहा हूँ...");
                    }

                    @Override
                    public void onError(int error) {

                        isListening = false;

                        if (continuousMode &&
                                !isSpeaking &&
                                !aiBusy) {

                            handler.postDelayed(
                                    () -> startListening(),
                                    500
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
                                    300
                            );
                        }
                    }

                    @Override
                    public void onPartialResults(Bundle partialResults) {
                    }

                    @Override
                    public void onEvent(
                            int eventType,
                            Bundle params) {
                    }
                }
        );
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
                isSpeaking ||
                aiBusy) {
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(
                        Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent intent =
                new Intent(
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

        intent.putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                3
        );

        try {
            speechRecognizer.startListening(intent);

        } catch (Exception e) {

            isListening = false;

            handler.postDelayed(
                    () -> startListening(),
                    700
            );
        }
    }

    private void handleCommand(String command) {

        output.setText("आप: " + command);

        String c = command
                .toLowerCase(Locale.ROOT)
                .trim();

        if (contains(
                c,
                "नमस्ते",
                "नमस्कार",
                "hello",
                "हेलो",
                "hi",
                "हाय"
        )) {

            speak(
                    "नमस्ते। मैं JARVIS हूँ। मैं आपकी सहायता के लिए तैयार हूँ।"
            );
            return;
        }

        if (contains(
                c,
                "तुम कौन हो",
                "आप कौन हो",
                "तुम्हारा नाम",
                "आपका नाम",
                "नाम क्या है"
        )) {

            speak("मेरा नाम JARVIS Lite है।");
            return;
        }

        if (contains(
                c,
                "समय",
                "टाइम",
                "time",
                "कितने बजे"
        )) {

            SimpleDateFormat format =
                    new SimpleDateFormat(
                            "hh:mm a",
                            Locale.getDefault()
                    );

            speak(
                    "अभी समय " +
                            format.format(new Date()) +
                            " है।"
            );
            return;
        }

        if (contains(
                c,
                "आज कौन सा दिन",
                "आज की तारीख",
                "तारीख",
                "date"
        )) {

            SimpleDateFormat format =
                    new SimpleDateFormat(
                            "EEEE, dd MMMM yyyy",
                            new Locale("hi", "IN")
                    );

            speak(
                    "आज " +
                            format.format(new Date()) +
                            " है।"
            );
            return;
        }

        if (contains(
                c,
                "बैटरी",
                "battery",
                "चार्ज"
        )) {

            BatteryManager bm =
                    (BatteryManager)
                            getSystemService(BATTERY_SERVICE);

            int percent =
                    bm.getIntProperty(
                            BatteryManager.BATTERY_PROPERTY_CAPACITY
                    );

            speak(
                    "आपके फोन की बैटरी " +
                            percent +
                            " प्रतिशत है।"
            );
            return;
        }

        if (contains(
                c,
                "youtube",
                "यूट्यूब"
        )) {

            openUrl("https://www.youtube.com/");
            speak("YouTube खोल रहा हूँ।");
            return;
        }

        if (contains(
                c,
                "google",
                "गूगल"
        )) {

            openUrl("https://www.google.com/");
            speak("Google खोल रहा हूँ।");
            return;
        }

        if (contains(
                c,
                "कितना होगा",
                "जोड़ो",
                "जोड़",
                "गुणा",
                "multiply",
                "calculate",
                "calculator"
        )) {

            speak(calculate(command));
            return;
        }

        if (contains(
                c,
                "बंद हो जाओ",
                "चुप हो जाओ",
                "stop",
                "रुक जाओ"
        )) {

            continuousMode = false;

            if (speechRecognizer != null) {
                speechRecognizer.cancel();
            }

            speak("ठीक है। मैं सुनना बंद कर रहा हूँ।");
            return;
        }

        askAI(command);
    }

    private void askAI(String question) {

        aiBusy = true;

        status.setText("🧠 JARVIS सोच रहा है...");

        Thread thread = new Thread(() -> {

            try {

                String answer =
                        sendQuestionToAI(question);

                runOnUiThread(() -> {

                    aiBusy = false;

                    output.setText(answer);

                    speak(answer);
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    aiBusy = false;

                    String message =
                            "AI से जवाब नहीं मिल पाया। इंटरनेट या backend connection जाँचें।";

                    output.setText(message);

                    speak(message);
                });
            }
        });

        thread.start();
    }

    private String sendQuestionToAI(String question)
            throws Exception {

        JSONObject request = new JSONObject();

        request.put("question", question);

        URL url = new URL(AI_BACKEND_URL);

        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);

        connection.setInstanceFollowRedirects(true);

        connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=UTF-8"
        );

        byte[] data =
                request.toString()
                        .getBytes(StandardCharsets.UTF_8);

        try (OutputStream os =
                     connection.getOutputStream()) {

            os.write(data);
            os.flush();
        }

        int responseCode =
                connection.getResponseCode();

        InputStream stream;

        if (responseCode >= 200 &&
                responseCode < 400) {

            stream = connection.getInputStream();

        } else {

            stream = connection.getErrorStream();
        }

        String response = readStream(stream);

        connection.disconnect();

        if (response == null ||
                response.isEmpty()) {

            throw new Exception("Empty server response");
        }

        JSONObject json = new JSONObject(response);

        if (json.has("error")) {
            throw new Exception(
                    json.getString("error")
            );
        }

        return json.optString(
                "answer",
                "मुझे अभी कोई उत्तर नहीं मिला।"
        );
    }

    private String readStream(InputStream stream)
            throws Exception {

        if (stream == null) {
            return "";
        }

        StringBuilder result =
                new StringBuilder();

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                stream,
                                StandardCharsets.UTF_8
                        )
                );

        String line;

        while ((line = reader.readLine()) != null) {
            result.append(line);
        }

        reader.close();

        return result.toString();
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

        String c =
                command.toLowerCase(Locale.ROOT);

        try {

            if (c.contains("गुणा") ||
                    c.contains("multiply")) {

                String[] parts;

                if (c.contains("गुणा")) {
                    parts = c.split("गुणा");
                } else {
                    parts = c.split("multiply");
                }

                if (parts.length >= 2) {

                    double a =
                            Double.parseDouble(
                                    parts[0].replaceAll(
                                            "[^0-9.]",
                                            ""
                                    )
                            );

                    double b =
                            Double.parseDouble(
                                    parts[1].replaceAll(
                                            "[^0-9.]",
                                            ""
                                    )
                            );

                    return "उत्तर " +
                            (a * b) +
                            " है।";
                }
            }

            if (c.contains("जोड़") ||
                    c.contains("जोड़ो")) {

                String[] numbers =
                        c.replaceAll(
                                "[^0-9.]+",
                                " "
                        )
                        .trim()
                        .split(" ");

                if (numbers.length >= 2) {

                    double sum = 0;

                    for (String n : numbers) {
                        sum += Double.parseDouble(n);
                    }

                    return "उत्तर " +
                            sum +
                            " है।";
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

        if (tts == null || !ttsReady) {
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

            speechRecognizer.cancel();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        if (tts != null) {

            tts.stop();
            tts.shutdown();
            tts = null;
        }

        super.onDestroy();
    }
                            }
