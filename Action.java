import java.util.ArrayList;
import java.io.*;
import java.util.Objects;


public class Action implements Serializable {
    
    private String name;
    private double prix;
    private double variation;
    private ArrayList<Double> historiqueValeurs;
    
    //Constructeur (pas de constructeur par défaut sans paramètres)
    public Action(String name, double prix){
        if (prix <= 0) {
            throw new IllegalArgumentException("Le prix initial doit être supérieur à 0");
        }
        this.name = name;
        this.prix = prix;
        this.variation = 0.0;
        this.historiqueValeurs = new ArrayList<>();
        this.historiqueValeurs.add(prix);
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

    public double getVariation(){
        return this.variation;
    }

    public ArrayList<Double> getHistoriqueValeurs(){
        return this.historiqueValeurs;
    }

    //Setters
    public void setPrix(double prix){
        if (prix <= 0) {
            throw new IllegalArgumentException("Le prix initial doit être supérieur à 0");
        }
        this.variation = (prix - this.prix) / Math.abs(this.prix);
        this.prix = prix;
        this.historiqueValeurs.add(prix);

    }

    //ToString pour l'affichage
    public String toString() {
        return ("Action: " + name + "\nPrix: " + prix + "\nDernière Variation: " 
            + variation*100 +"%" );
    }


    //Redef Equals et Hashcode par nom seulement

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
