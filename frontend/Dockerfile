# Multi-stage Dockerfile for the React frontend
# Builds static files and serves via nginx

# Stage 1: Build
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files for better caching
COPY package.json ./

# Install dependencies
RUN npm install --silent

# Copy source code
COPY public/ public/
COPY src/ src/

# Build for production
ENV REACT_APP_API_URL=/api
RUN npm run build

# Stage 2: Serve with nginx
FROM nginx:alpine

# Copy nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built files
COPY --from=builder /app/build /usr/share/nginx/html

# nginx runs on port 80
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q --spider http://localhost:80 || exit 1

CMD ["nginx", "-g", "daemon off;"]
