import java.util.*;

/**
 * Gestion de l'orientation des rues selon les hypothèses HO1, HO2, HO3
 * HO1 : Toutes les rues à double sens, ramassage des deux côtés
 * HO2 : Sens uniques + double sens multi-voies (ramassage d'un seul côté)
 * HO3 : Mixte (double sens 1 voie = ramassage 2 côtés, multi-voies = 1 côté)
 */
class OrientationRue {

    // Types d'orientation
    enum TypeOrientation {
        DOUBLE_SENS_UNE_VOIE,      // Double sens 1 voie : ramassage 2 côtés
        DOUBLE_SENS_MULTI_VOIES,   // Double sens 2+ voies : ramassage 1 côté
        SENS_UNIQUE                 // Sens unique : ramassage 1 côté
    }

    // Hypothèses d'orientation globale
    enum HypothèseOrientation {
        HO1,  // Toutes rues double sens, ramassage 2 côtés
        HO2,  // Graphe orienté, ramassage 1 côté
        HO3   // Graphe mixte selon nb voies
    }

    // Configuration d'une rue
    static class ConfigurationRue {
        String nomRue;
        TypeOrientation type;
        int nombreVoies;
        boolean sensUnique;
        String sensAutoriseDepart;  // Si sens unique : noeud de départ autorisé
        String sensAutoriseArrivee; // Si sens unique : noeud d'arrivée autorisé
        boolean ramassageDeuxCotes;

        public ConfigurationRue(String nomRue) {
            this.nomRue = nomRue;
            this.type = TypeOrientation.DOUBLE_SENS_MULTI_VOIES;
            this.nombreVoies = 2;
            this.sensUnique = false;
            this.ramassageDeuxCotes = false;
        }

        /**
         * Définir comme double sens avec 1 voie
         */
        public void setDoubleSensUneVoie() {
            this.type = TypeOrientation.DOUBLE_SENS_UNE_VOIE;
            this.nombreVoies = 1;
            this.sensUnique = false;
            this.ramassageDeuxCotes = true;
        }

        /**
         * Définir comme double sens avec plusieurs voies
         */
        public void setDoubleSensMultiVoies(int nbVoies) {
            this.type = TypeOrientation.DOUBLE_SENS_MULTI_VOIES;
            this.nombreVoies = nbVoies;
            this.sensUnique = false;
            this.ramassageDeuxCotes = false;
        }

        /**
         * Définir comme sens unique
         */
        public void setSensUnique(String depart, String arrivee) {
            this.type = TypeOrientation.SENS_UNIQUE;
            this.nombreVoies = 1;
            this.sensUnique = true;
            this.sensAutoriseDepart = depart;
            this.sensAutoriseArrivee = arrivee;
            this.ramassageDeuxCotes = false;
        }

        /**
         * Vérifier si le passage est autorisé dans ce sens
         */
        public boolean estPassageAutorise(String depart, String arrivee) {
            if (!sensUnique) return true;
            return depart.equals(sensAutoriseDepart) && arrivee.equals(sensAutoriseArrivee);
        }

        /**
         * Obtenir le symbole pour l'affichage
         */
        public String getSymbole() {
            if (sensUnique) return "→";
            if (ramassageDeuxCotes) return "⇄₁"; // Double sens 1 voie
            return "⇄₂"; // Double sens multi-voies
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(nomRue).append(" : ");

            if (sensUnique) {
                sb.append("→ SENS UNIQUE (")
                        .append(sensAutoriseDepart).append(" → ").append(sensAutoriseArrivee)
                        .append("), ramassage 1 côté");
            } else if (type == TypeOrientation.DOUBLE_SENS_UNE_VOIE) {
                sb.append("⇄ DOUBLE SENS 1 voie, ramassage 2 côtés");
            } else {
                sb.append("⇄ DOUBLE SENS ").append(nombreVoies)
                        .append(" voies, ramassage 1 côté");
            }

            return sb.toString();
        }
    }

    // Gestionnaire d'orientations
    private Map<String, ConfigurationRue> configurations;
    private HypothèseOrientation hypothèse;

    public OrientationRue(HypothèseOrientation hypothèse) {
        this.configurations = new HashMap<>();
        this.hypothèse = hypothèse;
    }

    /**
     * Configurer une rue
     */
    public void configurerRue(String nomRue, TypeOrientation type) {
        ConfigurationRue config = configurations.computeIfAbsent(nomRue, ConfigurationRue::new);

        switch (type) {
            case DOUBLE_SENS_UNE_VOIE:
                config.setDoubleSensUneVoie();
                break;
            case DOUBLE_SENS_MULTI_VOIES:
                config.setDoubleSensMultiVoies(2);
                break;
            case SENS_UNIQUE:
                // Sens unique nécessite depart/arrivee
                throw new IllegalArgumentException("Utilisez configurerSensUnique()");
        }
    }

    /**
     * Configurer une rue à sens unique
     */
    public void configurerSensUnique(String nomRue, String depart, String arrivee) {
        ConfigurationRue config = configurations.computeIfAbsent(nomRue, ConfigurationRue::new);
        config.setSensUnique(depart, arrivee);
    }

    /**
     * Vérifier si le passage est autorisé
     */
    public boolean estPassageAutorise(String nomRue, String depart, String arrivee) {
        // En HO1, tous les passages sont autorisés
        if (hypothèse == HypothèseOrientation.HO1) {
            return true;
        }

        ConfigurationRue config = configurations.get(nomRue);
        if (config == null) {
            // Par défaut : double sens
            return true;
        }

        return config.estPassageAutorise(depart, arrivee);
    }

    /**
     * Vérifier si le ramassage se fait des deux côtés
     */
    public boolean ramassageDeuxCotes(String nomRue) {
        // En HO1, toujours ramassage 2 côtés
        if (hypothèse == HypothèseOrientation.HO1) {
            return true;
        }

        ConfigurationRue config = configurations.get(nomRue);
        if (config == null) {
            // Par défaut selon hypothèse
            return hypothèse == HypothèseOrientation.HO3;
        }

        return config.ramassageDeuxCotes;
    }

    /**
     * Obtenir la configuration d'une rue
     */
    public ConfigurationRue getConfiguration(String nomRue) {
        return configurations.get(nomRue);
    }

    /**
     * Afficher toutes les configurations
     */
    public void afficherConfigurations() {
        System.out.println("\n🚦 CONFIGURATION DES RUES (Hypothèse " + hypothèse + ")");
        System.out.println("=".repeat(70));

        switch (hypothèse) {
            case HO1:
                System.out.println("Toutes les rues à double sens, ramassage des 2 côtés");
                break;
            case HO2:
                System.out.println("Rues à sens unique possibles, ramassage d'1 seul côté");
                break;
            case HO3:
                System.out.println("Rues mixtes : 1 voie = 2 côtés, multi-voies = 1 côté");
                break;
        }
        System.out.println("=".repeat(70));

        if (configurations.isEmpty()) {
            System.out.println("Aucune configuration spécifique (comportement par défaut)");
            return;
        }

        for (ConfigurationRue config : configurations.values()) {
            System.out.println(config.getSymbole() + " " + config);
        }
    }

    /**
     * Générer une configuration de test réaliste
     */
    public static OrientationRue creerConfigurationTest(HypothèseOrientation hypothèse) {
        OrientationRue orientation = new OrientationRue(hypothèse);

        if (hypothèse == HypothèseOrientation.HO1) {
            // HO1 : Tout en double sens, pas de config spécifique nécessaire
            System.out.println("Hypothèse HO1 : Configuration automatique (toutes rues à double sens)");
            return orientation;
        }

        if (hypothèse == HypothèseOrientation.HO2) {
            // HO2 : Certaines rues à sens unique
            orientation.configurerSensUnique("Rue Montmartre", "Maison La Defense", "Immeuble Tour Montparnasse");
            orientation.configurerSensUnique("Boulevard Nord", "Maison Esplanade de La Defense", "Maison Charles de Gaulle Etoile");

            // Les autres sont en double sens multi-voies
            orientation.configurerRue("Avenue Principale", TypeOrientation.DOUBLE_SENS_MULTI_VOIES);
            orientation.configurerRue("Rue Transversale", TypeOrientation.DOUBLE_SENS_MULTI_VOIES);
        }

        if (hypothèse == HypothèseOrientation.HO3) {
            // HO3 : Mélange selon nombre de voies

            // Grandes avenues : double sens multi-voies
            orientation.configurerRue("Avenue Principale", TypeOrientation.DOUBLE_SENS_MULTI_VOIES);
            orientation.configurerRue("Boulevard Nord", TypeOrientation.DOUBLE_SENS_MULTI_VOIES);
            orientation.configurerRue("Boulevard Sud", TypeOrientation.DOUBLE_SENS_MULTI_VOIES);

            // Petites rues : double sens 1 voie (ramassage 2 côtés)
            orientation.configurerRue("Rue Lafayette", TypeOrientation.DOUBLE_SENS_UNE_VOIE);
            orientation.configurerRue("Rue Victor Hugo", TypeOrientation.DOUBLE_SENS_UNE_VOIE);

            // Certaines rues à sens unique
            orientation.configurerSensUnique("Rue Montmartre", "Maison La Defense", "Immeuble Tour Montparnasse");
            orientation.configurerSensUnique("Avenue Est", "Place Centrale", "Immeuble Crystal Palace");
        }

        return orientation;
    }

    /**
     * Compter le nombre d'arcs à ramasser selon l'orientation
     */
    public int compterArcsARamasser(GrapheVille ville) {
        int count = 0;
        Set<String> arcsVus = new HashSet<>();

        for (Noeud noeud : ville.getNoeuds()) {
            for (Arc arc : noeud.getArcsSortants()) {
                if (arc.estChangementRue()) continue;

                String cleArc = arc.getRue() + ":" + arc.getDepart().getNom() + "->" + arc.getArrivee().getNom();

                if (arcsVus.contains(cleArc)) continue;
                arcsVus.add(cleArc);

                // En HO1, chaque rue compte pour 1 (ramassage 2 côtés en un passage)
                if (hypothèse == HypothèseOrientation.HO1) {
                    if (ramassageDeuxCotes(arc.getRue())) {
                        // Marquer aussi la direction inverse comme vue
                        String cleInverse = arc.getRue() + ":" + arc.getArrivee().getNom() + "->" + arc.getDepart().getNom();
                        arcsVus.add(cleInverse);
                    }
                }

                count++;
            }
        }

        return count;
    }

    /**
     * Obtenir la liste des rues configurées
     */
    public List<String> getRuesConfigurees() {
        return new ArrayList<>(configurations.keySet());
    }

    /**
     * Statistiques sur les orientations
     */
    public String getStatistiques() {
        int sensUnique = 0;
        int doubleSens1Voie = 0;
        int doubleSensMulti = 0;

        for (ConfigurationRue config : configurations.values()) {
            switch (config.type) {
                case SENS_UNIQUE:
                    sensUnique++;
                    break;
                case DOUBLE_SENS_UNE_VOIE:
                    doubleSens1Voie++;
                    break;
                case DOUBLE_SENS_MULTI_VOIES:
                    doubleSensMulti++;
                    break;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n📊 STATISTIQUES DES ORIENTATIONS\n");
        sb.append("=".repeat(50)).append("\n");
        sb.append("Hypothèse : ").append(hypothèse).append("\n");
        sb.append("Rues configurées : ").append(configurations.size()).append("\n");
        sb.append("  - Sens unique : ").append(sensUnique).append("\n");
        sb.append("  - Double sens 1 voie : ").append(doubleSens1Voie).append("\n");
        sb.append("  - Double sens multi-voies : ").append(doubleSensMulti).append("\n");

        return sb.toString();
    }
}