import java.util.*;
class Carrefour extends Noeud {
    private Set<String> rues;
    private String rueCourante;

    public Carrefour(String nom, String rue, double x, double y) {
        super(nom + "_" + rue, x, y);
        this.rues = new HashSet<>();
        this.rueCourante = rue;
    }

    public Carrefour(String nom, double x, double y) {
        super(nom, x, y);
        this.rues = new HashSet<>();
        this.rueCourante = null;
    }

    public void ajouterRue(String rue) {
        rues.add(rue);
    }

    public Set<String> getRues() {
        return rues;
    }

    public String getRueCourante() {
        return rueCourante;
    }

    public String getNomBase() {
        if (rueCourante != null) {
            return nom.substring(0, nom.lastIndexOf("_"));
        }
        return nom;
    }

    @Override
    public double getTempsTraitement() {
        return 0.0;
    }

    @Override
    public boolean estPointArret() {
        return true;
    }
}