# 백엔드 팀 전달 - 서버/배포 안내

전체 배포 원리와 트러블슈팅은 `DEPLOYMENT.md`에 자세히 정리되어 있습니다.
이 문서는 팀원이 매일 작업하면서 알아야 할 핵심만 요약한 것입니다.

## 1. 브랜치 전략과 자동 배포

- **`develop`**: 로컬 테스트용 브랜치. 여기에 push해도 아무 것도 배포되지
  않습니다. 자유롭게 커밋/push 하면서 개발하세요.
- **`main`**: 이 브랜치에 push(또는 PR merge)되는 순간, GitHub Actions가
  자동으로 실행됩니다 → Docker 이미지 빌드 → GHCR(`ghcr.io/welliverse/welli-be-b`)
  push → 가비아 서버에 SSH 접속해서 재배포까지 전부 자동.

**즉, `main`에 머지 = 바로 실서버 반영입니다.** `develop`에서 충분히
확인한 뒤에 머지해주세요. 배포 상태는 저장소 `Actions` 탭에서 확인할 수
있습니다.

## 2. 서버 정보

- 가비아 클라우드 서버 IP: `1.201.116.47`
- 접속: `ssh -i welliKeyPair.pem ubuntu@1.201.116.47` (키페어 파일은 서버
  최초 생성자에게 요청)
- 배포 디렉토리: `~/welli` (`docker-compose.yml`, `welli.env` 위치)
- API: `http://1.201.116.47:8080`, Swagger: `http://1.201.116.47:8080/swagger-ui/index.html`

## 3. 환경변수 (`welli.env`) 관리

- **로컬**: 각자 프로젝트 루트에 `welli.env`를 만들고 IntelliJ EnvFile
  플러그인으로 연결 (자세한 건 `DEPLOYMENT.md` 2번 참고)
- **서버**: `~/welli/welli.env`에 운영용 값이 별도로 있음. 로컬 값과 달라도
  무방하며, git에는 절대 올라가지 않음 (`.gitignore` 처리됨)
- 새 환경변수가 필요해지면: `application.yml`에 `${새변수명}` 추가 →
  로컬 `welli.env`와 서버 `welli.env` 양쪽에 값 추가 → `docker-compose.yml`의
  `app.environment`에도 전달하도록 추가해야 함 (셋 중 하나라도 빠지면 동작 안 함)

## 4. 서버 접근 권한이 새로 필요한 팀원

1. 서버 관리자에게 SSH 키페어(`welliKeyPair.pem`) 요청
2. GHCR(private 이미지) pull 권한이 필요하면, 본인 GitHub 계정으로 PAT
   (`read:packages` 스코프) 발급 후 서버에서 직접 로그인:
   ```bash
   echo "본인_토큰" | docker login ghcr.io -u <본인_GitHub아이디> --password-stdin
   ```
   (토큰은 공유하지 말고 각자 발급)

## 5. 자주 쓰는 서버 명령어

```bash
cd ~/welli
docker compose --env-file welli.env ps                 # 컨테이너 상태 확인
docker compose --env-file welli.env logs app --tail=50  # 앱 로그 확인
docker compose --env-file welli.env pull && \
docker compose --env-file welli.env up -d               # 수동 재배포 (평소엔 Actions가 자동으로 해줌)
```

## 6. 서버 DB 직접 조회

3306 포트는 외부에 안 열려있어서, 컨테이너 안으로 들어가서 조회합니다.
```bash
docker exec -it welli-mysql mysql -u root -p welli
```
비밀번호는 `welli.env`의 `DB_PASSWORD`. 오타 걱정되면:
```bash
docker exec -it welli-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" welli'
```

> **root 비밀번호는 컨테이너를 맨 처음 띄웠을 때의 `welli.env` 값으로
> 고정됩니다.** 이후 `welli.env`를 수정해도 이미 만들어진 DB 계정 비밀번호는
> 자동으로 안 바뀌니, `Access denied` 에러 나면 이 부분부터 의심하세요.
> 자세한 내용과 GUI 툴(DBeaver 등) 연결 방법은 `DEPLOYMENT.md` 7번 참고.

## 7. OpenAI 기반 캐릭터 분석

`/analysis/run`은 건강 기록 + 현재 컨디션 점수를 OpenAI에 보내서 점수 변화량과
요약/피드백 문구를 받아옵니다. `OPENAI_API_KEY`가 `welli.env`에 없거나 API 호출이
실패해도 자동으로 규칙 기반 분석으로 대체되니 앱이 죽지는 않습니다. 자세한 구조와
환경변수는 `DEPLOYMENT.md` 10번 항목 참고.

## 8. (예정) 도메인/HTTPS 전환

프론트팀 배포(HTTPS)가 완료되면 백엔드도 도메인 연결 + Nginx + Let's Encrypt로
HTTPS 전환이 필요합니다. 순서/명령어는 `DEPLOYMENT.md` 9번 항목에 미리 정리해
뒀으니, 프론트 배포 일정 잡히면 그대로 따라 진행하면 됩니다.

## 9. 막히면

`DEPLOYMENT.md`의 "자주 발생하는 문제" 표를 먼저 확인하고, 그래도 안 풀리면
Actions 로그 + `docker compose logs app` 결과를 같이 공유해주세요.
