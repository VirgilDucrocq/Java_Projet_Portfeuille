import java.util.ArrayList;
import java.io.*;
import java.util.Objects;

//Serializable car on va avoir besoin de les transférer via réseau (actions inlues dans les transactions)
public class Action implements Serializable{
    
    // Définir une constante pour la taille maximale (éviter les listes infinis comme on actualise très rapidement)
    private static final int TAILLE_MAX_HISTORIQUE = 20;

    //Protection contre la corruption à la deserialisation
    private static final long serialVersionUID = 1L;
    private String nom;
    private double prix;
    private ArrayList<Double> historiqueValeurs;
    private final ActionType typeAction; // Differents type d'actions + ou - stables 
    
    //Constructeur (pas de constructeur par défaut sans paramètres, on veut obligatoirement nom, prix et type)
    public Action(String nom, double prix, ActionType typeAction){
        if (prix <= 0){
            throw new IllegalArgumentException("Le prix initial doit être supérieur à 0");
        }
        this.nom = nom;
        this.prix = prix;
        this.historiqueValeurs = new ArrayList<>();
        this.historiqueValeurs.add(prix);
        this.typeAction = typeAction;
    }

    //Getters
    public int getNombrePrix(){
        return this.historiqueValeurs.size();
    }

    public String getNom(){
        return this.nom;
    }

    public double getPrix(){
        return this.prix;
    }

    public ArrayList<Double> getHistoriqueValeurs(){
        return this.historiqueValeurs;
    }

    public double getMu(){
        return typeAction.getMu();
    }

    public double getSigma(){
        return typeAction.getSigma();
    }
    
    public ActionType getTypeAction(){
        return typeAction;
    }

    //Setter de prix pour pouvoir actualiser
    public void setPrix(double prix){
        if (prix <= 0){
            throw new IllegalArgumentException("Le prix initial doit être supérieur à 0");
        }
        this.prix = prix;
        this.historiqueValeurs.add(prix);

        // FIFO (pour éviter de conserver et serialiser des listes trop grandes)
        if (this.historiqueValeurs.size() > TAILLE_MAX_HISTORIQUE){
        this.historiqueValeurs.remove(0); 
        }
        //Autre piste d'amelioration : ne pas serialiser l'historique (transient)
        //Et faire une methode Client qui le demande juste quand besoin (graphique par exemple)
        // = même logique que le "GET_ACTIONS" de synchronisation des cours
    }

    //ToString pour l'affichage
    public String toString(){
        return ("Action: " + nom + "\nPrix: " + prix);
    }


    // Redefinition de Equals et Hashcode par nom seulement
    // On veut que les actions soient considérées comme les mêmes du moment que le nom est le même
    public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof Action)) return false;
        Action action = (Action) o;
        return Objects.equals(nom, action.nom); // juste le nom
    }

    public int hashCode(){
        return Objects.hash(nom);
    }
}
