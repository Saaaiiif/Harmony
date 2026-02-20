-- Migration : demandes de salle pour événements (lieu = esprit)
-- À exécuter dans phpMyAdmin, base "projet"

-- Ajouter colonnes salle_id et statut_demande_salle
ALTER TABLE evenement ADD COLUMN salle_id INT NULL;
ALTER TABLE evenement ADD COLUMN statut_demande_salle VARCHAR(20) NULL;

-- Optionnel : supprimer approuve si elle existe (ancienne logique)
-- ALTER TABLE evenement DROP COLUMN approuve;
