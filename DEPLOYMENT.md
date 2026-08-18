# Welli-BE 배포 가이드

이 문서는 Welli-BE 백엔드를 로컬에서 개발하고, Docker + GHCR + GitHub Actions로
가비아 클라우드 서버에 자동 배포하는 전체 과정을 정리한 문서입니다. 새로 합류하는
팀원은 이 문서 하나로 로컬 개발 환경 세팅부터 서버 배포 원리까지 파악할 수 있습니다.

## 1. 전체 구조

```
로컬 개발 (IntelliJ)          GitHub                    가비아 클라우드 서버
─────────────────           ──────────                 ──────────────────
welli.env (로컬 전용)   →    main 브랜치 push
application.yml                  │
(${VAR} 참조만, git 커밋됨)       ▼
                          GitHub Actions
                          1) Docker 이미지 빌드
                          2) GHCR(ghcr.io)에 push   →   3) SSH로 서버 접속
                                                          docker compose pull/up
                                                          welli-app + welli-mysql
                                                          컨테이너 실행
```

핵심 원칙: **비밀번호/시크릿 값은 절대 git에 올리지 않는다.** `application.yml`에는
`${DB_URL}`, `${JWT_SECRET}` 같은 변수 참조만 있고, 실제 값은 로컬은 `welli.env`,
서버는 `~/welli/welli.env` 파일에만 존재하며 둘 다 `.gitignore`로 제외됩니다.

## 2. 로컬 개발 환경 세팅 (팀원 신규 합류 시)

1. 저장소 클론
   ```bash
   git clone https://github.com/Welliverse/Welli-BE-B.git
   cd Welli-BE-B
   ```

2. 프로젝트 루트에 `welli.env` 파일 생성 (git에는 없음, 각자 로컬에 직접 생성)
   ```bash
   cat > welli.env << 'EOF'
   DB_URL=jdbc:mysql://localhost:3306/welli?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
   DB_USERNAME=root
   DB_PASSWORD=본인_로컬_MySQL_비밀번호
   JWT_SECRET=welli-jwt-secret-key-must-be-longer-than-32-bytes
   JWT_EXPIRATION_MS=3600000
   EOF
   ```

3. 로컬에 MySQL 설치 후 `welli` 데이터베이스 생성

4. IntelliJ에서 `welli.env`를 자동으로 불러오게 하려면 **EnvFile 플러그인** 설치
   - `Settings → Plugins → Marketplace`에서 `EnvFile` 검색 후 설치, IntelliJ 재시작
   - `Run → Edit Configurations... → WelliBeApplication` 선택
   - `EnvFile` 탭 체크 → `+` 버튼으로 `welli.env` 추가
   - 이후 초록 실행 버튼(▶)으로 실행하면 환경변수가 자동 주입됨

   > 터미널에서 `./gradlew build`/`test`를 돌릴 때는 EnvFile 플러그인이 적용되지 않으므로
   > `export $(grep -v '^#' welli.env | xargs) && ./gradlew build`처럼 직접 export 필요

5. 앱 실행 후 Swagger로 API 확인: `http://localhost:8080/swagger-ui/index.html`

## 3. Docker/CI-CD 구성 파일

- `Dockerfile` — 멀티스테이지 빌드 (Gradle 빌드 → JRE 21 런타임 이미지)
- `.dockerignore` — 이미지에 포함하지 않을 파일 목록
- `docker-compose.yml` — 서버에서 앱 컨테이너 + MySQL 컨테이너를 함께 띄우는 구성
- `.github/workflows/deploy.yml` — `main` 브랜치 push 시 자동으로:
  1. Docker 이미지 빌드
  2. `ghcr.io/welliverse/welli-be-b`로 push (latest + 커밋 SHA 태그)
  3. SSH로 가비아 서버 접속 → `docker compose pull && up -d`

## 4. GitHub 저장소 설정 (관리자 1회 설정)

### 4-1. Secrets 등록
`Settings → Secrets and variables → Actions`에 등록:

| Secret | 값 |
|---|---|
| `SSH_HOST` | 가비아 서버 공인 IP |
| `SSH_USER` | `ubuntu` |
| `SSH_KEY` | 서버 접속용 프라이빗 키(.pem) 전체 내용 |
| `SSH_PORT` | `22` |

### 4-2. GHCR 패키지 접근 권한
`ghcr.io/welliverse/welli-be-b` 이미지는 기본적으로 **private**입니다. 서버에서
pull하려면 다음 중 하나가 필요합니다.

- **(권장, private 유지)** 서버에서 PAT로 1회 로그인 — 아래 5-3 참고
- 또는 GitHub 저장소 `Packages → welli-be-b → Package settings → Change visibility → Public`
  으로 공개 전환 (인증 없이 pull 가능, 단 이미지 자체가 외부에 노출됨)

## 5. 가비아 클라우드 서버 설정 (서버 관리 팀원용, 서버 1대당 1회)

### 5-1. Docker 설치 (Ubuntu)
```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
newgrp docker
```

### 5-2. 배포 디렉토리 준비
```bash
mkdir -p ~/welli && cd ~/welli
# docker-compose.yml을 이 디렉토리에 생성 (repo의 docker-compose.yml 내용 그대로)
```

`welli.env` 생성 (실제 운영용 값, 로컬 값과 달라도 무방):
```bash
cat > welli.env << 'EOF'
DB_USERNAME=root
DB_PASSWORD=강력한_운영용_비밀번호
JWT_SECRET=강력한_운영용_JWT_시크릿
JWT_EXPIRATION_MS=3600000
EOF
chmod 600 welli.env
```

> **주의**: 파일명이 `.env`가 아니라 `welli.env`라서 Docker Compose가 자동으로
> 읽지 않습니다. 아래처럼 모든 명령에 `--env-file welli.env`를 반드시 붙여야 합니다.

### 5-3. GHCR 인증 (private 이미지 pull용, 1회만)

1. GitHub → `Settings → Developer settings → Personal access tokens → Tokens (classic)`
   → `Generate new token (classic)`
   - Scope: `read:packages`만 체크
   - 조직(Welliverse) SSO 인증 요청이 뜨면 Authorize 진행

2. 서버에서 로그인 (토큰은 `~/.docker/config.json`에 저장되어 이후 계속 유효):
   ```bash
   echo "발급받은_토큰" | docker login ghcr.io -u <본인_GitHub아이디> --password-stdin
   ```

이 로그인은 GitHub Actions가 배포할 때도(`docker compose pull`) 그대로 재사용되므로,
서버당 딱 한 번만 해두면 됩니다. **팀원이 서버 관리 권한을 새로 받으면, 본인 GitHub
계정으로 발급한 PAT로 이 로그인을 각자 진행**하면 됩니다 (토큰을 공유하지 말고
각자 자기 계정으로 발급).

### 5-4. 최초 실행 / 수동 배포
```bash
cd ~/welli
docker compose --env-file welli.env pull
docker compose --env-file welli.env up -d
docker compose ps
```

이후로는 `main` 브랜치에 push할 때마다 GitHub Actions가 위 pull/up을 자동으로
대신 해줍니다. 수동 배포는 문제 상황 디버깅할 때만 필요합니다.

## 6. 배포 확인

```bash
docker compose --env-file welli.env logs app --tail=50
```

브라우저에서 접속 확인:
```
http://<서버IP>:8080/swagger-ui/index.html
```

접속이 안 되면 가비아 콘솔의 보안그룹/방화벽에서 **8080 포트 인바운드**가
열려 있는지 확인하세요 (22번 SSH와는 별도로 열어야 합니다).

## 7. 자주 발생하는 문제

| 증상 | 원인 | 해결 |
|---|---|---|
| `docker compose pull`에서 `denied` | GHCR에 이미지가 아직 없음 (Actions 미실행) | GitHub Actions 탭에서 워크플로우 실행 여부 확인 |
| `docker compose pull`에서 `unauthorized` | 이미지는 있는데 인증 안 됨 (private) | 5-3의 PAT 로그인 진행, 또는 패키지 Public 전환 |
| GitHub Actions는 success인데 컨테이너가 안 떠있음 | SSH 스크립트 중간 명령 실패가 마지막 명령 성공에 가려짐 | 서버에서 `docker compose --env-file welli.env pull`을 직접 실행해 실제 에러 확인 |
| `'url' must start with "jdbc"` 에러로 앱/테스트 실패 | `welli.env`의 값이 실제 환경변수로 주입 안 됨 | 로컬은 IntelliJ EnvFile 플러그인 연결 확인, 터미널은 `export` 필요, 서버는 `--env-file welli.env` 필요 |
