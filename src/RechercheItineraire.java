import java.util.*;

class RechercheItineraire {

    private static class InfoDijkstra implements Comparable<InfoDijkstra> {
        Noeud noeud;
        double distance;
        Arc arcPrecedent;

        public InfoDijkstra(Noeud noeud, double distance, Arc arcPrecedent) {
            this.noeud = noeud;
            this.distance = distance;
            this.arcPrecedent = arcPrecedent;
        }

        @Override
        public int compareTo(InfoDijkstra autre) {
            return Double.compare(this.distance, autre.distance);
        }
    }

    // Aller directement à UNE maison (sans ramassage intermédiaire)
    public static Itineraire trajetDirect(GrapheVille graphe, String nomArrivee) {
        Noeud depart = graphe.getEntrepot();
        if (depart == null) {
            throw new RuntimeException("Entrepôt non trouvé");
        }

        List<Noeud> noeudsArrivee = graphe.getToutesVersions(nomArrivee);
        if (noeudsArrivee.isEmpty()) {
            throw new RuntimeException("Destination inconnue: " + nomArrivee);
        }

        // Utiliser Dijkstra en ignorant les temps de ramassage
        Map<String, Double> distances = new HashMap<>();
        Map<String, Arc> predecesseurs = new HashMap<>();
        Set<String> traites = new HashSet<>();
        PriorityQueue<InfoDijkstra> file = new PriorityQueue<>();

        distances.put(depart.getNom(), 0.0);
        predecesseurs.put(depart.getNom(), null);
        file.add(new InfoDijkstra(depart, 0.0, null));

        Noeud meilleurArrivee = null;
        double meilleureDistance = Double.MAX_VALUE;

        while (!file.isEmpty()) {
            InfoDijkstra info = file.poll();
            Noeud courant = info.noeud;

            if (traites.contains(courant.getNom())) continue;
            traites.add(courant.getNom());

            for (Noeud arrivee : noeudsArrivee) {
                if (courant.equals(arrivee) || courant.getNom().equals(arrivee.getNom())) {
                    if (info.distance < meilleureDistance) {
                        meilleureDistance = info.distance;
                        meilleurArrivee = courant;
                    }
                }
            }

            if (meilleurArrivee != null && info.distance > meilleureDistance) {
                break;
            }

            for (Arc arc : courant.getArcsSortants()) {
                Noeud voisin = arc.getArrivee();

                if (traites.contains(voisin.getNom())) continue;

                // Pour trajet direct: on ignore les temps de ramassage des maisons intermédiaires
                double dureeSansRamassage = arc.getDuree() - voisin.getTempsTraitement();
                double nouvelleDistance = distances.get(courant.getNom()) + dureeSansRamassage;

                if (!distances.containsKey(voisin.getNom()) ||
                        nouvelleDistance < distances.get(voisin.getNom())) {
                    distances.put(voisin.getNom(), nouvelleDistance);
                    predecesseurs.put(voisin.getNom(), arc);
                    file.add(new InfoDijkstra(voisin, nouvelleDistance, arc));
                }
            }
        }

        if (meilleurArrivee == null) {
            throw new RuntimeException("Aucun chemin trouvé vers " + nomArrivee);
        }

        Itineraire itin = reconstruireChemin(depart, meilleurArrivee, predecesseurs);
        // Marquer seulement la destination finale comme point de ramassage
        itin.ajouterMaisonARamasser(nomArrivee);
        return itin;
    }

    // Tournée avec plusieurs maisons (avec ramassage)
    public static Itineraire tourneeRamassage(GrapheVille graphe, List<String> maisonsAVisiter) {
        if (maisonsAVisiter.isEmpty()) {
            throw new RuntimeException("Aucune maison à visiter");
        }

        Noeud depart = graphe.getEntrepot();
        if (depart == null) {
            throw new RuntimeException("Entrepôt non trouvé");
        }

        // On va construire une tournée qui passe par toutes les maisons
        // Algorithme simple: plus proche voisin
        Set<String> maisonsRestantes = new HashSet<>(maisonsAVisiter);
        List<Arc> cheminTotal = new ArrayList<>();
        Noeud positionActuelle = depart;

        while (!maisonsRestantes.isEmpty()) {
            // Trouver la maison la plus proche
            String maisonLaPlusProche = null;
            double distanceMin = Double.MAX_VALUE;
            List<Arc> cheminVersMaison = null;

            for (String maison : maisonsRestantes) {
                try {
                    // Chercher le chemin depuis la position actuelle
                    Itineraire itinTemp = dijkstraDepuis(graphe, positionActuelle, maison, maisonsRestantes);
                    double distance = itinTemp.dureeTotal();

                    if (distance < distanceMin) {
                        distanceMin = distance;
                        maisonLaPlusProche = maison;
                        cheminVersMaison = new ArrayList<>(itinTemp.getArcs());
                    }
                } catch (Exception e) {
                    // Cette maison n'est pas accessible
                }
            }

            if (maisonLaPlusProche == null) {
                throw new RuntimeException("Impossible d'atteindre toutes les maisons");
            }

            // Ajouter ce chemin au chemin total
            cheminTotal.addAll(cheminVersMaison);

            // Mettre à jour la position
            if (!cheminVersMaison.isEmpty()) {
                positionActuelle = cheminVersMaison.get(cheminVersMaison.size() - 1).getArrivee();
            }

            maisonsRestantes.remove(maisonLaPlusProche);
        }

        // Construire l'itinéraire final
        Itineraire itin = new Itineraire(depart, positionActuelle);
        for (Arc arc : cheminTotal) {
            itin.ajouterArc(arc);
        }
        itin.setMaisonsARamasser(new HashSet<>(maisonsAVisiter));

        return itin;
    }

    // Dijkstra depuis un noeud quelconque
    private static Itineraire dijkstraDepuis(GrapheVille graphe, Noeud depart, String nomArrivee, Set<String> maisonsARamasser) {
        List<Noeud> noeudsArrivee = graphe.getToutesVersions(nomArrivee);
        if (noeudsArrivee.isEmpty()) {
            throw new RuntimeException("Destination inconnue: " + nomArrivee);
        }

        Map<String, Double> distances = new HashMap<>();
        Map<String, Arc> predecesseurs = new HashMap<>();
        Set<String> traites = new HashSet<>();
        PriorityQueue<InfoDijkstra> file = new PriorityQueue<>();

        distances.put(depart.getNom(), 0.0);
        predecesseurs.put(depart.getNom(), null);
        file.add(new InfoDijkstra(depart, 0.0, null));

        Noeud meilleurArrivee = null;
        double meilleureDistance = Double.MAX_VALUE;

        while (!file.isEmpty()) {
            InfoDijkstra info = file.poll();
            Noeud courant = info.noeud;

            if (traites.contains(courant.getNom())) continue;
            traites.add(courant.getNom());

            for (Noeud arrivee : noeudsArrivee) {
                if (courant.equals(arrivee) || courant.getNom().equals(arrivee.getNom())) {
                    if (info.distance < meilleureDistance) {
                        meilleureDistance = info.distance;
                        meilleurArrivee = courant;
                    }
                }
            }

            if (meilleurArrivee != null && info.distance > meilleureDistance) {
                break;
            }

            for (Arc arc : courant.getArcsSortants()) {
                Noeud voisin = arc.getArrivee();

                if (traites.contains(voisin.getNom())) continue;

                // Calculer la durée: inclure le ramassage SI c'est une maison à ramasser
                double dureeArc = arc.getDuree();
                if ((voisin instanceof Maison || voisin instanceof Immeuble)
                        && !maisonsARamasser.contains(voisin.getNom())) {
                    // On passe par cette maison mais on ne ramasse pas
                    dureeArc -= voisin.getTempsTraitement();
                }

                double nouvelleDistance = distances.get(courant.getNom()) + dureeArc;

                if (!distances.containsKey(voisin.getNom()) ||
                        nouvelleDistance < distances.get(voisin.getNom())) {
                    distances.put(voisin.getNom(), nouvelleDistance);
                    predecesseurs.put(voisin.getNom(), arc);
                    file.add(new InfoDijkstra(voisin, nouvelleDistance, arc));
                }
            }
        }

        if (meilleurArrivee == null) {
            throw new RuntimeException("Aucun chemin trouvé vers " + nomArrivee);
        }

        return reconstruireChemin(depart, meilleurArrivee, predecesseurs);
    }

    private static Itineraire reconstruireChemin(Noeud depart, Noeud arrivee, Map<String, Arc> predecesseurs) {
        List<Arc> chemin = new ArrayList<>();
        Noeud courant = arrivee;

        while (!courant.equals(depart)) {
            Arc arc = predecesseurs.get(courant.getNom());
            if (arc == null) break;
            chemin.add(arc);
            courant = arc.getDepart();
        }

        Collections.reverse(chemin);
        Itineraire itineraire = new Itineraire(depart, arrivee);
        chemin.forEach(itineraire::ajouterArc);
        return itineraire;
    }
}