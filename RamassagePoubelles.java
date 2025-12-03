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
            System.out.println("Fichier plan_ville.txt chargé avec succès !");
        } catch (Exception e) {
            System.out.println("Erreur : " + e.getMessage());
            System.out.println("Données de test chargées.");

            ville.definirCoordonnees("Entrepot", 0, 0);
            ville.definirCoordonnees("Carrefour1", 100, 0);
            ville.definirCoordonnees("Maison1", 200, 0);
            ville.definirCoordonnees("Maison2", 300, 0);
            ville.definirCoordonnees("Carrefour2", 400, 0);
            ville.definirCoordonnees("Carrefour3", 100, 100);
            ville.definirCoordonnees("Immeuble1", 100, 200);
            ville.definirCoordonnees("Maison3", 100, 300);

            ville.ajouterTroncon("Rue1", "Entrepot", "Carrefour1", 3.0);
            ville.ajouterTroncon("Rue1", "Carrefour1", "Maison1", 2.0);
            ville.ajouterTroncon("Rue1", "Maison1", "Maison2", 1.5);
            ville.ajouterTroncon("Rue1", "Maison2", "Carrefour2", 2.0);
            ville.ajouterTroncon("Rue2", "Carrefour1", "Carrefour3", 3.0);
            ville.ajouterTroncon("Rue2", "Carrefour3", "Immeuble1", 2.0);
            ville.ajouterTroncon("Rue2", "Immeuble1", "Maison3", 1.5);
            ville.ajouterTroncon("Rue3", "Carrefour2", "Carrefour3", 3.5);
        }

        System.out.println("\n=== Système de ramassage de poubelles ===");

        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n========== MENU ==========");
            System.out.println("1 - Afficher tous les points");
            System.out.println("2 - Trajet direct vers une maison");
            System.out.println("3 - Tournée de ramassage (max 10 points)");
            System.out.println("4 - Tournée complète (ramasser toutes les rues)");
            System.out.println("5 - Visualiser le graphe complet"); // NOUVEAU
            System.out.println("0 - Quitter");
            System.out.print("Choix: ");

            int choix = sc.nextInt();
            sc.nextLine();

            switch (choix) {
                case 1:
                    System.out.println("\n=== Liste des points ===");
                    Set<String> nomsAffiches = new HashSet<>();
                    for (Noeud n : ville.getNoeuds()) {
                        String nom = n.getNom();
                        if (n instanceof Carrefour) {
                            nom = ((Carrefour)n).getNomBase();
                        }
                        if (!nomsAffiches.contains(nom)) {
                            System.out.println(n);
                            nomsAffiches.add(nom);
                        }
                    }
                    break;

                case 2:
                    System.out.print("Destination: ");
                    String destination = sc.nextLine().trim();

                    try {
                        Itineraire itin = RechercheItineraire.trajetDirect(ville, destination);
                        System.out.println(itin);

                        System.out.print("\nVoulez-vous visualiser ce trajet ? (o/n): ");
                        String reponse = sc.nextLine().trim().toLowerCase();
                        if (reponse.equals("o") || reponse.equals("oui")) {
                            Affichage.exporterVersDot(ville, itin);
                        }
                    } catch (Exception e) {
                        System.out.println("Erreur : " + e.getMessage());
                    }
                    break;

                case 3:
                    System.out.println("Entrez les maisons à visiter (max 10)");
                    System.out.println("Tapez 'fin' pour terminer");

                    List<String> maisonsAVisiter = new ArrayList<>();
                    for (int i = 0; i < 10; i++) {
                        System.out.print("Point " + (i + 1) + ": ");
                        String maison = sc.nextLine().trim();
                        if (maison.equalsIgnoreCase("fin")) break;
                        if (!maison.isEmpty()) maisonsAVisiter.add(maison);
                    }

                    if (maisonsAVisiter.isEmpty()) {
                        System.out.println("Aucune maison saisie.");
                        break;
                    }

                    try {
                        Itineraire itin = RechercheItineraire.tourneeRamassage(ville, maisonsAVisiter);
                        System.out.println(itin);

                        System.out.print("\nVoulez-vous visualiser cette tournée ? (o/n): ");
                        String reponse = sc.nextLine().trim().toLowerCase();
                        if (reponse.equals("o") || reponse.equals("oui")) {
                            Affichage.exporterTourneeVersDot(ville, itin);
                        }
                    } catch (Exception e) {
                        System.out.println("Erreur : " + e.getMessage());
                    }
                    break;

                case 4:
                    System.out.println("\n=== Génération de la tournée complète ===");
                    System.out.println("Le camion va ramasser toutes les rues de la ville.");
                    System.out.println("Ramassage uniquement du côté droit (sens de circulation).");
                    System.out.println("\nCalcul en cours...\n");

                    try {
                        Itineraire itinComplete = TourneeComplete.genererTourneeComplete(ville);
                        System.out.println(itinComplete);

                        System.out.print("\nVoulez-vous visualiser cette tournée ? (o/n): ");
                        String reponse = sc.nextLine().trim().toLowerCase();
                        if (reponse.equals("o") || reponse.equals("oui")) {
                            Affichage.exporterTourneeVersDot(ville, itinComplete);
                        }
                    } catch (Exception e) {
                        System.out.println("Erreur : " + e.getMessage());
                        e.printStackTrace();
                    }
                    break;

                case 5: // NOUVEAU CAS
                    System.out.println("\n=== Visualisation du graphe complet ===");
                    Affichage.exporterVersDot(ville);
                    break;

                case 0:
                    System.out.println("Au revoir !");
                    System.exit(0);

                default:
                    System.out.println("Choix invalide.");
            }
        }
    }
}
