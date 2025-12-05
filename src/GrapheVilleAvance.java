import java.util.*;

/**
 * Version avancée de GrapheVille avec gestion des contraintes horaires et orientations
 */
class GrapheVilleAvance extends GrapheVille {

    private ContraintesHoraires contraintes;
    private OrientationRue orientations;
    private Map<String, List<String>> ruesParNom; // Associe nom de rue -> liste de segments

    public GrapheVilleAvance(OrientationRue.HypothèseOrientation hypothèse) {
        super();
        this.contraintes = new ContraintesHoraires();
        this.orientations = new OrientationRue(hypothèse);
        this.ruesParNom = new HashMap<>();
    }

    /**
     * Ajouter un tronçon avec gestion de l'orientation
     */
    public void ajouterTronconOriente(String rue, String nomDepart, String nomArrivee, double duree) {
        // Enregistrer ce segment de rue
        ruesParNom.computeIfAbsent(rue, k -> new ArrayList<>()).add(nomDepart + "->" + nomArrivee);

        // Vérifier si le passage est autorisé dans ce sens
        boolean passageAutoriseDirecte = orientations.estPassageAutorise(rue, nomDepart, nomArrivee);
        boolean passageAutoriseInverse = orientations.estPassageAutorise(rue, nomArrivee, nomDepart);

        // Créer les arcs selon l'orientation
        if (passageAutoriseDirecte) {
            super.ajouterTroncon(rue, nomDepart, nomArrivee, duree);
        }

        // Si double sens, créer aussi l'arc inverse
        OrientationRue.ConfigurationRue config = orientations.getConfiguration(rue);
        boolean estSensUnique = (config != null && config.sensUnique);

        if (!estSensUnique && passageAutoriseInverse) {
            super.ajouterTroncon(rue, nomArrivee, nomDepart, duree);
        }
    }

    /**
     * Calculer la durée d'un arc en tenant compte des contraintes horaires
     */
    public double calculerDureeAvecContraintes(Arc arc) {
        String rue = arc.getRue();
        double dureeBase = arc.getDuree();

        // Appliquer les contraintes horaires
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

    /**
     * Définir l'heure de départ de la tournée
     */
    public void setHeureDepart(int heure) {
        contraintes.setHeureDepart(heure);
    }

    /**
     * Avancer dans le temps
     */
    public void avancerTemps(double minutes) {
        contraintes.avancerTemps(minutes);
    }

    /**
     * Obtenir l'heure actuelle
     */
    public int getHeureActuelle() {
        return contraintes.getHeureActuelle();
    }

    /**
     * Ajouter une contrainte horaire
     */
    public void ajouterContrainteHoraire(String rue, int heureOuverture, int heureFermeture) {
        contraintes.ajouterContrainteRue(rue, heureOuverture, heureFermeture);
    }

    /**
     * Générer des événements aléatoires
     */
    public void genererEvenementsAleatoires(int nbEvenements) {
        List<String> rues = new ArrayList<>(ruesParNom.keySet());
        if (!rues.isEmpty()) {
            contraintes.genererEvenementsAleatoires(rues, nbEvenements);
        }
    }

    /**
     * Configurer l'orientation d'une rue
     */
    public void configurerOrientationRue(String rue, OrientationRue.TypeOrientation type) {
        orientations.configurerRue(rue, type);
    }

    /**
     * Configurer une rue à sens unique
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
    public void afficherEtatComplet(GrapheVilleAvance ville) {
        contraintes.afficherEtat();
        orientations.afficherConfigurations();

        // Rapport des contraintes sur toutes les rues
        List<String> rues = new ArrayList<>(ruesParNom.keySet());
        if (!rues.isEmpty()) {
            System.out.println(contraintes.getRapportContraintes(rues));
        }

        System.out.println(orientations.getStatistiques());
    }

    /**
     * Obtenir le gestionnaire de contraintes
     */
    public ContraintesHoraires getContraintes() {
        return contraintes;
    }

    /**
     * Obtenir le gestionnaire d'orientations
     */
    public OrientationRue getOrientations() {
        return orientations;
    }

    /**
     * Compter les arcs à ramasser selon l'hypothèse d'orientation
     */
    public int compterArcsARamasser() {
        return orientations.compterArcsARamasser(this);
    }

    /**
     * Créer un graphe de test avec contraintes
     */
    public static GrapheVilleAvance creerGrapheTest(OrientationRue.HypothèseOrientation hypothèse) {
        GrapheVilleAvance ville = new GrapheVilleAvance(hypothèse);

        // Définir les coordonnées
        ville.definirCoordonnees("Entrepot Base", 0, 0);
        ville.definirCoordonnees("Maison La Defense", 200, 0);
        ville.definirCoordonnees("Maison Esplanade de La Defense", 400, 0);
        ville.definirCoordonnees("Maison Pont de Neuilly", 600, 0);
        ville.definirCoordonnees("Place Centrale", 800, 0);
        ville.definirCoordonnees("Immeuble Tour Montparnasse", 200, 200);
        ville.definirCoordonnees("Maison Charles de Gaulle Etoile", 400, 200);
        ville.definirCoordonnees("Immeuble Crystal Palace", 400, 400);

        // Ajouter les tronçons
        ville.ajouterTronconOriente("Avenue Principale", "Entrepot Base", "Maison La Defense", 2.6);
        ville.ajouterTronconOriente("Avenue Principale", "Maison La Defense", "Maison Esplanade de La Defense", 2.1);
        ville.ajouterTronconOriente("Avenue Principale", "Maison Esplanade de La Defense", "Maison Pont de Neuilly", 2.3);
        ville.ajouterTronconOriente("Avenue Principale", "Maison Pont de Neuilly", "Place Centrale", 2.4);

        ville.ajouterTronconOriente("Rue Montmartre", "Maison La Defense", "Immeuble Tour Montparnasse", 3.0);
        ville.ajouterTronconOriente("Boulevard Nord", "Maison Esplanade de La Defense", "Maison Charles de Gaulle Etoile", 3.0);
        ville.ajouterTronconOriente("Boulevard Nord", "Maison Charles de Gaulle Etoile", "Immeuble Crystal Palace", 1.6);
        ville.ajouterTronconOriente("Avenue Est", "Place Centrale", "Immeuble Crystal Palace", 3.9);

        // Configurer les orientations selon l'hypothèse
        OrientationRue orientations = OrientationRue.creerConfigurationTest(hypothèse);
        ville.orientations = orientations;

        // Ajouter quelques contraintes horaires
        ville.ajouterContrainteHoraire("Rue Montmartre", 7, 9);  // Fermée 7h-9h
        ville.ajouterContrainteHoraire("Avenue Est", 12, 14);     // Fermée 12h-14h (marché)

        // Générer des événements aléatoires
        ville.genererEvenementsAleatoires(2);

        // Départ à 8h
        ville.setHeureDepart(8);

        return ville;
    }

    /**
     * Charger depuis le fichier avec gestion des orientations
     */
    public void chargerDepuisFichier(String fichier, OrientationRue.HypothèseOrientation hypothèse) throws Exception {
        try (Scanner sc = new Scanner(new java.io.File(fichier))) {
            while (sc.hasNextLine()) {
                String ligne = sc.nextLine().trim();
                if (ligne.isEmpty() || ligne.startsWith("#")) continue;

                String[] parts = ligne.split(";");
                if (parts.length >= 3) {
                    String rue = parts[0].trim();
                    String depart = parts[1].trim();
                    String arrivee = parts[2].trim();
                    double duree = 2.0; // Durée par défaut

                    if (parts.length == 7) {
                        double xDepart = Double.parseDouble(parts[3].trim());
                        double yDepart = Double.parseDouble(parts[4].trim());
                        double xArrivee = Double.parseDouble(parts[5].trim());
                        double yArrivee = Double.parseDouble(parts[6].trim());

                        definirCoordonnees(depart, xDepart, yDepart);
                        definirCoordonnees(arrivee, xArrivee, yArrivee);

                        // Calculer durée basée sur distance euclidienne
                        double dx = xArrivee - xDepart;
                        double dy = yArrivee - yDepart;
                        double distance = Math.sqrt(dx * dx + dy * dy);
                        duree = distance / 100.0; // Normalisation
                    }

                    ajouterTronconOriente(rue, depart, arrivee, duree);
                }
            }
        }

        // Configurer les orientations selon l'hypothèse
        this.orientations = OrientationRue.creerConfigurationTest(hypothèse);
    }
}