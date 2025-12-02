# 프로젝트 개요

## 러닝한다이 (Running Handai)

부산의 다채로운 매력을 따라 달리는 데이터 기반 러닝코스 추천 플랫폼, [**러닝한다이**](https://runninghandai.com/)입니다

### 프로젝트 성과

- 🏆 2025 관광데이터 활용 공모전 **우수상** 수상
- 🎖️ 2025 관광데이터 활용 공모전 **부산관광공사 특별상** 수상

### 서비스 링크

- 서비스 : https://runninghandai.com/
- 시연 영상 : https://drive.google.com/file/d/1AQqq0j2C61L-bnMzuSFbhwNnwJe4bgQn/view?usp=sharing

### 서비스 소개

![1](https://github.com/user-attachments/assets/66db6bb5-3714-4c09-b38c-6cd61e02144c)
![2](https://github.com/user-attachments/assets/a9673e05-e07e-4b1c-9730-3ff1f979cffb)
![3](https://github.com/user-attachments/assets/8e658321-fc72-433f-a342-768d3013c2e8)
![4](https://github.com/user-attachments/assets/ca84d18a-6e83-4c72-af51-e6a439d1a7c5)
![5](https://github.com/user-attachments/assets/a0482cf1-abb3-4d71-8845-642685ae7214)
![6](https://github.com/user-attachments/assets/342ad5b4-c223-48ae-adca-fe41a5c906cf)
![7](https://github.com/user-attachments/assets/b0d4d804-3ea8-40a9-8b29-f475edacbbd8)
![8](https://github.com/user-attachments/assets/b93ecdd1-34ad-431a-9f76-325356032df5)
![9](https://github.com/user-attachments/assets/f609bf40-dcef-4344-9c0e-0d89e6885205)
![10](https://github.com/user-attachments/assets/37c6f052-4048-40d4-b12c-a793483fe87a)
![11](https://github.com/user-attachments/assets/7a3274f3-63b2-4f59-b9e8-4269f805665a)
![12](https://github.com/user-attachments/assets/d668f03d-b4d4-42a2-8c72-4d6b68ee5a9b)

# 프로젝트 소개

**데이터 기반 러닝 코스 추천 플랫폼, '러닝한다이'의 API 서버를 구현한 백엔드 프로젝트입니다.**

Spring Boot 기반으로 구축되었으며, 사용자의 위치/테마 기반 코스 추천, 코스 생성, 리뷰 관리 등 핵심 기능을 위한 비즈니스 로직을 담당합니다. 공공데이터 API를 연동하여 코스 및 즐길거리 추천 기능을 구현하였으며, AI 기반의 코스 난이도 분석과 위치 기반 서비스를 안정적인 RESTful API로 제공합니다.

- **Core Logic :** 사용자 위치/취향 기반 즐길거리 및 코스 필터링 로직 구현
- **Spatial Search :** 위치 좌표(GPS) 기반 주변 즐길거리 및 코스 검색 최적화
- **Data Pipeline :** 공공데이터 API 2종 연동 및 DB 동기화/전처리 파이프라인 구축
- **API Server:** 클라이언트(Web)와의 통신을 위한 RESTful API 설계 및 배포
- **System Infra :** 무중단 배포와 자동화된 CI/CD 파이프라인을 기반으로 안정적인 인프라 구축

## 주요 기능

### **1. 추천 코스 탐색**

부산 전역에 걸쳐 행정구역별·테마별 다양한 추천 코스를 조회할 수 있습니다.

사용자는 현위치 기준 반경 5km 이내 코스, 부산의 특정 구역 코스, 또는 특정 테마에 해당하는 코스를 선택해 자신의 취향에 맞게 탐색할 수 있습니다.

### **2. 코스 상세 조회**

특정 코스에 대하여 고도 정보, 전체 길이, 코스 실루엣 등 기본 정보부터 AI 기반 난이도 분석, 코스 주변 즐길거리, 사용자 리뷰 및 평점까지 다양한 정보를 한눈에 확인할 수 있습니다.

### **3. 코스 생성 및 공유**

사용자는 지도에서 웨이포인트를 터치해 경로를 그리거나, 기존 GPX 파일을 업로드하여 손쉽게 자신만의 러닝 코스를 만들 수 있습니다. 

생성된 코스는 거리, 예상 러닝 속도, 최대·최소 고도 등을 확인할 수 있으며, 되돌리기·다시 실행, 출발·종료 지점 전환, 코스 초기화, 마커 드래그 앤 드롭 등 편집 기능을 통해 원하는 형태로 수정한 뒤 공유할 수 있습니다.

### **4. 코스 리뷰**

사용자는 각 코스에 평점과 리뷰를 직접 남길 수 있으며, 다른 사용자들의 후기와 전체 평점도 함께 확인할 수 있습니다.

### **5. 코스 즐겨찾기**

마음에 드는 코스를 북마크해 두고 언제든 빠르게 다시 찾아볼 수 있습니다.

### **6. 마이페이지**

마이페이지에서는 내가 생성한 코스와 즐겨찾기한 코스를 한눈에 관리할 수 있으며, 러닝과 관련된 용어를 정리한 러닝 용어사전도 함께 제공합니다.

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


## ERD

<img width="4296" height="2142" alt="runninghandai_ERD" src="https://github.com/user-attachments/assets/fcb28458-3479-45e1-ab3f-8222943c0a36" />

## 시스템 아키텍처

<img width="2368" height="1792" alt="infra_system" src="https://github.com/user-attachments/assets/36825c09-beb6-45b3-948f-33966781626d" />


## 디렉토리 구조

> 각 도메인은 controller, service, repository, entity, dto 레이어를 기반으로 구성되며, Course와 Spot 도메인은 client, scheduler, event 레이어를 추가로 사용합니다.
전역 영역은 config, jwt, oauth, logging, response, util 등 공통 모듈을 담당합니다.
> 

```markdown
📦 Running-Handai-BE
├── 📁 docs
├── 📁 scripts
├── 📁 sql
├── 📁 src
│   ├── 📁 main
│   │   ├── 📁 java
│   │   │   └── 📁 com
│   │   │       └── 📁 server
│   │   │           └── 📁 running_handai
│   │   │               ├── 📁 domain
│   │   │               │   ├── 📁 admin
│   │   │               │   │   ├── controller
│   │   │               │   │   ├── dto
│   │   │               │   │   └── service
│   │   │               │   ├── 📁 bookmark
│   │   │               │   │   ├── controller
│   │   │               │   │   ├── dto
│   │   │               │   │   ├── entity
│   │   │               │   │   ├── repository
│   │   │               │   │   └── service
│   │   │               │   ├── 📁 course
│   │   │               │   │   ├── client
│   │   │               │   │   ├── controller
│   │   │               │   │   ├── dto
│   │   │               │   │   ├── entity
│   │   │               │   │   ├── event
│   │   │               │   │   ├── repository
│   │   │               │   │   ├── scheduler
│   │   │               │   │   └── service
│   │   │               │   ├── 📁 member
│   │   │               │   │   ├── controller
│   │   │               │   │   ├── dto
│   │   │               │   │   ├── entity
│   │   │               │   │   ├── repository
│   │   │               │   │   └── service
│   │   │               │   ├── 📁 review
│   │   │               │   │   ├── controller
│   │   │               │   │   ├── dto
│   │   │               │   │   ├── entity
│   │   │               │   │   ├── repository
│   │   │               │   │   └── service
│   │   │               │   └── 📁 spot
│   │   │               │       ├── client
│   │   │               │       ├── controller
│   │   │               │       ├── dto
│   │   │               │       ├── entity
│   │   │               │       ├── repository
│   │   │               │       ├── scheduler
│   │   │               │       └── service
│   │   │               ├── 📁 global
│   │   │               │   ├── config
│   │   │               │   ├── entity
│   │   │               │   ├── jwt
│   │   │               │   ├── log
│   │   │               │   ├── oauth
│   │   │               │   ├── response
│   │   │               │   └── util
│   │   │               └── 📄 RunningHandaiApplication.java
│   │   └── 📁 resources
│   └── 📁 test
│       └── 📁 java
│           └── (도메인별 서비스 단위 테스트)
├── 📄 build.gradle
├── 📄 docker-compose.yml
├── 📄 Dockerfile
├── 📄 appspec.yml
└── 📄 .env.example
```

## 팀원 및 개발 기간


> **팀명 : TeamChuck<br>
개발 기간 : 2025.05 ~ 2025.10**
> 

| 안소용 | 제예영 | [우인경](https://github.com/InKyungWoo) |
| --- | --- | --- |
| <img src="https://github.com/user-attachments/assets/c4ad82d1-8bad-4c66-a793-c7b255259289" width="250"> | <img src="https://github.com/user-attachments/assets/1e39d1d0-c967-4e52-bbf9-65e8b76d6d44" width="250"> | <img src="https://github.com/InKyungWoo.png" width="250"> |
| PM, 기획 | 디자인 | 프론트엔드 개발 |

| [문수현](https://github.com/moonxxpower) | [한슬기](https://github.com/ssggii) |
| --- | --- |
| <img src="https://github.com/user-attachments/assets/6de9da7d-685b-44db-b467-3f96119a8e98" width="250"> | <img src="https://github.com/user-attachments/assets/8657003b-d7a8-4f13-a82c-102b6a29194e" width="250"> |
| 백엔드 개발 | 백엔드 개발 |
