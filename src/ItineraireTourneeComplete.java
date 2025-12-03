import java.util.*;

class ItineraireTourneeComplete extends Itineraire {
    private List<TourneeComplete.ArcAParcourir> tournee;

    public ItineraireTourneeComplete(Noeud depart, Noeud arrivee, List tournee) {
        super(depart, arrivee);
        this.tournee = tournee;
    }

    @Override
    public double dureeTotal() {
        double total = 0.0;
        Set<String> maisonsRamassees = new HashSet<>();

        for (Object obj : tournee) {
            // Accéder via réflexion ou cast
            try {
                java.lang.reflect.Field arcField = obj.getClass().getDeclaredField("arc");
                java.lang.reflect.Field ramassageField = obj.getClass().getDeclaredField("ramassage");
                arcField.setAccessible(true);
                ramassageField.setAccessible(true);

                Arc arc = (Arc) arcField.get(obj);
                boolean ramassage = (boolean) ramassageField.get(obj);

                Noeud arrivee = arc.getArrivee();

                if (arrivee instanceof Maison || arrivee instanceof Immeuble) {
                    total += arc.getDuree() - arrivee.getTempsTraitement();

                    if (ramassage && !maisonsRamassees.contains(arrivee.getNom())) {
                        total += arrivee.getTempsTraitement();
                        maisonsRamassees.add(arrivee.getNom());
                    }
                } else {
                    total += arc.getDuree();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return total;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== TOURNÉE COMPLÈTE DE RAMASSAGE ===\n");

        if (tournee.isEmpty()) {
            sb.append("Aucune tournée générée.\n");
            return sb.toString();
        }

        sb.append("\n--- Trajet détaillé ---\n");

        List<Noeud> noeudsAffichage = new ArrayList<>();
        List<String> ruesAffichage = new ArrayList<>();
        List<Double> tempsAffichage = new ArrayList<>();
        List<String> infoRamassage = new ArrayList<>();
        List<Boolean> estRamassage = new ArrayList<>();

        Noeud pointActuel = null;
        String rueActuelle = null;
        double tempsDepuisPoint = 0.0;
        boolean ramassageEnCours = false;
        int nbArcsRamasses = 0;
        int nbMaisonsRamassees = 0;
        Set<String> maisonsVues = new HashSet<>();

        // Premier point
        try {
            java.lang.reflect.Field arcField = tournee.get(0).getClass().getDeclaredField("arc");
            arcField.setAccessible(true);
            Arc premierArc = (Arc) arcField.get(tournee.get(0));
            pointActuel = premierArc.getDepart();
            noeudsAffichage.add(pointActuel);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (Object obj : tournee) {
            try {
                java.lang.reflect.Field arcField = obj.getClass().getDeclaredField("arc");
                java.lang.reflect.Field ramassageField = obj.getClass().getDeclaredField("ramassage");
                arcField.setAccessible(true);
                ramassageField.setAccessible(true);

                Arc arc = (Arc) arcField.get(obj);
                boolean ramassage = (boolean) ramassageField.get(obj);

                Noeud noeudArrivee = arc.getArrivee();
                tempsDepuisPoint += arc.getDuree();

                if (!arc.estChangementRue()) {
                    rueActuelle = arc.getRue();
                    if (ramassage) {
                        nbArcsRamasses++;
                        ramassageEnCours = true;
                    }
                }

                boolean doitAfficher = false;

                if (noeudArrivee instanceof Carrefour) {
                    doitAfficher = true;
                } else if ((noeudArrivee instanceof Maison || noeudArrivee instanceof Immeuble) && ramassage) {
                    doitAfficher = true;
                }

                if (doitAfficher) {
                    double tempsReel = tempsDepuisPoint;
                    String infoRamass = "";

                    if ((noeudArrivee instanceof Maison || noeudArrivee instanceof Immeuble)
                            && ramassage && !maisonsVues.contains(noeudArrivee.getNom())) {
                        infoRamass = String.format(" + ramassage %.1f min", noeudArrivee.getTempsTraitement());
                        maisonsVues.add(noeudArrivee.getNom());
                        nbMaisonsRamassees++;
                    } else {
                        tempsReel -= noeudArrivee.getTempsTraitement();
                    }

                    noeudsAffichage.add(noeudArrivee);
                    ruesAffichage.add(rueActuelle);
                    tempsAffichage.add(tempsReel);
                    infoRamassage.add(infoRamass);
                    estRamassage.add(ramassageEnCours);

                    tempsDepuisPoint = 0.0;
                    pointActuel = noeudArrivee;
                    ramassageEnCours = false;
                } else {
                    tempsDepuisPoint -= noeudArrivee.getTempsTraitement();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Affichage avec directions et indication de ramassage
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

            String ligne = String.format("  %s -> %s", nomDepart, nomArrivee);

            // Indication si on ramasse sur ce segment
            if (estRamassage.get(i - 1)) {
                ligne += " [RAMASSAGE]";
            }

            if (suivant instanceof Carrefour && precedent != null) {
                String direction = DirectionCalculator.determinerDirection(precedent, carrefourActuel, suivant);
                if (!direction.isEmpty()) {
                    ligne += String.format(" (%s)", direction);
                }
            }

            if (ruesAffichage.get(i - 1) != null) {
                ligne += String.format(" [%s]", ruesAffichage.get(i - 1));
            }

            ligne += String.format(" (%.1f min)", tempsAffichage.get(i - 1));
            ligne += infoRamassage.get(i - 1);

            sb.append(ligne).append("\n");
        }

        sb.append("\n--- Récapitulatif ---\n");
        sb.append(String.format("Durée totale: %.1f minutes\n", dureeTotal()));
        sb.append("Segments de rue ramassés: " + nbArcsRamasses + "\n");
        sb.append("Points de ramassage (maisons/immeubles): " + nbMaisonsRamassees + "\n");

        return sb.toString();
    }
}