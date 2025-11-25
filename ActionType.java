//couples de Mu/Sigma cohérents pour differents types d'actions
public enum ActionType {
    INDICE_STABLE(0.08, 0.20),
    VALEUR_TECH_CLASSIQUE(0.10, 0.30),
    ACTION_VOLATILE_CRYPTO(0.15, 0.60),
    OBLIGATION_ETAT(0.03, 0.05);

    
    private final double mu;
    private final double sigma;

    //Consructeur basique
    ActionType(double mu, double sigma) {
        this.mu = mu;
        this.sigma = sigma;
    }

    // Méthodes d'accès (getters)
    public double getMu() {
        return mu;
    }

    public double getSigma() {
        return sigma;
    }

}

