-- ============================================================
-- Script pour phpMyAdmin (XAMPP) - Base "projet"
-- Conforme au diagramme UML : Calendrier, Evenement, Tache.
-- VueCalendrier, TypeEvenement, StatutTache = énumérations (VARCHAR).
-- Exécuter dans l'onglet SQL après avoir sélectionné la base "projet".
-- ============================================================

-- ----- Option : repartir de zéro (décommenter si vous supprimez tout) -----
-- DROP TABLE IF EXISTS tache;
-- DROP TABLE IF EXISTS evenement;
-- DROP TABLE IF EXISTS calendrier;
-- Puis recréer la table evenement si vous l'avez supprimée (voir plus bas).

-- 1) Table calendrier
--    VueCalendrier = énumération (JOUR, SEMAINE, MOIS) stockée en VARCHAR
CREATE TABLE IF NOT EXISTS calendrier (
    id INT AUTO_INCREMENT PRIMARY KEY,
    vue_calendrier VARCHAR(20) NOT NULL DEFAULT 'MOIS'
);

-- 2) Lier evenement à calendrier
-- Si erreur #1060 "calendrier_id déjà utilisé" : ne pas exécuter la ligne suivante.
ALTER TABLE evenement ADD COLUMN calendrier_id INT NULL;
-- Si erreur "Duplicate foreign key" : la contrainte existe déjà, ignorer.
ALTER TABLE evenement
  ADD CONSTRAINT fk_evenement_calendrier
  FOREIGN KEY (calendrier_id) REFERENCES calendrier(id)
  ON DELETE SET NULL ON UPDATE CASCADE;

-- 3) Table tache
--    StatutTache = énumération (A_FAIRE, EN_COURS, TERMINEE) en VARCHAR
--    parent_tache_id = sous-tâches (référence vers tache.id)
CREATE TABLE IF NOT EXISTS tache (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    deadline DATE NULL,
    notes TEXT NULL,
    statut_tache VARCHAR(20) NOT NULL DEFAULT 'A_FAIRE',
    calendrier_id INT NOT NULL,
    parent_tache_id INT NULL,
    CONSTRAINT fk_tache_calendrier
        FOREIGN KEY (calendrier_id) REFERENCES calendrier(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_tache_parent
        FOREIGN KEY (parent_tache_id) REFERENCES tache(id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- 4) Un calendrier par défaut (vue = MOIS)
INSERT IGNORE INTO calendrier (id, vue_calendrier) VALUES (1, 'MOIS');

-- 5) Optionnel : lier les événements existants au calendrier 1
-- UPDATE evenement SET calendrier_id = 1 WHERE calendrier_id IS NULL;

-- ========== Si vous avez tout supprimé (evenement inclus) ==========
-- Recréer evenement avec calendrier_id (TypeEvenement = VARCHAR) :
/*
CREATE TABLE evenement (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    description TEXT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    lieu VARCHAR(255) NULL,
    priorite INT NOT NULL DEFAULT 1,
    rappel_actif TINYINT(1) NOT NULL DEFAULT 1,
    type_evenement VARCHAR(20) NOT NULL DEFAULT 'REUNION',
    calendrier_id INT NULL,
    CONSTRAINT fk_evenement_calendrier
        FOREIGN KEY (calendrier_id) REFERENCES calendrier(id)
        ON DELETE SET NULL ON UPDATE CASCADE
);
*/
