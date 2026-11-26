import java.util.Random;


//On aurait pu utiliser une API ici pour obtenir des cours réels mais les demandes
//gratuites par jour étaient limitées -> obligations d'espacer plus les mises à jour.
//Pour les tests nous devions donc créer une mise à jour artificielle et nous avons
//Préferé l'ameliorer et nous pencher sur un modèle mathématique solide par curiosité


//Classe qui va gérer le thread de mise à jour des cours -> doit obligatoirement implémenter runnable donc
public class MajCoursThread implements Runnable{

    private Serveur serveur;                   //Serveur a MAJ
    private int intervalleMillis;              //temps entre maj en ms
    private Random generateurAleatoire;        //Besoin d'aléatoire dans le modèle

    //Constructeur mis à jour
    public MajCoursThread(Serveur serveur, int intervalleMillis){
        this.serveur = serveur;
        this.intervalleMillis = intervalleMillis;
        this.generateurAleatoire = new Random();
    }

    //Calcul du prix à l'aide du modèle 
    public double calculerNouveauPrix(double prixActuel, double mu, double sigma, double dtAnnee){
        double dt = dtAnnee;

        double termeStochastique = sigma * Math.sqrt(dt) * generateurAleatoire.nextGaussian();
        double termeDerive = (mu - (sigma * sigma) / 2.0) * dt;

        double nouveauPrix = prixActuel * Math.exp(termeDerive + termeStochastique);

        return nouveauPrix;
    }

    //run pour le thread (logique mise à jour)
    public void run(){
        final double SECONDES_PAR_AN = 365.25;
        final double dtAnnee = (intervalleMillis / 1000.0) / SECONDES_PAR_AN;

        while (true){
            try{
                Thread.sleep(intervalleMillis);

                synchronized (serveur.getStockGlobal()){ // Accès synchronisé à la ressource du Serveur
                    // On récupère la liste d'actions via le getter du Serveur
                    for (Action action : serveur.getActions()){ 
                        double mu = action.getTypeAction().getMu(); // Puis des paramètres avec le getter d'ActionType dans Action
                        double sigma = action.getTypeAction().getSigma();

                        double nouveauPrix = Math.floor(100*calculerNouveauPrix(action.getPrix(), mu, sigma, dtAnnee)) / 100.0;

                        // La modification des prix se fait directement sur l'objet Action
                        // qui est une clé de la Map stockGlobal du Serveur
                        action.setPrix(nouveauPrix);
                    }
                }

            } catch (InterruptedException e){
                System.out.println("Thread de mise à jour des prix interrompu.");
                break;
            }
        }
    }
}
