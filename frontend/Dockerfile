# Stage 1: Build
FROM node:20-alpine AS builder

WORKDIR /app

# 의존성 캐싱
COPY package.json package-lock.json* ./
RUN npm ci

# 빌드 인자로 API URL 받기
ARG VITE_API_BASE_URL=http://localhost:8080/api/v1
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# 소스 복사 및 빌드
COPY . .
RUN npm run build

# Stage 2: Nginx
FROM nginx:alpine

# Nginx 설정 복사
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 빌드된 파일 복사
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
