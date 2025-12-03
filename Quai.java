import java.util.*;


class Quai {
    private Maison maison;
    private String ligne;
    private List<Arc> arcsSortants;

    public Quai(Maison maison, String ligne) {
        this.maison = maison;
        this.ligne = ligne;
        this.arcsSortants = new ArrayList<>();
    }

    public Maison getStation() { return maison; }
    public String getLigne() { return ligne; }
    public List<Arc> getArcsSortants() { return arcsSortants; }

    public void ajouterArc(Arc arc) { arcsSortants.add(arc); }


    public String getCle() {
        return maison.getNom() + "_" + ligne;
    }

    @Override
    public String toString() {
        return "Quai " + ligne + " à " + maison.getNom();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quai quai = (Quai) o;
        return maison.getNom().equals(quai.maison.getNom()) && ligne.equals(quai.ligne);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maison.getNom(), ligne);
    }
}