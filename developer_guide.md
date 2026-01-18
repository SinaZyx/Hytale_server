# Guide du Développeur - Fancy Core & Hytale Plugin Development

Ce document est une référence technique destinée aux développeurs souhaitant créer des plugins ou des addons basés sur **Fancy Core** pour le serveur Hytale.

> [!WARNING]
> **Statut de la documentation** : Ce guide est basé sur l'analyse statique du code source v0.0.1 (Janvier 2026). Certaines API internes du jeu (`com.hypixel.hytale...`) sont sujettes à changement.

---

## A. Vue d'ensemble

**Fancy Core** agit comme un framework centralisé ("Core") qui unifie les systèmes essentiels d'un serveur (économie, chat, permissions, données joueurs). Il encapsule l'API native du serveur Hytale pour fournir une couche d'abstraction plus stable et riche aux autres plugins.

### Architecture
- **Noyau (Module `fc-api`)** : Contient toutes les interfaces, événements et objets de données (`FancyPlayer`, `Currency`, `ChatRoom`). C'est la seule dépendance nécessaire pour créer un addon.
- **Implémentation (Module `FancyCore`)** : Contient la logique métier, le stockage (JSON/Mongo), et les commandes.
- **Cycle de vie** : Le plugin est un `JavaPlugin` standard Hytale qui initialise un registre de services (`Service Locator`).

### Dépendances Techniques
- **Java** : Version 25 (Preview/Bleeding Edge).
- **Hytale Server API** : Dépendance locale `HytaleServer.jar`.
- **Système de Build** : Gradle (Kotlin DSL).

---

## B. Concepts & Cycle de Vie

### 1. Point d'Entrée
Tout plugin dépend de `FancyCore` et peut accéder à son API via le singleton `FancyCore.get()`.

```java
// Accès global à l'API
FancyCore api = FancyCore.get();
```

### 2. Services (Service Locator Pattern)
Fancy Core expose ses fonctionnalités via des "Services". Ne jamais instancier les services manuellement.

| Service | Accesseur | Rôle |
| :--- | :--- | :--- |
| **FancyPlayerService** | `api.getPlayerService()` | Gestion des données étendues des joueurs (`FancyPlayer`). |
| **EventService** | `api.getEventService()` | Bus d'événements personnalisé de Fancy Core. |
| **ChatService** | `api.getChatService()` | Canaux, mutes, messagerie privée. |
| **CurrencyService** | `api.getCurrencyService()` | Économie multi-devises. |
| **PermissionService** | `api.getPermissionService()` | Groupes et permissions. |

---

## C. API & Événements (Déduits)

### Objet Clé : `FancyPlayer`
Wrapper autour du `PlayerRef` natif de Hytale.
- **Obtention** : `FancyPlayerService.get().getByUUID(uuid)` ou `get().getOnlinePlayers()`.
- **Méthodes Utiles** :
  - `sendMessage(String)` : Envoi de message.
  - `hasPermission(String)` : Vérification de permission unifiée.
  - `getData()` : Accès aux métadonnées persistantes (`FancyPlayerData`).
  - `switchChatRoom(ChatRoom)` : Changement de canal.

### Bus d'Événements (Custom)
Fancy Core possède son propre système d'événements, séparé de celui de Hytale (`EventRegistry`), pour les événements de haut niveau.

**Événements Clés** :
- `PlayerJoinedEvent` / `PlayerLeftEvent` (Chargement des données FancyPlayer terminé).
- `PlayerChatEvent` (Message chat, cancellable).
- `PrivateMessageSentEvent` (PM).
- `PlayerPunishedEvent` (Ban/Mute/Kick).

---

## D. Guide "How-to" (Recettes)

### 1. Créer une Commande
Les commandes étendent `CommandBase` (API Hytale native).

```java
public class MaCommande extends CommandBase {

    // Définition d'un argument obligatoire
    private final RequiredArg<String> nomArg = this.withRequiredArg("nom", "Description", ArgTypes.STRING);

    public MaCommande() {
        super("moncommande", "Description de la commande");
        requirePermission("monplugin.commande.use"); // Permission requise
    }

    @Override
    protected void executeSync(CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("Seuls les joueurs peuvent faire ça !"));
            return;
        }

        // Récupération de l'argument
        String nom = nomArg.get(ctx);
        
        // Interaction avec Fancy Core
        FancyPlayer fp = FancyPlayerService.get().getByUUID(ctx.sender().getUuid());
        fp.sendMessage("Bonjour " + nom + " !");
    }
}

// Enregistrement (dans onStart)
CommandManager.get().register(new MaCommande());
```

### 2. Écouter un Événement FancyCore
Utilisez `EventService` pour écouter les événements métier.

```java
import com.fancyinnovations.fancycore.api.events.chat.PrivateMessageSentEvent;
import com.fancyinnovations.fancycore.api.events.service.EventListener;

public class MonListener implements EventListener<PrivateMessageSentEvent> {
    @Override
    public void on(PrivateMessageSentEvent event) {
        String message = event.getMessage();
        FancyPlayer sender = event.getSender();
        
        // Logique custom
        System.out.println(sender.getName() + " a envoyé un MP : " + message);
    }
}

// Enregistrement (dans onStart)
FancyCore.get().getEventService().registerListener(PrivateMessageSentEvent.class, new MonListener());
```

### 3. Manipuler l'Économie
```java
CurrencyService eco = FancyCore.get().getCurrencyService();
Currency gold = eco.getCurrency("Or"); // Récupérer la devise par nom

if (eco.has(player, gold, 50.0)) {
    eco.withdraw(player, gold, 50.0);
    player.sendMessage("Achat effectué !");
} else {
    player.sendMessage("Pas assez d'or !");
}
```

### 4. Gérer les Données Joueur
```java
FancyPlayer fp = FancyPlayerService.get().getPlayer(playerRef);
FancyPlayerData data = fp.getData();

// Exemple hypothétique (selon l'implémentation de FancyPlayerData non visible ici mais probable)
// data.set("mon_attribut", "valeur");
// FancyPlayerService.get().save(fp);
```

---

## E. Checklist Création Nouveau Plugin

- [ ] **Dépendances** : Ajouter `FancyCore` (ou `fc-api`) et `HytaleServer.jar` au classpath/Gradle.
- [ ] **Main Class** : Étendre `JavaPlugin`.
- [ ] **Config** : Créer `hytale.json` (ou `manifest.json`, à vérifier) dans `src/main/resources` avec le nom, version et main-class.
- [ ] **Lifecycle** : Implémenter `start()` et `setup()`.
- [ ] **Services** : Ne pas recréer de gestionnaire d'économie ou de chat, utiliser ceux de FancyCore.
- [ ] **Logging** : Utiliser `getFancyLogger()` hérité ou fourni par Fancy Core pour des logs uniformisés.

---

## F. Incertitudes & Vérifications
Ces points sont déduits du code mais nécessitent une validation in-game.

1.  [ ] **Format du Manifeste** : Le build script mentionne `manifest.json` ET `version.json`. Le standard Hytale est souvent `hytale.json`. Vérifiez quel fichier est réellement chargé par le serveur.
2.  [ ] **Persistence** : Le code montre du stockage JSON et MongoDB (`mongodb-driver`). Vérifiez dans `config` quel mode est activé par défaut.
3.  [ ] **Event Sync/Async** : Le `ChatEvent` est géré de manière asynchrone via `CompletableFuture` dans `FancyCorePlugin`. Vos listeners doivent être Thread-Safe.

## G. Glossaire
- **CMD** : Suffixe utilisé pour les classes de commandes (ex: `BanCMD`).
- **Service** : Singleton gérant une feature (ex: `ChatService`).
- **Storage** : Couche d'accès aux données (JSON/DB).
- **PlayerRef** : Référence native Hytale vers un joueur (peut être hors ligne/online).
- **FancyPlayer** : Surcouche riche ajoutée par ce plugin.
