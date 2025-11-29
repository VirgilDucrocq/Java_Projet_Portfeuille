// Aide de l'IA ici car nous n'avons pas eu le temps de prendre vraiment en main la librairie
// (Surtout une aide au niveau de la gestion des classes internes / gestion des couleurs, tables etc)

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.border.Border;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;



public class ClientGUI extends JFrame{

    //Herite d'une classe serializable
    private static final long serialVersionUID = 1L;
    private Client client;

    // Constantes CardLayout
    private static final String CARD_CONNEXION = "CONNEXION";
    private static final String CARD_SIMULATEUR = "SIMULATEUR";
    
    // UI Principale
    private CardLayout cardLayout;
    private JPanel cardPanel;

    // Composants du simulateur
    private JLabel clientNameLabel;
    private JLabel soldeLabel; // Pour afficher le solde dans le bandeau
    private PortefeuilleTableModel portefeuilleTableModel;
    private JTable portefeuilleTable;
    private ActionTableModel actionTableModel;
    private JTable actionsTable;
    private GraphiquePrix graphiquePanel;
    //Utilise juste pour selectionner un action pour laquelle on affiche le graphique
    private Action actionGraphiqueCourante = null;
    
    // Composants d'interaction 
    private JPanel interactionPanel;
    private JLabel actionSelectionLabel;
    private JButton acheterBtn;
    private JButton vendreBtn;
    private JButton connecterMarcheBtn;

    // COULEURS et STYLES POUR L'ESTHÉTIQUE
    public static final Color BG_DARK = new Color(30, 30, 30); // Gris très foncé
    public static final Color BG_MEDIUM = new Color(45, 45, 45); // Gris moyen pour les conteneurs
    public static final Color FG_LIGHT = new Color(220, 220, 220); // Blanc cassé
    public static final Color ACCENT_GREEN = new Color(0, 180, 0); // Vert pour la finance
    public static final Color ACCENT_RED = new Color(255, 60, 60); // Rouge pour la perte
    public static final Color ACCENT_BLUE = new Color(0, 120, 255); // Bleu pour l'information
    private static final Font FONT_MONO = new Font("Monospaced", Font.PLAIN, 14);
    private static final Font FONT_HEADER = new Font("Arial", Font.BOLD, 14);
    private static final Font FONT_BAND = new Font("Arial", Font.BOLD, 20); // Pour le bandeau supérieur

    //Constructeur
    @SuppressWarnings("this-escape")
    //Le warning vient du setTitle qui est inoffensif 
    public ClientGUI(){
        //Titre
        setTitle("Client Boursier - Marchés en Temps Réel");

        //On essaie d'appliquer le style nimbus qui est plus joli
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {}

        //Taille max
        setExtendedState(JFrame.MAXIMIZED_BOTH); 
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Utilisation de CardLayout sur le panneau principal
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        add(cardPanel, BorderLayout.CENTER);
        
        // Initialisation des deux vues
        cardPanel.add(createConnexionPanel(), CARD_CONNEXION);
        cardPanel.add(createSimulatorPanel(), CARD_SIMULATEUR);
        
        // Afficher l'écran de connexion au démarrage
        cardLayout.show(cardPanel, CARD_CONNEXION);

        setVisible(true);
    }
    
    //Création des écrans 

    //Crée le panneau de connexion initial 
    private JPanel createConnexionPanel(){
        //Cree le panel
        JPanel panel = new JPanel(new GridBagLayout()); 
        panel.setBackground(BG_DARK);
        
        JPanel content = new JPanel(new GridLayout(3, 2, 10, 10)); 
        content.setBackground(BG_MEDIUM);
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        //champs à remplir
        JTextField nomField = new JTextField(20);
        JTextField capitalField = new JTextField("1000", 20);

        JLabel nomLabel = new JLabel("Nom du Client :");
        nomLabel.setForeground(FG_LIGHT);
        nomLabel.setFont(FONT_HEADER);
        JLabel capitalLabel = new JLabel("Capital Initial (€) :");
        capitalLabel.setForeground(FG_LIGHT);
        capitalLabel.setFont(FONT_HEADER);
        
        nomField.setBackground(BG_DARK);
        nomField.setForeground(ACCENT_GREEN);
        capitalField.setBackground(BG_DARK);
        capitalField.setForeground(ACCENT_GREEN);

        //Bouttons
        JButton initierBtn = new JButton("Créer/Charger Client");


        content.add(nomLabel);
        content.add(nomField);
        content.add(capitalLabel);
        content.add(capitalField);
        content.add(initierBtn);
        
        JLabel titleLabel = new JLabel("SIMULATEUR DE MARCHÉ BOURSIER", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 30));
        titleLabel.setForeground(FG_LIGHT);
        
        JPanel mainLayout = new JPanel(new BorderLayout(20, 20));
        mainLayout.setBackground(BG_DARK);
        mainLayout.setBorder(BorderFactory.createEmptyBorder(100, 200, 100, 200));
        mainLayout.add(titleLabel, BorderLayout.NORTH);
        mainLayout.add(content, BorderLayout.CENTER);
        
        panel.add(mainLayout); 
        
        //Actions à declencher si appuie du boutton creer/charger client
        initierBtn.addActionListener(e -> handleInitialization(nomField.getText(), capitalField.getText()));
        return panel;
        }
    

    //Gère le chargement local du client (création ou chargement depuis fichier) et passe à l'écran du simulateur sans connexion réseau
    private void handleInitialization(String nom, String capitalStr) {
        if (nom.trim().isEmpty()) {
        JOptionPane.showMessageDialog(this, "Veuillez entrer un nom de client.");
            return;
        }

        Client clientCharge = Client.chargerClient(nom);

        if (clientCharge != null) {
        //cas 1 on a chargé un client
        this.client = clientCharge;
        JOptionPane.showMessageDialog(this, "Client chargé : " + nom);

        } else {
            // cas 2 c'est un nouveau client crée
            double capital;
            try {
                capital = Double.parseDouble(capitalStr);
                if (capital <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Capital initial invalide ou manquant. Entrez un capital > 0 pour créer un nouveau client.");
                return; // S'arrête ici si le capital est mauvais
            }
    
            this.client = new Client(nom, capital);
            this.client.getPortefeuille().setProprietaire(this.client);
            JOptionPane.showMessageDialog(this, "Nouveau client créé : " + nom);
        }

        // sécurité, exécuté uniquement si un client a été créé ou chargé avec succès
        if (this.client != null) {
            // Transition vers l'interface du simulateur
            clientNameLabel.setText("Client: " + client.getNom());
    
            // bien passer en mode hors ligne et mettre à jour l'affichage
            setSimulatorState(false); 
            updateSimulatorDisplay(); 
    
            cardLayout.show(cardPanel, CARD_SIMULATEUR);
        }
    }

     //Crée le panneau principal du simulateur (avec Bandeau et JTabbedPane)
     
    private JPanel createSimulatorPanel() {
        JPanel simulatorPanel = new JPanel(new BorderLayout());
        simulatorPanel.setBackground(BG_DARK);
        
        //Bandeau Supérieur (Nom du client, Solde, Déconnexion)
        JPanel topBanner = new JPanel(new BorderLayout(20, 0));
        topBanner.setBackground(BG_MEDIUM);
        topBanner.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        //Labels d'information
        clientNameLabel = new JLabel("Client: (Non connecté)");
        clientNameLabel.setForeground(FG_LIGHT);
        clientNameLabel.setFont(FONT_BAND);
        
        soldeLabel = new JLabel("Solde Disponible: 0.00 € | Valeur Totale: 0.00 €");
        soldeLabel.setForeground(ACCENT_BLUE);
        soldeLabel.setFont(FONT_BAND);
        
        //Bouton de déconnexion
        JButton deconnecterBtn = new JButton("Déconnexion");
        deconnecterBtn.addActionListener(e -> handleDisconnection());

        connecterMarcheBtn = new JButton("Connexion au Marché");
        connecterMarcheBtn.addActionListener(e -> handleNetworkConnection(connecterMarcheBtn));

        JPanel buttonGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonGroup.setBackground(BG_MEDIUM);
        buttonGroup.add(connecterMarcheBtn);
        buttonGroup.add(deconnecterBtn);

        topBanner.add(clientNameLabel, BorderLayout.WEST);
        topBanner.add(soldeLabel, BorderLayout.CENTER);
        topBanner.add(buttonGroup, BorderLayout.EAST);

        
        simulatorPanel.add(topBanner, BorderLayout.NORTH);
        
        //Zone Tabulée (Centre)
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(FONT_HEADER);
        tabbedPane.setBackground(BG_MEDIUM.darker());
        tabbedPane.setForeground(FG_LIGHT);
        
        //Onglet 1: Marché et Graphique 
        tabbedPane.addTab("Marché et Actions", createMarketPanel());
        
        //Onglet 2: Portefeuille 
        tabbedPane.addTab("Mon Portefeuille", createPortefeuillePanel());
        
        simulatorPanel.add(tabbedPane, BorderLayout.CENTER);

        return simulatorPanel;
    }
    
    //Gestion de l'action du bouton "se connecter"
    private void handleNetworkConnection(JButton btn) {
        if (client == null) return;

        try {
            // Tenter la connexion (serveur en dur, on pourrait changer cela)
            client.seConnecter("localhost", 5001);
            client.synchroniserStockMarche(); 

            // Lancer la MAJ des prix
            client.lancerMiseAJourActions(this::updateSimulatorDisplay); 

            //Activer les actions réseau
            setSimulatorState(true);
            JOptionPane.showMessageDialog(this, "Connexion au marché réussie !");
        
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erreur de connexion au marché : " + ex.getMessage());
        }
    }

    private void setSimulatorState(boolean connected) {
        // Mise à jour de l'affichage du statut
        if (connected) {
            clientNameLabel.setText("Client: " + client.getNom() + " (EN LIGNE)");
        } else {
            clientNameLabel.setText("Client: " + client.getNom() + " (HORS-LIGNE)");
        }
    
        //Activation/Désactivation des fonctionnalités en ligne
        actionsTable.setEnabled(connected); // Rendre la table des actions interactive
        acheterBtn.setEnabled(connected && (actionGraphiqueCourante != null));
        vendreBtn.setEnabled(connected && (actionGraphiqueCourante != null));
    
        //Les boutons sont visibles, mais leur état 'enabled' dépend de la connexion
        //(et du stock/portefeuille, géré dans handleActionSelection)
    
        //Gestion de l'affichage des données
        if (connecterMarcheBtn != null) {
            // Le bouton de connexion doit être visible et actif uniquement si nous sommes hors-ligne
            connecterMarcheBtn.setVisible(!connected);
        }
        if (!connected) {
            //En mode hors-ligne, la table des actions est vidée/cachée
            actionTableModel.clearData();
            graphiquePanel.setHistorique(null);
            actionSelectionLabel.setText("Connectez-vous pour voir les prix du marché.");
        
            //Mettre à jour le solde (Portefeuille est toujours consultable)
            Portefeuille portefeuille = client.getPortefeuille();
            double soldeDispo = portefeuille.getSoldeDispo();
            soldeLabel.setText(String.format("Solde Disponible: %.2f € | Valeur Totale: N/A (Hors-Ligne)", soldeDispo));

        } else {
            //Mettre à jour l'affichage complet (appellera synchroniserStockMarche)
            //L'appel à updateSimulatorDisplay() sera fait après la connexion
        }
    
        revalidate();
        repaint();
    }

    private JPanel createMarketPanel() {
        JPanel marketPanel = new JPanel(new BorderLayout(10, 10));
        marketPanel.setBackground(BG_DARK);
        marketPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        //Tableau des actions 
        actionTableModel = new ActionTableModel();
        actionsTable = new JTable(actionTableModel);
        
        //esthetique
        actionsTable.setFont(FONT_MONO);
        actionsTable.setBackground(BG_MEDIUM);
        actionsTable.setForeground(FG_LIGHT);
        actionsTable.setSelectionBackground(BG_MEDIUM.brighter());
        actionsTable.setSelectionForeground(Color.WHITE);
        actionsTable.setRowHeight(25);
        
        //Alignement des colonnes (Prix et Stock)
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        actionsTable.getColumnModel().getColumn(1).setCellRenderer(rightRenderer); 
        actionsTable.getColumnModel().getColumn(2).setCellRenderer(rightRenderer); 
        
        JScrollPane scrollActions = new JScrollPane(actionsTable);
        scrollActions.setBorder(createTitledBorder("ACTIONS DISPONIBLES EN TEMPS RÉEL", FG_LIGHT));
        
        //Listener pour la sélection de ligne (pour le graphique et les boutons)
        actionsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && actionsTable.getSelectedRow() != -1) {
                handleActionSelection(actionsTable.getSelectedRow());
            }
        });
        
        //Panneau de droite (Graphique + Interaction)
        JPanel eastPanel = new JPanel(new BorderLayout(10, 10));
        eastPanel.setPreferredSize(new Dimension(550, 0));
        eastPanel.setBackground(BG_DARK);

        graphiquePanel = new GraphiquePrix();
        graphiquePanel.setBackground(BG_MEDIUM); 
        graphiquePanel.setPreferredSize(new Dimension(450, 400)); 
        graphiquePanel.setBorder(createTitledBorder("HISTORIQUE (Sélectionnez une action)", FG_LIGHT));
        
        //Panneau d'interaction (Acheter/Vendre)
        interactionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        interactionPanel.setBackground(BG_MEDIUM.darker());
        interactionPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        
        actionSelectionLabel = new JLabel("Sélectionnez une action pour interagir.");
        actionSelectionLabel.setForeground(FG_LIGHT);
        actionSelectionLabel.setFont(FONT_HEADER);
        
        acheterBtn = new JButton("ACHETER 1");
        vendreBtn = new JButton("VENDRE 1");
        
        acheterBtn.setEnabled(false);
        vendreBtn.setEnabled(false);
        
        interactionPanel.add(actionSelectionLabel);
        interactionPanel.add(acheterBtn);
        interactionPanel.add(vendreBtn);
        
        eastPanel.add(graphiquePanel, BorderLayout.CENTER);
        eastPanel.add(interactionPanel, BorderLayout.SOUTH);
        
        //Assemblage final du Market Panel
        marketPanel.add(scrollActions, BorderLayout.CENTER);
        marketPanel.add(eastPanel, BorderLayout.EAST);
        
        return marketPanel;
    }
     //Crée le panneau de l'onglet "Mon Portefeuille" 
     
    private JPanel createPortefeuillePanel() {
        JPanel portefeuillePanel = new JPanel(new BorderLayout(10, 10));
        portefeuillePanel.setBackground(BG_DARK);
        portefeuillePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        //Tableau des actions détenues
        portefeuilleTableModel = new PortefeuilleTableModel();
        portefeuilleTable = new JTable(portefeuilleTableModel);
        
        //Style du JTable
        portefeuilleTable.setFont(FONT_MONO);
        portefeuilleTable.setBackground(BG_MEDIUM);
        portefeuilleTable.setForeground(FG_LIGHT);
        portefeuilleTable.setSelectionBackground(BG_MEDIUM.brighter());
        portefeuilleTable.setRowHeight(25);
        
        //centrer et colorer
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        
        //variation de prix (colonne 5)
        DefaultTableCellRenderer variationRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.RIGHT);
                
                double variation = (Double) value;
                //Affichage en vert ou rouge selon une pente ascendante ou descendante du cours
                if (variation > 0) {
                    label.setForeground(ACCENT_GREEN);
                    label.setText("+" + label.getText());
                } else if (variation < 0) {
                    label.setForeground(ACCENT_RED);
                } else {
                    label.setForeground(FG_LIGHT);
                }
                return label;
            }
        };

        //Application des renderers
        for (int i = 1; i <= 4; i++) {
            portefeuilleTable.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
        }
        portefeuilleTable.getColumnModel().getColumn(5).setCellRenderer(variationRenderer);

        JScrollPane scrollPortefeuille = new JScrollPane(portefeuilleTable);
        scrollPortefeuille.setBorder(createTitledBorder("ACTIONS DÉTENUES", FG_LIGHT));

        portefeuillePanel.add(scrollPortefeuille, BorderLayout.CENTER);
        
        return portefeuillePanel;
    }
    
    
   // MAJ et Interactions
    
    // Gère la déconnexion et le retour à l'écran de connexion
    private void handleDisconnection() {
        if (client != null) {
            client.sauvegarderClient(); 
            client.deconnecter();
            //Nettoyer la sélection du marché après la déconnexion
            actionsTable.getSelectionModel().clearSelection();
            actionGraphiqueCourante = null; 
            setSimulatorState(false);
            JOptionPane.showMessageDialog(this, "Déconnexion du marché réussie, consultation du portefeuille en mode Hors-Ligne");


        }
    }

    
    //Gère la sélection d'une ligne dans le JTable des actions disponibles
    private void handleActionSelection(int selectedRow) {
        // Convertit l'index de vue en index de modèle 
        int modelRow = actionsTable.convertRowIndexToModel(selectedRow);
        ActionStock as = actionTableModel.getActionStockAt(modelRow);
        
        if (as == null) return;
        
        Action a = as.action;
        
        //Mise à jour du graphique
        actionGraphiqueCourante = a;
        graphiquePanel.setHistorique(a.getHistoriqueValeurs());
        graphiquePanel.setBorder(createTitledBorder("HISTORIQUE DE " + a.getNom(), FG_LIGHT));
        
        //Mise à jour du panneau d'interaction
        actionSelectionLabel.setText(String.format("Action: %s (%.2f €) | Stock Marché: %d", 
                                                    a.getNom(), a.getPrix(), as.stock));
        
        acheterBtn.setEnabled(as.stock > 0); 
        vendreBtn.setEnabled(true);
        
        //Retrait des anciens listeners 
        for (java.awt.event.ActionListener al : acheterBtn.getActionListeners()) {
            acheterBtn.removeActionListener(al);
        }
        for (java.awt.event.ActionListener al : vendreBtn.getActionListeners()) {
            vendreBtn.removeActionListener(al);
        }

        //Ajout des nouveaux listeners (Achat/Vente)
        acheterBtn.addActionListener(e -> {
            boolean marketUpdateNeeded = client.demanderAchat(a, 1);
            if (marketUpdateNeeded) {
                client.synchroniserStockMarche(); 
            }
            updateSimulatorDisplay(); 
        });

        vendreBtn.addActionListener(e -> {
            boolean marketUpdateNeeded = client.demanderVente(a, 1);
            if (marketUpdateNeeded) {
                client.synchroniserStockMarche(); 
            }
            updateSimulatorDisplay(); 
        });
    }


    
    //Met à jour le bandeau, la table des actions et le portefeuille.
     
    public void updateSimulatorDisplay() {
        // Impossible si pas de client
        if (client == null) return;

        // Sécuriser les references (Garanti non-null)
        Portefeuille portefeuilleClient = client.getPortefeuille();
    
        // Sécurisation du portefeuille client (la Map interne)
        Map<Action, Integer> clientPortefeuille;
        if (portefeuilleClient == null || portefeuilleClient.getPortefeuille() == null) {
            clientPortefeuille = Collections.emptyMap(); 
        } else {
            clientPortefeuille = portefeuilleClient.getPortefeuille();
        }

        // Sécurisation du stock marché
        Map<Action, Integer> stockMarche = client.getDernierStockDisponible();
        if (stockMarche == null) {
            stockMarche = Collections.emptyMap();
        }
        // --------------------------------------------------------

        //Calculs securisés (Utilisation de portefeuilleClient après vérification)
    
        // Détermination sécurisée du solde et de la valeur (si portefeuilleClient est null, solde = 0)
        double soldeDispo = 0.0;
        double valeurTotale = 0.0;

        if (portefeuilleClient != null) {
        soldeDispo = portefeuilleClient.getSoldeDispo();
        
        // Calculer la valeur totale uniquement si nous avons des données marché (pour getValeurPortefeuille)
        if (!stockMarche.isEmpty()) {
                valeurTotale = soldeDispo + portefeuilleClient.getValeurPortefeuille();
            } else {
                // Si hors-ligne, la valeur totale est basée uniquement sur le solde
                // (Pour ne pas inidquer une fausse valeur si les prix ont chuté/augmenté après la deconnexion)
                valeurTotale = soldeDispo;
            }
        }
    
        //MAJ GUI

        //Mise à jour du Bandeau
        if (!stockMarche.isEmpty() && portefeuilleClient != null) {
            // En ligne : Afficher la valeur totale réelle
            soldeLabel.setText(String.format("Solde Disponible: %.2f € | Valeur Totale: %.2f €", soldeDispo, valeurTotale));
        } else {
            // Hors-ligne ou portefeuille null : Afficher seulement le solde disponible
            soldeLabel.setText(String.format("Solde Disponible: %.2f € | Valeur Totale: N/A (Hors-Ligne)", soldeDispo));
        }
    
        // Mise à jour de la Table des actions disponibles
        actionTableModel.setData(stockMarche);
    
        // Mise à jour de la Table du Portefeuille (clientPortefeuille est garanti non-null)
        portefeuilleTableModel.setData(clientPortefeuille, stockMarche);
        // Mise à jour du graphique en temps réel (si une action est sélectionnée)
        if (actionGraphiqueCourante != null) {
            Action updatedAction = stockMarche.keySet().stream()
                .filter(a -> a.getNom().equals(actionGraphiqueCourante.getNom()))
                .findFirst()
                .orElse(null);

            if (updatedAction != null) {
                actionGraphiqueCourante = updatedAction;
                graphiquePanel.setHistorique(actionGraphiqueCourante.getHistoriqueValeurs());
                graphiquePanel.setBorder(createTitledBorder("HISTORIQUE DE " + actionGraphiqueCourante.getNom(), FG_LIGHT));
                
                 int currentStock = stockMarche.getOrDefault(updatedAction, 0);

                actionSelectionLabel.setText(String.format("Action: %s (%.2f €) | Stock Marché: %d", 
                                                            actionGraphiqueCourante.getNom(), 
                                                            actionGraphiqueCourante.getPrix(),
                                                            currentStock));
                acheterBtn.setEnabled(currentStock > 0);
            }
        }
        
        revalidate();
        repaint();
    }
    
    //Méthodes additionelles

    //Bordure titrée stylée
    private Border createTitledBorder(String title, Color color) {
        return BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(color.darker(), 1), 
            title, 
            0, 0, 
            FONT_HEADER, 
            color
        );
    }
    

//Classe interne panneau pour graphique

// Rendu static pour couper le lien implicite avec l'instance ClientGUI (qui herite d'une classe serializable)
// Ceci évite des avertissements de sérialisation + solidifie la securité 
// (On ne gère pas vraiment la mémoire, on a surtout voulu éviter les warnings ici)
static class GraphiquePrix extends JPanel {
    
    private static final long serialVersionUID = 1L;
    transient private List<Double> historique;
    private static final int MARGIN = 30; 
    private static final int POINT_SIZE = 6; 

    //On garde seulement les 10 dernières valeurs
    public void setHistorique(List<Double> historique) {
        if(historique != null) {
            this.historique = historique.size() > 10 ? historique.subList(historique.size() - 10, historique.size()) : historique;
        } else {
            this.historique = null;
        }
        repaint();
    }

    
    protected void paintComponent(Graphics g) {
        //Héritage pour le constructeur
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(getBackground());
        g2d.fillRect(0, 0, getWidth(), getHeight());

        //On veut au moins 2 points
        if (historique == null || historique.size() < 2) {
            g2d.setColor(ClientGUI.FG_LIGHT);
            g2d.drawString("Sélectionnez une action ou en attente de données (min 2 points)", MARGIN, getHeight() / 2);
            return;
        }

        int width = getWidth();
        int height = getHeight();

        
        double minPrix = historique.stream().mapToDouble(d -> d).min().orElse(0.0);
        double maxPrix = historique.stream().mapToDouble(d -> d).max().orElse(0.0);
        //Etendue des valeurs
        double range = maxPrix - minPrix;
        
        // Grille de fond 
        g2d.setColor(ClientGUI.BG_DARK.brighter().brighter()); 
        int numGrids = 5;
        for (int i = 1; i < numGrids; i++) {
            int yGrid = MARGIN + i * (height - 2 * MARGIN) / numGrids;
            g2d.drawLine(MARGIN, yGrid, width - MARGIN, yGrid);
        }
        
        // Axes et Labels
        g2d.setColor(ClientGUI.FG_LIGHT); 
        g2d.drawLine(MARGIN, MARGIN, MARGIN, height - MARGIN); 
        g2d.drawLine(MARGIN, height - MARGIN, width - MARGIN, height - MARGIN); 
        
        g2d.drawString(String.format("%.2f", maxPrix), 5, MARGIN + 5);
        g2d.drawString(String.format("%.2f", minPrix), 5, height - MARGIN);

        // Points et lignes
        int nPoints = historique.size();
        
        int prevX = 0;
        int prevY = 0;

        for (int i = 0; i < nPoints; i++) {
            double prix = historique.get(i);
            
            int x = MARGIN + i * (width - 2 * MARGIN) / (nPoints - 1);
            
            int y;
            if (range == 0) { 
                y = height - MARGIN - (height - 2 * MARGIN) / 2; 
            } else {
                y = height - MARGIN - (int) ((prix - minPrix) / range * (height - 2 * MARGIN));
            }
            
            // Correction de la logique de couleur: Comparer au point précédent
            if (i > 0) {
                double prixPrecedent = historique.get(i - 1);
                Color lineColor = (prix >= prixPrecedent) ? ClientGUI.ACCENT_GREEN : ClientGUI.ACCENT_RED;
                
                g2d.setColor(lineColor);
                g2d.setStroke(new BasicStroke(2)); 
                g2d.drawLine(prevX, prevY, x, y);
            }

            // Dessiner le point
            g2d.setColor(Color.CYAN);
            g2d.fillOval(x - POINT_SIZE / 2, y - POINT_SIZE / 2, POINT_SIZE, POINT_SIZE);
            
            // Afficher le prix (dernier point)
            if (i == nPoints - 1) {
                g2d.setColor(ClientGUI.FG_LIGHT);
                g2d.drawString(String.format("%.2f", prix), x - 10, y - 10);
            }
            
            prevX = x;
            prevY = y;
        }
    }
}


// classes internes pour modèle de données et pour le JTable des Actions disponibles


static class ActionStock {
    public final Action action;
    public final int stock;

    //Constructeur
    public ActionStock(Action action, int stock) {
        this.action = action;
        this.stock = stock;
    }
}

// Rendu static aussi pour les mêmes raisons que l'autre classe interne
static class ActionTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    transient private List<ActionStock> actionStocks;
    private final String[] columnNames = {"Action", "Prix (€)", "Stock Marché"};
    
    public ActionTableModel() {
        this.actionStocks = new ArrayList<>(); 
    }
    
    public void setData(Map<Action, Integer> stockMarche) {
        this.actionStocks = stockMarche.entrySet().stream()
                .map(entry -> new ActionStock(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(as -> as.action.getNom()))
                .collect(Collectors.toList());
        fireTableDataChanged();
    }

    //Vider les données
    public void clearData() {
        this.actionStocks = new ArrayList<>();
        fireTableDataChanged();
    }
    
    public ActionStock getActionStockAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < actionStocks.size()) {
            return actionStocks.get(rowIndex);
        }
        return null;
    }


    public int getRowCount() {
        return actionStocks.size();
    }

 
    public int getColumnCount() {
        return columnNames.length;
    }

   
    public String getColumnName(int col) {
        return columnNames[col];
    }
    
    //Obtenir le type d'objet qu'on manipule
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 1) return Double.class; 
        if (columnIndex == 2) return Integer.class; 
        return String.class; 
    }

    
    public Object getValueAt(int rowIndex, int columnIndex) {
        ActionStock as = actionStocks.get(rowIndex);
        
        switch (columnIndex) {
            case 0: return as.action.getNom();
            case 1: return as.action.getPrix();
            case 2: return as.stock; 
            default: return null;
        }
    }
}


// classe interne pour modèle de données et pour pour le JTable du Portefeuille 

static class ActionPortefeuille {
    public final Action action;
    public final int quantite;
    public final double prixAchat;
    public final double valeurActuelle;
    public final double variation; // Variation % ou abs par rapport au prix d'achat

    //constructeur
    public ActionPortefeuille(Action action, int quantite, double prixAchat, double prixActuel) {
        this.action = action;
        this.quantite = quantite;
        this.prixAchat = prixAchat;
        this.valeurActuelle = prixActuel * quantite;
        // Calcul de la variation en pourcentage
        this.variation = (prixActuel / prixAchat - 1.0) * 100.0;
    }
}

//static pour les mêmes raisons
static class PortefeuilleTableModel extends AbstractTableModel {
    
    private static final long serialVersionUID = 1L;
    transient private List<ActionPortefeuille> actionsDetenues;
    private final String[] columnNames = {"Action", "Quantité", "Prix Achat (€)", "Prix Actuel (€)", "Valeur Totale (€)", "Variation (%)"};
    
    public PortefeuilleTableModel() {
        this.actionsDetenues = new ArrayList<>(); 
    }
    
    
     //Met à jour les données du tableau avec la Map <Action, Quantité détenue> et les prix du marché.
     //La Map stockMarche est utilisée pour récupérer l'objet Action le plus récent (avec les prix actuels)
     
    public void setData(Map<Action, Integer> portefeuilleDetenu, Map<Action, Integer> stockMarche) {
        this.actionsDetenues = portefeuilleDetenu.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> {
                    Action actionDetenue = entry.getKey();
                    int quantite = entry.getValue();
                    
                    // Trouver l'action correspondante dans le stock marché pour le prix actuel
                    Action actionMarche = stockMarche.keySet().stream()
                        .filter(a -> a.getNom().equals(actionDetenue.getNom()))
                        .findFirst()
                        .orElse(actionDetenue); // Utilise l'ancienne si non trouvée (normalment n'arrive pas)
                        
                    double prixAchatSupposed = actionDetenue.getPrix(); 
                    
                    return new ActionPortefeuille(
                        actionMarche, 
                        quantite, 
                        prixAchatSupposed, // Le prix de la première transaction d'achat est utilisé ici
                        actionMarche.getPrix()
                    );
                })
                .sorted(Comparator.comparing(ap -> ap.action.getNom()))
                .collect(Collectors.toList());
        fireTableDataChanged();
    }
    
    public void clearData() {
        this.actionsDetenues = new ArrayList<>();
        fireTableDataChanged();
    }

    
    public int getRowCount() {
        return actionsDetenues.size();
    }


    public int getColumnCount() {
        return columnNames.length;
    }

  
    public String getColumnName(int col) {
        return columnNames[col];
    }
  
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex >= 1) return Double.class; // Quantité, prix, valeur, variation sont des nombres
        return String.class; 
    }

    
    public Object getValueAt(int rowIndex, int columnIndex) {
        ActionPortefeuille ap = actionsDetenues.get(rowIndex);
        
        switch (columnIndex) {
            case 0: return ap.action.getNom();
            case 1: return ap.quantite;
            case 2: return ap.prixAchat;
            case 3: return ap.action.getPrix();
            case 4: return ap.valeurActuelle;
            case 5: return ap.variation;
            default: return null;
        }
    }
}

}
