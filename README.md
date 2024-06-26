# MINIO_TEST README

## Petit projet vite fait pour bidouiller avec MinIo avant de l'utiliser ailleurs.

## Pour utiliser ce projet tel quel vous avez besoin d'un PostgreSql ouvert sur le port 5432, et d'un Minio sur les port 9000 et 9001.

Je vous conseille de lancer un container docker pour minIo, c'est rapide et pratique ; dans votre bash, ou autre :

```
docker run -d --name MinioCtnzed -p 9000:9000 -p 9001:9001 \
quay.io/minio/minio server /data --console-address ":9001"
```

Vous aurez accès à la console admin de minIo sur le `http://localhost:9001` (mais c'est juste pour checker, le projet tourne même sans aller sur ce
port).

MinIo stocke les fichiers dans des Bucket, ce projet crée un Bucket `minio-test` si nécessaire.

Dans ***resources/db***, vous trouverez un fichier `schema.sql`, lancez-le pour créer un schéma "minio" et la table qui va bien, que j'utilise pour
logger les actions effectuées sur minio.

### La base

La MinioConfig crée un minioClient (credentials et minioUrl, ici  `http://localhost:9000`), et crée un Bucker si nécessaire

Un FileService et un FileController (REST) assez simples, avec un POST pour l'upload, un GET pour le download et aussi des GET pour voir si un fichier
existe et ce qu'il y a dans le Bucket, un DELETE pour supprimer un fichier.

Quand on ré-upload un fichier qui existe déjà, en fait dans ma version ça flingue celui qui est enregistré pour le ré-uploader. C'est pas idéal dans
l'absolu mais dans notre useCase ça me parait bien (je veux mettre à jour le pdf => je flingue celui en ligne et je le remet).

### Un p'tit log sous forme de table postgresql

J'ai rajouté des "logs" sous forme d'une table postgresql qui enregistre l'action tentée, l'action son succès ou non, un message, et le timestamp de
la réalisation de l'action.

### UPDATE 15/04/2024 :

- ***rajout <ins>contrôle du format</ins>*** des fichiers uploadés : accepte que du PDF et du PNG (c'est un exemple, essayer d'upload du jpeg par ex,
  ça
  passe en
  erreur);
- ***rajout <ins>contrôle taille des fichiers uploadés</ins>*** :
    - dans application.properties: réduction taille max des requêtes et des fichiers à 2 MB (de base c'est 10) => **envoyer un fichier plus gros de
      ça renvoie une erreur 413 : Request Entity Too Large !**
    - dans le controller, ajout limite à 1.3333 MO
      => **en envoyant un fichier >1.3333 mais < à 2 MO, on passe sur une 500 avec mon message d'erreur et enregistrement d'une ligne en DB.** *C'est
      surtout pour l'exemple que j'ai mis les 2 fonctionnements.*
    - NB : postman possède sa propre limite de taille de fichier autorisé en upload, vous pouvez la changer si besoin, ou bien faire la requête en
      curl dans votre CLI préférée :
      ```curl
      curl -X POST -F "file=@chemin/du/fichier/nomDuFichier.pdf" http://localhost:8080/api/file/upload

      ```
- MàJ de la collection postman

### Collection Postman pour tester :

Je ne suis pas sûr que le fichier que j'ai uploadé fonctionne directement, au pire, importez une petite image.png à la place de
mon `ERD-Notation.PNG`. (idem pour les autres fichiers).

Attention l'application <ins>n'accepte plus que du PDF et du PNG</ins> en upload. Si vous utilisez un autre format, c'est pour tester les cas d'
échec :)

```json
{
  "info": {
    "_postman_id": "37b307a1-9080-4b2b-be83-0d7bbf48f39c",
    "name": "Test minio",
    "description": "Petite collection pour tester la mini-appli que j'ai crée pour bidouiller avec io.minIO.",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
    "_exporter_id": "31689416"
  },
  "item": [
    {
      "name": "Uploader un truc",
      "request": {
        "method": "POST",
        "header": [],
        "body": {
          "mode": "formdata",
          "formdata": [
            {
              "key": "file",
              "type": "file",
              "src": "postman-cloud:///1eefa92d-6457-4730-a1c6-56e4da0877fd"
            }
          ]
        },
        "url": {
          "raw": "{{base_url}}api/file/upload",
          "host": [
            "{{base_url}}api"
          ],
          "path": [
            "file",
            "upload"
          ]
        }
      },
      "response": []
    },
    {
      "name": "Uploader un truc en changeant son nom",
      "request": {
        "method": "POST",
        "header": [],
        "body": {
          "mode": "formdata",
          "formdata": [
            {
              "key": "file",
              "type": "file",
              "src": "postman-cloud:///1eefa92d-6457-4730-a1c6-56e4da0877fd"
            },
            {
              "key": "newFileName",
              "value": "Michel.png",
              "type": "text"
            }
          ]
        },
        "url": {
          "raw": "{{base_url}}api/file/upload",
          "host": [
            "{{base_url}}api"
          ],
          "path": [
            "file",
            "upload"
          ],
          "query": [
            {
              "key": "file",
              "value": null,
              "disabled": true
            }
          ]
        }
      },
      "response": []
    },
    {
      "name": "Downloader un truc",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "{{base_url}}api/file/download?fileName=ERD-Notation.PNG",
          "host": [
            "{{base_url}}api"
          ],
          "path": [
            "file",
            "download"
          ],
          "query": [
            {
              "key": "fileName",
              "value": "ERD-Notation.PNG"
            }
          ]
        }
      },
      "response": []
    },
    {
      "name": "Essayer de DL qqch qui n'existe pas",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "{{base_url}}api/file/download?fileName=Michel.pdf",
          "host": [
            "{{base_url}}api"
          ],
          "path": [
            "file",
            "download"
          ],
          "query": [
            {
              "key": "fileName",
              "value": "Michel.pdf"
            }
          ]
        }
      },
      "response": []
    },
    {
      "name": "Delete michel.png",
      "request": {
        "method": "DELETE",
        "header": [],
        "body": {
          "mode": "formdata",
          "formdata": []
        },
        "url": {
          "raw": "{{base_url}}api/file/delete/Michel.png",
          "host": [
            "{{base_url}}api"
          ],
          "path": [
            "file",
            "delete",
            "Michel.png"
          ]
        }
      },
      "response": []
    },
    {
      "name": "Checker un truc qui existe pas",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "{{base_url}}api/file/verify/Michel.pdf",
          "host": [
            "{{base_url}}api"
          ],
          "path": [
            "file",
            "verify",
            "Michel.pdf"
          ]
        }
      },
      "response": []
    },
    {
      "name": "Checker un truc qui existe",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "{{base_url}}api/file/verify/ERD-Notation.PNG",
          "host": [
            "{{base_url}}api"
          ],
          "path": [
            "file",
            "verify",
            "ERD-Notation.PNG"
          ]
        }
      },
      "response": []
    },
    {
      "name": "checker tout ce qu'il y a dans le bucket",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "{{base_url}}api/file/listAllFiles/minio-test",
          "host": [
            "{{base_url}}api"
          ],
          "path": [
            "file",
            "listAllFiles",
            "minio-test"
          ]
        }
      },
      "response": []
    },
    {
      "name": "Essai upload truc mauvais format",
      "request": {
        "method": "POST",
        "header": [],
        "body": {
          "mode": "formdata",
          "formdata": [
            {
              "key": "file",
              "type": "file",
              "src": "postman-cloud:///1eefb016-a044-4d70-b72a-b27c3f3c50a6"
            }
          ]
        },
        "url": {
          "raw": "{{base_url}}api/file/upload",
          "host": [
            "{{base_url}}api"
          ],
          "path": [
            "file",
            "upload"
          ]
        }
      },
      "response": []
    },
    {
      "name": "Essai upload truc trop gros (limite taille descendue à 1MB)",
      "request": {
        "method": "POST",
        "header": [],
        "body": {
          "mode": "formdata",
          "formdata": [
            {
              "key": "file",
              "type": "file",
              "src": "postman-cloud:///1eefb104-7a79-4e80-99a6-d94f1dd29783"
            }
          ]
        },
        "url": {
          "raw": "{{base_url}}api/file/upload",
          "host": [
            "{{base_url}}api"
          ],
          "path": [
            "file",
            "upload"
          ]
        }
      },
      "response": []
    }
  ],
  "variable": [
    {
      "key": "base_url",
      "value": "http://localhost:8080/"
    },
    {
      "key": "api_file",
      "value": "api/file"
    }
  ]
}
```
