# 🎵 Seong-eun Song Player

Java Swing UI와 JavaFX MediaPlayer를 결합하여 구현한 MP3 플레이어입니다.
음악 재생 중 발생하는 **UI 프리징 문제를 해결하기 위해 구조를 개선한 프로젝트**입니다.

---

## 📌 프로젝트 개요

이 프로젝트는 단순한 음악 플레이어 구현을 넘어,
**UI 응답성 문제를 직접 경험하고 이를 해결하는 과정**을 담고 있습니다.

초기에는 단일 클래스 기반으로 플레이어를 구현했지만,
스레드 처리와 구조 설계의 한계를 경험하면서
**비동기 처리와 아키텍처 분리의 중요성**을 학습하게 되었습니다.

---

## 🚨 문제 상황 (초기 구현)

초기 버전은 Swing과 JavaZoom 기반으로 구현되었으며,
다음과 같은 문제가 발생했습니다:

* `Thread.stop()`, `suspend()` 등 **deprecated API 사용**
* 음악 재생 로직이 UI 스레드와 분리되지 않아 **UI 프리징 발생**
* UI, 재생 로직, 파일 관리가 하나의 클래스에 집중된 **God Class 구조**
* 상태 관리가 분산되어 **코드 복잡도 증가**

👉 결과:
재생 중 UI가 멈추거나, 안정성이 떨어지는 문제가 발생

---

## 🔧 개선 과정

문제를 해결하기 위해 다음과 같은 방향으로 구조를 개선했습니다:

* JavaFX MediaPlayer 기반으로 재생 엔진 변경
* UI 스레드와 재생 로직 분리
* `Platform.runLater()`를 활용한 안전한 UI 업데이트
* 역할 기반 패키지 구조 도입

---

## 🚀 개선된 구조

```
mp3
├── controller   # 사용자 입력 및 이벤트 처리
├── service      # 음악 재생 엔진 (MediaPlayBackEngine)
├── domain       # 데이터 모델
├── ui           # 사용자 인터페이스
```

### ✔ 주요 개선 사항

* 🎧 **재생 엔진 분리**

  * JavaFX MediaPlayer 사용
* 🔄 **UI 응답성 개선**

  * Platform.runLater 기반 UI 업데이트
* 🧱 **구조 분리**

  * controller / service / domain / ui
* 🛠 **유지보수성 향상**

  * 기능별 책임 분리

---

## 🛠 기술 스택

* **Language**: Java 18
* **UI**: Java Swing
* **Media Playback**: JavaFX MediaPlayer
* **Build Tool**: Maven

### Libraries

* javafx-controls 20.0.2
* javafx-media 20.0.2

---

## ▶ 실행 방법

### 1. 저장소 클론

```bash
git clone https://github.com/yangseongeun/Seong-eun-song.git
cd Seong-eun-song
```

### 2. 실행

```bash
mvn clean javafx:run
```

※ Java 18 이상 권장

---

## 📁 사용 방법

1. 프로그램 실행
2. 상단 메뉴 → **파일 → 열기**
3. MP3 파일 선택
4. 재생 버튼 클릭

---

## ⚠️ 실행 시 주의사항
### ✔ JavaFX 관련 오류 발생 시

JavaFX는 JDK에 기본 포함되지 않기 때문에 실행 오류가 발생할 수 있습니다.

예시 오류:

JavaFX runtime components are missing

Error: Could not find or load main class

👉 해결 방법:

1. Java 버전 확인

        java -version

2. Java 18 이상인지 확인
3. Maven 재설치 또는 환경 변수 확인

---

## 💡 핵심 학습 내용

이 프로젝트를 통해 다음을 학습했습니다:

* 스레드 관리의 위험성과 안정적인 처리 방식
* UI 스레드와 작업 스레드 분리의 중요성
* 구조 분리를 통한 유지보수성 향상
* 단순 기능 구현을 넘어 **문제 해결 중심 개발 방식**

---

## 🔥 Before vs After

| 항목     | 초기 구현           | 개선 후               |
|--------|-----------------|--------------------|
| 재생 엔진  | JavaZoom Player | JavaFX MediaPlayer |
| 스레드 처리 | Thread 직접 제어    | JavaFX 기반 처리       |
| 구조     | 단일 클래스          | 계층 구조 분리           |
| UI 반응성 | 프리징 발생          | 정상 동작              |

---

## 📌 향후 개선 방향

* 재생 진행 바 (Progress Bar) 기능 추가
* 볼륨 조절 UI 개선
* 플레이리스트 저장/불러오기 기능
* 테스트 코드 추가

---

## 🧑‍💻 개발자

* Yang Seong-eun

---

## 📎 참고

이 프로젝트는 단순 기능 구현이 아닌,
**문제를 직접 경험하고 구조적으로 해결한 리팩토링 중심 프로젝트**입니다.
