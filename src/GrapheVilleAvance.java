import java.util.*;

/**
 * Version avancée de GrapheVille avec gestion des contraintes horaires et orientations
 * CORRECTION v2 : Gestion correcte des routes bidirectionnelles (vrais double-sens)
 */
class GrapheVilleAvance extends GrapheVille {

    private ContraintesHoraires contraintes;
    private OrientationRue orientations;
    private Map<String, List<String>> ruesParNom;

    public GrapheVilleAvance(OrientationRue.HypothèseOrientation hypothèse) {
        super();
        this.contraintes = new ContraintesHoraires();
        this.orientations = new OrientationRue(hypothèse);
        this.ruesParNom = new HashMap<>();
    }

    /**
     * CORRECTION v2 : Ajouter un tronçon avec gestion RÉELLE de l'orientation
     * Cette méthode est appelée UNE FOIS par segment dans le fichier
     */
    public void ajouterTronconOriente(String rue, String nomDepart, String nomArrivee, double duree) {
        // Enregistrer ce segment de rue
        ruesParNom.computeIfAbsent(rue, k -> new ArrayList<>()).add(nomDepart + "->" + nomArrivee);

        // Récupérer la configuration de la rue
        OrientationRue.ConfigurationRue config = orientations.getConfiguration(rue);

        if (config == null) {
            // Pas de configuration spécifique : comportement par défaut selon l'hypothèse
            if (orientations.getHypothese() == OrientationRue.HypothèseOrientation.HO1) {
                // HO1 : Toutes les rues à double sens
                ajouterArcBidirectionnel(rue, nomDepart, nomArrivee, duree);
            } else {
                // HO2/HO3 : Par défaut, double sens multi-voies
                ajouterArcBidirectionnel(rue, nomDepart, nomArrivee, duree);
            }
            return;
        }

        // Configuration spécifique trouvée
        if (config.sensUnique) {
            // SENS UNIQUE : créer arc UNIQUEMENT dans le sens configuré
            if (config.sensAutoriseDepart.equals(nomDepart) && config.sensAutoriseArrivee.equals(nomArrivee)) {
                // Ce segment va dans le bon sens
                super.ajouterTroncon(rue, nomDepart, nomArrivee, duree);
            } else if (config.sensAutoriseDepart.equals(nomArrivee) && config.sensAutoriseArrivee.equals(nomDepart)) {
                // Ce segment va dans le sens inverse du fichier, mais c'est le bon sens pour le sens unique
                super.ajouterTroncon(rue, nomArrivee, nomDepart, duree);
            }
            // Sinon, ce segment n'est pas dans le sens autorisé, on ne crée rien
        } else {
            // DOUBLE SENS : créer arcs dans les deux sens
            ajouterArcBidirectionnel(rue, nomDepart, nomArrivee, duree);
        }
    }

    /**
     * Ajoute un arc bidirectionnel (aller ET retour)
     */
    private void ajouterArcBidirectionnel(String rue, String nomDepart, String nomArrivee, double duree) {
        super.ajouterTroncon(rue, nomDepart, nomArrivee, duree);
        super.ajouterTroncon(rue, nomArrivee, nomDepart, duree);
    }

    /**
     * Calculer la durée d'un arc en tenant compte des contraintes horaires
     */
    public double calculerDureeAvecContraintes(Arc arc) {
        String rue = arc.getRue();
        double dureeBase = arc.getDuree();
        return contraintes.calculerDureeAjustee(rue, dureeBase);
    }

    /**
     * Vérifier si un arc est praticable à l'heure actuelle
     */
    public boolean estArcPraticable(Arc arc) {
        String rue = arc.getRue();
        String depart = arc.getDepart().getNom();
        String arrivee = arc.getArrivee().getNom();

        // Vérifier orientation
        if (!orientations.estPassageAutorise(rue, depart, arrivee)) {
            return false;
        }

        // Vérifier contraintes horaires
        return contraintes.estRueAccessible(rue);
    }

    /**
     * Obtenir les arcs sortants praticables depuis un noeud
     */
    public List<Arc> getArcsSortantsPraticables(Noeud noeud) {
        List<Arc> praticables = new ArrayList<>();

        for (Arc arc : noeud.getArcsSortants()) {
            if (estArcPraticable(arc)) {
                praticables.add(arc);
            }
        }

        return praticables;
    }

    public void setHeureDepart(int heure) {
        contraintes.setHeureDepart(heure);
    }

    public void avancerTemps(double minutes) {
        contraintes.avancerTemps(minutes);
    }

    public int getHeureActuelle() {
        return contraintes.getHeureActuelle();
    }

    public void ajouterContrainteHoraire(String rue, int heureOuverture, int heureFermeture) {
        contraintes.ajouterContrainteRue(rue, heureOuverture, heureFermeture);
    }

    public void genererEvenementsAleatoires(int nbEvenements) {
        List<String> rues = new ArrayList<>(ruesParNom.keySet());
        if (!rues.isEmpty()) {
            contraintes.genererEvenementsAleatoires(rues, nbEvenements);
        }
    }

    /**
     * Configurer l'orientation d'une rue AVANT d'ajouter ses segments
     */
    public void configurerOrientationRue(String rue, OrientationRue.TypeOrientation type) {
        orientations.configurerRue(rue, type);
    }

    /**
     * Configurer une rue à sens unique AVANT d'ajouter ses segments
     */
    public void configurerSensUnique(String rue, String depart, String arrivee) {
        orientations.configurerSensUnique(rue, depart, arrivee);
    }

    /**
     * Vérifier si le ramassage se fait des deux côtés
     */
    public boolean ramassageDeuxCotes(String rue) {
        return orientations.ramassageDeuxCotes(rue);
    }

    /**
     * Afficher l'état complet du système
     */
    public void afficherEtatComplet() {
        contraintes.afficherEtat();
        orientations.afficherConfigurations();

        List<String> rues = new ArrayList<>(ruesParNom.keySet());
        if (!rues.isEmpty()) {
            System.out.println(contraintes.getRapportContraintes(rues));
        }

        System.out.println(orientations.getStatistiques());
    }

    public ContraintesHoraires getContraintes() {
        return contraintes;
    }

    public OrientationRue getOrientations() {
        return orientations;
    }

    public int compterArcsARamasser() {
        return orientations.compterArcsARamasser(this);
    }

    /**
     * Créer un graphe de test RÉALISTE avec les 3 types de routes
     * IMPORTANT : Configurer AVANT d'ajouter les segments
     */
    public static GrapheVilleAvance creerGrapheTestRealiste(OrientationRue.HypothèseOrientation hypothèse) {
        GrapheVilleAvance ville = new GrapheVilleAvance(hypothèse);

        // Définir les coordonnées
        ville.definirCoordonnees("Entrepot Base", 0, 500);
        ville.definirCoordonnees("Carrefour République", 200, 500);
        ville.definirCoordonnees("Maison Belle Vue", 300, 500);
        ville.definirCoordonnees("Carrefour Liberté", 500, 500);
        ville.definirCoordonnees("Carrefour Nation", 800, 500);
        ville.definirCoordonnees("Maison Tour Eiffel", 200, 350);
        ville.definirCoordonnees("Carrefour Opéra", 200, 200);
        ville.definirCoordonnees("Immeuble Grand Palais", 400, 200);
        ville.definirCoordonnees("Maison Panthéon", 500, 650);
        ville.definirCoordonnees("Carrefour Luxembourg", 500, 800);

        System.out.println("\n🏗️  Construction du graphe avec 3 types de routes :");
        System.out.println("=".repeat(60));

        // ========== TYPE 1 : ROUTES CLASSIQUES DOUBLE SENS (2+ voies) ==========
        System.out.println("\n🛣️  Routes classiques à double sens (multi-voies) :");

        // IMPORTANT : Configurer AVANT d'ajouter les segments
        ville.configurerOrientationRue("Avenue Principale", OrientationRue.TypeOrientation.DOUBLE_SENS_MULTI_VOIES);

        // Maintenant ajouter les segments (sera bidirectionnel automatiquement)
        ville.ajouterTronconOriente("Avenue Principale", "Entrepot Base", "Carrefour République", 2.5);
        ville.ajouterTronconOriente("Avenue Principale", "Carrefour République", "Maison Belle Vue", 1.8);
        ville.ajouterTronconOriente("Avenue Principale", "Maison Belle Vue", "Carrefour Liberté", 2.2);
        ville.ajouterTronconOriente("Avenue Principale", "Carrefour Liberté", "Carrefour Nation", 3.0);
        System.out.println("   ✓ Avenue Principale (4 segments, bidirectionnels)");

        // Boulevard Nord
        ville.configurerOrientationRue("Boulevard Nord", OrientationRue.TypeOrientation.DOUBLE_SENS_MULTI_VOIES);
        ville.ajouterTronconOriente("Boulevard Nord", "Carrefour Opéra", "Immeuble Grand Palais", 2.0);
        ville.ajouterTronconOriente("Boulevard Nord", "Immeuble Grand Palais", "Carrefour Liberté", 2.5);
        System.out.println("   ✓ Boulevard Nord (2 segments, bidirectionnels)");

        // ========== TYPE 2 : SENS UNIQUE ==========
        System.out.println("\n➡️  Routes à sens unique :");

        // Rue Montmartre : sens unique descendant (République → Tour Eiffel → Opéra)
        ville.configurerSensUnique("Rue Montmartre", "Carrefour République", "Carrefour Opéra");
        ville.ajouterTronconOriente("Rue Montmartre", "Carrefour République", "Maison Tour Eiffel", 1.5);
        ville.ajouterTronconOriente("Rue Montmartre", "Maison Tour Eiffel", "Carrefour Opéra", 1.8);
        System.out.println("   ✓ Rue Montmartre (sens unique : République → Opéra)");

        // Rue Saint Michel : sens unique montant (Liberté → Panthéon → Luxembourg)
        ville.configurerSensUnique("Rue Saint Michel", "Carrefour Liberté", "Carrefour Luxembourg");
        ville.ajouterTronconOriente("Rue Saint Michel", "Carrefour Liberté", "Maison Panthéon", 1.6);
        ville.ajouterTronconOriente("Rue Saint Michel", "Maison Panthéon", "Carrefour Luxembourg", 1.9);
        System.out.println("   ✓ Rue Saint Michel (sens unique : Liberté → Luxembourg)");

        // ========== TYPE 3 : PETITES ROUTES 1 VOIE DOUBLE SENS ==========
        System.out.println("\n🛤️  Petites routes de campagne (1 voie, double sens) :");

        // Rue Lafayette
        ville.configurerOrientationRue("Rue Lafayette", OrientationRue.TypeOrientation.DOUBLE_SENS_UNE_VOIE);
        ville.ajouterTronconOriente("Rue Lafayette", "Carrefour Liberté", "Carrefour Opéra", 2.8);
        System.out.println("   ✓ Rue Lafayette (bidirectionnel, ramassage 2 côtés)");

        // Rue Transversale
        ville.configurerOrientationRue("Rue Transversale", OrientationRue.TypeOrientation.DOUBLE_SENS_UNE_VOIE);
        ville.ajouterTronconOriente("Rue Transversale", "Carrefour République", "Carrefour Opéra", 2.0);
        System.out.println("   ✓ Rue Transversale (bidirectionnel, ramassage 2 côtés)");

        System.out.println("\n" + "=".repeat(60));

        // Ajouter quelques contraintes horaires
        ville.ajouterContrainteHoraire("Rue Montmartre", 7, 9);
        ville.ajouterContrainteHoraire("Boulevard Nord", 8, 10);

        // Générer des événements aléatoires
        ville.genererEvenementsAleatoires(2);

        // Départ à 8h
        ville.setHeureDepart(8);

        return ville;
    }

    /**
     * Charger depuis le fichier avec gestion des orientations
     */
    public void chargerDepuisFichier(String fichier) throws Exception {
        try (Scanner sc = new Scanner(new java.io.File(fichier))) {
            while (sc.hasNextLine()) {
                String ligne = sc.nextLine().trim();
                if (ligne.isEmpty() || ligne.startsWith("#")) continue;

                String[] parts = ligne.split(";");
                if (parts.length >= 3) {
                    String rue = parts[0].trim();
                    String depart = parts[1].trim();
                    String arrivee = parts[2].trim();
                    double duree = 2.0;

                    if (parts.length == 7) {
                        double xDepart = Double.parseDouble(parts[3].trim());
                        double yDepart = Double.parseDouble(parts[4].trim());
                        double xArrivee = Double.parseDouble(parts[5].trim());
                        double yArrivee = Double.parseDouble(parts[6].trim());

                        definirCoordonnees(depart, xDepart, yDepart);
                        definirCoordonnees(arrivee, xArrivee, yArrivee);

                        double dx = xArrivee - xDepart;
                        double dy = yArrivee - yDepart;
                        double distance = Math.sqrt(dx * dx + dy * dy);
                        duree = distance / 100.0;
                    }

                    ajouterTronconOriente(rue, depart, arrivee, duree);
                }
            }
        }

        // Configurer automatiquement selon l'hypothèse
        configurerOrientationsAutomatiques();
    }

    /**
     * Configure automatiquement les orientations selon l'hypothèse et les noms de rues
     */
    private void configurerOrientationsAutomatiques() {
        for (String rue : ruesParNom.keySet()) {
            String rueLower = rue.toLowerCase();

            // Heuristiques basées sur le nom
            if (rueLower.contains("avenue") || rueLower.contains("boulevard")) {
                // Grandes voies : double sens multi-voies
                configurerOrientationRue(rue, OrientationRue.TypeOrientation.DOUBLE_SENS_MULTI_VOIES);
            } else if (rueLower.contains("allée") || rueLower.contains("chemin") || rueLower.contains("sentier")) {
                // Petites voies : double sens 1 voie
                configurerOrientationRue(rue, OrientationRue.TypeOrientation.DOUBLE_SENS_UNE_VOIE);
            } else if (rueLower.contains("montmartre") || rueLower.contains("lafayette")) {
                // Certaines rues spécifiques en sens unique (à adapter)
                configurerOrientationRue(rue, OrientationRue.TypeOrientation.DOUBLE_SENS_MULTI_VOIES);
            }
        }
    }
}