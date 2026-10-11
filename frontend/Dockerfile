# syntax=docker/dockerfile:1

# =====================================================================
# Frontend React/Vite — immagine in due fasi.
#
#  1) build : Node 22, npm ci + vite build → cartella dist statica.
#  2) serve : nginx serve la SPA e fa da reverse proxy verso il backend
#             per /api e /ws (vedi nginx.conf): frontend e API sulla
#             stessa origine, quindi niente CORS e un solo indirizzo.
# =====================================================================

FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# VITE_API_BASE_URL resta vuoto: le chiamate sono relative (/api, /ws) e
# passano da nginx
RUN npm run build

# ─────────────────────────────────────────────────────────────────────
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
