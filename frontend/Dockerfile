# ─── Stage 1: Build the React app ──────────────────────────────────────────────
FROM node:22-alpine AS builder

WORKDIR /app

# Copy package files first (layer cache: only re-install if deps change)
COPY package.json package-lock.json ./
RUN npm ci --silent

# Copy source and build
# VITE_API_BASE_URL is intentionally left unset — Nginx will proxy /api to the backend
COPY . .
RUN npm run build

# ─── Stage 2: Serve with Nginx ──────────────────────────────────────────────────
FROM nginx:alpine AS runtime

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Copy our custom nginx config
COPY nginx.conf /etc/nginx/conf.d/app.conf

# Copy the built React app
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
