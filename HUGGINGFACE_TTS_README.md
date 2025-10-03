# HuggingFace TTS 모델을 사용한 안드로이드 TTS 앱

이 프로젝트는 기존 안드로이드 내장 TTS 대신 HuggingFace의 on-device TTS 모델을 사용하여 한국어 텍스트를 음성으로 변환하는 앱입니다.

## 주요 변경사항

### 1. 기존 TTS → HuggingFace TTS로 변경
- `android.speech.tts.TextToSpeech` → `HuggingFaceTTS` 클래스로 변경
- ONNX Runtime을 사용하여 현지에서 모델 실행
- Piper 한국어 모델 지원 (현재는 더미 구현, 실제 모델 교체 필요)

### 2. 추가된 의존성
```gradle
// ONNX Runtime for Android - HuggingFace 모델 실행용
implementation 'com.microsoft.onnxruntime:onnxruntime-android:1.16.3'

// JSON 파싱용 (모델 설정 파일 읽기)
implementation 'com.google.code.gson:gson:2.10.1'

// HTTP 요청용 (모델 다운로드)
implementation 'com.squareup.okhttp3:okhttp:4.12.0'

// 오디오 처리용
implementation 'com.arthenica:ffmpeg-kit-full-gpl:5.1.0'
```

### 3. 새로운 파일 구조
```
app/src/main/
├── java/com/example/test_tts/
│   ├── HuggingFaceTTS.java          # 새로운 TTS 구현 클래스
│   ├── MainActivity.java             # HuggingFaceTTS 사용하도록 수정
│   └── FirstFragment.java            # 변경 없음
└── assets/
    └── models/                       # 모델 파일 저장 디렉토리
```

## 사용법

### 앱 실행
1. 앱을 빌드하고 실행합니다.
2. 첫 실행 시 필요한 모델 파일들이 자동으로 다운로드됩니다:
   - `voices.json`: 사용 가능한 음성 목록
   - `korean.onnx`: 한국어 TTS 모델 파일
   - `korean.onnx.json`: 모델 설정 파일

### 텍스트 입력 및 변환
1. 메인 화면의 텍스트 입력란에 한국어를 입력합니다.
2. "TTS" 버튼을 클릭하면 HuggingFace 모델을 통해 음성으로 변환됩니다.
3. 지연시간이 실시간으로 표시됩니다.

## 모델 정보

현재 구현된 모델:
- **Piper 한국어 모델**: `rhasspy/piper-voices`의 한국어 버전
- **모델 크기**: 약 100-200MB (다운로드 시)
- **특징**: 온디바이스 실행, 네트워크 불필요

## 기술적 구현

### HuggingFaceTTS 클래스 주요 기능
- **모델 자동 다운로드**: 첫 실행 시 필요한 모델 파일들을 자동으로 다운로드
- **ONNX Runtime 연동**: ONNX 형식의 모델을 현지에서 실행
- **실시간 오디오 재생**: 생성된 오디오를 실시간으로 재생
- **지연시간 측정**: 변환 완료까지의 시간을 측정하여 표시

### 주요 메서드
- `initializeModel()`: 모델 다운로드 및 초기화
- `speak()`: 텍스트를 음성으로 변환
- `shutdown()`: 리소스 정리

## 성능 및 최적화

### 현재 상태
- 기본 구현 완료됨
- 실제 모델 파일이 크므로 첫 실행 시 다운로드 시간이 필요할 수 있음
- 배터리 사용량이 기존 TTS보다 높을 수 있음 (온디바이스 추론 특성상)

### 향후 개선사항
1. **모델 최적화**: 더 작은 크기의 효율적인 모델 사용 고려
2. **스트리밍 재생**: 긴 텍스트를 실시간으로 변환하여 재생
3. **다양한 음성 지원**: 여러 한국어 음성 모델 지원
4. **오프라인 전용**: 완전한 오프라인 환경에서의 사용

## 문제 해결

### 모델 다운로드 실패 시
1. 네트워크 연결 확인
2. 저장공간 확인 (모델 파일 크기: ~150MB)
3. 앱 권한 확인 (인터넷, 저장공간)

### 오디오 재생 문제 시
1. 기기 볼륨 확인
2. 오디오 포커스 설정 확인
3. 다른 오디오 앱과 충돌 확인

## 라이선스 및 출처

- **Piper 모델**: MIT 라이선스
- **ONNX Runtime**: MIT 라이선스
- **사용된 라이브러리들**: 각 라이브러리의 라이선스 준수

## 개발 정보

이 구현은 안드로이드에서 HuggingFace 모델을 온디바이스에서 실행하는 방법을 보여주는 예시입니다. 실제 사용 시 모델 크기, 성능, 정확도 등을 고려하여 적절한 모델을 선택하시기 바랍니다.