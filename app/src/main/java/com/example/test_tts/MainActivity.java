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
    private Locale currentLocale = Locale.KOREAN;

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
                Snackbar.make(view, getString(R.string.fab_message), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void initializeTTS() {
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    // TTS 엔진 초기화 성공
                    int result = tts.setLanguage(currentLocale);

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", getString(R.string.tts_language_not_supported) + " (" + currentLocale.getDisplayName() + ")");
                    } else {
                        // On-Device 엔진 사용 설정 (가능한 경우)
                        tts.setEngineByPackageName("com.google.android.tts");

                        isTtsInitialized = true;
                        Log.d("TTS", getString(R.string.tts_init_success) + " (" + currentLocale.getDisplayName() + ")");
                    }
                } else {
                    Log.e("TTS", getString(R.string.tts_init_failed));
                }
            }
        });
    }

    public void setLanguage(Locale locale) {
        currentLocale = locale;
        // Re-initialize TTS with new language if already initialized
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            isTtsInitialized = false;
            initializeTTS();
        }
    }

    public void speakText(String text) {
        if (!isTtsInitialized || tts == null) {
            Log.e("TTS", getString(R.string.tts_not_initialized));
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

                Log.d("TTS Latency", String.format(getString(R.string.tts_latency_log), latency));

                // Fragment에 실시간으로 latency 업데이트
                updateFragmentLatency(latency);

                Snackbar.make(binding.getRoot(), String.format(getString(R.string.tts_completed), latency), Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String utteranceId) {
                // TTS 오류 발생
                endTime = System.currentTimeMillis();
                long latency = endTime - startTime;

                Log.e("TTS Error", String.format(getString(R.string.tts_error_log), latency));

                // Fragment에 오류 상태 업데이트
                updateFragmentError(latency);

                Snackbar.make(binding.getRoot(), getString(R.string.tts_error), Snackbar.LENGTH_SHORT).show();
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