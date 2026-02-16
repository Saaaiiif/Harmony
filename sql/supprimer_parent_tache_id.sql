-- ============================================================
-- À exécuter dans phpMyAdmin (onglet SQL, base "projet")
-- Supprime la colonne parent_tache_id de la table tache
-- ============================================================

-- 1) Supprimer la contrainte de clé étrangère (parent_tache_id -> tache.id)
ALTER TABLE tache DROP FOREIGN KEY fk_tache_parent;

-- 2) Supprimer la colonne parent_tache_id
ALTER TABLE tache DROP COLUMN parent_tache_id;
