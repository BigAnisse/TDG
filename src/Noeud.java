import java.util.*;

abstract class Noeud {
    protected String nom;
    protected List<Arc> arcsSortants;
    protected double x; // Coordonnée X
    protected double y; // Coordonnée Y

    public Noeud(String nom, double x, double y) {
        this.nom = nom;
        this.x = x;
        this.y = y;
        this.arcsSortants = new ArrayList<>();
    }

    public String getNom() { return nom; }
    public double getX() { return x; }
    public double getY() { return y; }
    public List<Arc> getArcsSortants() { return arcsSortants; }
    public void ajouterArc(Arc arc) { arcsSortants.add(arc); }

    public abstract double getTempsTraitement();
    public abstract boolean estPointArret();

    @Override
    public String toString() {
        return nom + " (" + x + ", " + y + ")";
    }
}