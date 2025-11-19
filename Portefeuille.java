import java.util.*;
import java.io.*;
import java.util.stream.Collectors;

public class Portefeuille implements Serializable{
    
    // Attributs

    private Client proprietaire;
    private double soldeDispo;
    private Map<Action, Integer> portefeuille; // Action + Quantité

    // Constructeur
    public Portefeuille(Client proprietaire, double soldeInitial){
        if (soldeInitial < 0) {
            throw new IllegalArgumentException("Le solde initial doit être positif");
        }
        this.proprietaire = proprietaire;
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

    public String toMarketValueString(List<Action> actionsMarche) {
    
        Map<String, Double> prixMarche = new HashMap<>();
        for (Action a : actionsMarche) {
            prixMarche.put(a.getName(), a.getPrix());
        }

        StringBuilder sb = new StringBuilder();
        double valeurTotaleActions = 0.0;
    
        // --- En-tête Général ---
        sb.append(String.format("╔═══════════════════════════════════════════════════╗\n"));
        sb.append(String.format("║ PORT. DE %s | SOLDE DISPO: %.2f €\n", 
                                proprietaire.getName().toUpperCase(), soldeDispo));
        sb.append(String.format("╠═══════════════════════════════════════════════════╣\n"));

        // --- En-tête des Colonnes ---
        sb.append(String.format("║ %-10s | %-5s | %-8s | %-8s | %-10s ║\n", 
                                "ACTION", "QTÉ", "PRIX ACHAT", "PRIX ACTUEL", "VALEUR ACT."));
        sb.append(String.format("╠═══════════════════════════════════════════════════╣\n"));

        // --- Lignes de Données ---
        for (Map.Entry<Action, Integer> entry : portefeuille.entrySet()) {
            Action actionDetenue = entry.getKey();
            Integer quantite = entry.getValue();
            String nomAction = actionDetenue.getName();
        
            double prixActuel = prixMarche.getOrDefault(nomAction, actionDetenue.getPrix()); 
            double prixAchat = actionDetenue.getPrix();
            double valeurActuelle = prixActuel * quantite;
            valeurTotaleActions += valeurActuelle;

            // Ligne de données formatée
            sb.append(String.format("║ %-10s | %-5d | %-8.2f € | %-8.2f € | %-9.2f € ║\n", 
                                                nomAction, 
                                                quantite, 
                                                prixAchat, 
                                                prixActuel, 
                                                valeurActuelle));
        }
    
        double valeurTotale = soldeDispo + valeurTotaleActions;
    
        // --- Pied de Page ---
        sb.append(String.format("╠═══════════════════════════════════════════════════╣\n"));
        sb.append(String.format("║ VALEUR TOTALE (Solde + Actions) : %.2f €", valeurTotale));
        sb.append(String.format("\n╚═══════════════════════════════════════════════════╝"));

        return sb.toString();
    }

    public synchronized void retirerAction(Action action, int quantite){
        if (!portefeuille.containsKey(action)){
            throw new IllegalArgumentException("L'action n'existe pas dans le portefeuille");
        }
        int quantiteCourrante = portefeuille.get(action);

        if (quantite <= 0 || quantite > quantiteCourrante){
            throw new IllegalArgumentException("Quantité invalide (<= 0 ou plus granqe que la capacité détenue)");
        }

        portefeuille.put(action, quantiteCourrante - quantite );

        // Suppession de l'action si on n'en a plus
        if (portefeuille.get(action) == 0){
            portefeuille.remove(action);
        }
    }

    public String toString(){
        return "Solde disponible: " + this.getSoldeDispo() + "\nValeur totale des actions: " + this.getValeurPortefeuille() + "\n\nActions: " + portefeuille.toString() + "\n" ;
    }
}
