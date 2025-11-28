import java.time.LocalDateTime;
import java.io.*;
import java.net.*;
import java.util.*;
import java.time.temporal.ChronoUnit;


//Serializable car c'est ce qu'on va majoritairement s'envoyer à travers le reseau 
public class Transaction implements Serializable{
    
    //Attributs
    private static final long serialVersionUID = 1L;
    private int quantite;
    private TypeTransaction typeTransaction;
    private LocalDateTime dateHeure;
    private String clientNom ;         
    private Action action; 
    private boolean valide ;

    //Constructeur (pas de constructeur sans paramètres, on doit bien tout spécifier)
    public Transaction(String clientNom, Action action, int quantite, TypeTransaction typeTransaction, LocalDateTime dateHeure){
        this.clientNom = clientNom;
        this.action = action;
        this.quantite = quantite;
        this.typeTransaction = typeTransaction;
        this.dateHeure = dateHeure;
        this.valide = false;
    }

    // Getters 
    public int getQuantite(){
        return quantite;
    }

    public TypeTransaction getTypeTransaction(){
        return typeTransaction;
    }

    public String getClientNom(){ 
        return clientNom;
    }
    
    public Action getAction(){
        return action; 
    }

    public LocalDateTime getDateHeure(){
        return dateHeure;
    }

    public boolean estAcceptee(){
        return valide; 
    }
  
    //Setters
    public void setQuantite(int quantite){
        this.quantite = quantite;
    }

    public void setTypeTransaction(TypeTransaction typeTransaction){
        this.typeTransaction = typeTransaction;
    }

    public void setDateHeure(LocalDateTime dateHeure){
        this.dateHeure = dateHeure;
    }

    public void setValide(boolean valide){
        this.valide = valide;
    }


    //Méthode toString

    public String toString(){
        LocalDateTime heureTronquee = this.dateHeure.truncatedTo(ChronoUnit.SECONDS);
        return this.clientNom + " " + this.typeTransaction + " " + quantite + " x " + action.getNom() + " à " + action.getPrix() + " le " + heureTronquee; 
    }

    
}
