import java.util.*;

/**
 * Thème 2 : Optimiser les ramassages des points de collecte
 * Résout le problème du voyageur de commerce avec deux approches :
 * - Approche 1 : Plus proche voisin
 * - Approche 2 : MST (Arbre couvrant de poids minimum)
 */
class VoyageurCommerce {

    /**
     * APPROCHE 1 : Plus proche voisin
     * Visite à chaque étape le point le plus proche non encore visité
     */
    public static Itineraire approcheProchevoisin(GrapheVille ville, List<String> pointsCollecte) {
        if (pointsCollecte.isEmpty()) {
            throw new RuntimeException("Aucun point de collecte spécifié");
        }

        Entrepot entrepot = ville.getEntrepot();
        if (entrepot == null) {
            throw new RuntimeException("Entrepôt non trouvé");
        }

        System.out.println("\n=== Approche 1 : Plus Proche Voisin ===");

        Set<String> nonVisites = new HashSet<>(pointsCollecte);
        List<Arc> cheminTotal = new ArrayList<>();
        Noeud positionActuelle = entrepot;
        double distanceTotale = 0.0;

        System.out.println("Départ de : " + entrepot.getNom());

        while (!nonVisites.isEmpty()) {
            String plusProche = null;
            double distanceMin = Double.MAX_VALUE;
            List<Arc> cheminVersProche = null;

            // Trouver le point le plus proche
            for (String point : nonVisites) {
                List<Noeud> versions = ville.getToutesVersions(point);
                if (versions.isEmpty()) continue;

                // Chercher le chemin le plus court vers ce point
                for (Noeud noeudPoint : versions) {
                    try {
                        List<Arc> chemin = cheminLePlusCourt(ville, positionActuelle, noeudPoint);
                        double distance = calculerDistance(chemin);

                        if (distance < distanceMin) {
                            distanceMin = distance;
                            plusProche = point;
                            cheminVersProche = chemin;
                        }
                    } catch (Exception e) {
                        // Point non accessible
                    }
                }
            }

            if (plusProche == null) {
                throw new RuntimeException("Impossible d'atteindre tous les points de collecte");
            }

            // Ajouter ce chemin
            cheminTotal.addAll(cheminVersProche);
            distanceTotale += distanceMin;

            System.out.println("→ " + plusProche + " (distance: " + String.format("%.1f", distanceMin) + " min)");

            // Mise à jour
            positionActuelle = cheminVersProche.get(cheminVersProche.size() - 1).getArrivee();
            nonVisites.remove(plusProche);
        }

        // Retour à l'entrepôt
        List<Arc> retour = cheminLePlusCourt(ville, positionActuelle, entrepot);
        cheminTotal.addAll(retour);
        double distanceRetour = calculerDistance(retour);
        distanceTotale += distanceRetour;

        System.out.println("→ Retour à " + entrepot.getNom() + " (distance: " + String.format("%.1f", distanceRetour) + " min)");
        System.out.println("\nDistance totale : " + String.format("%.1f", distanceTotale) + " min");

        // Construire l'itinéraire
        Itineraire itin = new Itineraire(entrepot, entrepot);
        for (Arc arc : cheminTotal) {
            itin.ajouterArc(arc);
        }
        itin.setMaisonsARamasser(new HashSet<>(pointsCollecte));

        return itin;
    }

    /**
     * APPROCHE 2 : MST (Arbre couvrant de poids minimum)
     */
    public static Itineraire approcheMST(GrapheVille ville, List<String> pointsCollecte) {
        if (pointsCollecte.isEmpty()) {
            throw new RuntimeException("Aucun point de collecte spécifié");
        }

        Entrepot entrepot = ville.getEntrepot();
        if (entrepot == null) {
            throw new RuntimeException("Entrepôt non trouvé");
        }

        System.out.println("\n=== Approche 2 : MST (Arbre Couvrant Minimum) ===");

        // Étape 1 : Créer le graphe complet des distances
        List<String> points = new ArrayList<>(pointsCollecte);
        points.add(0, entrepot.getNom()); // Ajouter l'entrepôt au début

        Map<String, Map<String, Double>> grapheComplet = new HashMap<>();
        Map<String, Map<String, List<Arc>>> chemins = new HashMap<>();

        System.out.println("Étape 1 : Calcul du graphe complet...");

        for (String p1 : points) {
            grapheComplet.put(p1, new HashMap<>());
            chemins.put(p1, new HashMap<>());

            for (String p2 : points) {
                if (!p1.equals(p2)) {
                    Noeud n1 = getNoeudPourNom(ville, p1);
                    Noeud n2 = getNoeudPourNom(ville, p2);

                    if (n1 != null && n2 != null) {
                        List<Arc> chemin = cheminLePlusCourt(ville, n1, n2);
                        double distance = calculerDistance(chemin);
                        grapheComplet.get(p1).put(p2, distance);
                        chemins.get(p1).put(p2, chemin);
                    }
                }
            }
        }

        // Étape 2 : Construire l'arbre couvrant minimum (Algorithme de Prim)
        System.out.println("Étape 2 : Construction du MST...");
        Map<String, String> mst = construireMST(grapheComplet, entrepot.getNom());

        // Étape 3 : Parcours préfixe de l'arbre
        System.out.println("Étape 3 : Parcours préfixe du MST...");
        List<String> ordreParcours = new ArrayList<>();
        Set<String> visites = new HashSet<>();
        parcoursPrefixe(entrepot.getNom(), mst, ordreParcours, visites, grapheComplet);

        // Étape 4 : Shortcutting - construire le chemin final
        System.out.println("Étape 4 : Application du shortcutting...");
        List<Arc> cheminFinal = new ArrayList<>();
        double distanceTotale = 0.0;

        for (int i = 0; i < ordreParcours.size() - 1; i++) {
            String depart = ordreParcours.get(i);
            String arrivee = ordreParcours.get(i + 1);

            List<Arc> segment = chemins.get(depart).get(arrivee);
            if (segment != null) {
                cheminFinal.addAll(segment);
                double dist = calculerDistance(segment);
                distanceTotale += dist;
                System.out.println(depart + " → " + arrivee + " (" + String.format("%.1f", dist) + " min)");
            }
        }

        System.out.println("\nDistance totale : " + String.format("%.1f", distanceTotale) + " min");

        // Construire l'itinéraire
        Itineraire itin = new Itineraire(entrepot, entrepot);
        for (Arc arc : cheminFinal) {
            itin.ajouterArc(arc);
        }
        itin.setMaisonsARamasser(new HashSet<>(pointsCollecte));

        return itin;
    }

    /**
     * APPROCHE 2 avec prise en compte des capacités
     * Découpe le parcours en plusieurs tournées respectant la capacité maximale
     */
    public static List<Itineraire> approcheMSTAvecCapacite(GrapheVille ville,
                                                           Map<String, Double> contenances,
                                                           double capaciteMax) {
        if (contenances.isEmpty()) {
            throw new RuntimeException("Aucun point de collecte spécifié");
        }

        Entrepot entrepot = ville.getEntrepot();
        if (entrepot == null) {
            throw new RuntimeException("Entrepôt non trouvé");
        }

        System.out.println("\n=== Approche 2 : MST avec Capacités ===");
        System.out.println("Capacité maximale du camion : " + capaciteMax + " unités");

        // Étape 1-4 : Obtenir le parcours MST complet
        List<String> pointsCollecte = new ArrayList<>(contenances.keySet());
        List<String> points = new ArrayList<>(pointsCollecte);
        points.add(0, entrepot.getNom());

        // Créer le graphe complet
        Map<String, Map<String, Double>> grapheComplet = new HashMap<>();
        Map<String, Map<String, List<Arc>>> chemins = new HashMap<>();

        for (String p1 : points) {
            grapheComplet.put(p1, new HashMap<>());
            chemins.put(p1, new HashMap<>());

            for (String p2 : points) {
                if (!p1.equals(p2)) {
                    Noeud n1 = getNoeudPourNom(ville, p1);
                    Noeud n2 = getNoeudPourNom(ville, p2);

                    if (n1 != null && n2 != null) {
                        List<Arc> chemin = cheminLePlusCourt(ville, n1, n2);
                        double distance = calculerDistance(chemin);
                        grapheComplet.get(p1).put(p2, distance);
                        chemins.get(p1).put(p2, chemin);
                    }
                }
            }
        }

        // Construire MST
        Map<String, String> mst = construireMST(grapheComplet, entrepot.getNom());

        // Parcours préfixe
        List<String> ordreParcours = new ArrayList<>();
        Set<String> visites = new HashSet<>();
        parcoursPrefixe(entrepot.getNom(), mst, ordreParcours, visites, grapheComplet);

        // Étape 5 : Découper en tournées selon les capacités
        System.out.println("\nDécoupage en tournées :");
        List<Itineraire> tournees = new ArrayList<>();
        List<Arc> tourneeActuelle = new ArrayList<>();
        Set<String> pointsTourneeActuelle = new HashSet<>();
        double chargeActuelle = 0.0;
        int numTournee = 1;

        for (int i = 0; i < ordreParcours.size() - 1; i++) {
            String depart = ordreParcours.get(i);
            String arrivee = ordreParcours.get(i + 1);

            // Vérifier si on peut ajouter ce point
            double contenance = contenances.getOrDefault(arrivee, 0.0);

            if (chargeActuelle + contenance > capaciteMax && !pointsTourneeActuelle.isEmpty()) {
                // Finir la tournée actuelle et revenir à l'entrepôt
                String dernierPoint = ordreParcours.get(i);
                List<Arc> retour = chemins.get(dernierPoint).get(entrepot.getNom());
                tourneeActuelle.addAll(retour);

                // Créer l'itinéraire de la tournée
                Itineraire itin = new Itineraire(entrepot, entrepot);
                for (Arc arc : tourneeActuelle) {
                    itin.ajouterArc(arc);
                }
                itin.setMaisonsARamasser(pointsTourneeActuelle);
                tournees.add(itin);

                System.out.println("Tournée " + numTournee + " : " + pointsTourneeActuelle +
                        " (charge: " + String.format("%.1f", chargeActuelle) + "/" + capaciteMax + ")");

                // Commencer une nouvelle tournée
                numTournee++;
                tourneeActuelle = new ArrayList<>();
                pointsTourneeActuelle = new HashSet<>();
                chargeActuelle = 0.0;

                // Aller de l'entrepôt au point actuel
                List<Arc> allerVersPoint = chemins.get(entrepot.getNom()).get(arrivee);
                tourneeActuelle.addAll(allerVersPoint);
            } else {
                // Ajouter le segment à la tournée actuelle
                List<Arc> segment = chemins.get(depart).get(arrivee);
                if (segment != null) {
                    tourneeActuelle.addAll(segment);
                }
            }

            // Ajouter le point à la tournée actuelle
            if (contenances.containsKey(arrivee)) {
                pointsTourneeActuelle.add(arrivee);
                chargeActuelle += contenance;
            }
        }

        // Finir la dernière tournée
        if (!pointsTourneeActuelle.isEmpty()) {
            String dernierPoint = ordreParcours.get(ordreParcours.size() - 2);
            List<Arc> retour = chemins.get(dernierPoint).get(entrepot.getNom());
            tourneeActuelle.addAll(retour);

            Itineraire itin = new Itineraire(entrepot, entrepot);
            for (Arc arc : tourneeActuelle) {
                itin.ajouterArc(arc);
            }
            itin.setMaisonsARamasser(pointsTourneeActuelle);
            tournees.add(itin);

            System.out.println("Tournée " + numTournee + " : " + pointsTourneeActuelle +
                    " (charge: " + String.format("%.1f", chargeActuelle) + "/" + capaciteMax + ")");
        }

        System.out.println("\nNombre total de tournées : " + tournees.size());

        return tournees;
    }

    // ============ MÉTHODES UTILITAIRES ============

    private static Noeud getNoeudPourNom(GrapheVille ville, String nom) {
        List<Noeud> versions = ville.getToutesVersions(nom);
        return versions.isEmpty() ? null : versions.get(0);
    }

    private static List<Arc> cheminLePlusCourt(GrapheVille ville, Noeud depart, Noeud arrivee) {
        if (depart.equals(arrivee)) {
            return new ArrayList<>();
        }

        Map<String, Double> distances = new HashMap<>();
        Map<String, Arc> predecesseurs = new HashMap<>();
        Set<String> traites = new HashSet<>();
        PriorityQueue<InfoDijkstra> file = new PriorityQueue<>();

        distances.put(depart.getNom(), 0.0);
        file.add(new InfoDijkstra(depart, 0.0));

        while (!file.isEmpty()) {
            InfoDijkstra info = file.poll();
            Noeud courant = info.noeud;

            if (traites.contains(courant.getNom())) continue;
            traites.add(courant.getNom());

            if (courant.getNom().equals(arrivee.getNom())) {
                break;
            }

            for (Arc arc : courant.getArcsSortants()) {
                Noeud voisin = arc.getArrivee();
                if (traites.contains(voisin.getNom())) continue;

                double dureeSansRamassage = arc.getDuree() - voisin.getTempsTraitement();
                double nouvelleDist = distances.get(courant.getNom()) + dureeSansRamassage;

                if (!distances.containsKey(voisin.getNom()) || nouvelleDist < distances.get(voisin.getNom())) {
                    distances.put(voisin.getNom(), nouvelleDist);
                    predecesseurs.put(voisin.getNom(), arc);
                    file.add(new InfoDijkstra(voisin, nouvelleDist));
                }
            }
        }

        // Reconstruire le chemin
        List<Arc> chemin = new ArrayList<>();
        Noeud courant = arrivee;

        while (!courant.getNom().equals(depart.getNom())) {
            Arc arc = predecesseurs.get(courant.getNom());
            if (arc == null) break;
            chemin.add(arc);
            courant = arc.getDepart();
        }

        Collections.reverse(chemin);
        return chemin;
    }

    private static double calculerDistance(List<Arc> chemin) {
        double total = 0.0;
        for (Arc arc : chemin) {
            total += arc.getDuree() - arc.getArrivee().getTempsTraitement();
        }
        return total;
    }

    /**
     * Construit un arbre couvrant de poids minimum avec l'algorithme de Prim
     * @return Map parent[noeud] = parent dans le MST
     */
    private static Map<String, String> construireMST(Map<String, Map<String, Double>> graphe, String racine) {
        Map<String, String> parent = new HashMap<>();
        Map<String, Double> cles = new HashMap<>();
        Set<String> dansMST = new HashSet<>();
        PriorityQueue<PairDistanceNoeud> file = new PriorityQueue<>();

        // Initialisation
        for (String noeud : graphe.keySet()) {
            cles.put(noeud, Double.MAX_VALUE);
        }

        cles.put(racine, 0.0);
        file.add(new PairDistanceNoeud(racine, 0.0));
        parent.put(racine, null);

        while (!file.isEmpty()) {
            String u = file.poll().noeud;

            if (dansMST.contains(u)) continue;
            dansMST.add(u);

            // Pour chaque voisin de u
            if (graphe.containsKey(u)) {
                for (Map.Entry<String, Double> entry : graphe.get(u).entrySet()) {
                    String v = entry.getKey();
                    double poids = entry.getValue();

                    if (!dansMST.contains(v) && poids < cles.get(v)) {
                        cles.put(v, poids);
                        parent.put(v, u);
                        file.add(new PairDistanceNoeud(v, poids));
                    }
                }
            }
        }

        return parent;
    }

    /**
     * Parcours préfixe de l'arbre MST
     */
    private static void parcoursPrefixe(String noeud, Map<String, String> mst,
                                        List<String> ordre, Set<String> visites,
                                        Map<String, Map<String, Double>> graphe) {
        if (visites.contains(noeud)) return;

        ordre.add(noeud);
        visites.add(noeud);

        // Trouver les enfants de ce noeud dans le MST
        for (Map.Entry<String, String> entry : mst.entrySet()) {
            if (entry.getValue() != null && entry.getValue().equals(noeud)) {
                parcoursPrefixe(entry.getKey(), mst, ordre, visites, graphe);
            }
        }

        // Shortcutting : retour direct à l'entrepôt après avoir visité tous les enfants
        if (!ordre.isEmpty() && !noeud.equals(ordre.get(0))) {
            ordre.add(ordre.get(0)); // Retour à la racine (entrepôt)
        }
    }

    // Classes internes
    private static class InfoDijkstra implements Comparable<InfoDijkstra> {
        Noeud noeud;
        double distance;

        InfoDijkstra(Noeud noeud, double distance) {
            this.noeud = noeud;
            this.distance = distance;
        }

        @Override
        public int compareTo(InfoDijkstra autre) {
            return Double.compare(this.distance, autre.distance);
        }
    }

    private static class PairDistanceNoeud implements Comparable<PairDistanceNoeud> {
        String noeud;
        double distance;

        PairDistanceNoeud(String noeud, double distance) {
            this.noeud = noeud;
            this.distance = distance;
        }

        @Override
        public int compareTo(PairDistanceNoeud autre) {
            return Double.compare(this.distance, autre.distance);
        }
    }
}