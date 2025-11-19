import java.util.List;
import java.util.Map;
import java.util.Random;

public class MajCoursThread implements Runnable {

    private Map<Action, Integer> stockGlobal; 
    private List<Action> actions;              // Liste des actions
    private int intervalMillis;                // temps entre maj en ms
    private Random random;

    public MajCoursThread(List<Action> actions, Map<Action, Integer> stockGlobal, int intervalMillis){
        this.actions = actions;
        this.stockGlobal = stockGlobal;
        this.intervalMillis = intervalMillis;
        this.random = new Random();
    }

    public double calculNouveauPrix(double S, double mu, double sigma, double dtSeconds){
    double dt = dtSeconds;

    double stochTerm = sigma * Math.sqrt(dt) * random.nextGaussian();
    double driftTerm = (mu - (sigma * sigma) / 2.0) * dt;

    double nouveauPrix = S * Math.exp(driftTerm + stochTerm);

    return nouveauPrix;
}

    public void run(){
        // Conversion de l'intervalle de temps en secondes (dt)
        final double SECONDS_PER_YEAR = 60.0 * 60.0 * 24.0 * 365.25;
        final double dtAnnee = (intervalMillis / 1000.0) / SECONDS_PER_YEAR;

        while (true){
            try{
                Thread.sleep(intervalMillis);

                synchronized (stockGlobal){
                    for (Action action : actions){
                        double mu = 0.1;  //Taux de croissance annuelle espérée
                        double sigma = 0.2;  //Volatilité annuelle espérée

                        double nouveauPrix = calculNouveauPrix(action.getPrix(), mu, sigma, dtAnnee);

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
