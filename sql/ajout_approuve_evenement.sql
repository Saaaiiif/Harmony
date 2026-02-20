-- Ajouter la colonne approuve à la table evenement (notification admin)
-- À exécuter dans phpMyAdmin, base "projet"

ALTER TABLE evenement ADD COLUMN approuve TINYINT(1) NOT NULL DEFAULT 0;
-- 0 = en attente d'approbation, 1 = approuvé par l'admin
