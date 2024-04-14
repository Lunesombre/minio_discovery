-- Créer le schéma s'il n'existe pas
CREATE SCHEMA IF NOT EXISTS minio;

-- Créer la table pour les fichiers téléchargés
CREATE TABLE IF NOT EXISTS minio.uploaded_files
(
    id                SERIAL PRIMARY KEY,
    filename          VARCHAR(255) NOT NULL,
    minio_object_name VARCHAR(255) NOT NULL,
    action            VARCHAR(255) NOT NULL,
    occurred_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
