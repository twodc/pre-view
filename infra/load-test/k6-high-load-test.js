import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// 커스텀 메트릭
const aiResponseTime = new Trend('ai_response_time');
const successRate = new Rate('success_rate');
const requestCounter = new Counter('total_requests');

// 고부하 테스트 설정 (300명 동시 사용자)
export const options = {
  scenarios: {
    high_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '15s', target: 100 },   // 15초 동안 100명까지 증가
        { duration: '15s', target: 300 },   // 15초 동안 300명까지 증가
        { duration: '30s', target: 300 },   // 30초간 300명 유지 (핵심 측정 구간)
        { duration: '10s', target: 0 },     // 10초 동안 0명으로 감소
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<15000'],  // 95%가 15초 이내
    success_rate: ['rate>0.8'],          // 80% 이상 성공 (고부하 시 일부 실패 허용)
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
  console.log(`\n========================================`);
  console.log(`Pre-View 고부하 테스트 (최대 300 VUs)`);
  console.log(`Target: ${BASE_URL}`);
  console.log(`========================================\n`);

  // 헬스 체크
  const healthRes = http.get(`${BASE_URL}/api/loadtest/health`);
  if (healthRes.status !== 200) {
    console.error('헬스 체크 실패! 서버가 실행 중인지 확인하세요.');
  } else {
    console.log('✓ 서버 헬스 체크 통과');
  }

  return { startTime: new Date().toISOString() };
}

export default function () {
  group('AI 호출 시뮬레이션 (고부하)', function () {
    const payload = JSON.stringify({
      content: '저는 Spring Boot와 JPA를 사용한 REST API 개발 경험이 있습니다.',
    });

    const headers = {
      'Content-Type': 'application/json',
    };

    const startTime = Date.now();

    const response = http.post(
      `${BASE_URL}/api/loadtest/ai-simulation`,
      payload,
      { headers, timeout: '30s' }
    );

    const duration = Date.now() - startTime;
    aiResponseTime.add(duration);
    requestCounter.add(1);

    const success = check(response, {
      'status is 200': (r) => r.status === 200,
      'response time < 15s': (r) => r.timings.duration < 15000,
    });

    successRate.add(success);

    if (!success && response.status !== 200) {
      console.log(`Failed: ${response.status}`);
    }
  });

  // 요청 간 최소 간격
  sleep(Math.random() * 0.5 + 0.2); // 0.2-0.7초 랜덤 대기
}

export function teardown(data) {
  console.log(`\n========================================`);
  console.log(`고부하 테스트 완료`);
  console.log(`시작: ${data.startTime}`);
  console.log(`종료: ${new Date().toISOString()}`);
  console.log(`========================================\n`);
}
