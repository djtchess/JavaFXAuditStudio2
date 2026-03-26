-- JAS-003 : ajout du compteur de methodes lifecycle exclues de l'analyse
ALTER TABLE classification_result
    ADD COLUMN excluded_lifecycle_methods_count INTEGER NOT NULL DEFAULT 0;
