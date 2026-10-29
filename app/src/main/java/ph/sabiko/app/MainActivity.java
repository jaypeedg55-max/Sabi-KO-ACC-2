package ph.sabiko.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import java.io.File;
import java.nio.file.Files;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.webkit.*;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import androidx.webkit.WebViewAssetLoader;
import java.io.ByteArrayInputStream;
import java.util.Locale;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final String ORIGIN = "https://appassets.androidplatform.net";
    private WebView web;
    private TextToSpeech tts;
    private volatile boolean ttsReady = false;
    private volatile boolean filipinoReady = false;
    private PermissionRequest microphone;
    private ValueCallback<Uri[]> fileCallback;
    private MediaRecorder nativeRecorder;
    private File recordingFile;
    private boolean waitingForRecordPermission;
    private final Handler recordHandler = new Handler(Looper.getMainLooper());
    private final Runnable stopAtLimit = () -> finishRecording(true);

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        web = new WebView(this);
        setContentView(web);
        if (android.os.Build.VERSION.SDK_INT >= 35) {
            web.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
        }
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest request) {
                WebResourceResponse result = loader.shouldInterceptRequest(request.getUrl());
                return result != null ? result : new WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", null, new ByteArrayInputStream(new byte[0]));
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                return !request.getUrl().toString().equals(ORIGIN + "/assets/index.html");
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> {
                    if (!ORIGIN.equals(request.getOrigin().toString().replaceAll("/$", ""))) { request.deny(); return; }
                    boolean audio = false;
                    for (String resource : request.getResources()) if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) audio = true;
                    if (!audio) { request.deny(); return; }
                    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        request.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});
                    } else {
                        if (microphone != null) microphone.deny();
                        microphone = request;
                        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 10);
                    }
                });
            }
            @Override public void onPermissionRequestCanceled(PermissionRequest request) { if (microphone == request) microphone = null; }
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    picker.addCategory(Intent.CATEGORY_OPENABLE);
                    picker.setType("image/*");
                    startActivityForResult(picker, 11);
                    return true;
                } catch (Exception e) { fileCallback.onReceiveValue(null); fileCallback = null; return false; }
            }
        });
        web.addJavascriptInterface(new SpeechBridge(), "NativeAndroid");
        tts = new TextToSpeech(getApplicationContext(), result -> runOnUiThread(() -> {
            if (result != TextToSpeech.SUCCESS || tts == null) return;
            ttsReady = true;
            int language = tts.setLanguage(Locale.forLanguageTag("fil-PH"));
            if (language < 0) language = tts.setLanguage(Locale.forLanguageTag("tl-PH"));
            filipinoReady = language >= 0;
            tts.setSpeechRate(0.85f);
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String id) { }
                @Override public void onDone(String id) { finishSpeech(id, false); }
                @Override public void onError(String id) { finishSpeech(id, true); }
            });
        }));
        web.loadUrl(ORIGIN + "/assets/index.html");
    }
    private void finishSpeech(String id, boolean error) {
        runOnUiThread(() -> { if (web != null) web.evaluateJavascript("window.sabiNativeDone && window.sabiNativeDone(" + JSONObject.quote(id) + "," + error + ")", null); });
    }
    private void recordingEvent(String state, String value) {
        if (web != null) web.evaluateJavascript("window.sabiRecordingEvent && window.sabiRecordingEvent(" + JSONObject.quote(state) + "," + JSONObject.quote(value) + ")", null);
    }
    private void requestRecording() {
        if (nativeRecorder != null || waitingForRecordPermission) return;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            waitingForRecordPermission = true;
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 12);
        } else beginRecording();
    }
    @SuppressWarnings("deprecation")
    private void beginRecording() {
        try {
            if (tts != null) tts.stop();
            recordingFile = File.createTempFile("sabi-voice-", ".m4a", getCacheDir());
            nativeRecorder = android.os.Build.VERSION.SDK_INT >= 31 ? new MediaRecorder(this) : new MediaRecorder();
            nativeRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            nativeRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            nativeRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            nativeRecorder.setAudioEncodingBitRate(64000);
            nativeRecorder.setAudioSamplingRate(44100);
            nativeRecorder.setOutputFile(recordingFile.getAbsolutePath());
            nativeRecorder.setOnErrorListener((recorder, what, extra) -> {
                finishRecording(false);
                recordingEvent("error", "Hindi makapag-record. Isara muna ang ibang app na gumagamit ng mikropono.");
            });
            nativeRecorder.prepare();
            nativeRecorder.start();
            recordingEvent("started", "Nagre-record… hanggang 15 segundo.");
            recordHandler.postDelayed(stopAtLimit, 15000);
        } catch (Exception error) {
            finishRecording(false);
            recordingEvent("error", "Hindi makapag-record. Tingnan ang Microphone permission at microphone privacy switch sa Settings.");
        }
    }
    private void finishRecording(boolean keep) {
        recordHandler.removeCallbacks(stopAtLimit);
        if (nativeRecorder == null) return;
        MediaRecorder recorder = nativeRecorder;
        File file = recordingFile;
        nativeRecorder = null;
        recordingFile = null;
        boolean stopped = false;
        try { recorder.stop(); stopped = true; } catch (RuntimeException ignored) { }
        finally { recorder.release(); }
        try {
            if (keep && stopped && file != null && file.length() > 0 && file.length() < 2 * 1024 * 1024) {
                String data = "data:audio/mp4;base64," + Base64.encodeToString(Files.readAllBytes(file.toPath()), Base64.NO_WRAP);
                recordingEvent("saved", data);
            } else if (keep) recordingEvent("error", "Masyadong maikli ang recording. Magsalita nang 1–2 segundo bago itigil.");
        } catch (Exception error) {
            recordingEvent("error", "Hindi nabasa ang recording. Subukan muli.");
        } finally { if (file != null) file.delete(); }
    }
    private class SpeechBridge {
        @JavascriptInterface public void startRecording() { runOnUiThread(() -> requestRecording()); }
        @JavascriptInterface public void stopRecording() { runOnUiThread(() -> finishRecording(true)); }
        @JavascriptInterface public boolean hasFilipino() { return ttsReady && filipinoReady; }
        @JavascriptInterface public void speak(String text, String id) {
            runOnUiThread(() -> {
                if (!ttsReady || !filipinoReady || tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) == TextToSpeech.ERROR) finishSpeech(id, true);
            });
        }
        @JavascriptInterface public void stop() { runOnUiThread(() -> { if (tts != null) tts.stop(); }); }
    }
    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == 12 && waitingForRecordPermission) {
            waitingForRecordPermission = false;
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) beginRecording();
            else recordingEvent("error", "Hindi pinayagan ang mikropono. Sa Settings → Apps → Sabi Ko → Permissions, payagan ang Microphone.");
        }
        if (code == 10 && microphone != null) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) microphone.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});
            else microphone.deny();
            microphone = null;
        }
    }
    @Override protected void onActivityResult(int code, int result, Intent data) {
        super.onActivityResult(code, result, data);
        if (code == 11 && fileCallback != null) {
            fileCallback.onReceiveValue(result == RESULT_OK && data != null && data.getData() != null ? new Uri[]{data.getData()} : null);
            fileCallback = null;
        }
    }
    @Override protected void onPause() {
        finishRecording(true);
        if (web != null) web.evaluateJavascript("stopSpeech(); if(recorder && recorder.state==='recording') recorder.stop();", null);
        if (tts != null) tts.stop();
        super.onPause();
    }
    @Override protected void onDestroy() {
        waitingForRecordPermission = false;
        finishRecording(false);
        recordHandler.removeCallbacks(stopAtLimit);
        if (microphone != null) microphone.deny();
        if (fileCallback != null) fileCallback.onReceiveValue(null);
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (web != null) { web.removeJavascriptInterface("NativeAndroid"); web.destroy(); web = null; }
        super.onDestroy();
    }
}
