import java.io.*;
import java.net.*;
import java.time.*;

public class Client implements Serializable {

    private String name;
    private Socket socket;
    private Portefeuille portefeuille;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public Client(String name, double soldeInitial){
        this.name = name;
        this.portefeuille = new Portefeuille(this, soldeInitial);
    }

    public String getName(){
        return this.name;
    }

    public Portefeuille getPortefeuille(){
        return this.portefeuille;
    }


    public void seConnecter(String host, int port){
        try {
            socket = new Socket(host, port);

            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println(this.getName() + " connecté à " + host + ":" + port);
        } catch (IOException e) {
            System.err.println("Erreur de connexion: " + e.getMessage());
        }
    }


    public void demanderAchat(Action action, int quantite){
        Transaction demande = new Transaction(this.getName(), action, quantite, TypeTransaction.ACHAT, LocalDate.now());
        Transaction resultat = envoyerTransaction(demande);

        if (resultat != null && resultat.estAcceptee()) { //Si on a une réponse positive (verifié côté serveur)

            double cout = action.getPrix() * quantite;

            //Verification solde côté client
            if (portefeuille.getSoldeDispo() >= cout) {
                portefeuille.ajouterAction(action, quantite);
                portefeuille.setSoldeDispo(portefeuille.getSoldeDispo() - cout);
                System.out.println("Achat validé côté client : " + quantite + " x " + action.getName());
            } else {
                System.out.println("Achat refusé côté client : solde insuffisant");
            }

        } else {
            System.out.println("Achat refusé côté serveur : stock insuffisant");
        }
    }

    public void demanderVente(Action action, int quantite){
        Transaction demande = new Transaction(this.getName(), action, quantite, TypeTransaction.VENTE, LocalDate.now());
        Transaction resultat = envoyerTransaction(demande);

        if (resultat != null && resultat.estAcceptee()) { //Si on a une réponse positive (verifié côté serveur)

            int possede = portefeuille.getPortefeuille().getOrDefault(action, 0);

            //verification quantité suffisante dans le portefeuille client
            if (possede >= quantite) {
                portefeuille.retirerAction(action, quantite);
                portefeuille.setSoldeDispo(portefeuille.getSoldeDispo() + action.getPrix() * quantite);
                System.out.println("Vente validée côté client : " + quantite + " x " + action.getName()+ "");
            } else {
                System.out.println("Vente refusée côté client : pas assez d'actions");
            }

        } else {
            System.out.println("Vente refusée côté serveur");
        }
    }


    public Transaction envoyerTransaction(Transaction t) {
        try {
            System.out.println("\n[Client] Envoi transaction: " + t);
            out.writeObject(t);
            out.flush();

            System.out.println("[Client] Attente réponse serveur");
            Transaction result = (Transaction) in.readObject();
            System.out.println("[Client] Réponse reçue: " + result + " " + (result.estAcceptee()? "ACCEPTEE":"REFUSEE"));

            return result;

        } catch (Exception e) {
            System.err.println("Erreur d’envoi transaction : " + e.getMessage());
            return null;
        }
    }
}