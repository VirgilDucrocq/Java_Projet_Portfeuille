import java.util.List;
import java.util.Map;
import java.util.Random;

//Classe qui va gérer le thread de mise à jour des cours -> doit obligatoirement implémenter runnable donc
public class MajCoursThread implements Runnable {

    private Map<Action, Integer> stockGlobal; 
    private List<Action> actions;              // Liste des actions
    private int intervalMillis;                // temps entre maj en ms
    private Random random;                     // Besoin d'aléatoire

    //Constructeur
    public MajCoursThread(List<Action> actions, Map<Action, Integer> stockGlobal, int intervalMillis){
        this.actions = actions;
        this.stockGlobal = stockGlobal;
        this.intervalMillis = intervalMillis;
        this.random = new Random();
    }

    //Calcul du prix à l'aide du modèle
    public double calculNouveauPrix(double S, double mu, double sigma, double dtSeconds){
    double dt = dtSeconds;

    double stochTerm = sigma * Math.sqrt(dt) * random.nextGaussian();
    double driftTerm = (mu - (sigma * sigma) / 2.0) * dt;

    double nouveauPrix = S * Math.exp(driftTerm + stochTerm);

    return nouveauPrix;
}

    //run pour le thread 
    public void run(){
        // Conversion de l'intervalle de temps en secondes (dt) 
        final double SECONDS_PER_YEAR = 365.25;
        final double dtAnnee = (intervalMillis / 1000.0) / SECONDS_PER_YEAR;

        while (true){
            try{
                Thread.sleep(intervalMillis);

                synchronized (stockGlobal){
                    for (Action action : actions){
                        double mu = action.getMu(); // Taux de croissance annuel espéré
                        double sigma = action.getSigma(); // Volatilité annuelle espérée

                        double nouveauPrix = Math.floor(100*calculNouveauPrix(action.getPrix(), mu, sigma, dtAnnee)) / 100.0;

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
 
