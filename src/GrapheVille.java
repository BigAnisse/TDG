import java.util.*;

class GrapheVille {
    private Map<String, Noeud> noeuds;
    private Map<String, Noeud> noeudsOriginaux;
    private Map<String, double[]> coordonnees; // Stocke les coordonnées x,y par nom
    private Entrepot entrepot;

    private static final double DUREE_MIN = 1.0;
    private static final double DUREE_MAX = 5.0;
    private static final double TEMPS_CHANGEMENT_BASE = 1.0;

    private Random random;

    public GrapheVille() {
        noeuds = new HashMap<>();
        noeudsOriginaux = new HashMap<>();
        coordonnees = new HashMap<>();
        random = new Random();
    }

    // Enregistrer les coordonnées d'un noeud
    public void definirCoordonnees(String nom, double x, double y) {
        coordonnees.put(nom, new double[]{x, y});
    }

    // Obtenir les coordonnées (ou générer aléatoirement si pas définies)
    private double[] getCoordonnees(String nom) {
        if (!coordonnees.containsKey(nom)) {
            // Générer des coordonnées aléatoires si non définies
            double x = random.nextDouble() * 1000;
            double y = random.nextDouble() * 1000;
            coordonnees.put(nom, new double[]{x, y});
        }
        return coordonnees.get(nom);
    }

    private Noeud creerNoeud(String nom) {
        double[] coords = getCoordonnees(nom);
        double x = coords[0];
        double y = coords[1];

        String nomLower = nom.toLowerCase();

        if (nomLower.startsWith("entrepot")) {
            if (entrepot == null) {
                entrepot = new Entrepot(nom, x, y);
                return entrepot;
            }
            return entrepot;
        } else if (nomLower.startsWith("maison")) {
            return new Maison(nom, x, y);
        } else if (nomLower.startsWith("immeuble")) {
            return new Immeuble(nom, x, y);
        } else if (nomLower.startsWith("carrefour")) {
            return new Carrefour(nom, x, y);
        }

        return new Carrefour(nom, x, y);
    }

    public Noeud getOuCreerNoeud(String nom) {
        return noeuds.computeIfAbsent(nom, this::creerNoeud);
    }

    public void ajouterTroncon(String rue, String nomDepart, String nomArrivee) {
        double duree = DUREE_MIN + random.nextDouble() * (DUREE_MAX - DUREE_MIN);
        duree = Math.round(duree * 10) / 10.0;
        ajouterTroncon(rue, nomDepart, nomArrivee, duree);
    }

    public void ajouterTroncon(String rue, String nomDepart, String nomArrivee, double duree) {
        Noeud n1Original = getOuCreerNoeud(nomDepart);
        Noeud n2Original = getOuCreerNoeud(nomArrivee);

        noeudsOriginaux.put(nomDepart, n1Original);
        noeudsOriginaux.put(nomArrivee, n2Original);

        Noeud n1 = n1Original;
        Noeud n2 = n2Original;

        if (n1Original instanceof Carrefour && !(n1Original instanceof Entrepot)) {
            String cleN1 = nomDepart + "_" + rue;
            if (!noeuds.containsKey(cleN1)) {
                double[] coords = getCoordonnees(nomDepart);
                n1 = new Carrefour(nomDepart, rue, coords[0], coords[1]);
                noeuds.put(cleN1, n1);
                ((Carrefour) n1Original).ajouterRue(rue);
            } else {
                n1 = noeuds.get(cleN1);
            }
        }

        if (n2Original instanceof Carrefour && !(n2Original instanceof Entrepot)) {
            String cleN2 = nomArrivee + "_" + rue;
            if (!noeuds.containsKey(cleN2)) {
                double[] coords = getCoordonnees(nomArrivee);
                n2 = new Carrefour(nomArrivee, rue, coords[0], coords[1]);
                noeuds.put(cleN2, n2);
                ((Carrefour) n2Original).ajouterRue(rue);
            } else {
                n2 = noeuds.get(cleN2);
            }
        }

        double dureeAvecTraitement = duree + n2.getTempsTraitement();
        Arc arc = new Arc(n1, n2, rue, dureeAvecTraitement);
        n1.ajouterArc(arc);

        creerChangementsRue(nomDepart);
        creerChangementsRue(nomArrivee);
    }

    private void creerChangementsRue(String nomCarrefour) {
        Noeud noeudOriginal = noeudsOriginaux.get(nomCarrefour);

        if (!(noeudOriginal instanceof Carrefour) || noeudOriginal instanceof Entrepot) {
            return;
        }

        Carrefour carrefour = (Carrefour) noeudOriginal;
        Set<String> rues = carrefour.getRues();

        if (rues.size() <= 1) return;

        double tempsChangement = TEMPS_CHANGEMENT_BASE + 0.5 * rues.size();

        for (String rue1 : rues) {
            for (String rue2 : rues) {
                if (!rue1.equals(rue2)) {
                    String cle1 = nomCarrefour + "_" + rue1;
                    String cle2 = nomCarrefour + "_" + rue2;

                    Noeud n1 = noeuds.get(cle1);
                    Noeud n2 = noeuds.get(cle2);

                    if (n1 != null && n2 != null) {
                        boolean existe = false;
                        for (Arc arc : n1.getArcsSortants()) {
                            if (arc.estChangementRue() && arc.getArrivee().equals(n2)) {
                                existe = true;
                                break;
                            }
                        }

                        if (!existe) {
                            Arc arcChangement = new Arc(n1, n2, tempsChangement);
                            n1.ajouterArc(arcChangement);
                        }
                    }
                }
            }
        }
    }

    public Collection<Noeud> getNoeuds() {
        return noeuds.values();
    }

    public Noeud getNoeud(String nom) {
        return noeuds.get(nom);
    }

    public Noeud getNoeudOriginal(String nom) {
        return noeudsOriginaux.get(nom);
    }

    public Entrepot getEntrepot() {
        return entrepot;
    }

    public List<Noeud> getToutesVersions(String nomBase) {
        List<Noeud> versions = new ArrayList<>();
        for (String cle : noeuds.keySet()) {
            if (cle.startsWith(nomBase)) {
                versions.add(noeuds.get(cle));
            }
        }
        return versions;
    }
}