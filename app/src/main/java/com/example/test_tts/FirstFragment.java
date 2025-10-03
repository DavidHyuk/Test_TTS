package com.example.test_tts;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.test_tts.databinding.FragmentFirstBinding;

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

        binding.buttonTts.setOnClickListener(v -> {
            String text = binding.edittextKorean.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                Toast.makeText(requireContext(), "한국어 텍스트를 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            // 대기 중 상태로 초기화
            resetLatencyDisplay();

            // MainActivity의 TTS 기능 호출
            MainActivity mainActivity = (MainActivity) requireActivity();
            mainActivity.speakKorean(text);
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
            binding.textviewLatency.setText("⚡ 지연시간: " + latencyMs + "ms");
            binding.textviewLatency.setTextColor(0xFF4CAF50); // 녹색
        }
    }

    /**
     * 오류 발생 시 latency 값과 함께 오류 상태를 표시하는 메서드
     */
    public void updateErrorDisplay(long latencyMs) {
        if (binding != null && binding.textviewLatency != null) {
            binding.textviewLatency.setText("❌ 오류 (" + latencyMs + "ms)");
            binding.textviewLatency.setTextColor(0xFFF44336); // 빨간색
        }
    }

    /**
     * 초기 상태로 되돌리는 메서드
     */
    public void resetLatencyDisplay() {
        if (binding != null && binding.textviewLatency != null) {
            binding.textviewLatency.setText("대기 중...");
            binding.textviewLatency.setTextColor(0xFF666666); // 회색
        }
    }

}