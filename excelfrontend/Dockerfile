ARG NODE_VERSION=24-alpine
FROM  node:${NODE_VERSION} AS builder

WORKDIR /app

COPY package.json package-lock.json* ./
RUN --mount=type=cache, target=/root/.npm npm ci

COPY . .
RUN npm run build -- --configuration=production

FROM nginxinc/nginx-unprivileged:alpine AS runner


COPY nginx.conf /etc/nginx/nginx.conf
COPY --chown=nginx:nginx --from=builder /app/dist/*/browser /usr/share/nginx/html

USER nginx
EXPOSE 8080

ENTRYPOINT ["nginx", "-c", "/etc/nginx/nginx.conf"]
CMD ["-g", "daemon off;"]