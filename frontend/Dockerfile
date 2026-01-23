# Deployment Dockerfile (Uses pre-built assets from GitHub Actions)
FROM nginx:alpine

# Nginx 설정 복사
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 빌드된 파일 복사
# 주의: 로컬에서 빌드하려면 먼저 'npm run build'를 실행해서 dist 폴더가 있어야 함.
COPY dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
