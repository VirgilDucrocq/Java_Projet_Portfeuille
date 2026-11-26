#!/bin/bash

# --- Variables ---
SERVER_PID=""
CLIENTS_DIR="clients"
SERVER_HIST_BIN="historique_binaire.ser"
SERVER_STOCK_BIN="stock_binaire.ser"
SERVER_TRANS_TXT="transactions_lisibles.txt"

# --- Fonction de Nettoyage (Appelée à la sortie - Trap) ---
cleanup() {
    echo ""
    echo "--- Arrêt du système ---"
    
    if [ -n "$SERVER_PID" ]; then
        echo "Arrêt du processus Serveur (PID : $SERVER_PID)"
        kill -15 "$SERVER_PID" 2>/dev/null
        sleep 1
        kill -9 "$SERVER_PID" 2>/dev/null
    fi
    
    if ls *.class 1> /dev/null 2>&1; then
        rm -f *.class
    fi
    
    echo "Fermeture complète"
}

# --- Fonction de Reset ---
reset_data() {
    echo ""
    echo "ATTENTION : Réinitialisation complète (perte des clients et de l'historique de transactions, remise à 0 des stocks"
    echo -n "Êtes-vous sûr ? (o/n) : "
    read -r confirm
    
    if [ "$confirm" = "o" ]; then
        if [ -n "$SERVER_PID" ]; then
            echo "Arrêt immédiat du serveur"
            kill "$SERVER_PID" 2>/dev/null
            # On vide la variable pour que le 'cleanup' final ne tente pas de le retuer
            SERVER_PID=""
        fi

        # on supprime les fichiers (maintenant que le serveur est mort)
        echo "Suppression des fichiers de données"
        rm -f "$SERVER_HIST_BIN"
        rm -f "$SERVER_STOCK_BIN"
        rm -f "$SERVER_TRANS_TXT"
        if [ -d "$CLIENTS_DIR" ]; then
            rm -rf "$CLIENTS_DIR"/*
        fi
        
        echo "Données effacées avec succès"
        return 0
    else
        echo "Annulation du reset"
        return 1
    fi
}

# Piège cleanup
trap cleanup EXIT

# --- Fonction Menu ---
afficher_menu() {
    echo ""
    echo "--- Menu de Gestion Serveur/Client ---"
    echo "1) Lancer un Client "
    echo "2) Afficher l'état des processus Java"
    echo "3) Arrêter et Quitter"
    echo "4) Réinitialiser les données et quitter"
    echo "---------------------------------------"
    echo -n "Votre choix : "
}

# --- Compilation ---
echo "Étape 1 : Compilation"
rm -f *.class
javac *.java

if [ $? -ne 0 ]; then
    echo "Erreur de compilation"
    exit 1
fi

# --- Lancement Serveur ---
echo "Étape 2 : Lancement du Serveur"
java Serveur &
SERVER_PID=$!
echo "Serveur lancé (PID : $SERVER_PID)"
sleep 2

# --- Boucle Principale ---
while true; do
    afficher_menu
    read -r choix

    case $choix in
        1)
            echo "Lancement Client"
            java Client &
            ;;
        2)
            echo "--- Processus Java ---"
            ps aux | grep java | grep -E 'Serveur|Client' | grep -v grep
            ;;
        3)
            exit 0
            ;;
        4)
            reset_data
            # $? nous dit si on reset ou pas
            if [ $? -eq 0 ]; then
                exit 0 # Quitte le script, ce qui déclenche le trap cleanup et kill le serveur
            fi
            ;;
        *)
            echo "Choix invalide"
            ;;
    esac
done
