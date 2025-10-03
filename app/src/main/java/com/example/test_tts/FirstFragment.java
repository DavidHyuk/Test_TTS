package com.example.test_tts;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.test_tts.databinding.FragmentFirstBinding;

import java.util.Locale;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;

    public interface LatencyUpdateListener {
        void onLatencyUpdate(long latencyMs);
    }

    private LatencyUpdateListener latencyListener;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // MainActivity에 현재 Fragment 참조 설정
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).setCurrentFragment(this);
        }

        // 언어 선택 Spinner 설정
        setupLanguageSpinner();

        binding.buttonTts.setOnClickListener(v -> {
            String text = binding.edittextKorean.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                Toast.makeText(requireContext(), getString(R.string.enter_korean_text), Toast.LENGTH_SHORT).show();
                return;
            }

            // 대기 중 상태로 초기화
            resetLatencyDisplay();

            // MainActivity의 TTS 기능 호출
            MainActivity mainActivity = (MainActivity) requireActivity();
            mainActivity.speakText(text);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // MainActivity에서 Fragment 참조 해제
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).setCurrentFragment(null);
        }
        binding = null;
    }

    /**
     * 실시간으로 latency 값을 업데이트하는 메서드
     */
    public void updateLatencyDisplay(long latencyMs) {
        if (binding != null && binding.textviewLatency != null) {
            binding.textviewLatency.setText(String.format(getString(R.string.latency_display), latencyMs));
            binding.textviewLatency.setTextColor(0xFF4CAF50); // 녹색
        }
    }

    /**
     * 오류 발생 시 latency 값과 함께 오류 상태를 표시하는 메서드
     */
    public void updateErrorDisplay(long latencyMs) {
        if (binding != null && binding.textviewLatency != null) {
            binding.textviewLatency.setText(String.format(getString(R.string.error_display), latencyMs));
            binding.textviewLatency.setTextColor(0xFFF44336); // 빨간색
        }
    }

    /**
     * 초기 상태로 되돌리는 메서드
     */
    public void resetLatencyDisplay() {
        if (binding != null && binding.textviewLatency != null) {
            binding.textviewLatency.setText(getString(R.string.waiting));
            binding.textviewLatency.setTextColor(0xFF666666); // 회색
        }
    }

    /**
     * 언어 선택 Spinner를 설정하는 메서드
     */
    private void setupLanguageSpinner() {
        String[] languages = {
            getString(R.string.language_korean),
            getString(R.string.language_english),
            getString(R.string.language_spanish)
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        binding.spinnerLanguage.setAdapter(adapter);

        // 기본 선택: 한국어 (인덱스 0)
        binding.spinnerLanguage.setSelection(0);

        binding.spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Locale selectedLocale;
                switch (position) {
                    case 0:
                        selectedLocale = Locale.KOREAN;
                        break;
                    case 1:
                        selectedLocale = Locale.ENGLISH;
                        break;
                    case 2:
                        selectedLocale = new Locale("es", "ES"); // Spanish
                        break;
                    default:
                        selectedLocale = Locale.KOREAN;
                        break;
                }

                // MainActivity에 언어 변경 요청
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).setLanguage(selectedLocale);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 아무것도 선택되지 않음
            }
        });
    }

}