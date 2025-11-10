import java.io.*;
import java.net.*;
import java.util.*;

public class Serveur{

    private int port;
    private ServerSocket serverSocket;
    private Map<Action, Integer> stockGlobal; // Actions + Quantités
    private List<ThreadClientServeur> clients = new ArrayList<>(); //Clients

    public Serveur(int port, Map<Action, Integer> stockInitial){
        this.port = port;
        this.stockGlobal = new HashMap<>(stockInitial);
    }

    public void demarrer(){
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Serveur démarré sur le port " + port);

            while (true){
                Socket socketClient = serverSocket.accept();
                ThreadClientServeur tcs = new ThreadClientServeur(socketClient, this);
                clients.add(tcs);
                new Thread(tcs).start();
            }

        } catch (IOException e){
            System.err.println("Erreur serveur : " + e.getMessage());
        }
    }

    
    public synchronized void traiterTransaction(Transaction t){
        Action action = t.getAction();
        int qte = t.getQuantite();

        if (t.getTypeTransaction() == TypeTransaction.ACHAT){
            if (stockGlobal.get(action) >= qte){
                stockGlobal.put(action, stockGlobal.get(action) - qte);
                t.setValide(true);
            } else{
                t.setValide(false);
            }
        } else{ 
            stockGlobal.put(action, stockGlobal.get(action) + qte);
            t.setValide(true);
        }
    }

    public int getStock(Action a){
        return stockGlobal.getOrDefault(a, 0);
    }
}
