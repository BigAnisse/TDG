class Arc {
    private Noeud depart;
    private Noeud arrivee;
    private String rue;
    private double duree;
    private boolean estChangementRue;
    private boolean ramassageEffectue; // AJOUT : pour marquer si le ramassage a été fait sur cet arc

    public Arc(Noeud depart, Noeud arrivee, String rue, double duree) {
        this.depart = depart;
        this.arrivee = arrivee;
        this.rue = rue;
        this.duree = duree;
        this.estChangementRue = false;
        this.ramassageEffectue = false;
    }

    public Arc(Noeud depart, Noeud arrivee, double duree) {
        this.depart = depart;
        this.arrivee = arrivee;
        this.rue = "CHANGEMENT";
        this.duree = duree;
        this.estChangementRue = true;
        this.ramassageEffectue = false;
    }

    public Noeud getDepart() { return depart; }
    public Noeud getArrivee() { return arrivee; }
    public String getRue() { return rue; }
    public double getDuree() { return duree; }
    public boolean estChangementRue() { return estChangementRue; }

    // AJOUT : méthodes pour gérer le ramassage
    public boolean estRamassageEffectue() { return ramassageEffectue; }
    public void marquerRamassageEffectue() { this.ramassageEffectue = true; }
    public void reinitialiserRamassage() { this.ramassageEffectue = false; }

    // AJOUT : créer une clé unique pour identifier un arc directionnel
    public String getCleDirectionnelle() {
        return depart.getNom() + "->" + arrivee.getNom() + "[" + rue + "]";
    }

    @Override
    public String toString() {
        if (estChangementRue) {
            return "Changement de rue à " + ((Carrefour)depart).getNomBase() +
                    " (" + String.format("%.1f", duree) + " min)";
        }
        return depart.getNom() + " -> " + arrivee.getNom() +
                " [" + rue + "] (" + String.format("%.1f", duree) + " min)";
    }
}