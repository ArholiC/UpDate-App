FROM node:18-alpine

WORKDIR /app

# Paket bağımlılıklarını kopyala ve yükle
COPY package*.json ./
RUN npm install

# Tüm uygulama kodlarını kopyala
COPY . .

# Uygulamanın çalışacağı portu dışarı aç
EXPOSE 5000

# Uygulamayı başlat
CMD ["npm", "start"]
