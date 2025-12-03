
class Maison extends Noeud {
    private static final double TEMPS_RAMASSAGE = 2.0;

    public Maison(String nom, double x, double y) {
        super(nom, x, y);
    }

    @Override
    public double getTempsTraitement() {
        return TEMPS_RAMASSAGE;
    }

    @Override
    public boolean estPointArret() {
        return true;
    }

    @Override
    public String toString() {
        return "[MAISON] " + nom;
    }
}
