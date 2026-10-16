# 러닝한다이 (Running Handai)

부산의 러닝코스를 위치와 테마에 맞춰 추천하고, 사용자가 직접 코스를 만들어 공유하는 서비스 [**러닝한다이**](https://runninghandai.com/)입니다.

> 이 저장소는 팀 프로젝트 원본 [RunChuck/Running-Handai-BE](https://github.com/RunChuck/Running-Handai-BE)를 fork하여, <br>
> 프로젝트 종료 후 성능과 안정성 개선을 개인적으로 이어 간 저장소입니다.

## 프로젝트 성과

- 🏆 2025 관광데이터 활용 공모전 **우수상** 수상
- 🎖️ 2025 관광데이터 활용 공모전 **부산관광공사 특별상** 수상

## 내 역할

백엔드 2인 중 **인프라 구축 및 운영**과 **코스·리뷰·북마크 도메인**을 맡았습니다.

### 인프라 및 배포

- AWS 인프라 아키텍처를 설계하고 EC2, RDS, S3, ECR, CodeDeploy 리소스 구성
- Nginx와 Docker 기반 Blue-Green 무중단 배포 구현
- GitHub Actions로 테스트, 이미지 빌드, 배포까지 이어지는 CI/CD 파이프라인 구축

### 모니터링 및 운영

- CloudWatch로 애플리케이션 로그를 모으고, CPU · 메모리 · 디스크 경보를 Slack으로 연동
- 모든 요청 로그에 요청 ID와 IP를 남기는 MDC 로그 설정
- 운영 서버 다운의 원인을 메모리 지표와 시스템 로그로 추적해, Nginx 차단과 자동 업데이트 시간 조정으로 해결

### 코스 도메인

- 공공데이터 두루누비 API 연동과 코스 및 좌표 동기화 구현
- 위치/지역/테마별 추천 코스 조회 API 구현
- 코스 상세, 코스 요약 조회 API 구현
- 사용자 코스 생성·수정·삭제 API 구현

### 리뷰·북마크·회원 도메인

- 리뷰 CRUD와 내 리뷰 조회 API 구현
- 북마크 등록 및 해제 API 구현
- 북마크한 코스 조회 API 구현 및 페이징 적용
- 내 정보 조회 API 구현

### 협업 환경

- 로컬 개발 환경과 Swagger API 문서 구성, GitHub와 Slack 알림 연동

## 개선 작업 (fork)

팀 프로젝트가 끝난 뒤 운영 로그와 코드에서 찾은 문제를 로컬에서 재현하고, 개선 전후를 측정했습니다. 작업마다 이슈에 문제와 원인을, PR에 해결 과정과 측정 결과를 남겼습니다.

| 문제                                                            | 해결                            | 결과                                        | 기록                                                                                                                          |
|---------------------------------------------------------------|-------------------------------|-------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| 코스 생성이 OpenAI·S3 응답을 기다리는 동안 DB 커넥션을 점유해 다른 API까지 지연          | 외부 호출을 트랜잭션 밖으로 분리, OSIV 비활성화 | 조회 API p95 9.8초 → 0.04초, 커넥션 대기 96건 → 0건  | [이슈 #5](https://github.com/ssggii/Running-Handai-BE/issues/5) · [PR #6](https://github.com/ssggii/Running-Handai-BE/pull/6) |
| 공공데이터 동기화에서 코스 좌표가 한 건씩 삭제·저장                                 | JDBC 배치 저장, 벌크 삭제             | 코스 1개 교체 쿼리 2,613회 → 6회, 157.9ms → 20.1ms | [이슈 #3](https://github.com/ssggii/Running-Handai-BE/issues/3) · [PR #4](https://github.com/ssggii/Running-Handai-BE/pull/4) |
| 내 주변 코스 검색이 공간 인덱스를 쓰지 못하고 모든 코스의 거리를 계산                      | 사각형 범위 조건으로 공간 인덱스를 먼저 타게 변경  | 코스 420만 개 기준 22.5초 → 2.0초, 검색 결과 동일       | [이슈 #1](https://github.com/ssggii/Running-Handai-BE/issues/1) · [PR #2](https://github.com/ssggii/Running-Handai-BE/pull/2) |
| 없는 경로 요청이 404가 아닌 500으로 응답. 팀 기간 스캐닝 대응 때 넣은 처리가 실제로는 동작하지 않음 | 서블릿용 예외 클래스로 수정하고 테스트 추가      | 500 → 404                                 | [이슈 #7](https://github.com/ssggii/Running-Handai-BE/issues/7) · [PR #8](https://github.com/ssggii/Running-Handai-BE/pull/8) |

### 측정 방식

- 환경: 로컬 Docker, 운영과 같은 MySQL 8.0.41과 운영 스키마
- 규모: 서비스가 전국으로 확장해 1년 운영한 상황을 가정(코스 420만 개, 코스 생성 피크 초당 2.4건). 코스당 좌표 수와 외부 API 소요 시간은 운영 DB와 CloudWatch 로그의 실측값
- 외부 API: OpenAI, 카카오, S3는 운영 로그의 중앙값만큼 기다리는 지연 스텁으로 대체
- 반복: 반경 검색과 좌표 저장은 워밍업 후 10회 실행의 중앙값, 트랜잭션 범위는 60초 부하(코스 생성 초당 2.4건과 조회 초당 5건)에서의 p95

### 재현 방법

| 폴더                                                       | 내용                      |
|----------------------------------------------------------|-------------------------|
| [measure/e-tx-boundary](measure/e-tx-boundary)           | 부하 스크립트, 측정 데이터 준비, 결과  |
| [measure/c-trackpoint-batch](measure/c-trackpoint-batch) | 저장 구간 측정 스크립트, 결과       |
| [measure/b-spatial-index](measure/b-spatial-index)       | 코스 시드 데이터 생성, 쿼리 측정, 결과 |

측정 도구는 measure 프로필에서만 켜지고 운영 설정에는 들어가지 않습니다.

## 주요 기능

| 기능         | 내용                                                           |
|------------|--------------------------------------------------------------|
| 추천 코스 탐색   | 현위치 반경 5km, 부산 행정구역, 테마 중 하나로 코스를 골라 가까운 순으로 조회              |
| 코스 상세 조회   | 거리, 고도, 코스 경로부터 AI 난이도 분석, 주변 즐길거리, 리뷰와 평점까지 한 번에 조회         |
| 코스 생성 및 공유 | 지도에서 경로를 그리거나 GPX 파일을 올리면, 거리·소요 시간·고도를 서버에서 계산해 코스로 저장하고 공유 |
| 코스 리뷰      | 코스별 평점과 리뷰를 남기고, 전체 평점과 다른 사용자의 후기 확인                        |
| 코스 즐겨찾기    | 마음에 드는 코스를 북마크하고 모아 보기                                       |
| 마이페이지      | 내가 만든 코스와 즐겨찾기한 코스 관리, 러닝 용어사전 제공                            |

## 기술 스택

### Environment

<p> 
  <img src="https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white"/>
  <img src="https://img.shields.io/badge/AWS_ECR-FF9900?style=for-the-badge&logoColor=white"/> 
  <img src="https://img.shields.io/badge/AWS_EC2-FF9900?style=for-the-badge&logoColor=white"/> 
  <img src="https://img.shields.io/badge/AWS_S3-FF9900?style=for-the-badge&logoColor=white"/> 
  <img src="https://img.shields.io/badge/AWS_CodeDeploy-232F3E?style=for-the-badge&logoColor=white"/> 
  <img src="https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white"/> 
  <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white"/> 
  <img src="https://img.shields.io/badge/AWS_CloudWatch-232F3E?style=for-the-badge&logoColor=white"/> 
</p>

### Development

<p> 
  <img src="https://img.shields.io/badge/Java-007396?style=for-the-badge&logoColor=white"/>
  <img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white"/> 
  <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"/> 
  <img src="https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white"/> 
  <img src="https://img.shields.io/badge/Hibernate_Spatial-59666C?style=for-the-badge&logoColor=white"/> 
  <img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white"/> 
  <img src="https://img.shields.io/badge/OAuth2-EC1C24?style=for-the-badge&logoColor=white"/> 
  <img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white"/> 
  <img src="https://img.shields.io/badge/Spring_AI-6DB33F?style=for-the-badge&logo=spring&logoColor=white"/> 
  <img src="https://img.shields.io/badge/WebClient-59666C?style=for-the-badge&logoColor=white"/> 
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white"/> 
  <img src="https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black"/> 
</p>

### Test

<p> 
  <img src="https://img.shields.io/badge/JUnit5-25A162?style=for-the-badge&logo=junit5&logoColor=white"/>
  <img src="https://img.shields.io/badge/Mockito-59666C?style=for-the-badge&logoColor=white"/> 
</p>

### Communication

<p>
  <img src="https://img.shields.io/badge/Jira-0052CC?style=for-the-badge&logo=jira&logoColor=white"/>
  <img src="https://img.shields.io/badge/Slack-4A154B?style=for-the-badge&logo=slack&logoColor=white"/>
  <img src="https://img.shields.io/badge/Notion-000000?style=for-the-badge&logo=notion&logoColor=white"/>
  <img src="https://img.shields.io/badge/Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white"/>
</p>

## 시스템 아키텍처 · ERD

<img width="2368" height="1792" alt="infra_system" src="https://github.com/user-attachments/assets/36825c09-beb6-45b3-948f-33966781626d" />

<img width="4296" height="2142" alt="runninghandai_ERD" src="https://github.com/user-attachments/assets/fcb28458-3479-45e1-ab3f-8222943c0a36" />

## 디렉토리 구조

<details>
<summary>디렉토리 구조 보기</summary>

> 각 도메인은 controller, service, repository, entity, dto 레이어를 기반으로 구성되며, Course와 Spot 도메인은 client, scheduler, event 레이어를
> 추가로 사용합니다.
> 전역 영역은 config, jwt, oauth, logging, response, util 등 공통 모듈을 담당합니다.
>

```markdown
📦 Running-Handai-BE
├── 📁 docs
├── 📁 scripts
├── 📁 sql
├── 📁 src
│ ├── 📁 main
│ │ ├── 📁 java
│ │ │ └── 📁 com
│ │ │ └── 📁 server
│ │ │ └── 📁 running_handai
│ │ │ ├── 📁 domain
│ │ │ │ ├── 📁 admin
│ │ │ │ │ ├── controller
│ │ │ │ │ ├── dto
│ │ │ │ │ └── service
│ │ │ │ ├── 📁 bookmark
│ │ │ │ │ ├── controller
│ │ │ │ │ ├── dto
│ │ │ │ │ ├── entity
│ │ │ │ │ ├── repository
│ │ │ │ │ └── service
│ │ │ │ ├── 📁 course
│ │ │ │ │ ├── client
│ │ │ │ │ ├── controller
│ │ │ │ │ ├── dto
│ │ │ │ │ ├── entity
│ │ │ │ │ ├── event
│ │ │ │ │ ├── repository
│ │ │ │ │ ├── scheduler
│ │ │ │ │ └── service
│ │ │ │ ├── 📁 member
│ │ │ │ │ ├── controller
│ │ │ │ │ ├── dto
│ │ │ │ │ ├── entity
│ │ │ │ │ ├── repository
│ │ │ │ │ └── service
│ │ │ │ ├── 📁 review
│ │ │ │ │ ├── controller
│ │ │ │ │ ├── dto
│ │ │ │ │ ├── entity
│ │ │ │ │ ├── repository
│ │ │ │ │ └── service
│ │ │ │ └── 📁 spot
│ │ │ │ ├── client
│ │ │ │ ├── controller
│ │ │ │ ├── dto
│ │ │ │ ├── entity
│ │ │ │ ├── repository
│ │ │ │ ├── scheduler
│ │ │ │ └── service
│ │ │ ├── 📁 global
│ │ │ │ ├── config
│ │ │ │ ├── entity
│ │ │ │ ├── jwt
│ │ │ │ ├── log
│ │ │ │ ├── oauth
│ │ │ │ ├── response
│ │ │ │ └── util
│ │ │ └── 📄 RunningHandaiApplication.java
│ │ └── 📁 resources
│ └── 📁 test
│ └── 📁 java
│ └── (도메인별 서비스 단위 테스트)
├── 📄 build.gradle
├── 📄 docker-compose.yml
├── 📄 Dockerfile
├── 📄 appspec.yml
└── 📄 .env.example
```

</details>

## 팀원 및 개발 기간

> **팀명 : TeamChuck<br>
개발 기간 : 2025.05 ~ 2025.10**
>

| 안소용                                                                                                     | 제예영                                                                                                     | [우인경](https://github.com/InKyungWoo)                      |
|---------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------|-----------------------------------------------------------|
| <img src="https://github.com/user-attachments/assets/c4ad82d1-8bad-4c66-a793-c7b255259289" width="250"> | <img src="https://github.com/user-attachments/assets/1e39d1d0-c967-4e52-bbf9-65e8b76d6d44" width="250"> | <img src="https://github.com/InKyungWoo.png" width="250"> |
| PM, 기획                                                                                                  | 디자인                                                                                                     | 프론트엔드 개발                                                  |

| [문수현](https://github.com/moonxxpower)                                                                   | [한슬기](https://github.com/ssggii)                                                                        |
|---------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------|
| <img src="https://github.com/user-attachments/assets/6de9da7d-685b-44db-b467-3f96119a8e98" width="250"> | <img src="https://github.com/user-attachments/assets/8657003b-d7a8-4f13-a82c-102b6a29194e" width="250"> |
| 백엔드 개발                                                                                                  | 백엔드 개발                                                                                                  |
