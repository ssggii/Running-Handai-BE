# **Running-Handai 로컬 개발 환경 가이드 🚀**

본 문서는 `Running-Handai` 백엔드 프로젝트의 표준 로컬 개발 환경 구축 가이드입니다. Docker를 활용하여 개발환경을 컨테이너화함으로써, 모든 팀원이 개인 PC의 OS나 설정에 따른 환경 차이를 방지하고 일관된 환경에서 프로젝트를 실행하는 것을 목표로 합니다.

## **1. 사전 준비**

로컬 개발을 시작하기 전에, 아래 프로그램이 반드시 설치되어 있어야 합니다.

- [**Docker Desktop**](https://www.docker.com/products/docker-desktop/)

## **2. 초기 설정**

프로젝트를 처음 `clone` 받았을 때 로컬환경 구축을 위해 최초 1회만 진행하는 과정입니다.

### **1) Git Repository 복제**

프로젝트를 로컬 PC로 내려받습니다.

```bash
git clone [프로젝트_저장소_URL]
cd [프로젝트_폴더명]
```

### **2) 환경 변수 파일 생성 (`.env`)**

템플릿 파일인 `.env.example`을 복사하여 실제 환경 변수 파일인 `.env`를 생성합니다. `.env.example` 파일에는 없는 환경변수 값들을 추가해줍니다.

```bash
cp .env.example .env
```

ℹ️ **Note:** `.env` 파일은 민감 정보를 담고 있어 Git에 올라가지 않으며, 각자 로컬 환경에서만 사용됩니다.

### **3) Docker 컨테이너 최초 빌드 및 실행**

아래 명령어를 실행하여 Docker 이미지를 빌드하고 모든 서비스(애플리케이션, DB)를 실행합니다.

```bash
docker compose up --build -d
```

## 3. Docker 명령어

초기 설정이 끝난 후, 일반적인 개발 상황에서는 아래 명령어를 통해 서비스(애플리케이션, DB)를 실행합니다.

### 1) 코드 변경사항 반영 후 실행

기능 개발, 버그 수정 등 **소스 코드를 변경한 후 로컬에서 테스트할 때, 변경사항을 반영하여 다시 실행**하는 명령어입니다. 변경된 코드를 적용하여 새로운 이미지를 빌드한 후 컨테이너를 실행합니다.

```bash
docker compose up --build -d
```

### 2) 단순 재실행 및 종료

코드나 설정 변경 없이 단순히 컨테이너를 켜고 끌 때 사용합니다.

- **컨테이너 실행**

  변경사항 없이 단순히 서비스를 재실행합니다. 컨테이너를 삭제하지 않았다면, 직전에 작업한 내역을 그대로 빠르게 불러올 수 있습니다.

    ```bash
    docker compose start
    ```

- **컨테이너 중지**

  컨테이너 삭제가 아니기 때문에, 다음 작업 때 `docker compose start`를 통해 빠르게 직전 작업 내역을 빠르게 불러올 수 있습니다.

    ```bash
    docker compose stop
    ```


### 3) 기타 상태 확인

- 모든 docker 컨테이너의 상태를 확인할 수 있습니다.

    ```bash
    docker ps -a
    ```


## **4. 확인 및 디버깅**

### **로그 확인 방법**

- **(추천) Docker Desktop 사용**
1. **Docker Desktop** 앱 실행 후 왼쪽 **[Containers]** 탭 클릭
2. 컨테이너 목록에서 로그를 보고 싶은 컨테이너 (예: `running-handai-app`) 클릭
3. 상세 화면의 **[Logs]** 탭에서 실시간 로그 확인
4. 로그 검색 등 다양한 기능 활용 가능
  
- **터미널 명령어 사용**

    ```
    # 애플리케이션 로그만 실시간으로 보기 
    docker compose logs -f running-handai-app
    
    # DB 로그만 실시간으로 보기
    docker compose logs -f running-handai-local-db
    ```


## **5. 데이터베이스 접속 정보**

DBeaver, DataGrip 등 선호하는 DB 툴로 아래 정보로 접속할 수 있습니다.

- **Host:** `localhost`
- **Port:** `3307`
- **Database:** `.env` 파일의 `MYSQL_DATABASE` 값
- **Username:** `.env` 파일의 `MYSQL_USER` 값
- **Password:** `.env` 파일의 `MYSQL_PASSWORD` 값