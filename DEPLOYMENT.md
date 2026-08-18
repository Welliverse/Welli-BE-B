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

## 7. 서버 DB 직접 조회

MySQL 컨테이너는 보안을 위해 3306 포트를 호스트/외부에 노출하지 않습니다.
조회는 컨테이너 안으로 직접 들어가서 합니다.

```bash
docker exec -it welli-mysql mysql -u root -p welli
```
비밀번호 프롬프트에는 `welli.env`의 `DB_PASSWORD`를 입력합니다. 직접 타이핑하다
오타 낼 걱정이 없게 하려면, 컨테이너에 저장된 값을 그대로 쓰는 방식도 가능합니다.
```bash
docker exec -it welli-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" welli'
```

> **주의**: MySQL 컨테이너는 **최초 실행 시 볼륨이 비어있을 때만**
> `MYSQL_ROOT_PASSWORD`(=`welli.env`의 `DB_PASSWORD`)로 root 계정을 초기화합니다.
> 이후 `welli.env`의 `DB_PASSWORD`를 바꿔도 이미 생성된 DB 계정 비밀번호는
> 자동으로 바뀌지 않으니, **root 비밀번호는 항상 "컨테이너를 맨 처음 띄웠을
> 때 welli.env에 있던 값"** 이라는 점을 기억하세요.

접속 후 자주 쓰는 조회:
```sql
SHOW TABLES;
SELECT * FROM users;
SELECT * FROM analysis_results ORDER BY id DESC LIMIT 20;
```

GUI 툴(DBeaver, MySQL Workbench 등)로 조회하고 싶다면, SSH 터널을 통해 접속합니다.
```bash
ssh -i welliKeyPair.pem -L 3306:localhost:3306 ubuntu@1.201.116.47
```
단 이 방식을 쓰려면 `docker-compose.yml`의 `mysql` 서비스에 호스트 로컬에만
바인딩되는 포트 매핑이 필요합니다 (외부 공인 IP로는 계속 안 열리고, SSH
터널을 거쳐야만 접근되므로 안전합니다). 필요하면 아래를 `mysql` 서비스에 추가:
```yaml
    ports:
      - "127.0.0.1:3306:3306"
```

## 8. 자주 발생하는 문제

| 증상 | 원인 | 해결 |
|---|---|---|
| `docker compose pull`에서 `denied` | GHCR에 이미지가 아직 없음 (Actions 미실행) | GitHub Actions 탭에서 워크플로우 실행 여부 확인 |
| `docker compose pull`에서 `unauthorized` | 이미지는 있는데 인증 안 됨 (private) | 5-3의 PAT 로그인 진행, 또는 패키지 Public 전환 |
| GitHub Actions는 success인데 컨테이너가 안 떠있음 | SSH 스크립트 중간 명령 실패가 마지막 명령 성공에 가려짐 | 서버에서 `docker compose --env-file welli.env pull`을 직접 실행해 실제 에러 확인 |
| `'url' must start with "jdbc"` 에러로 앱/테스트 실패 | `welli.env`의 값이 실제 환경변수로 주입 안 됨 | 로컬은 IntelliJ EnvFile 플러그인 연결 확인, 터미널은 `export` 필요, 서버는 `--env-file welli.env` 필요 |
| `docker exec`로 mysql 접속 시 `Access denied for user 'root'@'localhost'` | 비밀번호 오타, 또는 컨테이너 최초 생성 이후 `welli.env`의 `DB_PASSWORD`만 바꾸고 컨테이너는 재생성 안 함 | 위 7번의 `$MYSQL_ROOT_PASSWORD` 방식으로 재시도. 그래도 안 되면 최초 생성 당시 비밀번호가 뭐였는지 확인하거나, 테스트 데이터라면 볼륨(`welli_mysql_data`) 삭제 후 재생성 |

## 9. (예정) 도메인 연결 및 HTTPS 전환

현재는 `http://1.201.116.47:8080`처럼 IP + 포트로 직접 서비스하고 있습니다.
**프론트팀 배포(Vercel/Netlify 등 HTTPS)가 완료되면**, 브라우저의 Mixed
Content 정책 때문에 백엔드도 HTTPS로 응답해야 프론트에서 정상 호출할 수
있습니다. 프론트 배포 일정이 잡히면 아래 순서로 진행합니다.

### 8-1. 도메인 준비
- 가비아에서 도메인 구매(가비아는 원래 도메인 등록 서비스가 주력이라 절차가
  간단함) 또는 팀에서 이미 보유한 도메인 사용
- API용 서브도메인 하나 결정 (예: `api.welli.com`)

### 8-2. DNS 연결
- 도메인 관리 콘솔(가비아 My가비아 등)에서 A 레코드 추가
  - 호스트: `api` (서브도메인)
  - 값: `1.201.116.47` (서버 공인 IP)
- 전파까지 최대 몇 시간 걸릴 수 있음, `dig api.welli.com` 또는
  `nslookup api.welli.com`으로 확인 가능

### 8-3. 가비아 보안그룹에 80번 포트 추가
Let's Encrypt 인증서 발급(HTTP-01 challenge)과 이후 HTTP→HTTPS 리다이렉트를
위해 80번(HTTP)도 인바운드로 열어야 합니다. 인바운드 규칙 추가 시:
- 타입: `HTTP` (프리셋에 있음) 또는 `USER`
- 프로토콜: `TCP`, 포트: `80`, CIDR: `0.0.0.0/0`

443(HTTPS)은 서버 생성 시 이미 인바운드 규칙에 포함되어 있어 별도 작업
불필요합니다.

### 8-4. 서버에 Nginx 설치 + 리버스 프록시 구성
```bash
sudo apt update && sudo apt install -y nginx
```
`/etc/nginx/sites-available/welli`에 아래 내용으로 설정 후 `sites-enabled`에
심볼릭 링크 연결:
```nginx
server {
    listen 80;
    server_name api.welli.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```
```bash
sudo ln -s /etc/nginx/sites-available/welli /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

### 8-5. Let's Encrypt 무료 SSL 인증서 발급 (Certbot)
```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d api.welli.com
```
Certbot이 Nginx 설정을 자동으로 HTTPS(443)용으로 수정하고, 인증서 자동
갱신 cron도 함께 등록해줍니다.

### 8-6. 배포 설정 갱신
- 서버 `welli.env`의 `CORS_ALLOWED_ORIGINS`에 프론트 운영 도메인 추가
  (예: `CORS_ALLOWED_ORIGINS=http://localhost:3000,https://welli-frontend.vercel.app`)
  → 수정 후 `docker compose --env-file welli.env up -d`로 재시작
- 프론트팀에 최종 API 주소를 `https://api.welli.com`으로 전달
- (선택) 외부에서 8080 직접 접근을 막고 싶으면, 가비아 보안그룹에서 8080
  인바운드 규칙을 삭제하고 Nginx(80/443)를 통해서만 접근하도록 정리

이 작업은 프론트 배포 일정이 확정되면 진행하면 되고, 그 전까지는 지금
구성(IP + HTTP + CORS `localhost:3000` 허용) 그대로 유지하면 됩니다.
