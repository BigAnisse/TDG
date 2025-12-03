import java.util.*;

/**
 * Thème 3 : Planifier les jours de passage dans les différents secteurs
 * Résout le problème de coloration de graphe avec contraintes de capacité
 */
class PlanificationSecteurs {

    static class Secteur {
        String nom;
        double quantiteDechets;
        Set<String> voisins; // Secteurs géographiquement contigus

        public Secteur(String nom, double quantiteDechets) {
            this.nom = nom;
            this.quantiteDechets = quantiteDechets;
            this.voisins = new HashSet<>();
        }

        public void ajouterVoisin(String nomVoisin) {
            voisins.add(nomVoisin);
        }

        @Override
        public String toString() {
            return nom + " (quantité: " + quantiteDechets + ")";
        }
    }

    static class Planning {
        Map<Integer, List<Secteur>> creneaux; // jour -> liste de secteurs
        int nbJoursUtilises;

        public Planning() {
            this.creneaux = new TreeMap<>();
            this.nbJoursUtilises = 0;
        }

        public void ajouterSecteur(int jour, Secteur secteur) {
            creneaux.computeIfAbsent(jour, k -> new ArrayList<>()).add(secteur);
            nbJoursUtilises = Math.max(nbJoursUtilises, jour + 1);
        }

        public double getChargeJour(int jour) {
            if (!creneaux.containsKey(jour)) return 0.0;
            return creneaux.get(jour).stream()
                    .mapToDouble(s -> s.quantiteDechets)
                    .sum();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n=== PLANNING DE COLLECTE ===\n");
            sb.append("Nombre de jours nécessaires : ").append(nbJoursUtilises).append("\n\n");

            for (int jour = 0; jour < nbJoursUtilises; jour++) {
                sb.append("JOUR ").append(jour + 1).append(" :\n");
                if (creneaux.containsKey(jour)) {
                    for (Secteur s : creneaux.get(jour)) {
                        sb.append("  - ").append(s).append("\n");
                    }
                    sb.append("  Charge totale : ").append(String.format("%.1f", getChargeJour(jour))).append(" unités\n");
                } else {
                    sb.append("  (Aucune collecte)\n");
                }
                sb.append("\n");
            }

            return sb.toString();
        }
    }

    /**
     * HYPOTHÈSE 1 : Deux secteurs voisins ne doivent pas être collectés le même jour
     * Aucune contrainte sur les capacités
     *
     * C'est un problème de coloration de graphe : trouver le nombre chromatique minimal
     */
    public static Planning planifierSansCapacite(Map<String, Secteur> secteurs) {
        System.out.println("\n=== Planification sans contrainte de capacité ===");
        System.out.println("Contrainte : Deux secteurs voisins ne peuvent pas être collectés le même jour\n");

        Planning planning = new Planning();
        Set<String> secteursNonPlanifies = new HashSet<>(secteurs.keySet());
        int jourActuel = 0;

        // Algorithme glouton de coloration
        while (!secteursNonPlanifies.isEmpty()) {
            Set<String> planifiesAujourdhui = new HashSet<>();
            List<String> candidats = new ArrayList<>(secteursNonPlanifies);

            // Trier par nombre de voisins décroissant (heuristique)
            candidats.sort((a, b) -> Integer.compare(
                    secteurs.get(b).voisins.size(),
                    secteurs.get(a).voisins.size()
            ));

            for (String nomSecteur : candidats) {
                Secteur secteur = secteurs.get(nomSecteur);

                // Vérifier si ce secteur peut être planifié aujourd'hui
                boolean peutEtrePlanifie = true;
                for (String voisin : secteur.voisins) {
                    if (planifiesAujourdhui.contains(voisin)) {
                        peutEtrePlanifie = false;
                        break;
                    }
                }

                if (peutEtrePlanifie) {
                    planning.ajouterSecteur(jourActuel, secteur);
                    planifiesAujourdhui.add(nomSecteur);
                    secteursNonPlanifies.remove(nomSecteur);
                }
            }

            jourActuel++;

            // Sécurité : éviter boucle infinie
            if (jourActuel > secteurs.size()) {
                throw new RuntimeException("Impossible de planifier tous les secteurs");
            }
        }

        System.out.println("Planification terminée !");
        System.out.println("Nombre chromatique (nombre minimum de jours) : " + planning.nbJoursUtilises);

        return planning;
    }

    /**
     * HYPOTHÈSE 2 : Deux secteurs voisins ne doivent pas être collectés le même jour
     * + Contraintes de capacité :
     * - Max 1 tournée par jour par secteur
     * - Nombre de camions limité (N camions utilisables simultanément)
     * - Capacité maximale par camion (C)
     */
    public static Planning planifierAvecCapacite(Map<String, Secteur> secteurs,
                                                 double capaciteCamion,
                                                 int nbCamionsMax) {
        System.out.println("\n=== Planification avec contraintes de capacité ===");
        System.out.println("Contraintes :");
        System.out.println("  - Deux secteurs voisins ne peuvent pas être collectés le même jour");
        System.out.println("  - Max 1 tournée par jour par secteur");
        System.out.println("  - Capacité par camion : " + capaciteCamion + " unités");
        System.out.println("  - Nombre de camions disponibles : " + nbCamionsMax + "\n");

        Planning planning = new Planning();
        Set<String> secteursNonPlanifies = new HashSet<>(secteurs.keySet());
        int jourActuel = 0;

        while (!secteursNonPlanifies.isEmpty()) {
            Set<String> planifiesAujourdhui = new HashSet<>();
            double chargeTotaleJour = 0.0;
            int nbTourneesJour = 0;

            List<String> candidats = new ArrayList<>(secteursNonPlanifies);

            // Trier par quantité décroissante (on traite les gros secteurs en priorité)
            candidats.sort((a, b) -> Double.compare(
                    secteurs.get(b).quantiteDechets,
                    secteurs.get(a).quantiteDechets
            ));

            for (String nomSecteur : candidats) {
                Secteur secteur = secteurs.get(nomSecteur);

                // Vérifier si ce secteur peut être planifié aujourd'hui
                boolean peutEtrePlanifie = true;

                // 1. Vérifier contrainte de voisinage
                for (String voisin : secteur.voisins) {
                    if (planifiesAujourdhui.contains(voisin)) {
                        peutEtrePlanifie = false;
                        break;
                    }
                }

                if (!peutEtrePlanifie) continue;

                // 2. Vérifier contrainte de capacité
                // Chaque secteur nécessite un nombre de tournées
                int nbTourneesNecessaires = (int) Math.ceil(secteur.quantiteDechets / capaciteCamion);

                // 3. Vérifier si on a assez de camions disponibles
                if (nbTourneesJour + nbTourneesNecessaires <= nbCamionsMax) {
                    planning.ajouterSecteur(jourActuel, secteur);
                    planifiesAujourdhui.add(nomSecteur);
                    secteursNonPlanifies.remove(nomSecteur);
                    chargeTotaleJour += secteur.quantiteDechets;
                    nbTourneesJour += nbTourneesNecessaires;

                    System.out.println("Jour " + (jourActuel + 1) + " : Ajout de " + nomSecteur +
                            " (" + nbTourneesNecessaires + " tournée(s))");
                }
            }

            jourActuel++;

            // Sécurité
            if (jourActuel > secteurs.size() * 2) {
                throw new RuntimeException("Impossible de planifier tous les secteurs avec ces contraintes");
            }
        }

        System.out.println("\nPlanification terminée !");
        System.out.println("Nombre de jours nécessaires : " + planning.nbJoursUtilises);

        // Vérifier l'utilisation des ressources
        for (int jour = 0; jour < planning.nbJoursUtilises; jour++) {
            double charge = planning.getChargeJour(jour);
            int nbTournees = 0;
            if (planning.creneaux.containsKey(jour)) {
                for (Secteur s : planning.creneaux.get(jour)) {
                    nbTournees += (int) Math.ceil(s.quantiteDechets / capaciteCamion);
                }
            }
            System.out.println("Jour " + (jour + 1) + " : " + nbTournees + " tournées / " + nbCamionsMax);
        }

        return planning;
    }

    /**
     * Méthode pour calculer le nombre chromatique (borne inférieure théorique)
     */
    public static int calculerBorneInferieure(Map<String, Secteur> secteurs) {
        // Le nombre chromatique est au minimum le degré maximum + 1
        int degreMax = 0;
        for (Secteur s : secteurs.values()) {
            degreMax = Math.max(degreMax, s.voisins.size());
        }
        return degreMax + 1;
    }

    /**
     * Affiche les statistiques du graphe de secteurs
     */
    public static void afficherStatistiques(Map<String, Secteur> secteurs) {
        System.out.println("\n=== Statistiques des secteurs ===");
        System.out.println("Nombre de secteurs : " + secteurs.size());

        double quantiteTotale = secteurs.values().stream()
                .mapToDouble(s -> s.quantiteDechets)
                .sum();
        System.out.println("Quantité totale de déchets : " + String.format("%.1f", quantiteTotale) + " unités");

        int nbAretesTotal = 0;
        int degreMax = 0;
        for (Secteur s : secteurs.values()) {
            nbAretesTotal += s.voisins.size();
            degreMax = Math.max(degreMax, s.voisins.size());
        }
        nbAretesTotal /= 2; // Chaque arête compte 2 fois

        System.out.println("Nombre de relations de voisinage : " + nbAretesTotal);
        System.out.println("Degré maximum : " + degreMax);
        System.out.println("Borne inférieure théorique (jours min) : " + calculerBorneInferieure(secteurs));
    }

    /**
     * Méthode utilitaire pour créer un graphe de secteurs à partir d'une liste
     */
    public static Map<String, Secteur> creerGrapheSecteurs(List<String[]> donneesSecteurs,
                                                           List<String[]> relations) {
        Map<String, Secteur> secteurs = new HashMap<>();

        // Créer les secteurs
        for (String[] data : donneesSecteurs) {
            String nom = data[0];
            double quantite = Double.parseDouble(data[1]);
            secteurs.put(nom, new Secteur(nom, quantite));
        }

        // Ajouter les relations de voisinage
        for (String[] relation : relations) {
            String secteur1 = relation[0];
            String secteur2 = relation[1];

            if (secteurs.containsKey(secteur1) && secteurs.containsKey(secteur2)) {
                secteurs.get(secteur1).ajouterVoisin(secteur2);
                secteurs.get(secteur2).ajouterVoisin(secteur1);
            }
        }

        return secteurs;
    }

    /**
     * Valide qu'un planning respecte toutes les contraintes
     */
    public static boolean validerPlanning(Planning planning, Map<String, Secteur> secteurs,
                                          double capaciteCamion, int nbCamionsMax) {
        // Vérifier que chaque secteur est planifié exactement une fois
        Set<String> secteursVus = new HashSet<>();
        for (List<Secteur> liste : planning.creneaux.values()) {
            for (Secteur s : liste) {
                if (secteursVus.contains(s.nom)) {
                    System.err.println("Erreur : " + s.nom + " est planifié plusieurs fois");
                    return false;
                }
                secteursVus.add(s.nom);
            }
        }

        if (secteursVus.size() != secteurs.size()) {
            System.err.println("Erreur : Tous les secteurs ne sont pas planifiés");
            return false;
        }

        // Vérifier les contraintes de voisinage
        for (int jour = 0; jour < planning.nbJoursUtilises; jour++) {
            if (!planning.creneaux.containsKey(jour)) continue;

            List<Secteur> secteursJour = planning.creneaux.get(jour);
            for (int i = 0; i < secteursJour.size(); i++) {
                for (int j = i + 1; j < secteursJour.size(); j++) {
                    Secteur s1 = secteursJour.get(i);
                    Secteur s2 = secteursJour.get(j);

                    if (s1.voisins.contains(s2.nom)) {
                        System.err.println("Erreur : " + s1.nom + " et " + s2.nom +
                                " sont voisins mais planifiés le même jour");
                        return false;
                    }
                }
            }

            // Vérifier les contraintes de capacité
            int nbTournees = 0;
            for (Secteur s : secteursJour) {
                nbTournees += (int) Math.ceil(s.quantiteDechets / capaciteCamion);
            }

            if (nbTournees > nbCamionsMax) {
                System.err.println("Erreur : Jour " + (jour + 1) + " nécessite " + nbTournees +
                        " tournées mais seulement " + nbCamionsMax + " camions disponibles");
                return false;
            }
        }

        System.out.println("✓ Planning valide !");
        return true;
    }
}