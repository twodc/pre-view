import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// 커스텀 메트릭
const aiResponseTime = new Trend('ai_response_time');
const successRate = new Rate('success_rate');
const requestCounter = new Counter('total_requests');

// 테스트 설정
export const options = {
  scenarios: {
    // 점진적 부하 테스트
    gradual_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 10 },   // 10초 동안 10명까지 증가
        { duration: '20s', target: 30 },   // 20초 동안 30명까지 증가
        { duration: '30s', target: 30 },   // 30초간 30명 유지
        { duration: '10s', target: 0 },    // 10초 동안 0명으로 감소
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<10000'],  // 95%가 10초 이내
    success_rate: ['rate>0.9'],          // 90% 이상 성공
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
  console.log(`\n========================================`);
  console.log(`Pre-View 가상 스레드 부하 테스트`);
  console.log(`Target: ${BASE_URL}`);
  console.log(`========================================\n`);

  // 헬스 체크
  const healthRes = http.get(`${BASE_URL}/api/loadtest/health`);
  if (healthRes.status !== 200) {
    console.error('헬스 체크 실패! 서버가 실행 중인지 확인하세요.');
    console.error(`Status: ${healthRes.status}, Body: ${healthRes.body}`);
  } else {
    console.log('✓ 서버 헬스 체크 통과');
  }

  return { startTime: new Date().toISOString() };
}

export default function () {
  group('AI 호출 시뮬레이션', function () {
    const payload = JSON.stringify({
      content: '저는 Spring Boot와 JPA를 사용한 REST API 개발 경험이 있습니다. 특히 N+1 문제 해결과 트랜잭션 관리에 집중하여 성능을 최적화했습니다.',
    });

    const headers = {
      'Content-Type': 'application/json',
    };

    const startTime = Date.now();

    const response = http.post(
      `${BASE_URL}/api/loadtest/ai-simulation`,
      payload,
      { headers, timeout: '15s' }
    );

    const duration = Date.now() - startTime;
    aiResponseTime.add(duration);
    requestCounter.add(1);

    const success = check(response, {
      'status is 200': (r) => r.status === 200,
      'response has data': (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.data !== undefined;
        } catch {
          return false;
        }
      },
      'response time < 10s': (r) => r.timings.duration < 10000,
    });

    successRate.add(success);

    if (!success && response.status !== 200) {
      console.log(`Request failed: ${response.status} - ${response.body}`);
    }
  });

  // 요청 간 간격 (실제 사용자 시뮬레이션)
  sleep(Math.random() * 1 + 0.5); // 0.5-1.5초 랜덤 대기
}

export function teardown(data) {
  console.log(`\n========================================`);
  console.log(`테스트 완료`);
  console.log(`시작 시간: ${data.startTime}`);
  console.log(`종료 시간: ${new Date().toISOString()}`);
  console.log(`========================================\n`);
}
