# syntax=docker/dockerfile:1
# oneBite 웹(Next.js) — 멀티스테이지: deps → build(standalone) → runner
# NEXT_PUBLIC_* 는 빌드 시 번들에 인라인되므로 build-arg 로 주입한다(공개값만).

FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM node:20-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ARG NEXT_PUBLIC_API_URL
ARG NEXT_PUBLIC_GOOGLE_CLIENT_ID
ARG NEXT_PUBLIC_KAKAO_REST_KEY
ARG NEXT_PUBLIC_NAVER_CLIENT_ID
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL \
    NEXT_PUBLIC_GOOGLE_CLIENT_ID=$NEXT_PUBLIC_GOOGLE_CLIENT_ID \
    NEXT_PUBLIC_KAKAO_REST_KEY=$NEXT_PUBLIC_KAKAO_REST_KEY \
    NEXT_PUBLIC_NAVER_CLIENT_ID=$NEXT_PUBLIC_NAVER_CLIENT_ID \
    NEXT_TELEMETRY_DISABLED=1
RUN npm run build && mkdir -p public  # public/ 없을 수도 있어 보장(runner COPY 안전)

FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production NEXT_TELEMETRY_DISABLED=1 PORT=3000 HOSTNAME=0.0.0.0
RUN addgroup -g 1001 nodejs && adduser -S -u 1001 nextjs
COPY --from=build /app/public ./public
COPY --from=build --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=build --chown=nextjs:nodejs /app/.next/static ./.next/static
USER nextjs
EXPOSE 3000
CMD ["node", "server.js"]
