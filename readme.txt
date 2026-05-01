# My Study Mate (MSM) README

## 1. 프로젝트 개요 (Project Overview)
MSM(My Study Mate)은 청각적 자극과 능동적 참여를 통해 학습 효율을 극대화하는 자기 주도형 AI 교육 플랫폼입니다. 파인만 기법(Feynman Technique)과 프로테제 효과(Protégé Effect)를 핵심 학습 논리로 채택하여, 사용자가 AI '학생'에게 개념을 설명하는 방식으로 학습이 진행됩니다. AI는 사용자의 음성을 분석하여 지식의 공백을 파악하고 심층적인 질문을 통해 개념의 명확한 이해를 돕습니다.

## 2. 기술 스택 (Tech Stack)
* **Backend:** Java 17, Spring Boot, Gradle, MyBatis
* **Frontend:** React (대시보드 및 UI 시각화)
* **AI & 3rd Party:** STT (Speech-to-Text), TTS (Text-to-Speech), 대형 언어 모델(LLM) 통합
* **Cache & Messaging:** Redis (AI 응답 디바운싱 및 실시간 세션 관리)

## 3. 핵심 기능 (Core Features)
* **AI 학생과의 양방향 상호작용:** STT를 통해 사용자의 음성 설명을 텍스트로 변환하고, AI가 이를 분석하여 TTS로 피드백 및 꼬리 질문을 수행합니다.
* **누적 학습 대시보드:** 학습 이력을 트래킹하고 누적된 데이터를 기반으로 사용자의 취약점과 성취도를 분석하여 시각화합니다.
* **인터랙티브 시각화 캐릭터:** 사용자의 학습 진행 상태와 피드백 상황에 맞춰 실시간으로 반응하는 UI 요소를 제공하여 몰입도를 높입니다.

## 4. 프로젝트 구조 (Project Structure)
```text
msm-project/
├── backend/                          /* Spring Boot 서버 로직 */
│   ├── src/main/java/com/msm/
│   │   ├── config/                   /* Redis, Security, CORS 및 외부 API 설정 */
│   │   ├── controller/               /* 클라이언트 요청을 처리하는 REST API 엔드포인트 */
│   │   ├── service/                  /* 파인만 기법 분석 및 AI 프롬프트 생성 비즈니스 로직 */
│   │   ├── repository/               /* MyBatis 매퍼 인터페이스 및 DB 통신 로직 */
│   │   └── domain/                   /* DTO 및 데이터베이스 엔티티 */
│   └── build.gradle                  /* 의존성 및 빌드 스크립트 관리 */
└── frontend/                         /* React 클라이언트 로직 */
    ├── src/
    │   ├── components/               /* 캐릭터, 대시보드 차트 등 재사용 가능한 UI 컴포넌트 */
    │   ├── hooks/                    /* 오디오 스트리밍 및 STT 상태 관리를 위한 커스텀 훅 */
    │   └── pages/                    /* 라우팅 별 메인 화면 */
    └── package.json
```
**구조 설계 논리 기반 (Rationale):** 백엔드와 프론트엔드를 물리적/논리적으로 완벽히 분리한 RESTful 아키텍처를 채택했습니다. 이는 실시간 오디오 스트리밍과 무거운 AI 처리 로직(Backend)이 사용자 인터페이스 렌더링(Frontend)의 성능에 병목을 일으키지 않도록 책임을 분리하기 위함입니다. 또한, 상태 관리와 API 통신 로직을 격리하여 추후 다른 플랫폼으로의 확장을 용이하게 합니다.

## 5. 개발 환경

* Java 17 JDK
* Node.js (v18+)
* Redis Server (포트 6379 개방)
* 필수 API Key (STT, TTS, LLM) 확보

##. 6. 배포중인 라이브 URL
아래 링크에서 확인 가능하며 서버 구동 시에만 작동합니다.
https://velog.io/@ruwu/%EB%9D%BC%EC%9D%B4%EB%B8%8C-URL


## 7. 개발 과정 로그
* github: https://github.com/r-uwu/myStudyMate_26_04
* 개발과정 velog 시리즈: https://velog.io/@ruwu/series/%EA%B0%9C%EC%9D%B8%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B82