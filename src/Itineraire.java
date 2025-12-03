import java.util.*;

class Itineraire {
    private Noeud depart;
    private Noeud arrivee;
    private List<Arc> arcs;
    private Set<String> maisonsARamasser;

    public Itineraire(Noeud depart, Noeud arrivee) {
        this.depart = depart;
        this.arrivee = arrivee;
        this.arcs = new ArrayList<>();
        this.maisonsARamasser = new HashSet<>();
    }

    public void ajouterArc(Arc arc) { arcs.add(arc); }
    public List<Arc> getArcs() { return arcs; }

    public void ajouterMaisonARamasser(String nomMaison) {
        maisonsARamasser.add(nomMaison);
    }

    public void setMaisonsARamasser(Set<String> maisons) {
        this.maisonsARamasser = maisons;
    }

    public double dureeTotal() {
        double total = 0.0;
        for (Arc arc : arcs) {
            Noeud arrivee = arc.getArrivee();
            String nomArrivee = arrivee.getNom();

            if (arrivee instanceof Maison || arrivee instanceof Immeuble) {
                total += arc.getDuree() - arrivee.getTempsTraitement();
                if (maisonsARamasser.contains(nomArrivee)) {
                    total += arrivee.getTempsTraitement();
                }
            } else {
                total += arc.getDuree();
            }
        }
        return total;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== ITINÉRAIRE DE RAMASSAGE ===\n");

        if (arcs.isEmpty()) {
            sb.append("Aucun chemin trouvé.\n");
            return sb.toString();
        }

        sb.append("\n--- Trajet ---\n");

        // Liste pour stocker les noeuds affichés et calculer les directions
        List<Noeud> noeudsAffichage = new ArrayList<>();
        List<String> ruesAffichage = new ArrayList<>();
        List<Double> tempsAffichage = new ArrayList<>();
        List<String> infoRamassage = new ArrayList<>();

        noeudsAffichage.add(depart);

        Noeud pointActuel = depart;
        String rueActuelle = null;
        double tempsDepuisPoint = 0.0;

        for (Arc arc : arcs) {
            Noeud noeudArrivee = arc.getArrivee();
            tempsDepuisPoint += arc.getDuree();

            if (!arc.estChangementRue()) {
                rueActuelle = arc.getRue();
            }

            boolean doitAfficher = false;

            if (noeudArrivee instanceof Carrefour) {
                doitAfficher = true;
            } else if ((noeudArrivee instanceof Maison || noeudArrivee instanceof Immeuble)
                    && maisonsARamasser.contains(noeudArrivee.getNom())) {
                doitAfficher = true;
            }

            if (doitAfficher) {
                double tempsReel = tempsDepuisPoint;
                String infoRamass = "";

                if (maisonsARamasser.contains(noeudArrivee.getNom())) {
                    infoRamass = String.format(" + ramassage %.1f min", noeudArrivee.getTempsTraitement());
                } else {
                    tempsReel -= noeudArrivee.getTempsTraitement();
                }

                noeudsAffichage.add(noeudArrivee);
                ruesAffichage.add(rueActuelle);
                tempsAffichage.add(tempsReel);
                infoRamassage.add(infoRamass);

                tempsDepuisPoint = 0.0;
                pointActuel = noeudArrivee;
            } else {
                tempsDepuisPoint -= noeudArrivee.getTempsTraitement();
            }
        }

        // Affichage avec directions
        for (int i = 1; i < noeudsAffichage.size(); i++) {
            Noeud precedent = (i >= 2) ? noeudsAffichage.get(i - 2) : null;
            Noeud carrefourActuel = noeudsAffichage.get(i - 1);
            Noeud suivant = noeudsAffichage.get(i);

            String nomDepart = carrefourActuel.getNom();
            String nomArrivee = suivant.getNom();

            if (carrefourActuel instanceof Carrefour) {
                nomDepart = ((Carrefour)carrefourActuel).getNomBase();
            }
            if (suivant instanceof Carrefour) {
                nomArrivee = ((Carrefour)suivant).getNomBase();
            }

            sb.append(String.format("  %s -> %s", nomDepart, nomArrivee));

            // Afficher la direction si c'est un carrefour
            if (suivant instanceof Carrefour && precedent != null) {
                String direction = DirectionCalculator.determinerDirection(precedent, carrefourActuel, suivant);
                if (!direction.isEmpty()) {
                    sb.append(String.format(" (%s)", direction));
                }
            }

            if (ruesAffichage.get(i - 1) != null) {
                sb.append(String.format(" [%s]", ruesAffichage.get(i - 1)));
            }

            sb.append(String.format(" (%.1f min)", tempsAffichage.get(i - 1)));
            sb.append(infoRamassage.get(i - 1));
            sb.append("\n");
        }

        sb.append("\n--- Récapitulatif ---\n");
        sb.append(String.format("Durée totale: %.1f minutes\n", dureeTotal()));

        if (!maisonsARamasser.isEmpty()) {
            sb.append("Points de ramassage effectués: " + maisonsARamasser.size() + "\n");
        }

        return sb.toString();
    }
}