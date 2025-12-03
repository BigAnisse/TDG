import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Affichage {

    /**
     * Exporte le graphe de la ville en format DOT pour visualisation
     * @param ville Le graphe de la ville
     * @param itineraire L'itinéraire à mettre en évidence (optionnel, peut être null)
     */
    public static void exporterVersDot(GrapheVille ville, Itineraire itineraire) {
        Set<String> arcsVisites = new HashSet<>();
        Set<String> arcsItineraire = new HashSet<>();

        // Si un itinéraire est fourni, extraire ses arcs
        if (itineraire != null) {
            for (Arc arc : itineraire.getArcs()) {
                if (!arc.estChangementRue()) {
                    String nomDepart = arc.getDepart().getNom();
                    String nomArrivee = arc.getArrivee().getNom();

                    // Enlever les suffixes de rue pour les carrefours
                    if (arc.getDepart() instanceof Carrefour) {
                        nomDepart = ((Carrefour)arc.getDepart()).getNomBase();
                    }
                    if (arc.getArrivee() instanceof Carrefour) {
                        nomArrivee = ((Carrefour)arc.getArrivee()).getNomBase();
                    }

                    arcsItineraire.add(nomDepart + "--" + nomArrivee);
                    arcsItineraire.add(nomArrivee + "--" + nomDepart); // Bidirectionnel
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("graph G {\n");
        sb.append("\trankdir=LR;\n"); // Orientation gauche-droite
        sb.append("\tnode [shape=box, style=rounded];\n\n");

        // Définir les styles des noeuds
        for (Noeud noeud : ville.getNoeuds()) {
            String nom = noeud.getNom();

            // Éviter les doublons de carrefours
            if (noeud instanceof Carrefour) {
                Carrefour c = (Carrefour) noeud;
                if (c.getRueCourante() != null) {
                    continue; // On ne dessine que la version "base" du carrefour
                }
            }

            sb.append("\t\"").append(nom).append("\" [");

            if (noeud instanceof Entrepot) {
                sb.append("shape=diamond, style=filled, fillcolor=lightblue, color=blue, penwidth=2");
            } else if (noeud instanceof Immeuble) {
                sb.append("shape=box, style=\"rounded,filled\", fillcolor=lightyellow");
            } else if (noeud instanceof Maison) {
                sb.append("shape=box, style=\"rounded,filled\", fillcolor=lightgreen");
            } else if (noeud instanceof Carrefour) {
                sb.append("shape=circle, style=filled, fillcolor=lightgray");
            }

            sb.append("];\n");
        }

        sb.append("\n");

        // Dessiner les arcs
        for (Noeud noeud : ville.getNoeuds()) {
            for (Arc arc : noeud.getArcsSortants()) {
                // Ignorer les changements de rue
                if (arc.estChangementRue()) {
                    continue;
                }

                String nomDepart = arc.getDepart().getNom();
                String nomArrivee = arc.getArrivee().getNom();

                // Pour les carrefours, utiliser le nom de base
                if (arc.getDepart() instanceof Carrefour) {
                    Carrefour c = (Carrefour) arc.getDepart();
                    nomDepart = c.getNomBase();
                }
                if (arc.getArrivee() instanceof Carrefour) {
                    Carrefour c = (Carrefour) arc.getArrivee();
                    nomArrivee = c.getNomBase();
                }

                String cleArc = nomDepart + "--" + nomArrivee;
                String cleArcInverse = nomArrivee + "--" + nomDepart;

                // Éviter les doublons (graphe non orienté)
                if (!arcsVisites.contains(cleArc) && !arcsVisites.contains(cleArcInverse)) {
                    arcsVisites.add(cleArc);

                    sb.append("\t\"").append(nomDepart).append("\"");
                    sb.append(" -- ");
                    sb.append("\"").append(nomArrivee).append("\"");
                    sb.append(" [label=\"").append(arc.getRue()).append("\\n");
                    sb.append(String.format("%.1f min", arc.getDuree() - arc.getArrivee().getTempsTraitement()));
                    sb.append("\"");

                    // Mettre en rouge si dans l'itinéraire
                    if (arcsItineraire.contains(cleArc) || arcsItineraire.contains(cleArcInverse)) {
                        sb.append(", color=red, penwidth=3");
                    } else {
                        sb.append(", color=black");
                    }

                    sb.append("];\n");
                }
            }
        }

        sb.append("}\n");

        // Écrire dans le fichier
        String content = sb.toString();
        String filePath = "graphe.txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(content);
            System.out.println("\n✓ Graphe enregistré dans " + filePath);
            afficherInstructionsVisualisation();
        } catch (IOException e) {
            System.err.println("Erreur d'enregistrement : " + e.getMessage());
        }
    }

    /**
     * Exporte uniquement le graphe sans itinéraire
     */
    public static void exporterVersDot(GrapheVille ville) {
        exporterVersDot(ville, null);
    }

    /**
     * Affiche les instructions pour visualiser le graphe
     */
    private static void afficherInstructionsVisualisation() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("VISUALISER LE GRAPHE :");
        System.out.println("=".repeat(70));
        System.out.println("1. Ouvrir le fichier : graphe.txt");
        System.out.println("2. Sélectionner TOUT (Ctrl + A) + Copier (Ctrl + C)");
        System.out.println("3. Aller sur : https://dreampuf.github.io/GraphvizOnline/");
        System.out.println("4. Dans l'éditeur de gauche (noir) :");
        System.out.println("   - Sélectionner TOUT (Ctrl + A)");
        System.out.println("   - Supprimer (Touche Retour Arrière)");
        System.out.println("   - Coller votre graphe (Ctrl + V)");
        System.out.println("\n" + "=".repeat(70));
        System.out.println("LÉGENDE DU GRAPHE :");
        System.out.println("=".repeat(70));
        System.out.println("🔷 Losange bleu     = Entrepôt");
        System.out.println("⬤  Cercle gris      = Carrefour");
        System.out.println("🟢 Rectangle vert   = Maison (ramassage 2 min)");
        System.out.println("🟡 Rectangle jaune  = Immeuble (ramassage 5 min)");
        System.out.println("━  Trait noir       = Rue normale");
        System.out.println("━  Trait rouge épais = Itinéraire suivi");
        System.out.println("=".repeat(70) + "\n");
    }

    /**
     * Exporte une version orientée du graphe (avec flèches) pour la tournée complète
     */
    public static void exporterTourneeVersDot(GrapheVille ville, Itineraire itineraire) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph G {\n"); // digraph pour graphe orienté
        sb.append("\trankdir=LR;\n");
        sb.append("\tnode [shape=box, style=rounded];\n\n");

        // Définir les styles des noeuds
        Set<String> noeudsAffiches = new HashSet<>();
        for (Noeud noeud : ville.getNoeuds()) {
            String nom = noeud.getNom();

            if (noeud instanceof Carrefour) {
                Carrefour c = (Carrefour) noeud;
                nom = c.getNomBase();
                if (noeudsAffiches.contains(nom)) continue;
            }

            noeudsAffiches.add(nom);

            sb.append("\t\"").append(nom).append("\" [");

            if (noeud instanceof Entrepot) {
                sb.append("shape=diamond, style=filled, fillcolor=lightblue, color=blue, penwidth=2");
            } else if (noeud instanceof Immeuble) {
                sb.append("shape=box, style=\"rounded,filled\", fillcolor=lightyellow");
            } else if (noeud instanceof Maison) {
                sb.append("shape=box, style=\"rounded,filled\", fillcolor=lightgreen");
            } else if (noeud instanceof Carrefour) {
                sb.append("shape=circle, style=filled, fillcolor=lightgray");
            }

            sb.append("];\n");
        }

        sb.append("\n");

        // Dessiner l'itinéraire avec des flèches
        if (itineraire != null) {
            int ordre = 1;
            for (Arc arc : itineraire.getArcs()) {
                if (arc.estChangementRue()) continue;

                String nomDepart = arc.getDepart().getNom();
                String nomArrivee = arc.getArrivee().getNom();

                if (arc.getDepart() instanceof Carrefour) {
                    nomDepart = ((Carrefour)arc.getDepart()).getNomBase();
                }
                if (arc.getArrivee() instanceof Carrefour) {
                    nomArrivee = ((Carrefour)arc.getArrivee()).getNomBase();
                }

                sb.append("\t\"").append(nomDepart).append("\"");
                sb.append(" -> ");
                sb.append("\"").append(nomArrivee).append("\"");
                sb.append(" [label=\"").append(ordre++).append(". ").append(arc.getRue()).append("\"");
                sb.append(", color=red, penwidth=2");
                sb.append("];\n");
            }
        }

        sb.append("}\n");

        String content = sb.toString();
        String filePath = "graphe_tournee.txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(content);
            System.out.println("\n✓ Graphe de la tournée enregistré dans " + filePath);
            System.out.println("Les flèches rouges indiquent l'ordre de passage du camion.");
            afficherInstructionsVisualisation();
        } catch (IOException e) {
            System.err.println("Erreur d'enregistrement : " + e.getMessage());
        }
    }
}