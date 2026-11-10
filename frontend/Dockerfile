# --- 1. Asama: build ---
# Uygulamayi Docker'in icinde derler; makinede Node kurulu olmasi gerekmez.
FROM node:24-alpine AS build
WORKDIR /app

# Once bagimlilik tanimlari (katman onbellegi: kod degisince paketler yeniden inmez).
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# Bos adres: istekler sayfanin geldigi adrese (/api/...) gider, nginx onlari backend'e iletir.
# Boylece imaja backend adresi gomulmez ve tarayici tek bir adresle konustugu icin CORS gerekmez.
ARG VITE_API_BASE_URL=
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

# --- 2. Asama: runtime ---
# Yalnizca derlenmis statik dosyalar ve nginx.
FROM nginx:stable-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY security-headers.conf /etc/nginx/snippets/security-headers.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
