# Projet AMIO - Surveillance des Lumières IoTLab

**Auteurs :** Mouna IDRISSI HASSANI AZAMI & Hedi BAYOUDH


---

## Description du projet

L'application **ProjetAMIO** est une application Android de surveillance temps réel des capteurs de luminosité du réseau IoTLab de TELECOM Nancy. Son objectif principal est de **détecter les lumières laissées actives dans les bureaux en soirée** et d'alerter l'utilisateur via des notifications ou des emails selon des plages horaires configurables.

L'application interroge périodiquement un web service REST exposant les données des capteurs, analyse les valeurs de luminosité, détecte les changements d'état (allumée/éteinte), et envoie des alertes selon les conditions définies par l'utilisateur.

---

## Fonctionnalités implémentées

### 1. **Affichage des capteurs actifs**
- Liste en temps réel des capteurs détectés
- Affichage de la valeur de luminosité pour chaque capteur
- Indicateur visuel (cercle coloré) : vert si lumière allumée, gris si éteinte
- Badge d'état : "ON" ou "OFF"

### 2. **Historique des mesures**
- Cards individuelles pour chaque mesure
- Affichage des informations : numéro de mesure, luminosité, capteur, label, date/heure
- Code couleur selon l'état (allumée/éteinte)
- Défilement vertical pour consulter l'historique complet

### 3. **Service de surveillance en arrière-plan**
- Service foreground avec notification persistante
- Vérification périodique configurable (par défaut : 30 secondes)
- Requêtes HTTP automatiques vers le serveur IoTLab
- Parsing JSON et détection de changements d'état

### 4. **Système de notifications**
- Notifications Android lors de changements d'état
- Plages horaires configurables (par défaut : 19h-23h en semaine)
- Vibration du téléphone lors d'une alerte
- Notifications avec titre, message détaillé et auto-dismiss

### 5. **Envoi d'emails**
- Composition automatique d'email lors de changements d'état
- Conditions d'envoi :
  - Week-end entre 19h et 23h
  - Semaine entre 23h et 6h
- Email pré-rempli avec informations du capteur

### 6. **Configuration complète**
- Menu Settings accessible depuis l'ActionBar
- Paramètres configurables :
  - Adresse email de destination
  - Horaires de début et fin des notifications
  - Activation/désactivation des emails week-end
  - Activation/désactivation des emails nuit
  - Seuil de luminosité pour détecter une lumière allumée
  - Intervalle de vérification du service

### 7. **Démarrage automatique**
- Option de démarrage du service au boot du téléphone
- BroadcastReceiver pour l'événement BOOT_COMPLETED
- Sauvegarde de la préférence dans SharedPreferences

### 8. **Communication Service ↔ Activité**
- BroadcastReceiver pour mise à jour temps réel de l'interface
- Intent avec action personnalisée (ACTION_RESULT)
- Mise à jour automatique des capteurs actifs et de l'interface

---


## Interface utilisateur

### Écran principal

//Screen main screen

L'interface principale comprend :

1. **En-tête**
   - Titre "Surveillance des Lumières"
   - Menu settings (3 points en haut à droite)

2. **Section "Capteurs actifs"**
   - Card affichant la liste des capteurs détectés
   - Pour chaque capteur :
     - Cercle coloré (vert/gris)
     - Nom du capteur (ex: Capteur 9.138)
     - Label (ex: light1)
     - Valeur de luminosité
     - Badge d'état ON/OFF

3. **Section "Informations"**
   - État du service (En cours / Arrêté)
   - Dernière alerte détectée
   - Date de dernière mise à jour

4. **Section "Contrôles"**
   - Switch : Activer le service
   - Switch : Activer les notifications
   - Checkbox : Démarrer au boot

5. **Bouton "Vérifier maintenant"**
   - Lance une requête HTTP immédiate
   - Affiche un Toast avec le résultat

6. **Section "Historique des mesures"**
   - Cards individuelles pour chaque mesure
   - Informations détaillées par mesure
   - Défilement vertical

### Design

- **Couleurs** : Palette cohérente (bleu primaire, vert/gris pour états)
- **Cards** : Material Design avec élévation et coins arrondis
- **Typography** : Textes hiérarchisés (titres, labels, valeurs)
- **Icônes visuels** : Cercles colorés au lieu d'emojis
- **Responsive** : ScrollView pour adaptation à toutes tailles d'écran

---

## Configuration et préférences

### Menu Settings

//Screen settings screen

Accessible via le menu (⋮) en haut à droite de l'écran principal.

### Paramètres disponibles

**Configuration Email**
- **Adresse email** : Email de destination pour les alertes
  - Type : EditTextPreference
  - Valeur par défaut : brstuvh@gmail.com

**Horaires des Notifications**
- **Heure de début** : Début de la plage horaire (0-23)
  - Type : EditTextPreference (nombre)
  - Valeur par défaut : 19
- **Heure de fin** : Fin de la plage horaire (0-23)
  - Type : EditTextPreference (nombre)
  - Valeur par défaut : 23

**Envoi d'Emails**
- **Email le week-end** : Envoyer email si changement entre 19h-23h le week-end
  - Type : SwitchPreference
  - Valeur par défaut : activé
- **Email la nuit** : Envoyer email si changement entre 23h-6h en semaine
  - Type : SwitchPreference
  - Valeur par défaut : activé

**Paramètres de Détection**
- **Seuil de luminosité** : Valeur au-dessus de laquelle la lumière est considérée allumée
  - Type : EditTextPreference (nombre)
  - Valeur par défaut : 500
- **Intervalle de vérification** : Fréquence de vérification en secondes
  - Type : EditTextPreference (nombre)
  - Valeur par défaut : 30

### Persistence des données

- Stockage : **SharedPreferences**
- Fichier : `com.example.projetamio_preferences`
- Lecture au démarrage du service
- Application immédiate des changements

---

##  Détection et notifications

### Algorithme de détection

1. **Collecte des données**
   - Requête HTTP vers l'API IoTLab
   - Parsing du JSON (format : `{"data":[{...}]}`)
   - Extraction : timestamp, label, value, mote

2. **Analyse de l'état**
   - Comparaison `value > seuil` → lumière allumée/éteinte
   - Stockage dans HashMap : `Map<String, Boolean> previousLightStates`

3. **Détection du changement**
```java
   if (previousState != null && previousState != isLightOn) {
       // Changement détecté !
   }
```

4. **Actions déclenchées**
   - Mise à jour de l'interface (BroadcastReceiver)
   - Notification Android (si horaires respectés)
   - Email (si conditions week-end/nuit respectées)
   - Vibration du téléphone

### Système de notifications

**Canaux de notification (Android 8.0+)**
- `MainServiceChannel` : Notification foreground du service (importance LOW)
- `LightChangeChannel` : Alertes de changement d'état (importance HIGH)

**Contenu de la notification**
- **Titre** : " Lumière allumée" ou " Lumière éteinte"
- **Message** : "Mote [ID] ([label]) : [valeur]"
- **Icône** : Icône Android standard
- **Action** : Auto-dismiss au clic

**Conditions d'envoi**
- Vérification de la plage horaire configurée
- Par défaut : 19h-23h en semaine
- Logs détaillés pour débogage


### Envoi d'emails

**Mécanisme**
- Utilisation de `Intent.ACTION_SEND`
- Type : `text/plain`
- Data : `mailto:`
- Ouverture du chooser d'apps email

**Contenu de l'email**
```
À : [email configuré]
Objet :  Lumière allumée /  Lumière éteinte

Détection de changement d'état :

Mote : 9.138
Label : light1
Valeur : 542.86
État : ALLUMÉE  / ÉTEINTE 

Détecté à : 18/02/2026 16:37:31
```

**Conditions d'envoi**
- Week-end (samedi/dimanche) entre 19h et 23h
- Semaine entre 23h et 6h
- Vérification via `Calendar.DAY_OF_WEEK` et `HOUR_OF_DAY`


---

## Limitations et tests

### Limitations techniques

**Accès réseau**
L'application nécessite une connexion au serveur IoTLab de TELECOM Nancy :
- URL : `http://iotlab.telecomnancy.eu:8080/iotlab/rest/data/1/light1/last`
- **Accès restreint** : Serveur accessible uniquement depuis le réseau de l'école ou via VPN

### Tests effectués

 **Tests réalisés avec succès**
- Interface utilisateur (affichage, navigation, scroll)
- Requêtes HTTP avec URL de test publique (`jsonplaceholder.typicode.com`)
- Parsing JSON et création de cards dynamiques
- Affichage des capteurs actifs
- Communication Service ↔ Activité via BroadcastReceiver
- Sauvegarde et lecture des préférences (SharedPreferences)
- Navigation vers le menu Settings
- Création et affichage de l'historique des mesures

### Tests non effectués

 **Tests impossibles à réaliser**

**Raison principale : Problème de connectivité VPN**

L'accès au serveur IoTLab nécessite le VPN de TELECOM Nancy. Malgré plusieurs tentatives, le VPN n'a pas pu être configuré correctement depuis mon domicile, rendant impossible l'accès au serveur réel.

**Fonctionnalités non testées en conditions réelles :**


1. **Notifications en soirée (19h-23h)**
   - Les tests ont été effectués en journée
   - Impossible de vérifier le comportement exact aux horaires configurés
   - Logique implémentée et vérifiée par revue de code

2. **Emails le week-end (19h-23h)**
   - Tests effectués en semaine
   - Conditions de week-end non testées en situation réelle
   - Logique de détection de jour (Calendar.DAY_OF_WEEK) implémentée

3. **Emails la nuit (23h-6h)**
   - Tests effectués en journée
   - Impossible de tester aux horaires nocturnes
   - Vérification de la logique horaire par analyse de code



## Conclusion

Ce projet a permis de mettre en pratique les concepts fondamentaux du développement Android :
- Architecture d'application avec Services
- Communication inter-composants
- Gestion du cycle de vie Android
- Requêtes HTTP et parsing JSON
- Interface utilisateur Material Design
- Persistance de données
- Notifications et interactions utilisateur

Malgré les limitations liées à l'accès VPN, l'application est **fonctionnelle et prête à être déployée** dès que l'accès au réseau IoTLab sera disponible. La structure du code permet une maintenance et une évolution faciles.

