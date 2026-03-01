# Stage 1: Node Build
# Using Node 20 LTS for consistency with CI and ARM64 support
FROM node:20 AS builder

WORKDIR /build

# Copy package files for dependency caching
COPY package.json package-lock.json* ./

# Install dependencies (npm; pnpm overrides in package.json are for local dev)
RUN npm ci 2>/dev/null || npm install

# Copy source code
COPY . .

# Build production bundle (Vite output: dist/)
RUN npm run build

# Stage 2: Nginx Runtime
FROM nginx:alpine

WORKDIR /usr/share/nginx/html

# Remove default nginx static assets
RUN rm -rf ./*

# Copy built assets from builder stage
COPY --from=builder /build/dist ./

# Copy custom nginx configuration (SPA fallback + API/WS proxy)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Create non-privileged user and adjust permissions
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chmod -R 755 /usr/share/nginx/html

# Expose HTTP port
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost/ || exit 1

# Nginx will run as root but worker processes will run as nginx user (default behavior)
CMD ["nginx", "-g", "daemon off;"]
