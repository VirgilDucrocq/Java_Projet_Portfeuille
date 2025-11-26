import java.util.*;
import java.io.*;
import java.util.stream.Collectors;

public class Portefeuille implements Serializable{
    
    // Attributs (client proprietaire, solde et actions détenues)

    //Protection contre la corruption à la deserialisation
    private static final long serialVersionUID = 1L;
    private Client proprietaire;
    private double soldeDispo;
    //On n'envoie pas de map
    transient private Map<Action, Integer> portefeuille; // Action + Quantité

    // Constructeur
    // On initialise le proprietaire en dehors comme c'est lui qui va 
    // Creer le portefeuille on va avoir un potentiel this-escape
    public Portefeuille(double soldeInitial){
        if (soldeInitial < 0) {
            throw new IllegalArgumentException("Le solde initial doit être positif");
        }
        this.soldeDispo = soldeInitial;
        this.portefeuille = new HashMap<>();
    }


    // Getters

    public Client getProprietaire(){
        return this.proprietaire;
    }
    
    
    public double getSoldeDispo(){
        return soldeDispo;
    }

    public Map<Action, Integer> getPortefeuille(){
        return portefeuille;
    }

    public int getCompteurNbActions(){
        int total = 0;
        for (int q : portefeuille.values()) {
            total += q; //Somme de toutes les quantités d'actions
        }
        return total;
    }

    
    public double getValeurPortefeuille(){
        double valeurActions = 0.0;
        for (Map.Entry<Action, Integer> entree : portefeuille.entrySet()){
            valeurActions += entree.getKey().getPrix() * entree.getValue(); //Pour chaque entrée de la HashMap on fait Prix*Quantité
        }
        return valeurActions;
    }

    


    // Setter

    public void setProprietaire(Client client){
        this.proprietaire = client;
    }

    public void setSoldeDispo(double nvSolde){
        this.soldeDispo = nvSolde;
    }

    // Méthodes
    public synchronized void ajouterAction(Action action, int quantite){
        if (quantite <= 0){
            throw new IllegalArgumentException("La quantité doit être stt positive");
        }
        //Si on en a déjà on rajoute sinon on crée la quantité
        portefeuille.put(action, portefeuille.containsKey(action) ? portefeuille.get(action) + quantite : quantite);
    }

    


    public synchronized void retirerAction(Action action, int quantite){
        if (!portefeuille.containsKey(action)){
            throw new IllegalArgumentException("L'action n'existe pas dans le portefeuille");
        }
        int quantiteCourrante = portefeuille.get(action);

        if (quantite <= 0 || quantite > quantiteCourrante){
            throw new IllegalArgumentException("Quantité invalide (<= 0 ou plus grande que la capacité détenue)");
        }

        portefeuille.put(action, quantiteCourrante - quantite );

        // Suppession de l'action si on n'en a plus
        if (portefeuille.get(action) == 0){
            portefeuille.remove(action);
        }
    }

    
}
