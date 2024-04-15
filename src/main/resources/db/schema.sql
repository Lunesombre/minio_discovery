-- Créer le schéma s'il n'existe pas
CREATE SCHEMA IF NOT EXISTS minio;

-- Créer la table pour les fichiers téléchargés
CREATE TABLE IF NOT EXISTS minio.uploaded_files
(
    id          SERIAL PRIMARY KEY,
    filename    VARCHAR(255) NOT NULL,
    action      VARCHAR(255) NOT NULL,
    success     bool         NOT NULL,
    message     varchar(255) not null,
    occurred_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
