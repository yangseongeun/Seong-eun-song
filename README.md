# Java MP3 Player (Multithreading & Async Study)

## 📌 Project Goal
이 프로젝트는 MP3 플레이어 구현을 통해
멀티스레딩과 비동기 처리 구조를 이해하기 위해 제작했습니다.

특히 C# async/await 기반 서버 프로그래밍의
비동기 I/O 개념을 이해하는 것을 목표로 했습니다.

---

## 📌 Problem
음악 재생 시 플레이어 UI가 멈추고,
음악 종료 후에만 다시 동작하는 문제가 발생했습니다.

---

## 📌 Cause
재생 로직(File I/O + Audio Decode)이
UI(Main Thread)에서 동기적으로 실행되어
이벤트 루프가 블로킹되었습니다.

---

## 📌 Solution
- ExecutorService 기반 백그라운드 스레드 분리
- UI 업데이트는 UI Thread에서만 실행
- 재생 상태 머신(Playing / Pause / Stop) 구현

---

## 📌 Result
재생 중에도 UI가 정상 동작하며,
서버 프로그래밍 async/await 구조와 동일한
비동기 처리 패턴을 이해했습니다.

---

## 📌 Tech Stack
- Java
- Java Swing / Java Sound API
- Maven

---

## 📌 Note
Audio files are excluded due to copyright.
Place your own mp3 files in `/music` folder.
