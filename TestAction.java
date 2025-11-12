public class TestAction {
    public static void main(String[] args) {

        // Création de 2 actions
        Action apple = new Action("Apple", 1000.0);
        Action google = new Action("Google", 2000.0);

        // Test affichage initial
        System.out.println("Avant mise à jour :");
        System.out.println(apple);
        System.out.println(google);

        // Test Maj Prix
        try {
            apple.setPrix(1200.0);
            System.out.println("\nMise à jour réussie pour Apple : " + apple);

            //Erreur prix <= 0 ici
            google.setPrix(-500.0);  
            System.out.println("Mise à jour réussie pour Google : " + google);

        } catch (IllegalArgumentException e) {
            System.out.println("\nErreur lors de la mise à jour : " + e.getMessage());
        }

        // Vérification finale de l'état des actions
        System.out.println("\nÉtat final des actions :");
        System.out.println(apple);
        System.out.println(google);
    }
}
