import java.util.*;

/**
 * Gestion des contraintes horaires et événements aléatoires
 * - Heures de pointe (trafic dense)
 * - Événements aléatoires (travaux, accidents, etc.)
 * - Contraintes horaires par rue
 */
class ContraintesHoraires {

    // Plages horaires de la journée
    enum PlageHoraire {
        MATIN_CALME(6, 8, 1.0),           // 6h-8h : Trafic normal
        HEURE_POINTE_MATIN(8, 10, 1.8),   // 8h-10h : Forte densité
        MILIEU_JOURNEE(10, 16, 1.2),      // 10h-16h : Trafic modéré
        HEURE_POINTE_SOIR(16, 19, 2.0),   // 16h-19h : Très forte densité
        SOIREE_CALME(19, 22, 1.1);        // 19h-22h : Trafic léger

        final int heureDebut;
        final int heureFin;
        final double coefficientTrafic;

        PlageHoraire(int debut, int fin, double coeff) {
            this.heureDebut = debut;
            this.heureFin = fin;
            this.coefficientTrafic = coeff;
        }

        public static PlageHoraire getPlage(int heure) {
            for (PlageHoraire plage : values()) {
                if (heure >= plage.heureDebut && heure < plage.heureFin) {
                    return plage;
                }
            }
            return SOIREE_CALME;
        }

        @Override
        public String toString() {
            return String.format("%02dh-%02dh (×%.1f)", heureDebut, heureFin, coefficientTrafic);
        }
    }

    // Types d'événements aléatoires
    enum TypeEvenement {
        TRAVAUX("Travaux", 3.0, 60),              // +200%, durée 60min
        ACCIDENT("Accident", 2.5, 45),             // +150%, durée 45min
        MANIFESTATION("Manifestation", 4.0, 90),   // +300%, durée 90min
        LIVRAISON("Camion en livraison", 1.5, 15), // +50%, durée 15min
        MARCHE("Marché", 1.8, 180);                // +80%, durée 3h

        final String description;
        final double coefficientRetard;
        final int dureeMinutes;

        TypeEvenement(String desc, double coeff, int duree) {
            this.description = desc;
            this.coefficientRetard = coeff;
            this.dureeMinutes = duree;
        }
    }

    // Événement sur une rue
    static class Evenement {
        String rue;
        TypeEvenement type;
        int heureDebut;
        int heureFin;
        String description;

        public Evenement(String rue, TypeEvenement type, int heureDebut) {
            this.rue = rue;
            this.type = type;
            this.heureDebut = heureDebut;
            this.heureFin = heureDebut + (type.dureeMinutes / 60);
            this.description = type.description + " sur " + rue;
        }

        public boolean estActif(int heure) {
            return heure >= heureDebut && heure < heureFin;
        }

        @Override
        public String toString() {
            return String.format("⚠️  %s (%02dh-%02dh, retard ×%.1f)",
                    description, heureDebut, heureFin, type.coefficientRetard);
        }
    }

    // Contraintes horaires d'une rue
    static class ContrainteRue {
        String nomRue;
        boolean interditLaJournee;  // Rue fermée certaines heures
        int heureOuverture;          // Heure d'ouverture si fermée
        int heureFermeture;          // Heure de fermeture si fermée

        public ContrainteRue(String nomRue) {
            this.nomRue = nomRue;
            this.interditLaJournee = false;
        }

        public void definirPlageInterdite(int ouverture, int fermeture) {
            this.interditLaJournee = true;
            this.heureOuverture = ouverture;
            this.heureFermeture = fermeture;
        }

        public boolean estAccessible(int heure) {
            if (!interditLaJournee) return true;
            return heure < heureOuverture || heure >= heureFermeture;
        }

        @Override
        public String toString() {
            if (interditLaJournee) {
                return String.format("%s : Interdite %02dh-%02dh",
                        nomRue, heureOuverture, heureFermeture);
            }
            return nomRue + " : Toujours accessible";
        }
    }

    // Gestionnaire de contraintes
    private Map<String, ContrainteRue> contraintesRues;
    private List<Evenement> evenements;
    private int heureActuelle;
    private Random random;

    public ContraintesHoraires() {
        this.contraintesRues = new HashMap<>();
        this.evenements = new ArrayList<>();
        this.heureActuelle = 8; // Départ à 8h par défaut
        this.random = new Random();
    }

    /**
     * Définir l'heure de départ de la tournée
     */
    public void setHeureDepart(int heure) {
        if (heure < 6 || heure > 22) {
            throw new IllegalArgumentException("Heure doit être entre 6h et 22h");
        }
        this.heureActuelle = heure;
    }

    public int getHeureActuelle() {
        return heureActuelle;
    }

    /**
     * Avancer dans le temps
     */
    public void avancerTemps(double minutes) {
        int minutesInt = (int) Math.ceil(minutes);
        heureActuelle += minutesInt / 60;

        // Limiter à la journée
        if (heureActuelle > 22) {
            heureActuelle = 22;
        }
    }

    /**
     * Ajouter une contrainte horaire sur une rue
     */
    public void ajouterContrainteRue(String nomRue, int heureOuverture, int heureFermeture) {
        ContrainteRue contrainte = contraintesRues.computeIfAbsent(nomRue, ContrainteRue::new);
        contrainte.definirPlageInterdite(heureOuverture, heureFermeture);
    }

    /**
     * Générer des événements aléatoires
     */
    public void genererEvenementsAleatoires(List<String> rues, int nbEvenements) {
        System.out.println("\n🎲 Génération d'événements aléatoires...");

        for (int i = 0; i < nbEvenements; i++) {
            // Choisir une rue au hasard
            String rue = rues.get(random.nextInt(rues.size()));

            // Choisir un type d'événement
            TypeEvenement type = TypeEvenement.values()[random.nextInt(TypeEvenement.values().length)];

            // Choisir une heure aléatoire
            int heure = 6 + random.nextInt(16); // Entre 6h et 22h

            Evenement evt = new Evenement(rue, type, heure);
            evenements.add(evt);

            System.out.println("  " + evt);
        }
    }

    /**
     * Vérifier si une rue est accessible à l'heure actuelle
     */
    public boolean estRueAccessible(String nomRue) {
        ContrainteRue contrainte = contraintesRues.get(nomRue);
        if (contrainte == null) return true;
        return contrainte.estAccessible(heureActuelle);
    }

    /**
     * Calculer le coefficient de retard total pour une rue à l'heure actuelle
     */
    public double getCoefficientRetard(String nomRue) {
        double coeff = 1.0;

        // Coefficient dû à l'heure de la journée
        PlageHoraire plage = PlageHoraire.getPlage(heureActuelle);
        coeff *= plage.coefficientTrafic;

        // Coefficients dus aux événements
        for (Evenement evt : evenements) {
            if (evt.rue.equals(nomRue) && evt.estActif(heureActuelle)) {
                coeff *= evt.type.coefficientRetard;
            }
        }

        return coeff;
    }

    /**
     * Calculer la durée ajustée d'un arc en tenant compte des contraintes
     */
    public double calculerDureeAjustee(String nomRue, double dureeBase) {
        if (!estRueAccessible(nomRue)) {
            return Double.POSITIVE_INFINITY; // Rue inaccessible
        }

        double coeff = getCoefficientRetard(nomRue);
        return dureeBase * coeff;
    }

    /**
     * Afficher l'état actuel des contraintes
     */
    public void afficherEtat() {
        System.out.println("\n⏰ État actuel : " + String.format("%02dh00", heureActuelle));

        PlageHoraire plage = PlageHoraire.getPlage(heureActuelle);
        System.out.println("📊 Plage horaire : " + plage);

        // Événements actifs
        List<Evenement> evtsActifs = new ArrayList<>();
        for (Evenement evt : evenements) {
            if (evt.estActif(heureActuelle)) {
                evtsActifs.add(evt);
            }
        }

        if (!evtsActifs.isEmpty()) {
            System.out.println("\n⚠️  Événements en cours :");
            for (Evenement evt : evtsActifs) {
                System.out.println("  " + evt);
            }
        }

        // Rues fermées
        List<ContrainteRue> ruesFermees = new ArrayList<>();
        for (ContrainteRue c : contraintesRues.values()) {
            if (!c.estAccessible(heureActuelle)) {
                ruesFermees.add(c);
            }
        }

        if (!ruesFermees.isEmpty()) {
            System.out.println("\n🚫 Rues actuellement fermées :");
            for (ContrainteRue c : ruesFermees) {
                System.out.println("  " + c);
            }
        }
    }

    /**
     * Obtenir un rapport des contraintes pour une liste de rues
     */
    public String getRapportContraintes(List<String> rues) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n📋 RAPPORT DES CONTRAINTES\n");
        sb.append("=".repeat(50)).append("\n");

        for (String rue : rues) {
            double coeff = getCoefficientRetard(rue);
            boolean accessible = estRueAccessible(rue);

            sb.append(String.format("%-30s", rue));

            if (!accessible) {
                sb.append(" 🚫 FERMÉE");
            } else if (coeff > 1.5) {
                sb.append(String.format(" ⚠️  Retard ×%.1f", coeff));
            } else if (coeff > 1.0) {
                sb.append(String.format(" ⚡ Ralenti ×%.1f", coeff));
            } else {
                sb.append(" ✓ Fluide");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Créer un scénario de contraintes pour tests
     */
    public static ContraintesHoraires creerScenarioTest() {
        ContraintesHoraires contraintes = new ContraintesHoraires();

        // Rues avec restrictions horaires
        contraintes.ajouterContrainteRue("Rue Montmartre", 7, 9);  // Fermée 7h-9h (livraisons)
        contraintes.ajouterContrainteRue("Avenue Est", 12, 14);     // Fermée 12h-14h (marché)

        // Événements aléatoires
        List<String> ruesTest = Arrays.asList(
                "Avenue Principale", "Rue Montmartre", "Boulevard Nord",
                "Rue Transversale", "Avenue Est", "Rue Lafayette"
        );
        contraintes.genererEvenementsAleatoires(ruesTest, 3);

        return contraintes;
    }

    /**
     * Réinitialiser tous les événements
     */
    public void reinitialiserEvenements() {
        evenements.clear();
    }

    /**
     * Obtenir tous les événements
     */
    public List<Evenement> getEvenements() {
        return new ArrayList<>(evenements);
    }

    /**
     * Obtenir toutes les contraintes de rues
     */
    public Map<String, ContrainteRue> getContraintesRues() {
        return new HashMap<>(contraintesRues);
    }
}