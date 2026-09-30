package com.jarvis.lite;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
 private static final int REQ_SPEECH=10, REQ_AUDIO=11;
 private TextView status, output; private TextToSpeech tts;
 @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_main);status=findViewById(R.id.status);output=findViewById(R.id.output);Button listen=findViewById(R.id.listenButton);tts=new TextToSpeech(this,s->{if(s==TextToSpeech.SUCCESS)tts.setLanguage(Locale.getDefault());});listen.setOnClickListener(v->startListening());if(android.os.Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO);}
 private void startListening(){Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"hi-IN");i.putExtra(RecognizerIntent.EXTRA_PROMPT,"बोलिए...");try{status.setText("Listening...");startActivityForResult(i,REQ_SPEECH);}catch(Exception e){status.setText("Speech service unavailable");}}
 @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==REQ_SPEECH&&c==RESULT_OK&&d!=null){ArrayList<String>x=d.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);if(x!=null&&!x.isEmpty())handleCommand(x.get(0));}else status.setText("Ready");}
 private void handleCommand(String command){output.setText("You: "+command);String c=command.toLowerCase(Locale.ROOT);String reply;if(c.contains("hello")||c.contains("हेलो")||c.contains("नमस्ते"))reply="नमस्ते। JARVIS Lite तैयार है।";else if(c.contains("time")||c.contains("समय")){java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("hh:mm a",Locale.getDefault());reply="अभी समय "+f.format(new java.util.Date())+" है।";}else if(c.contains("battery")||c.contains("बैटरी")){android.os.BatteryManager bm=(android.os.BatteryManager)getSystemService(BATTERY_SERVICE);int p=bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);reply="बैटरी लगभग "+p+" प्रतिशत है।";}else if(c.contains("youtube खोलो")||c.contains("open youtube")){try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://www.youtube.com/")));reply="YouTube खोल रहा हूँ।";}catch(Exception e){reply="YouTube नहीं खुल पाया।";}}else reply="मैंने सुना: "+command+". यह lightweight demo command है।";output.setText(reply);status.setText("Ready");if(tts!=null)tts.speak(reply,TextToSpeech.QUEUE_FLUSH,null,"jarvis");}
 @Override protected void onDestroy(){if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
}
