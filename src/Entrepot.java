class Entrepot extends Noeud {
    public Entrepot(String nom, double x, double y) {
        super(nom, x, y);
    }

    @Override
    public double getTempsTraitement() {
        return 0.0;
    }

    @Override
    public boolean estPointArret() {
        return true;
    }

    @Override
    public String toString() {
        return "[ENTREPOT] " + nom;
    }
}
