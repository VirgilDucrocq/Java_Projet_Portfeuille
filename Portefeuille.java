import java.util.*;
import java.io.*;
import java.util.stream.Collectors;
import java.util.HashMap;

public class Portefeuille implements Serializable{
    
    // Attributs (client proprietaire, solde et actions détenues)

    //Protection contre la corruption à la deserialisation
    private static final long serialVersionUID = 1L;
    private Client proprietaire;
    private double soldeDispo;
    private Map<Action, Integer> portefeuille = new HashMap<>(); // Action + Quantité

    // Constructeur
    // On rattache le proprietaire au portefeuille en dehors du consructeur comme c'est une instance client qui va 
    // appeler la création du portefeuille on aurait un potentiel "this-escape" sinon
    public Portefeuille(double soldeInitial){
        if (soldeInitial < 0) {
            throw new IllegalArgumentException("Le solde initial doit être positif");
        }
        this.soldeDispo = soldeInitial;
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

    
    public double getMontantTotalInvesti(){
        double totalInvesti = 0.0;
        for (Map.Entry<Action, Integer> entree : portefeuille.entrySet()){
            // entree.getKey().getPrix() est ici le PRIX MOYEN D'ACHAT
            totalInvesti += entree.getKey().getPrix() * entree.getValue(); 
        }
        return totalInvesti;
    }

    


    // Setter

    //Rattachement du proprio
    public void setProprietaire(Client client){
        this.proprietaire = client;
    }

    public void setSoldeDispo(double nvSolde){
        this.soldeDispo = nvSolde;
    }

    // Méthodes
    public synchronized void ajouterAction(Action actionNouvelle, int quantiteAjoutee){
        if (quantiteAjoutee <= 0) throw new IllegalArgumentException("La quantité doit être > 0");

        // Cas 1 : Nouvelle action, on l'ajoute simplement avec son prix actuel
        if (!portefeuille.containsKey(actionNouvelle)) {
            portefeuille.put(actionNouvelle, quantiteAjoutee);
        } 
        // Cas 2 : On possède déjà l'action, on doit lisser le prix 
        // sinon la clef reste la même et on a un prix d'achat faux
        else {
            Action actionAncienne = null;
            // On retrouve l'objet Action qui sert de clef avec l'ancien prix
            for (Action a : portefeuille.keySet()) {
                if (a.equals(actionNouvelle)) { 
                    actionAncienne = a;
                    break;
                }
            }

            if (actionAncienne != null) {
                int qteAncienne = portefeuille.get(actionAncienne);
                
                // Calcul du coût total (ce qu'on avait payé avant + ce qu'on paie maintenant)
                double coutTotalAncien = actionAncienne.getPrix() * qteAncienne;
                double coutTotalNouveau = actionNouvelle.getPrix() * quantiteAjoutee;
                
                int nouvelleQteTotal = qteAncienne + quantiteAjoutee;
                
                // nouveau prix = moyenne pondérée
                double nouveauPrixMoyen = (coutTotalAncien + coutTotalNouveau) / nouvelleQteTotal;

                // Mise à jour de la clef dans la Map
                actionAncienne.setPrix(nouveauPrixMoyen);
                portefeuille.put(actionAncienne, nouvelleQteTotal);
            }
        }
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
