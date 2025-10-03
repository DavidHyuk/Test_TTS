package com.example.test_tts;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;


import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * HuggingFace의 on-device TTS 모델을 사용한 TTS 구현 클래스
 * ONNX Runtime을 사용하여 현지에서 모델을 실행
 */
public class HuggingFaceTTS {
    private static final String TAG = "HuggingFaceTTS";

    // 모델 설정
    private static final String MODEL_CONFIG_URL = "https://huggingface.co/rhasspy/piper-voices/resolve/main/ko/ko_KO/voices.json";
    private static final String MODEL_BASE_URL = "https://huggingface.co/rhasspy/piper-voices/resolve/main/ko/ko_KO/";
    private static final String MODEL_DIR = "models";
    private static final String DEFAULT_VOICE = "korean";

    private Context context;
    private OrtEnvironment ortEnvironment;
    private OrtSession ortSession;
    private boolean isInitialized = false;

    // 오디오 설정
    private static final int SAMPLE_RATE = 22050;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_FLOAT;
    private static final int BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

    private ExecutorService executorService;
    private AudioTrack audioTrack;
    private Handler mainHandler;

    // 콜백 인터페이스
    public interface TTSListener {
        void onTTSReady();
        void onTTSError(String error);
        void onTTSComplete(long latencyMs);
    }

    private TTSListener ttsListener;

    public HuggingFaceTTS(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());

        initializeAudioTrack();
        initializeONNX();
    }

    /**
     * 오디오 트랙 초기화
     */
    private void initializeAudioTrack() {
        audioTrack = new AudioTrack(
            AudioManager.STREAM_MUSIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            BUFFER_SIZE,
            AudioTrack.MODE_STREAM
        );
    }

    /**
     * ONNX Runtime 환경 초기화
     */
    private void initializeONNX() {
        try {
            ortEnvironment = OrtEnvironment.getEnvironment();
            Log.d(TAG, "ONNX Runtime 환경 초기화 완료");
        } catch (Exception e) {
            Log.e(TAG, "ONNX Runtime 초기화 실패", e);
        }
    }

    /**
     * 모델 초기화 및 다운로드
     */
    public void initializeModel(TTSListener listener) {
        this.ttsListener = listener;

        executorService.execute(() -> {
            try {
                // 모델 파일 확인 및 다운로드
                File modelDir = new File(context.getFilesDir(), MODEL_DIR);
                if (!modelDir.exists()) {
                    modelDir.mkdirs();
                }

                // 모델 설정 파일 다운로드
                File configFile = new File(modelDir, "voices.json");
                if (!configFile.exists()) {
                    downloadFile(MODEL_CONFIG_URL, configFile);
                    Log.d(TAG, "모델 설정 파일 다운로드 완료");
                }

                // 한국어 음성 모델 파일 다운로드 (필요한 경우)
                File modelFile = new File(modelDir, DEFAULT_VOICE + ".onnx");
                if (!modelFile.exists()) {
                    String modelUrl = MODEL_BASE_URL + DEFAULT_VOICE + ".onnx";
                    downloadFile(modelUrl, modelFile);
                    Log.d(TAG, "모델 파일 다운로드 완료: " + DEFAULT_VOICE + ".onnx");
                }

                // 추가 설정 파일 다운로드 (필요한 경우)
                File configJsonFile = new File(modelDir, DEFAULT_VOICE + ".onnx.json");
                if (!configJsonFile.exists()) {
                    String configUrl = MODEL_BASE_URL + DEFAULT_VOICE + ".onnx.json";
                    downloadFile(configUrl, configJsonFile);
                    Log.d(TAG, "모델 설정 파일 다운로드 완료: " + DEFAULT_VOICE + ".onnx.json");
                }

                // ONNX 세션 초기화 (실제 모델 로드)
                initializeONNXSession(modelDir);

                mainHandler.post(() -> {
                    isInitialized = true;
                    if (ttsListener != null) {
                        ttsListener.onTTSReady();
                    }
                    Log.d(TAG, "HuggingFace TTS 모델 초기화 완료");
                });

            } catch (Exception e) {
                Log.e(TAG, "모델 초기화 실패", e);
                mainHandler.post(() -> {
                    if (ttsListener != null) {
                        ttsListener.onTTSError("모델 초기화 실패: " + e.getMessage());
                    }
                });
            }
        });
    }

    /**
     * 파일 다운로드
     */
    private void downloadFile(String url, File outputFile) throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
            .url(url)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("다운로드 실패: " + response.code());
            }

            BufferedSink sink = Okio.buffer(Okio.sink(outputFile));
            sink.writeAll(response.body().source());
            sink.close();

            Log.d(TAG, "파일 다운로드 완료: " + outputFile.getName());
        }
    }

    /**
     * ONNX 세션 초기화
     */
    private void initializeONNXSession(File modelDir) throws OrtException {
        try {
            File modelFile = new File(modelDir, DEFAULT_VOICE + ".onnx");
            if (modelFile.exists()) {
                // ONNX 세션 옵션 설정
                OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
                sessionOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT);

                // 모델 파일에서 세션 생성
                ortSession = ortEnvironment.createSession(modelFile.getAbsolutePath(), sessionOptions);
                Log.d(TAG, "ONNX 세션 생성 완료");
            } else {
                Log.w(TAG, "모델 파일이 존재하지 않음: " + modelFile.getAbsolutePath());
            }
        } catch (OrtException e) {
            Log.e(TAG, "ONNX 세션 초기화 실패", e);
            throw e;
        }
    }

    /**
     * 텍스트를 음성으로 변환
     */
    public void speak(String text) {
        if (!isInitialized) {
            Log.e(TAG, "TTS가 초기화되지 않음");
            return;
        }

        executorService.execute(() -> {
            long startTime = System.currentTimeMillis();

            try {
                // 텍스트 전처리
                String processedText = preprocessText(text);

                // 모델 추론 (여기서는 더미 구현 - 실제 모델 필요)
                float[] audioData = generateAudio(processedText);

                // 오디오 재생
                playAudio(audioData);

                long endTime = System.currentTimeMillis();
                long latency = endTime - startTime;

                mainHandler.post(() -> {
                    if (ttsListener != null) {
                        ttsListener.onTTSComplete(latency);
                    }
                });

            } catch (Exception e) {
                long endTime = System.currentTimeMillis();
                long latency = endTime - startTime;

                Log.e(TAG, "TTS 실행 실패", e);
                mainHandler.post(() -> {
                    if (ttsListener != null) {
                        ttsListener.onTTSError("TTS 실행 실패: " + e.getMessage());
                    }
                });
            }
        });
    }

    /**
     * 텍스트 전처리 (한국어 텍스트 정규화)
     */
    private String preprocessText(String text) {
        // 기본적인 한국어 텍스트 정규화
        return text.trim();
    }

    /**
     * 오디오 생성 (실제 모델 추론 - 현재는 더미 구현)
     */
    private float[] generateAudio(String text) throws OrtException {
        // 실제 모델이 준비되기 전까지는 더미 오디오 생성
        // 추후 실제 Piper 모델이나 다른 TTS 모델로 교체 가능

        if (ortSession != null) {
            Log.d(TAG, "실제 모델 추론 준비됨 (현재는 더미 구현 사용)");
        }

        // 임시 더미 구현 - 실제 모델 준비 시 교체 필요
        int sampleCount = SAMPLE_RATE * 2; // 2초 분량
        float[] audioData = new float[sampleCount];

        // 간단한 사인파 생성 (테스트용)
        for (int i = 0; i < sampleCount; i++) {
            audioData[i] = (float) Math.sin(2 * Math.PI * 440 * i / SAMPLE_RATE) * 0.1f;
        }

        return audioData;
    }

    /**
     * 오디오 데이터 재생
     */
    private void playAudio(float[] audioData) {
        try {
            audioTrack.play();

            // float 배열을 바이트 배열로 변환
            ByteBuffer byteBuffer = ByteBuffer.allocate(audioData.length * 4);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

            FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
            floatBuffer.put(audioData);

            byte[] byteArray = byteBuffer.array();

            // 오디오 데이터 쓰기
            int written = audioTrack.write(byteArray, 0, byteArray.length);
            Log.d(TAG, "오디오 데이터 작성: " + written + " 바이트");

        } catch (Exception e) {
            Log.e(TAG, "오디오 재생 실패", e);
        }
    }

    /**
     * 리소스 정리
     */
    public void shutdown() {
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }

        if (executorService != null) {
            executorService.shutdown();
        }

        if (ortSession != null) {
            try {
                ortSession.close();
            } catch (OrtException e) {
                Log.e(TAG, "세션 종료 실패", e);
            }
        }

        if (ortEnvironment != null) {
            try {
                ortEnvironment.close();
            } catch (Exception e) {
                Log.e(TAG, "환경 종료 실패", e);
            }
        }

        Log.d(TAG, "HuggingFace TTS 종료 완료");
    }

    /**
     * 초기화 상태 확인
     */
    public boolean isInitialized() {
        return isInitialized;
    }
}