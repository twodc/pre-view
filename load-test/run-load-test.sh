#!/bin/bash

# Pre-View 부하 테스트 스크립트
# 가상 스레드 ON/OFF 비교 테스트

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
WIREMOCK_PORT=9090
APP_PORT=8080
RESULTS_DIR="$SCRIPT_DIR/results"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Pre-View 부하 테스트 스크립트${NC}"
echo -e "${BLUE}========================================${NC}"

# 결과 디렉토리 생성
mkdir -p "$RESULTS_DIR"

# k6 설치 확인
check_k6() {
    if ! command -v k6 &> /dev/null; then
        echo -e "${RED}k6가 설치되어 있지 않습니다.${NC}"
        echo -e "${YELLOW}설치 방법: brew install k6${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ k6 확인됨${NC}"
}

# WireMock 다운로드 및 실행
start_wiremock() {
    echo -e "\n${YELLOW}WireMock 서버 시작 중...${NC}"

    # WireMock JAR 다운로드 (없는 경우)
    if [ ! -f "$SCRIPT_DIR/wiremock-standalone.jar" ]; then
        echo "WireMock 다운로드 중..."
        curl -L -o "$SCRIPT_DIR/wiremock-standalone.jar" \
            "https://repo1.maven.org/maven2/org/wiremock/wiremock-standalone/3.3.1/wiremock-standalone-3.3.1.jar"
    fi

    # 기존 WireMock 종료
    pkill -f "wiremock-standalone" 2>/dev/null || true
    sleep 1

    # WireMock 시작 (백그라운드)
    java -jar "$SCRIPT_DIR/wiremock-standalone.jar" \
        --port $WIREMOCK_PORT \
        --root-dir "$SCRIPT_DIR/wiremock-mappings" \
        --global-response-templating &

    WIREMOCK_PID=$!
    echo "WireMock PID: $WIREMOCK_PID"

    # WireMock 준비 대기
    sleep 3

    # 매핑 로드
    echo "AI 응답 매핑 로드 중..."
    curl -X POST "http://localhost:$WIREMOCK_PORT/__admin/mappings" \
        -H "Content-Type: application/json" \
        -d @"$SCRIPT_DIR/wiremock-mappings/ai-chat.json" 2>/dev/null || true

    echo -e "${GREEN}✓ WireMock 서버 시작됨 (포트: $WIREMOCK_PORT)${NC}"
}

# 애플리케이션 빌드 및 실행
start_app() {
    local virtual_threads=$1
    local profile_suffix=$2

    echo -e "\n${YELLOW}애플리케이션 빌드 중...${NC}"
    cd "$PROJECT_DIR"

    # 가상 스레드 설정 변경
    if [ "$virtual_threads" = "true" ]; then
        echo -e "${GREEN}가상 스레드: ON${NC}"
    else
        echo -e "${RED}가상 스레드: OFF${NC}"
        # application-loadtest.yaml에 가상 스레드 OFF 추가
        sed -i.bak 's/# 가상 스레드 설정/spring:\n  threads:\n    virtual:\n      enabled: false\n\n# 가상 스레드 설정/' \
            src/main/resources/application-loadtest.yaml 2>/dev/null || true
    fi

    # 빌드
    ./gradlew build -x test --quiet

    # 기존 앱 종료
    pkill -f "pre-view.*jar" 2>/dev/null || true
    sleep 2

    # 앱 시작
    echo -e "${YELLOW}애플리케이션 시작 중...${NC}"
    java -jar build/libs/pre-view-*.jar \
        --spring.profiles.active=loadtest \
        --spring.threads.virtual.enabled=$virtual_threads &

    APP_PID=$!
    echo "App PID: $APP_PID"

    # 앱 준비 대기
    echo "애플리케이션 준비 대기 중..."
    for i in {1..30}; do
        if curl -s "http://localhost:$APP_PORT/api-docs" > /dev/null 2>&1; then
            echo -e "${GREEN}✓ 애플리케이션 시작됨${NC}"
            return 0
        fi
        sleep 2
    done

    echo -e "${RED}애플리케이션 시작 실패${NC}"
    exit 1
}

# k6 테스트 실행
run_k6_test() {
    local test_name=$1
    local output_file="$RESULTS_DIR/${test_name}_$(date +%Y%m%d_%H%M%S).json"

    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}k6 부하 테스트 실행: $test_name${NC}"
    echo -e "${BLUE}========================================${NC}"

    k6 run \
        --out json="$output_file" \
        --summary-export="$RESULTS_DIR/${test_name}_summary.json" \
        "$SCRIPT_DIR/k6-test.js"

    echo -e "${GREEN}결과 저장됨: $output_file${NC}"
}

# 정리
cleanup() {
    echo -e "\n${YELLOW}정리 중...${NC}"
    pkill -f "wiremock-standalone" 2>/dev/null || true
    pkill -f "pre-view.*jar" 2>/dev/null || true
    echo -e "${GREEN}✓ 정리 완료${NC}"
}

# 사용법 출력
usage() {
    echo "사용법: $0 [옵션]"
    echo ""
    echo "옵션:"
    echo "  --wiremock-only    WireMock만 시작"
    echo "  --test-off         가상 스레드 OFF 테스트만 실행"
    echo "  --test-on          가상 스레드 ON 테스트만 실행"
    echo "  --both             둘 다 실행 (기본값)"
    echo "  --cleanup          프로세스 정리"
    echo ""
}

# 메인 실행
main() {
    trap cleanup EXIT

    check_k6

    case "${1:-both}" in
        --wiremock-only)
            start_wiremock
            echo -e "\n${GREEN}WireMock 서버가 실행 중입니다.${NC}"
            echo "종료하려면 Ctrl+C를 누르세요."
            wait
            ;;
        --test-off)
            start_wiremock
            start_app "false" "off"
            run_k6_test "virtual_threads_OFF"
            ;;
        --test-on)
            start_wiremock
            start_app "true" "on"
            run_k6_test "virtual_threads_ON"
            ;;
        --both)
            start_wiremock

            # 가상 스레드 OFF 테스트
            start_app "false" "off"
            run_k6_test "virtual_threads_OFF"

            # 잠시 대기
            pkill -f "pre-view.*jar" 2>/dev/null || true
            sleep 3

            # 가상 스레드 ON 테스트
            start_app "true" "on"
            run_k6_test "virtual_threads_ON"

            # 결과 비교
            echo -e "\n${BLUE}========================================${NC}"
            echo -e "${BLUE}테스트 결과 비교${NC}"
            echo -e "${BLUE}========================================${NC}"
            echo -e "${YELLOW}결과 파일 위치: $RESULTS_DIR${NC}"
            ls -la "$RESULTS_DIR"
            ;;
        --cleanup)
            cleanup
            ;;
        --help|-h)
            usage
            ;;
        *)
            echo -e "${RED}알 수 없는 옵션: $1${NC}"
            usage
            exit 1
            ;;
    esac
}

main "$@"
