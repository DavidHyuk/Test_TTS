package com.example.test_tts;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.fragment.app.FragmentManager;

import com.example.test_tts.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private TextToSpeech tts;
    private long startTime;
    private long endTime;
    private boolean isTtsInitialized = false;
    private FirstFragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        // TTS 엔진 초기화
        initializeTTS();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "상단의 텍스트 입력란에 한국어를 입력하고 TTS 버튼을 클릭하세요", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void initializeTTS() {
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    // TTS 엔진 초기화 성공
                    int result = tts.setLanguage(Locale.KOREAN);

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "한국어 언어가 지원되지 않습니다.");
                    } else {
                        // On-Device 엔진 사용 설정 (가능한 경우)
                        tts.setEngineByPackageName("com.google.android.tts");

                        isTtsInitialized = true;
                        Log.d("TTS", "TTS 엔진 초기화 완료 (한국어)");
                    }
                } else {
                    Log.e("TTS", "TTS 엔진 초기화 실패");
                }
            }
        });
    }

    public void speakKorean(String text) {
        if (!isTtsInitialized || tts == null) {
            Log.e("TTS", "TTS가 초기화되지 않았습니다.");
            return;
        }

        // 시작 시간 기록
        startTime = System.currentTimeMillis();

        // UtteranceProgressListener 설정 (완료 시간 측정용)
        tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                // TTS 시작됨
            }

            @Override
            public void onDone(String utteranceId) {
                // TTS 완료됨 - 종료 시간 기록 및 로그 출력
                endTime = System.currentTimeMillis();
                long latency = endTime - startTime;

                Log.d("TTS Latency", "TTS 처리 시간: " + latency + "ms");

                // Fragment에 실시간으로 latency 업데이트
                updateFragmentLatency(latency);

                Snackbar.make(binding.getRoot(), "TTS 완료 (지연시간: " + latency + "ms)", Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String utteranceId) {
                // TTS 오류 발생
                endTime = System.currentTimeMillis();
                long latency = endTime - startTime;

                Log.e("TTS Error", "TTS 오류 발생 (지연시간: " + latency + "ms)");

                // Fragment에 오류 상태 업데이트
                updateFragmentError(latency);

                Snackbar.make(binding.getRoot(), "TTS 오류 발생", Snackbar.LENGTH_SHORT).show();
            }
        });

        // 한국어 텍스트를 음성으로 변환
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utteranceId");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        currentFragment = null;
    }

    /**
     * 현재 Fragment 참조를 설정하는 메서드
     */
    public void setCurrentFragment(FirstFragment fragment) {
        this.currentFragment = fragment;
    }

    /**
     * Fragment에 실시간으로 latency 값을 업데이트하는 메서드
     */
    private void updateFragmentLatency(long latencyMs) {
        if (currentFragment != null) {
            currentFragment.updateLatencyDisplay(latencyMs);
        }
    }

    /**
     * Fragment에 오류 상태와 latency 값을 업데이트하는 메서드
     */
    private void updateFragmentError(long latencyMs) {
        if (currentFragment != null) {
            currentFragment.updateErrorDisplay(latencyMs);
        }
    }
}