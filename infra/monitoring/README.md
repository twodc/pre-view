# Pre-View 모니터링 & 부하 테스트 가이드

> **참고**: 모든 명령어는 저장소 루트에서 실행하는 것을 기준으로 합니다.

## 1. 모니터링 스택 실행

```bash
# 모니터링 스택 시작 (Prometheus + Grafana)
cd infra/monitoring
docker-compose up -d

# 상태 확인
docker-compose ps
```

**접속 URL:**
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3001 (admin / admin)

## 2. Spring Boot 애플리케이션 실행

```bash
# 로컬 프로파일로 실행 (Actuator 전체 노출)
./gradlew bootRun --args='--spring.profiles.active=local'
```

**Actuator 엔드포인트 확인:**
```bash
# Prometheus 메트릭 확인
curl http://localhost:8080/actuator/prometheus | head -50

# 헬스 체크
curl http://localhost:8080/actuator/health
```

## 3. Grafana 대시보드 접속

1. http://localhost:3001 접속
2. admin / admin 으로 로그인
3. 좌측 메뉴 → Dashboards → Pre-View Load Test Dashboard

## 4. 부하 테스트 실행

### 일반 부하 테스트 (30 VUs)
```bash
cd infra/load-test
k6 run k6-test.js
```

### 고부하 테스트 (300 VUs)
```bash
cd infra/load-test
k6 run k6-high-load-test.js
```

## 5. 가상 스레드 ON/OFF 비교 테스트

### 가상 스레드 OFF로 테스트
```bash
# application.yaml 수정
# spring.threads.virtual.enabled: false

# 서버 재시작 후 부하 테스트
k6 run k6-high-load-test.js
```

### 가상 스레드 ON으로 테스트
```bash
# application.yaml 수정
# spring.threads.virtual.enabled: true

# 서버 재시작 후 부하 테스트
k6 run k6-high-load-test.js
```

## 6. 대시보드에서 확인할 메트릭

| 패널 | 설명 | 가상스레드 ON 시 기대 |
|------|------|---------------------|
| Throughput | 초당 요청 처리량 | 15-20% 증가 |
| Response Time | p50/p95/p99 응답 시간 | p95 20-30% 감소 |
| Active Threads | 활성 스레드 수 | 급격한 증가 없음 |
| JVM Heap | 힙 메모리 사용률 | 큰 차이 없음 |

## 7. 스크린샷 저장 (Notion 문서용)

Grafana 대시보드에서:
1. 부하 테스트 실행 중 대시보드 확인
2. 우측 상단 Share → Snapshot 또는 직접 스크린샷
3. 가상스레드 OFF/ON 각각 저장하여 비교

## 8. 정리

```bash
# 모니터링 스택 종료
cd infra/monitoring
docker-compose down

# 볼륨까지 삭제 (완전 초기화)
docker-compose down -v
```

## 트러블슈팅

### Prometheus가 메트릭을 수집하지 못할 때
```bash
# Spring Boot가 실행 중인지 확인
curl http://localhost:8080/actuator/prometheus

# Prometheus 타겟 상태 확인
# http://localhost:9090/targets 접속
```

### Grafana에서 데이터가 안 보일 때
1. Data Sources → Prometheus → Test 버튼 클릭
2. 대시보드 우측 상단 시간 범위를 "Last 5 minutes"로 설정
3. 부하 테스트가 실행 중인지 확인
