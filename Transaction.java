import java.time.LocalDate;
import java.io.*;
import java.net.*;
import java.util.*;


//Serializable car c'est ce qu'on va majoritairement s'envoyer à travers le reseau 
public class Transaction implements Serializable{
    
    //Attributs
    private static final long serialVersionUID = 1L;
    private int quantite;
    private TypeTransaction typeTransaction;
    private LocalDate date;
    private String clientNom ;         
    private Action action; 
    private boolean valide ;

    //Constructeur (pas de constructeur sans paramètres, on doit bien tout spécifier)
    public Transaction(String clientNom, Action action, int quantite, TypeTransaction typeTransaction, LocalDate date){
        this.clientNom = clientNom;
        this.action = action;
        this.quantite = quantite;
        this.typeTransaction = typeTransaction;
        this.date = date;
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

    public LocalDate getDate(){
        return date;
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

    public void setDate(LocalDate date){
        this.date = date;
    }

    public void setValide(boolean valide){
        this.valide = valide;
    }


    //Méthode

    public String toString(){
        return this.clientNom + " " + this.typeTransaction + " " + quantite + " x " + action.getNom() + " à " + action.getPrix();
    }

    
}
