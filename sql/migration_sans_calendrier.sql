-- Migration : rendre tache indépendante du calendrier (après suppression de la classe Calendrier)
-- Exécuter dans phpMyAdmin sur la base "projet" si l'ajout de tâches échoue.

-- 1) Supprimer la contrainte de clé étrangère (ignorer l'erreur si elle n'existe plus)
ALTER TABLE tache DROP FOREIGN KEY fk_tache_calendrier;

-- 2) Rendre calendrier_id nullable (obligatoire pour que l'ajout de tâches fonctionne)
ALTER TABLE tache MODIFY calendrier_id INT NULL;
