import java.time.LocalDateTime;
import java.io.*;
import java.net.*;
import java.util.*;
import java.time.temporal.ChronoUnit;


//Serializable car c'est ce qu'on va majoritairement s'envoyer à travers le reseau 
public class Transaction implements Serializable{
    
    //Attributs (beaucoup en final car immuables, on ne les modifiera pas)
    private static final long serialVersionUID = 1L;
    private final int quantite;
    private final TypeTransaction typeTransaction;
    private final LocalDateTime dateHeure;
    private final String clientNom ;         
    private final Action action; 
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
  
    //Setters (pas de setter à part valide, une transaction ne doit pas 
    //être modifiée après création)

    public void setValide(boolean valide){
        this.valide = valide;
    }


    //Méthode toString

    public String toString(){
        LocalDateTime heureTronquee = this.dateHeure.truncatedTo(ChronoUnit.SECONDS);
        return this.clientNom + " " + this.typeTransaction + " " + quantite + " x " + action.getNom() + " à " + action.getPrix() + " le " + heureTronquee; 
    }

    
}
