import java.io.File;
import java.util.*;

public class RamassagePoubelles {
    public static void main(String[] args) {
        GrapheVille ville = new GrapheVille();

        try (Scanner fichier = new Scanner(new File("plan_ville.txt"))) {
            while (fichier.hasNextLine()) {
                String ligne = fichier.nextLine().trim();
                if (ligne.isEmpty() || ligne.startsWith("#")) continue;

                String[] parts = ligne.split(";");
                if (parts.length >= 3) {
                    String rue = parts[0].trim();
                    String depart = parts[1].trim();
                    String arrivee = parts[2].trim();

                    if (parts.length == 7) {
                        double xDepart = Double.parseDouble(parts[3].trim());
                        double yDepart = Double.parseDouble(parts[4].trim());
                        double xArrivee = Double.parseDouble(parts[5].trim());
                        double yArrivee = Double.parseDouble(parts[6].trim());

                        ville.definirCoordonnees(depart, xDepart, yDepart);
                        ville.definirCoordonnees(arrivee, xArrivee, yArrivee);
                    }

                    ville.ajouterTroncon(rue, depart, arrivee);
                }
            }
            System.out.println("✓ Fichier plan_ville.txt chargé avec succès !");
        } catch (Exception e) {
            System.out.println("Erreur : " + e.getMessage());
            System.out.println("Chargement des données de test...");

            ville.definirCoordonnees("Entrepot Base", 0, 0);
            ville.definirCoordonnees("Carrefour1", 100, 0);
            ville.definirCoordonnees("Maison1", 200, 0);
            ville.definirCoordonnees("Maison2", 300, 0);
            ville.definirCoordonnees("Carrefour2", 400, 0);
            ville.definirCoordonnees("Carrefour3", 100, 100);
            ville.definirCoordonnees("Immeuble1", 100, 200);
            ville.definirCoordonnees("Maison3", 100, 300);

            ville.ajouterTroncon("Rue1", "Entrepot Base", "Carrefour1", 3.0);
            ville.ajouterTroncon("Rue1", "Carrefour1", "Maison1", 2.0);
            ville.ajouterTroncon("Rue1", "Maison1", "Maison2", 1.5);
            ville.ajouterTroncon("Rue1", "Maison2", "Carrefour2", 2.0);
            ville.ajouterTroncon("Rue2", "Carrefour1", "Carrefour3", 3.0);
            ville.ajouterTroncon("Rue2", "Carrefour3", "Immeuble1", 2.0);
            ville.ajouterTroncon("Rue2", "Immeuble1", "Maison3", 1.5);
            ville.ajouterTroncon("Rue3", "Carrefour2", "Carrefour3", 3.5);
        }

        System.out.println("\n" + "=".repeat(70));
        System.out.println("   SYSTÈME DE GESTION DE COLLECTE DES DÉCHETS");
        System.out.println("=".repeat(70));

        Scanner sc = new Scanner(System.in);

        while (true) {
            afficherMenu();
            System.out.print("Votre choix : ");

            int choix = sc.nextInt();
            sc.nextLine();

            try {
                switch (choix) {
                    case 1:
                        afficherPoints(ville);
                        break;
                    case 2:
                        trajetDirectVersUneMaison(ville, sc);
                        break;
                    case 3:
                        tourneeRamassageLimitee(ville, sc);
                        break;
                    case 4:
                        tourneeCompleteRues(ville, sc);
                        break;
                    case 5:
                        visualiserGraphe(ville);
                        break;
                    case 6:
                        voyageurCommerceProchevoisin(ville, sc);
                        break;
                    case 7:
                        voyageurCommerceMST(ville, sc);
                        break;
                    case 8:
                        voyageurCommerceMSTAvecCapacite(ville, sc);
                        break;
                    case 9:
                        planificationSansCapacite(sc);
                        break;
                    case 10:
                        planificationAvecCapacite(sc);
                        break;
                    case 0:
                        System.out.println("\n" + "=".repeat(70));
                        System.out.println("Merci d'avoir utilisé le système de collecte !");
                        System.out.println("=".repeat(70));
                        System.exit(0);
                    default:
                        System.out.println("❌ Choix invalide. Veuillez réessayer.");
                }
            } catch (Exception e) {
                System.err.println("\n❌ Erreur : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void afficherMenu() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("MENU PRINCIPAL");
        System.out.println("=".repeat(70));
        System.out.println("\n--- THÈME 1 : Ramassage aux pieds des habitations ---");
        System.out.println("  1 - Afficher tous les points de la ville");
        System.out.println("  2 - Trajet direct vers UNE maison (Problématique 1)");
        System.out.println("  3 - Tournée de ramassage limitée - max 10 points (Problématique 1)");
        System.out.println("  4 - Tournée complète - ramasser toutes les rues (Problématique 2)");
        System.out.println("  5 - Visualiser le graphe complet");

        System.out.println("\n--- THÈME 2 : Optimisation des points de collecte ---");
        System.out.println("  6 - Voyageur de commerce : Approche Plus Proche Voisin");
        System.out.println("  7 - Voyageur de commerce : Approche MST");
        System.out.println("  8 - Voyageur de commerce : MST avec capacités");

        System.out.println("\n--- THÈME 3 : Planification des secteurs ---");
        System.out.println("  9 - Planifier les secteurs (sans capacité)");
        System.out.println(" 10 - Planifier les secteurs (avec capacités)");

        System.out.println("\n  0 - Quitter");
        System.out.println("=".repeat(70));
    }

    // ============ THÈME 1 ============

    private static void afficherPoints(GrapheVille ville) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("LISTE DES POINTS DE LA VILLE");
        System.out.println("=".repeat(70));

        Set<String> nomsAffiches = new HashSet<>();
        List<Noeud> maisons = new ArrayList<>();
        List<Noeud> immeubles = new ArrayList<>();
        List<Noeud> carrefours = new ArrayList<>();
        Noeud entrepot = null;

        for (Noeud n : ville.getNoeuds()) {
            String nom = n.getNom();
            if (n instanceof Carrefour) {
                nom = ((Carrefour) n).getNomBase();
            }

            if (!nomsAffiches.contains(nom)) {
                if (n instanceof Entrepot) entrepot = n;
                else if (n instanceof Maison) maisons.add(n);
                else if (n instanceof Immeuble) immeubles.add(n);
                else if (n instanceof Carrefour) carrefours.add(n);
                nomsAffiches.add(nom);
            }
        }

        if (entrepot != null) {
            System.out.println("\n🔷 ENTREPÔT :");
            System.out.println("  " + entrepot);
        }

        if (!maisons.isEmpty()) {
            System.out.println("\n🟢 MAISONS (" + maisons.size() + ") :");
            for (Noeud m : maisons) {
                System.out.println("  " + m);
            }
        }

        if (!immeubles.isEmpty()) {
            System.out.println("\n🟡 IMMEUBLES (" + immeubles.size() + ") :");
            for (Noeud i : immeubles) {
                System.out.println("  " + i);
            }
        }

        if (!carrefours.isEmpty()) {
            System.out.println("\n⬤  CARREFOURS (" + carrefours.size() + ") :");
            for (Noeud c : carrefours) {
                System.out.println("  " + ((Carrefour)c).getNomBase());
            }
        }

        System.out.println("\n" + "=".repeat(70));
    }

    private static void trajetDirectVersUneMaison(GrapheVille ville, Scanner sc) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TRAJET DIRECT VERS UNE MAISON");
        System.out.println("=".repeat(70));
        System.out.print("Nom de la destination : ");
        String destination = sc.nextLine().trim();

        Itineraire itin = RechercheItineraire.trajetDirect(ville, destination);
        System.out.println(itin);

        System.out.print("\n📊 Voulez-vous visualiser ce trajet ? (o/n) : ");
        String reponse = sc.nextLine().trim().toLowerCase();
        if (reponse.equals("o") || reponse.equals("oui")) {
            Affichage.exporterVersDot(ville, itin);
        }
    }

    private static void tourneeRamassageLimitee(GrapheVille ville, Scanner sc) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TOURNÉE DE RAMASSAGE (MAX 10 POINTS)");
        System.out.println("=".repeat(70));
        System.out.println("Entrez les maisons à visiter (tapez 'fin' pour terminer)\n");

        List<String> maisonsAVisiter = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            System.out.print("Point " + (i + 1) + " : ");
            String maison = sc.nextLine().trim();
            if (maison.equalsIgnoreCase("fin")) break;
            if (!maison.isEmpty()) maisonsAVisiter.add(maison);
        }

        if (maisonsAVisiter.isEmpty()) {
            System.out.println("❌ Aucune maison saisie.");
            return;
        }

        Itineraire itin = RechercheItineraire.tourneeRamassage(ville, maisonsAVisiter);
        System.out.println(itin);

        System.out.print("\n📊 Voulez-vous visualiser cette tournée ? (o/n) : ");
        String reponse = sc.nextLine().trim().toLowerCase();
        if (reponse.equals("o") || reponse.equals("oui")) {
            Affichage.exporterTourneeVersDot(ville, itin);
        }
    }

    private static void tourneeCompleteRues(GrapheVille ville, Scanner sc) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TOURNÉE COMPLÈTE - RAMASSER TOUTES LES RUES");
        System.out.println("=".repeat(70));
        System.out.println("Le camion va ramasser toutes les rues de la ville.");
        System.out.println("Ramassage uniquement du côté droit (sens de circulation).");
        System.out.println("\nCalcul en cours...\n");

        Itineraire itinComplete = TourneeComplete.genererTourneeComplete(ville);
        System.out.println(itinComplete);

        System.out.print("\n📊 Voulez-vous visualiser cette tournée ? (o/n) : ");
        String reponse = sc.nextLine().trim().toLowerCase();
        if (reponse.equals("o") || reponse.equals("oui")) {
            Affichage.exporterTourneeVersDot(ville, itinComplete);
        }
    }

    private static void visualiserGraphe(GrapheVille ville) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("VISUALISATION DU GRAPHE COMPLET");
        System.out.println("=".repeat(70));
        Affichage.exporterVersDot(ville);
    }

    // ============ THÈME 2 ============

    private static void voyageurCommerceProchevoisin(GrapheVille ville, Scanner sc) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("THÈME 2 - VOYAGEUR DE COMMERCE : PLUS PROCHE VOISIN");
        System.out.println("=".repeat(70));

        List<String> points = saisirPointsCollecte(sc);
        if (points.isEmpty()) return;

        Itineraire itin = VoyageurCommerce.approcheProchevoisin(ville, points);
        System.out.println(itin);

        System.out.print("\n📊 Voulez-vous visualiser cette tournée ? (o/n) : ");
        String reponse = sc.nextLine().trim().toLowerCase();
        if (reponse.equals("o") || reponse.equals("oui")) {
            Affichage.exporterTourneeVersDot(ville, itin);
        }
    }

    private static void voyageurCommerceMST(GrapheVille ville, Scanner sc) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("THÈME 2 - VOYAGEUR DE COMMERCE : MST");
        System.out.println("=".repeat(70));

        List<String> points = saisirPointsCollecte(sc);
        if (points.isEmpty()) return;

        Itineraire itin = VoyageurCommerce.approcheMST(ville, points);
        System.out.println(itin);

        System.out.print("\n📊 Voulez-vous visualiser cette tournée ? (o/n) : ");
        String reponse = sc.nextLine().trim().toLowerCase();
        if (reponse.equals("o") || reponse.equals("oui")) {
            Affichage.exporterTourneeVersDot(ville, itin);
        }
    }

    private static void voyageurCommerceMSTAvecCapacite(GrapheVille ville, Scanner sc) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("THÈME 2 - VOYAGEUR DE COMMERCE : MST AVEC CAPACITÉS");
        System.out.println("=".repeat(70));

        System.out.println("Entrez les points de collecte avec leurs contenances :");
        System.out.println("Format : NomPoint Contenance");
        System.out.println("Tapez 'fin' pour terminer\n");

        Map<String, Double> contenances = new HashMap<>();
        while (true) {
            System.out.print("Point : ");
            String ligne = sc.nextLine().trim();
            if (ligne.equalsIgnoreCase("fin")) break;

            String[] parts = ligne.split("\\s+");
            if (parts.length >= 2) {
                try {
                    String nom = parts[0];
                    double contenance = Double.parseDouble(parts[1]);
                    contenances.put(nom, contenance);
                } catch (NumberFormatException e) {
                    System.out.println("❌ Format invalide. Utilisez : NomPoint Contenance");
                }
            }
        }

        if (contenances.isEmpty()) {
            System.out.println("❌ Aucun point saisi.");
            return;
        }

        System.out.print("\nCapacité maximale du camion : ");
        double capacite = sc.nextDouble();
        sc.nextLine();

        List<Itineraire> tournees = VoyageurCommerce.approcheMSTAvecCapacite(ville, contenances, capacite);

        System.out.println("\n" + "=".repeat(70));
        System.out.println("RÉSULTATS DES TOURNÉES");
        System.out.println("=".repeat(70));

        for (int i = 0; i < tournees.size(); i++) {
            System.out.println("\n--- TOURNÉE " + (i + 1) + " ---");
            System.out.println(tournees.get(i));
        }
    }

    private static List<String> saisirPointsCollecte(Scanner sc) {
        System.out.println("Entrez les points de collecte à visiter :");
        System.out.println("Tapez 'fin' pour terminer\n");

        List<String> points = new ArrayList<>();
        while (true) {
            System.out.print("Point " + (points.size() + 1) + " : ");
            String point = sc.nextLine().trim();
            if (point.equalsIgnoreCase("fin")) break;
            if (!point.isEmpty()) points.add(point);
        }

        if (points.isEmpty()) {
            System.out.println("❌ Aucun point saisi.");
        }

        return points;
    }

    // ============ THÈME 3 ============

    private static void planificationSansCapacite(Scanner sc) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("THÈME 3 - PLANIFICATION DES SECTEURS (SANS CAPACITÉ)");
        System.out.println("=".repeat(70));

        Map<String, PlanificationSecteurs.Secteur> secteurs = creerExempleSecteurs();

        PlanificationSecteurs.afficherStatistiques(secteurs);

        PlanificationSecteurs.Planning planning = PlanificationSecteurs.planifierSansCapacite(secteurs);
        System.out.println(planning);
    }

    private static void planificationAvecCapacite(Scanner sc) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("THÈME 3 - PLANIFICATION DES SECTEURS (AVEC CAPACITÉS)");
        System.out.println("=".repeat(70));

        Map<String, PlanificationSecteurs.Secteur> secteurs = creerExempleSecteurs();

        PlanificationSecteurs.afficherStatistiques(secteurs);

        System.out.print("\nCapacité par camion : ");
        double capacite = sc.nextDouble();

        System.out.print("Nombre de camions disponibles : ");
        int nbCamions = sc.nextInt();
        sc.nextLine();

        PlanificationSecteurs.Planning planning =
                PlanificationSecteurs.planifierAvecCapacite(secteurs, capacite, nbCamions);

        System.out.println(planning);

        // Validation
        boolean valide = PlanificationSecteurs.validerPlanning(planning, secteurs, capacite, nbCamions);
        if (valide) {
            System.out.println("\n✅ Le planning respecte toutes les contraintes !");
        }
    }

    /**
     * Crée un exemple de graphe de secteurs pour les tests
     */
    private static Map<String, PlanificationSecteurs.Secteur> creerExempleSecteurs() {
        // Données des secteurs : [nom, quantité de déchets]
        List<String[]> donnees = Arrays.asList(
                new String[]{"Secteur Nord", "15"},
                new String[]{"Secteur Sud", "20"},
                new String[]{"Secteur Est", "12"},
                new String[]{"Secteur Ouest", "18"},
                new String[]{"Secteur Centre", "25"},
                new String[]{"Secteur Nord-Est", "10"},
                new String[]{"Secteur Sud-Ouest", "14"}
        );

        // Relations de voisinage : [secteur1, secteur2]
        List<String[]> relations = Arrays.asList(
                new String[]{"Secteur Nord", "Secteur Centre"},
                new String[]{"Secteur Nord", "Secteur Nord-Est"},
                new String[]{"Secteur Sud", "Secteur Centre"},
                new String[]{"Secteur Sud", "Secteur Sud-Ouest"},
                new String[]{"Secteur Est", "Secteur Centre"},
                new String[]{"Secteur Est", "Secteur Nord-Est"},
                new String[]{"Secteur Ouest", "Secteur Centre"},
                new String[]{"Secteur Ouest", "Secteur Sud-Ouest"},
                new String[]{"Secteur Nord-Est", "Secteur Est"},
                new String[]{"Secteur Sud-Ouest", "Secteur Ouest"}
        );

        return PlanificationSecteurs.creerGrapheSecteurs(donnees, relations);
    }
}