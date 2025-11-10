import java.io.*;
import java.net.*;


public class ThreadClientServeur implements Runnable {

    private Socket socket;
    private Serveur serveur;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ThreadClientServeur(Socket socket, Serveur serveur) throws IOException{
        this.socket = socket;
        this.serveur = serveur;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    public void run(){
        try {
            while (true){
                Transaction t = (Transaction) in.readObject();
                System.out.println("[Serveur] Reçu: " + t);
                serveur.traiterTransaction(t);
                out.writeObject(t);
                System.out.println("[Serveur] renvoyé: " + t +" "+ (t.estAcceptee()? "ACCEPTEE":"REFUSEE"));
                out.flush();
            }
        } catch (IOException | ClassNotFoundException e){
            System.out.println("Client déconnecté : " + e.getMessage());
        } 
    }
}
