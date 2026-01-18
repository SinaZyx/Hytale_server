# Documentation API Native Hytale (Rétro-ingénierie)

Ce document est le fruit d'une analyse du code de **FancyCore** pour en extraire les mécanismes de l'**API Serveur Native de Hytale**.
Il sert de guide pour créer un plugin Hytale "From Scratch", sans utiliser FancyCore.

> [!WARNING]
> **API Non Officielle** : Hytale n'a pas encore publié de documentation officielle. Ces informations sont déduites du code existant et concernent probablement une version "Alpha/Beta" du serveur.

---

## 1. Structure d'un Plugin

Tout plugin semble devoir étendre la classe `com.hypixel.hytale.server.core.plugin.JavaPlugin`.

### Déclaration (Main Class)
```java
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import javax.annotation.Nonnull;

public class MonPluginHytale extends JavaPlugin {

    // Constructeur obligatoire
    public MonPluginHytale(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        // Appelé au chargement initial (avant le démarrage du monde ?)
        // C'est ici qu'on instancie les managers/services
    }

    @Override
    public void start() {
        // Appelé quand le serveur démarre
        // Enregistrement des commandes et listeners
        registerCommands();
        registerListeners();
    }

    @Override
    protected void shutdown() {
        // Appelé à l'arrêt du serveur
    }
}
```

### Manifeste (`hytale.json` ou `version.json`)
Le serveur détecte probablement les plugins via un fichier JSON à la racine du JAR (non visible dans le code Java, mais le build script mentionne `manifest.json`).

---

## 2. Système de Commandes

Hytale utilise un système de commandes hiérarchique via `CommandManager`.

### Créer une commande simple
Il faut étendre `CommandBase`.

```java
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.Message;
import org.jetbrains.annotations.NotNull;

public class MaCommande extends CommandBase {

    public MaCommande() {
        // Nom de la commande, Description
        super("bonjour", "Affiche un message de bienvenue");
    }

    @Override
    protected void executeSync(@NotNull CommandContext ctx) {
        // Vérifier si c'est un joueur
        if (ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("Bonjour joueur !"));
        } else {
            ctx.sendMessage(Message.raw("Bonjour console !"));
        }
    }
}
```

### Arguments Typés
L'API fournit un système d'arguments typés (`RequiredArg`).

```java
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;

public class TeleportCmd extends CommandBase {

    // Définition de l'argument (Nom, Description, Type)
    private final RequiredArg<String> playerArg = this.withRequiredArg("joueur", "Nom du joueur", ArgTypes.STRING);

    public TeleportCmd() {
        super("tp", "Téléporte un joueur");
    }

    @Override
    protected void executeSync(CommandContext ctx) {
        // Récupération de la valeur
        String targetName = playerArg.get(ctx);
        // ... logique
    }
}
```

### Enregistrement
```java
import com.hypixel.hytale.server.core.command.system.CommandManager;

// Dans la méthode start() du plugin
CommandManager.get().register(new MaCommande());
```

---

## 3. Système d'Événements

Les événements sont gérés par `EventRegistry`. Il semble y avoir une distinction entre événements synchrones et asynchrones.

### Événements Connus
Classes situées dans `com.hypixel.hytale.server.core.event.events.player.*` :
- `PlayerJoinEvent` / `PlayerConnectEvent`
- `PlayerLeaveEvent` / `PlayerDisconnectEvent`
- `PlayerChatEvent`
- `PlayerReadyEvent`

### Écouter un événement
L'approche semble être fonctionnelle (référence de méthode) plutôt que par annotations `@EventHandler`.

```java
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;

public class MonPlugin extends JavaPlugin {
    
    @Override
    public void start() {
        EventRegistry registry = this.getEventRegistry();
        
        // Enregistrement global
        registry.registerGlobal(PlayerConnectEvent.class, this::onPlayerConnect);
    }
    
    private void onPlayerConnect(PlayerConnectEvent event) {
        System.out.println("Un joueur se connecte !");
    }
}
```

### Événements Asynchrones (Chat)
Le chat semble être géré de manière asynchrone via `CompletableFuture`.

```java
registry.registerAsyncGlobal(PlayerChatEvent.class, future -> 
    future.thenApply(event -> {
        // Logique de chat (Thread-Safe requise !)
        // event.setCancelled(true);
        return event;
    })
);
```

---

## 4. Manipulation du Monde et des Joueurs

### PlayerRef
Le joueur n'est pas manipulé directement via une entité "Bukkit-like", mais souvent via `PlayerRef` ou des composants ECS (Entity Component System).

- **Package** : `com.hypixel.hytale.server.core.universe.PlayerRef`
- **Récupération** : Probablement via `Universe.get().getPlayer(...)`.

### Permissions
Hytale possède un module de permission natif.
```java
import com.hypixel.hytale.server.core.permissions.PermissionsModule;

if (PermissionsModule.get().hasPermission(playerUUID, "monplugin.admin")) {
    // ...
}
```

---

## 5. Inventaires & Items

Les imports révèlent une gestion d'inventaire native.
- **ItemStack** : `com.hypixel.hytale.server.core.inventory.ItemStack`
- **Container** : `com.hypixel.hytale.server.core.inventory.container.ItemContainer`

---

## Checklist pour Développeur "Native"

Si vous créez un plugin sans FancyCore :
1.  [ ] Créer un projet Gradle avec `HytaleServer.jar` en dépendance (`compileOnly`).
2.  [ ] Créer le `hytale.json` (ou `manifest.json`).
3.  [ ] Étendre `JavaPlugin`.
4.  [ ] Utiliser `CommandManager` pour vos commandes.
5.  [ ] Utiliser `getEventRegistry()` pour vos listeners.
6.  [ ] Gérer scrupuleusement le Threading (le serveur Hytale semble très asynchrone par défaut).
