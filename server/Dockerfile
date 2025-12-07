# Dockerfile pour Control Deck Server

FROM node:20-alpine

# Métadonnées
LABEL maintainer="Control Deck Team"
LABEL description="Control Deck Server - Remote control server"

# Créer le répertoire de travail
WORKDIR /app

# Copier les fichiers de dépendances
COPY package*.json ./

# Installer les dépendances
RUN npm ci --only=production

# Copier le code source
COPY . .

# Créer les répertoires nécessaires
RUN mkdir -p logs config profiles plugins data

# Exposer le port
EXPOSE 4455

# Variables d'environnement par défaut
ENV NODE_ENV=production
ENV PORT=4455
ENV LOG_LEVEL=info

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD node scripts/health-check.js --url=http://localhost:${PORT} || exit 1

# Commande de démarrage
CMD ["node", "index.js"]





