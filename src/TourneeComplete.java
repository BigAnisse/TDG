import java.util.*;

class TourneeComplete {

    // Classe pour stocker les informations d'un arc à parcourir
    static class ArcAParcourir {
        Arc arc;
        boolean ramassage; // true si on ramasse en parcourant cet arc

        public ArcAParcourir(Arc arc, boolean ramassage) {
            this.arc = arc;
            this.ramassage = ramassage;
        }
    }

    /**
     * Génère une tournée qui ramasse toutes les rues de la ville
     * Le camion ramasse uniquement du côté droit (sens de circulation)
     * CORRECTION : Prend en compte les contraintes horaires
     */
    public static Itineraire genererTourneeComplete(GrapheVille ville) {
        Entrepot entrepot = ville.getEntrepot();
        if (entrepot == null) {
            throw new RuntimeException("Entrepôt non trouvé");
        }

        // AJOUT : Vérifier si c'est un GrapheVilleAvance
        boolean avecContraintes = ville instanceof GrapheVilleAvance;
        GrapheVilleAvance villeAvance = avecContraintes ? (GrapheVilleAvance) ville : null;

        if (avecContraintes) {
            System.out.println("⏰ Prise en compte des contraintes horaires");
            villeAvance.getContraintes().afficherEtat();
        }

        // Collecter tous les arcs à ramasser (en excluant les changements de rue)
        Set<String> arcsARamasser = new HashSet<>();
        Map<String, Arc> arcsParCle = new HashMap<>();

        for (Noeud noeud : ville.getNoeuds()) {
            for (Arc arc : noeud.getArcsSortants()) {
                if (!arc.estChangementRue()) {
                    // AJOUT : Vérifier si l'arc est praticable
                    if (avecContraintes && !villeAvance.estArcPraticable(arc)) {
                        continue; // Ignorer les arcs non praticables
                    }

                    String cle = arc.getCleDirectionnelle();
                    arcsARamasser.add(cle);
                    arcsParCle.put(cle, arc);
                    arc.reinitialiserRamassage();
                }
            }
        }

        System.out.println("Nombre d'arcs à ramasser: " + arcsARamasser.size());

        // Construire la tournée avec l'algorithme du postier chinois simplifié
        List<ArcAParcourir> tournee = new ArrayList<>();
        Set<String> arcsRamasses = new HashSet<>();
        Noeud positionActuelle = entrepot;
        double tempsTotal = 0.0;

        while (arcsRamasses.size() < arcsARamasser.size()) {
            // Chercher un arc non ramassé depuis la position actuelle
            Arc arcNonRamasse = trouverArcNonRamasse(positionActuelle, arcsARamasser, arcsRamasses, villeAvance);

            if (arcNonRamasse != null) {
                // On peut ramasser directement depuis notre position
                double dureeArc = avecContraintes ?
                        villeAvance.calculerDureeAvecContraintes(arcNonRamasse) :
                        arcNonRamasse.getDuree();

                tournee.add(new ArcAParcourir(arcNonRamasse, true));
                arcsRamasses.add(arcNonRamasse.getCleDirectionnelle());
                positionActuelle = arcNonRamasse.getArrivee();
                tempsTotal += dureeArc;

                // AJOUT : Avancer le temps
                if (avecContraintes) {
                    villeAvance.avancerTemps(dureeArc);
                }
            } else {
                // Il faut aller vers un arc non ramassé (sans ramasser en chemin)
                Arc procheArcNonRamasse = trouverProcheArcNonRamasse(ville, positionActuelle, arcsARamasser, arcsRamasses, villeAvance);

                if (procheArcNonRamasse == null) {
                    break; // Tous les arcs ont été ramassés
                }

                // Trouver le chemin le plus court vers cet arc
                List<Arc> cheminVers = cheminLePlusCourt(ville, positionActuelle, procheArcNonRamasse.getDepart(), villeAvance);

                // Ajouter le chemin (sans ramassage)
                for (Arc arc : cheminVers) {
                    double dureeArc = avecContraintes ?
                            villeAvance.calculerDureeAvecContraintes(arc) :
                            arc.getDuree();

                    tournee.add(new ArcAParcourir(arc, false));
                    tempsTotal += dureeArc;

                    // AJOUT : Avancer le temps
                    if (avecContraintes) {
                        villeAvance.avancerTemps(dureeArc);
                    }
                }

                // Se positionner au début de l'arc non ramassé
                if (!cheminVers.isEmpty()) {
                    positionActuelle = cheminVers.get(cheminVers.size() - 1).getArrivee();
                } else {
                    positionActuelle = procheArcNonRamasse.getDepart();
                }

                // Ramasser cet arc
                double dureeRamassage = avecContraintes ?
                        villeAvance.calculerDureeAvecContraintes(procheArcNonRamasse) :
                        procheArcNonRamasse.getDuree();

                tournee.add(new ArcAParcourir(procheArcNonRamasse, true));
                arcsRamasses.add(procheArcNonRamasse.getCleDirectionnelle());
                positionActuelle = procheArcNonRamasse.getArrivee();
                tempsTotal += dureeRamassage;

                // AJOUT : Avancer le temps
                if (avecContraintes) {
                    villeAvance.avancerTemps(dureeRamassage);
                }
            }
        }

        // Retourner à l'entrepôt
        List<Arc> cheminRetour = cheminLePlusCourt(ville, positionActuelle, entrepot, villeAvance);
        for (Arc arc : cheminRetour) {
            double dureeArc = avecContraintes ?
                    villeAvance.calculerDureeAvecContraintes(arc) :
                    arc.getDuree();

            tournee.add(new ArcAParcourir(arc, false));
            tempsTotal += dureeArc;

            // AJOUT : Avancer le temps
            if (avecContraintes) {
                villeAvance.avancerTemps(dureeArc);
            }
        }

        if (avecContraintes) {
            System.out.println("\n⏰ Heure d'arrivée : " + villeAvance.getHeureActuelle() + "h00");
            System.out.println("⏱️  Temps total ajusté : " + String.format("%.1f", tempsTotal) + " minutes");
        }

        // Construire l'itinéraire final
        return construireItineraire(entrepot, tournee, ville);
    }

    // Trouve un arc non ramassé depuis le noeud actuel
    private static Arc trouverArcNonRamasse(Noeud noeud, Set<String> tousArcs, Set<String> arcsRamasses, GrapheVilleAvance villeAvance) {
        for (Arc arc : noeud.getArcsSortants()) {
            if (!arc.estChangementRue()) {
                // AJOUT : Vérifier si l'arc est praticable
                if (villeAvance != null && !villeAvance.estArcPraticable(arc)) {
                    continue;
                }

                String cle = arc.getCleDirectionnelle();
                if (tousArcs.contains(cle) && !arcsRamasses.contains(cle)) {
                    return arc;
                }
            }
        }
        return null;
    }

    // Trouve l'arc non ramassé le plus proche
    private static Arc trouverProcheArcNonRamasse(GrapheVille ville, Noeud position,
                                                  Set<String> tousArcs, Set<String> arcsRamasses,
                                                  GrapheVilleAvance villeAvance) {
        Arc plusProche = null;
        double distanceMin = Double.MAX_VALUE;

        for (Noeud noeud : ville.getNoeuds()) {
            for (Arc arc : noeud.getArcsSortants()) {
                if (!arc.estChangementRue()) {
                    // AJOUT : Vérifier si l'arc est praticable
                    if (villeAvance != null && !villeAvance.estArcPraticable(arc)) {
                        continue;
                    }

                    String cle = arc.getCleDirectionnelle();
                    if (tousArcs.contains(cle) && !arcsRamasses.contains(cle)) {
                        // Calculer la distance de notre position au début de cet arc
                        List<Arc> chemin = cheminLePlusCourt(ville, position, arc.getDepart(), villeAvance);
                        double distance = calculerDistance(chemin, villeAvance);

                        if (distance < distanceMin) {
                            distanceMin = distance;
                            plusProche = arc;
                        }
                    }
                }
            }
        }

        return plusProche;
    }

    // Calcule le chemin le plus court entre deux noeuds
    private static List<Arc> cheminLePlusCourt(GrapheVille ville, Noeud depart, Noeud arrivee, GrapheVilleAvance villeAvance) {
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

            if (courant.equals(arrivee)) {
                break;
            }

            for (Arc arc : courant.getArcsSortants()) {
                // AJOUT : Vérifier si l'arc est praticable
                if (villeAvance != null && !villeAvance.estArcPraticable(arc)) {
                    continue;
                }

                Noeud voisin = arc.getArrivee();
                if (traites.contains(voisin.getNom())) continue;

                // AJOUT : Utiliser la durée avec contraintes
                double dureeArc = villeAvance != null ?
                        villeAvance.calculerDureeAvecContraintes(arc) :
                        arc.getDuree();
                double nouvelleDist = distances.get(courant.getNom()) + dureeArc;

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

        while (!courant.equals(depart)) {
            Arc arc = predecesseurs.get(courant.getNom());
            if (arc == null) break;
            chemin.add(arc);
            courant = arc.getDepart();
        }

        Collections.reverse(chemin);
        return chemin;
    }

    // Calcule la distance totale d'un chemin
    private static double calculerDistance(List<Arc> chemin, GrapheVilleAvance villeAvance) {
        double total = 0.0;
        for (Arc arc : chemin) {
            // AJOUT : Utiliser la durée avec contraintes
            double duree = villeAvance != null ?
                    villeAvance.calculerDureeAvecContraintes(arc) :
                    arc.getDuree();
            total += duree;
        }
        return total;
    }

    // Construit l'itinéraire à partir de la tournée
    private static Itineraire construireItineraire(Entrepot entrepot, List<ArcAParcourir> tournee, GrapheVille ville) {
        // Identifier toutes les maisons/immeubles rencontrés pendant le ramassage
        Set<String> maisonsRamassees = new HashSet<>();

        for (ArcAParcourir ap : tournee) {
            if (ap.ramassage) {
                Noeud arrivee = ap.arc.getArrivee();
                if ((arrivee instanceof Maison || arrivee instanceof Immeuble)) {
                    maisonsRamassees.add(arrivee.getNom());
                }
            }
        }

        Noeud dernierNoeud = entrepot;
        if (!tournee.isEmpty()) {
            dernierNoeud = tournee.get(tournee.size() - 1).arc.getArrivee();
        }

        Itineraire itin = new ItineraireTourneeComplete(entrepot, dernierNoeud, tournee);
        itin.setMaisonsARamasser(maisonsRamassees);

        return itin;
    }

    // Classe interne pour Dijkstra
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
}