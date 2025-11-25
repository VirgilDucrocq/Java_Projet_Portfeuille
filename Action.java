import java.util.ArrayList;
import java.io.*;
import java.util.Objects;

//###############
//#Classe Action#
//###############

//Serializable car on va avoir besoin de les transférer via réseau (actions inlues dans les transactions)
public class Action implements Serializable {
    
    private String name;
    private double prix;
    private ArrayList<Double> historiqueValeurs;
    private final ActionType type; // Differents type d'actions + ou - stables 
    private final double mu; 
    private final double sigma; 
    
    //Constructeur (pas de constructeur par défaut sans paramètres, on veut obligatoirement nom, prix et type)
    public Action(String name, double prix, ActionType type){
        if (prix <= 0) {
            throw new IllegalArgumentException("Le prix initial doit être supérieur à 0");
        }
        this.name = name;
        this.prix = prix;
        this.historiqueValeurs = new ArrayList<>();
        this.historiqueValeurs.add(prix);
        this.type = type;
        this.mu = type.getMu();
        this.sigma = type.getSigma();
    }

    //Getters
    public int getNombrePrix(){
        return this.historiqueValeurs.size();
    }

    public String getName(){
        return this.name;
    }

    public double getPrix(){
        return this.prix;
    }

    public ArrayList<Double> getHistoriqueValeurs(){
        return this.historiqueValeurs;
    }

    public double getMu() {
        return mu;
    }

    public double getSigma() {
        return sigma;
    }
    
    public ActionType getType() {
        return type;
    }

    //Setter de prix pour pouvoir actualiser
    public void setPrix(double prix){
        if (prix <= 0) {
            throw new IllegalArgumentException("Le prix initial doit être supérieur à 0");
        }
        this.prix = prix;
        this.historiqueValeurs.add(prix);

    }

    //ToString pour l'affichage
    public String toString() {
        return ("Action: " + name + "\nPrix: " + prix);
    }


    // Redefinition de Equals et Hashcode par nom seulement
    // On veut que les actions soient considérées comme les mêmes du moment que le nom est le même
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Action)) return false;
        Action action = (Action) o;
        return Objects.equals(name, action.name); // juste le nom
    }

    public int hashCode() {
        return Objects.hash(name);
    }



}
