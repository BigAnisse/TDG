class DirectionCalculator {

    // Calcule l'angle entre deux vecteurs en degrés
    private static double calculerAngle(double x1, double y1, double x2, double y2) {
        double angle = Math.toDegrees(Math.atan2(y2, x2) - Math.atan2(y1, x1));
        // Normaliser l'angle entre -180 et 180
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    // Détermine la direction à prendre
    public static String determinerDirection(Noeud precedent, Noeud carrefour, Noeud suivant) {
        if (precedent == null || carrefour == null || suivant == null) {
            return "";
        }

        // Vecteur d'arrivée (de precedent vers carrefour)
        double dx1 = carrefour.getX() - precedent.getX();
        double dy1 = carrefour.getY() - precedent.getY();

        // Vecteur de sortie (de carrefour vers suivant)
        double dx2 = suivant.getX() - carrefour.getX();
        double dy2 = suivant.getY() - carrefour.getY();

        // Calculer l'angle de rotation
        double angle = calculerAngle(dx1, dy1, dx2, dy2);

        // Déterminer la direction selon l'angle
        if (Math.abs(angle) < 30) {
            return "tout droit";
        } else if (angle > 30 && angle < 150) {
            return "à gauche";
        } else if (angle < -30 && angle > -150) {
            return "à droite";
        } else {
            return "demi-tour";
        }
    }
}