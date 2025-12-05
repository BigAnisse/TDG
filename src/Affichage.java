import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Affichage {

    /**
     * Exporte le graphe de la ville en format DOT pour visualisation
     * @param ville Le graphe de la ville
     * @param itineraire L'itinéraire à mettre en évidence (optionnel, peut être null)
     * @param nomFichier Le nom du fichier de sortie
     */
    public static void exporterVersDot(GrapheVille ville, Itineraire itineraire, String nomFichier) {
        Set<String> arcsVisites = new HashSet<>();
        List<String> arcsItineraireOrdonnés = new ArrayList<>();

        // Si un itinéraire est fourni, extraire ses arcs dans l'ordre
        if (itineraire != null) {
            for (Arc arc : itineraire.getArcs()) {
                if (!arc.estChangementRue()) {
                    String nomDepart = arc.getDepart().getNom();
                    String nomArrivee = arc.getArrivee().getNom();

                    if (arc.getDepart() instanceof Carrefour) {
                        nomDepart = ((Carrefour)arc.getDepart()).getNomBase();
                    }
                    if (arc.getArrivee() instanceof Carrefour) {
                        nomArrivee = ((Carrefour)arc.getArrivee()).getNomBase();
                    }

                    arcsItineraireOrdonnés.add(nomDepart + "->" + nomArrivee);
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("digraph G {\n");
        sb.append("\trankdir=LR;\n");
        sb.append("\tnode [shape=box, style=rounded];\n");
        sb.append("\tedge [fontsize=10];\n\n");

        // Définir les styles des noeuds
        Set<String> noeudsAffiches = new HashSet<>();
        for (Noeud noeud : ville.getNoeuds()) {
            String nom = noeud.getNom();

            if (noeud instanceof Carrefour) {
                Carrefour c = (Carrefour) noeud;
                if (c.getRueCourante() != null) {
                    continue;
                }
                nom = c.getNomBase();
            }

            if (noeudsAffiches.contains(nom)) continue;
            noeudsAffiches.add(nom);

            sb.append("\t\"").append(nom).append("\" [");

            if (noeud instanceof Entrepot) {
                sb.append("shape=diamond, style=filled, fillcolor=\"#87CEEB\", color=\"#4169E1\", penwidth=3");
            } else if (noeud instanceof Immeuble) {
                sb.append("shape=box, style=\"rounded,filled\", fillcolor=\"#FFFACD\", color=\"#DAA520\"");
            } else if (noeud instanceof Maison) {
                sb.append("shape=box, style=\"rounded,filled\", fillcolor=\"#90EE90\", color=\"#228B22\"");
            } else if (noeud instanceof Carrefour) {
                sb.append("shape=circle, style=filled, fillcolor=\"#D3D3D3\", color=\"#696969\"");
            }

            sb.append("];\n");
        }

        sb.append("\n");

        // Dessiner d'abord tous les arcs normaux (en noir)
        for (Noeud noeud : ville.getNoeuds()) {
            for (Arc arc : noeud.getArcsSortants()) {
                if (arc.estChangementRue()) continue;

                String nomDepart = arc.getDepart().getNom();
                String nomArrivee = arc.getArrivee().getNom();

                if (arc.getDepart() instanceof Carrefour) {
                    nomDepart = ((Carrefour) arc.getDepart()).getNomBase();
                }
                if (arc.getArrivee() instanceof Carrefour) {
                    nomArrivee = ((Carrefour) arc.getArrivee()).getNomBase();
                }

                String cleArc = nomDepart + "->" + nomArrivee;

                if (!arcsVisites.contains(cleArc)) {
                    arcsVisites.add(cleArc);

                    sb.append("\t\"").append(nomDepart).append("\"");
                    sb.append(" -> ");
                    sb.append("\"").append(nomArrivee).append("\"");
                    sb.append(" [label=\"").append(arc.getRue()).append("\\n");
                    sb.append(String.format("%.1f min", arc.getDuree() - arc.getArrivee().getTempsTraitement()));
                    sb.append("\"");
                    sb.append(", color=\"#808080\", penwidth=1");
                    sb.append("];\n");
                }
            }
        }

        // Dessiner ensuite l'itinéraire (en rouge par-dessus)
        if (itineraire != null && !arcsItineraireOrdonnés.isEmpty()) {
            sb.append("\n\t// === ITINÉRAIRE SUIVI PAR LE CAMION ===\n");

            for (int i = 0; i < arcsItineraireOrdonnés.size(); i++) {
                String[] parts = arcsItineraireOrdonnés.get(i).split("->");
                String nomDepart = parts[0];
                String nomArrivee = parts[1];

                sb.append("\t\"").append(nomDepart).append("\"");
                sb.append(" -> ");
                sb.append("\"").append(nomArrivee).append("\"");
                sb.append(" [label=\"").append(i + 1).append("\"");
                sb.append(", color=\"#FF0000\", penwidth=4, fontcolor=\"#FF0000\", fontsize=12, weight=10");
                sb.append("];\n");
            }
        }

        sb.append("}\n");

        // Écrire dans le fichier
        String content = sb.toString();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(nomFichier))) {
            writer.write(content);
            System.out.println("\n✅ Graphe enregistré dans " + nomFichier);
            afficherInstructionsVisualisation(nomFichier);
        } catch (IOException e) {
            System.err.println("❌ Erreur d'enregistrement : " + e.getMessage());
        }
    }

    /**
     * Exporte uniquement le graphe sans itinéraire
     */
    public static void exporterVersDot(GrapheVille ville) {
        exporterVersDot(ville, null, "graphe_ville.txt");
    }

    /**
     * Exporte avec itinéraire (fichier par défaut)
     */
    public static void exporterVersDot(GrapheVille ville, Itineraire itineraire) {
        exporterVersDot(ville, itineraire, "graphe_parcours.txt");
    }

    /**
     * Exporte une tournée complète avec les segments ramassés
     */
    public static void exporterTourneeVersDot(GrapheVille ville, Itineraire itineraire, String nomFichier) {
        if (itineraire instanceof ItineraireTourneeComplete) {
            exporterTourneeCompleteAvecRamassage(ville, (ItineraireTourneeComplete) itineraire, nomFichier);
        } else {
            exporterVersDot(ville, itineraire, nomFichier);
        }
    }

    /**
     * Exporte la tournée complète en distinguant les segments ramassés
     */
    private static void exporterTourneeCompleteAvecRamassage(GrapheVille ville,
                                                             ItineraireTourneeComplete itineraire,
                                                             String nomFichier) {
        Set<String> arcsVisites = new HashSet<>();
        List<String> arcsRamassage = new ArrayList<>();
        List<String> arcsSansRamassage = new ArrayList<>();

        // Extraire les arcs avec distinction ramassage/non-ramassage
        try {
            java.lang.reflect.Field tourneeField = itineraire.getClass().getDeclaredField("tournee");
            tourneeField.setAccessible(true);
            List<?> tournee = (List<?>) tourneeField.get(itineraire);

            for (Object obj : tournee) {
                java.lang.reflect.Field arcField = obj.getClass().getDeclaredField("arc");
                java.lang.reflect.Field ramassageField = obj.getClass().getDeclaredField("ramassage");
                arcField.setAccessible(true);
                ramassageField.setAccessible(true);

                Arc arc = (Arc) arcField.get(obj);
                boolean ramassage = (boolean) ramassageField.get(obj);

                if (!arc.estChangementRue()) {
                    String nomDepart = arc.getDepart().getNom();
                    String nomArrivee = arc.getArrivee().getNom();

                    if (arc.getDepart() instanceof Carrefour) {
                        nomDepart = ((Carrefour)arc.getDepart()).getNomBase();
                    }
                    if (arc.getArrivee() instanceof Carrefour) {
                        nomArrivee = ((Carrefour)arc.getArrivee()).getNomBase();
                    }

                    String cleArc = nomDepart + "->" + nomArrivee;

                    if (ramassage) {
                        arcsRamassage.add(cleArc);
                    } else {
                        arcsSansRamassage.add(cleArc);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("digraph G {\n");
        sb.append("\trankdir=LR;\n");
        sb.append("\tnode [shape=box, style=rounded];\n");
        sb.append("\tedge [fontsize=10];\n\n");

        // Définir les styles des noeuds
        Set<String> noeudsAffiches = new HashSet<>();
        for (Noeud noeud : ville.getNoeuds()) {
            String nom = noeud.getNom();

            if (noeud instanceof Carrefour) {
                Carrefour c = (Carrefour) noeud;
                if (c.getRueCourante() != null) continue;
                nom = c.getNomBase();
            }

            if (noeudsAffiches.contains(nom)) continue;
            noeudsAffiches.add(nom);

            sb.append("\t\"").append(nom).append("\" [");

            if (noeud instanceof Entrepot) {
                sb.append("shape=diamond, style=filled, fillcolor=\"#87CEEB\", color=\"#4169E1\", penwidth=3");
            } else if (noeud instanceof Immeuble) {
                sb.append("shape=box, style=\"rounded,filled\", fillcolor=\"#FFFACD\", color=\"#DAA520\"");
            } else if (noeud instanceof Maison) {
                sb.append("shape=box, style=\"rounded,filled\", fillcolor=\"#90EE90\", color=\"#228B22\"");
            } else if (noeud instanceof Carrefour) {
                sb.append("shape=circle, style=filled, fillcolor=\"#D3D3D3\", color=\"#696969\"");
            }

            sb.append("];\n");
        }

        sb.append("\n");

        // Dessiner tous les arcs normaux
        for (Noeud noeud : ville.getNoeuds()) {
            for (Arc arc : noeud.getArcsSortants()) {
                if (arc.estChangementRue()) continue;

                String nomDepart = arc.getDepart().getNom();
                String nomArrivee = arc.getArrivee().getNom();

                if (arc.getDepart() instanceof Carrefour) {
                    nomDepart = ((Carrefour) arc.getDepart()).getNomBase();
                }
                if (arc.getArrivee() instanceof Carrefour) {
                    nomArrivee = ((Carrefour) arc.getArrivee()).getNomBase();
                }

                String cleArc = nomDepart + "->" + nomArrivee;

                if (!arcsVisites.contains(cleArc)) {
                    arcsVisites.add(cleArc);

                    sb.append("\t\"").append(nomDepart).append("\"");
                    sb.append(" -> ");
                    sb.append("\"").append(nomArrivee).append("\"");
                    sb.append(" [label=\"").append(arc.getRue()).append("\"");
                    sb.append(", color=\"#D3D3D3\", penwidth=1");
                    sb.append("];\n");
                }
            }
        }

        // Dessiner les arcs parcourus SANS ramassage (en bleu)
        if (!arcsSansRamassage.isEmpty()) {
            sb.append("\n\t// === ARCS PARCOURUS SANS RAMASSAGE ===\n");
            for (int i = 0; i < arcsSansRamassage.size(); i++) {
                String[] parts = arcsSansRamassage.get(i).split("->");
                sb.append("\t\"").append(parts[0]).append("\"");
                sb.append(" -> ");
                sb.append("\"").append(parts[1]).append("\"");
                sb.append(" [color=\"#0000FF\", penwidth=3");
                sb.append("];\n");
            }
        }

        // Dessiner les arcs AVEC ramassage (en rouge)
        if (!arcsRamassage.isEmpty()) {
            sb.append("\n\t// === ARCS AVEC RAMASSAGE ===\n");
            for (int i = 0; i < arcsRamassage.size(); i++) {
                String[] parts = arcsRamassage.get(i).split("->");
                sb.append("\t\"").append(parts[0]).append("\"");
                sb.append(" -> ");
                sb.append("\"").append(parts[1]).append("\"");
                sb.append(" [label=\"RAMASSAGE\", color=\"#FF0000\", penwidth=5, fontcolor=\"#FF0000\"");
                sb.append("];\n");
            }
        }

        sb.append("}\n");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(nomFichier))) {
            writer.write(sb.toString());
            System.out.println("\n✅ Graphe de tournée complète enregistré dans " + nomFichier);
            afficherInstructionsVisualisation(nomFichier);
        } catch (IOException e) {
            System.err.println("❌ Erreur d'enregistrement : " + e.getMessage());
        }
    }

    /**
     * Affiche les instructions pour visualiser le graphe
     */
    private static void afficherInstructionsVisualisation(String nomFichier) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("📊 COMMENT VISUALISER LE GRAPHE :");
        System.out.println("=".repeat(70));
        System.out.println("1. Ouvrir le fichier : " + nomFichier);
        System.out.println("2. Sélectionner TOUT (Ctrl + A) + Copier (Ctrl + C)");
        System.out.println("3. Aller sur : https://dreampuf.github.io/GraphvizOnline/");
        System.out.println("4. Dans l'éditeur de gauche (noir) :");
        System.out.println("   - Sélectionner TOUT (Ctrl + A)");
        System.out.println("   - Supprimer (Touche Retour Arrière)");
        System.out.println("   - Coller votre graphe (Ctrl + V)");
        System.out.println("\n" + "=".repeat(70));
        System.out.println("🎨 LÉGENDE DU GRAPHE :");
        System.out.println("=".repeat(70));
        System.out.println("🔷 Losange bleu      = Entrepôt (point de départ/retour)");
        System.out.println("⬤  Cercle gris       = Carrefour");
        System.out.println("🟢 Rectangle vert    = Maison (ramassage 2 min)");
        System.out.println("🟡 Rectangle jaune   = Immeuble (ramassage 5 min)");
        System.out.println("━  Trait gris        = Rue non utilisée");
        System.out.println("━  Trait ROUGE épais = Chemin parcouru par le camion");
        System.out.println("━  Trait BLEU        = Déplacement sans ramassage (tournée complète)");
        System.out.println("🔢 Numéros rouges    = Ordre de passage du camion");
        System.out.println("=".repeat(70) + "\n");
    }

    /**
     * Méthode simplifiée pour exporter depuis le menu
     */
    public static void exporterTourneeVersDot(GrapheVille ville, Itineraire itineraire) {
        exporterTourneeVersDot(ville, itineraire, "graphe_parcours.txt");
    }
}