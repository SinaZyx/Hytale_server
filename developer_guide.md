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

## 5. Interface Utilisateur (GUI / Menus)

Hytale permet de créer des interfaces dynamiques via HTML/CSS (fichier `.ui`) et de les contrôler via Java.

### Créer une Page Interactive
Il faut étendre `InteractiveCustomUIPage`.

```java
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

// InteractiveCustomUIPage<VotreDataType>
public class MonMenu extends InteractiveCustomUIPage<MonMenuData> {

    public MonMenu(PlayerRef player, BuilderCodec<MonMenuData> codec) {
        super(player, CustomPageLifetime.CanDismiss, codec);
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd, UIEventBuilder event, Store<EntityStore> store) {
        // Construction de l'UI
        // Charger le fichier .ui
        // cmd.page("Pages/MonMenu.ui");
        
        // Modifier un text
        // cmd.text("#Titre", "Bienvenue sur le menu");
        
        // Ajouter un event click
        // event.onClick("#BoutonStart", "action:start_game");
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, MonMenuData data) {
        super.handleDataEvent(ref, store, data);
        
        // Réagir aux clics
        // if (data.getAction().equals("start_game")) { ... }
    }
}
```

### Ouvrir le Menu
```java
// Récupérer le composant Player de l'entité
Player playerEntity = store.getComponent(ref, Player.getComponentType());
playerEntity.getPageManager().openCustomPage(ref, store, new MonMenu(...));
```

---

## 6. Mécaniques de Jeu (Mini-Jeux)

### Téléportation (Système de Composants)
C'est un pattern récurrent : on n'appelle pas `player.teleport()`, on **ajoute un composant** `Teleport` à l'entité.

```java
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;

// Code à exécuter sur le Thread du Monde cible
targetWorld.execute(() -> {
    // Création du composant
    Teleport tpComponent = new Teleport(
        targetWorld, 
        new Vector3d(x, y, z), // Position
        new Vector3f(yaw, pitch, 0) // Rotation
    );
    
    // Application
    store.addComponent(playerRef, Teleport.getComponentType(), tpComponent);
});
```

### Kits & Inventaire
Manipulation directe des containers d'items.

```java
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

// Récupérer l'inventaire
Player player = ...;
ItemContainer hotbar = player.getInventory().getHotbar();

// Créer un item (ID Hytale)
ItemStack sword = new ItemStack("hytale:iron_sword");

// Ajouter l'item
if (hotbar.canAddItemStack(sword)) {
    hotbar.addItemStack(sword);
}
```

### Gestion de l'Économie
Il n'y a pas d'économie native ("Coins") dans l'API Hytale. Vous devez créer votre propre Map/Base de données.

```java
// Exemple simple
public class EconomyService {
    private Map<UUID, Double> balances = new ConcurrentHashMap<>();
    
    public void addMoney(UUID player, double amount) {
        balances.merge(player, amount, Double::sum);
    }
}
```

---

---

## 7. Threading & Sédules (Important !)

Contrairement à Bukkit/Spigot qui fournit un `BukkitScheduler`, Hytale semble encourager l'utilisation de **Java Standard** pour l'asynchrone, et de `World.execute()` pour le synchrone.

### Exécuter une tâche répétée
FancyCore instancie son propre `ScheduledExecutorService` :

```java
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class MonPlugin extends JavaPlugin {
    private ScheduledExecutorService scheduler;

    @Override
    public void start() {
        scheduler = Executors.newScheduledThreadPool(2);
        
        // Tâche toutes les secondes
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Tâche de fond...");
        }, 0, 1, TimeUnit.SECONDS);
    }
    
    @Override
    public void shutdown() {
        scheduler.shutdown();
    }
}
```

### Revenir sur le Thread Principal (Synchronisation)
Si vous devez modifier le monde depuis une tâche asynchrone, vous **DEVEZ** utiliser `world.execute()`.

```java
// Dans le thread asynchrone
String playerName = "Bob";
Universe.get().getWorld("world").execute(() -> {
    // Ici on est "Safe" pour modifier des blocs ou entités
    PlayerRef player = ...;
    player.sendMessage(Message.raw("Coucou " + playerName));
});
```

---

## 8. Utilitaires & Divers

Quelques classes utiles repérées :

- **`com.hypixel.hytale.server.core.NameMatching`** : Probablement utilisé pour la complétion de noms ou la recherche floue de joueurs.
- **`com.hypixel.hytale.server.core.io.ServerManager`** : Pour obtenir des infos sur le serveur (Uptime, IP, version ?).
- **`com.hypixel.hytale.component.Ref` / `Store`** : Hytale utilise une architecture **ECS (Entity Component System)** très marquée. On ne manipule pas des objets `Player` directement, mais des `Ref<EntityStore>` qui contiennent des composants (`Transform`, `Player`, `Inventory`).

---

## 10. Formatage et Couleurs

### API Native (Message Builder)
Hytale n'utilise pas de "ChatColor" comme Minecraft, mais un système de composants (JSON-like) avec support Hexadécimal natif.

```java
import com.hypixel.hytale.server.core.Message;

// Texte Rouge
Message msg = Message.raw("Attention !").color("#FF0000");

// Texte Gras et Italique
Message style = Message.raw("Important").bold(true).italic(true);

// Combinaison
Message full = Message.join(msg, Message.raw(" "), style);
player.sendMessage(full);
```

### Avec FancyCore (Legacy Codes)
Si vous utilisez FancyCore, il convertit automatiquement les anciens codes couleurs (`&a`, `&6`, `&l`) en composants Hytale.

```java
// FancyCoreUtils est un exemple fictif, voir ColorUtils dans le code de FancyCore
player.sendMessage(ColorUtils.colour("&aCeci est vert et &lGras !"));
```

---

## 11. Checklist pour Développeur "Native"

Si vous créez un plugin sans FancyCore :
1.  [ ] Créer un projet Gradle avec `HytaleServer.jar` en dépendance (`compileOnly`).
2.  [ ] Créer le `hytale.json` (ou `manifest.json`).
3.  [ ] Étendre `JavaPlugin` et gérer son cycle de vie (`start`/`shutdown`).
4.  [ ] Utiliser `CommandManager` pour vos commandes.
5.  [ ] Utiliser `getEventRegistry()` pour vos listeners.
6.  [ ] **Threading** : Créer votre propre `ExecutorService` pour les timers.
7.  [ ] **ECS** : Apprendre à manipuler `Store`, `Ref` et `Component`.
8.  [ ] Pour les GUIs : Étendre `InteractiveCustomUIPage`.

---

## 12. Référence Complète des Classes API

> Cette section liste toutes les classes Hytale connues, découvertes par rétro-ingénierie de FancyCore.
> **Total : 68 classes identifiées**

### 12.1 Système de Commandes (13 classes)

| Classe | Package | Description |
|--------|---------|-------------|
| `CommandManager` | `com.hypixel.hytale.server.core.command.system` | Gestionnaire global des commandes |
| `CommandContext` | `com.hypixel.hytale.server.core.command.system` | Contexte d'exécution de commande |
| `CommandBase` | `com.hypixel.hytale.server.core.command.system.basecommands` | Classe de base pour les commandes |
| `AbstractCommandCollection` | `com.hypixel.hytale.server.core.command.system.basecommands` | Base pour groupes de commandes |
| `AbstractPlayerCommand` | `com.hypixel.hytale.server.core.command.system.basecommands` | Base pour commandes joueur uniquement |
| `AbstractWorldCommand` | `com.hypixel.hytale.server.core.command.system.basecommands` | Base pour commandes monde-spécifiques |
| `RequiredArg<T>` | `com.hypixel.hytale.server.core.command.system.arguments.system` | Argument obligatoire |
| `OptionalArg<T>` | `com.hypixel.hytale.server.core.command.system.arguments.system` | Argument optionnel |
| `DefaultArg<T>` | `com.hypixel.hytale.server.core.command.system.arguments.system` | Argument avec valeur par défaut |
| `ArgTypes` | `com.hypixel.hytale.server.core.command.system.arguments.types` | Types d'arguments intégrés (STRING, PLAYER_REF, etc.) |
| `SingleArgumentType<T>` | `com.hypixel.hytale.server.core.command.system.arguments.types` | Interface pour types personnalisés |
| `RelativeDoublePosition` | `com.hypixel.hytale.server.core.command.system.arguments.types` | Position relative (~x ~y ~z) |
| `ParseResult` | `com.hypixel.hytale.server.core.command.system` | Résultat du parsing d'arguments |

**Méthodes clés :**
```java
// Enregistrement
CommandManager.get().register(new MaCommande());

// Contexte
ctx.sender()        // Entity source
ctx.isPlayer()      // Boolean
ctx.sendMessage()   // Envoyer message
ctx.getInputString() // Commande brute

// Arguments
RequiredArg<PlayerRef> target = withRequiredArg("player", "desc", ArgTypes.PLAYER_REF);
target.get(ctx);      // Récupérer valeur
target.provided(ctx); // Vérifier si fourni
```

---

### 12.2 Système d'Événements (7 classes)

| Classe | Package | Description |
|--------|---------|-------------|
| `EventRegistry` | `com.hypixel.hytale.event` | Registre global des événements |
| `EventRegistration` | `com.hypixel.hytale.event` | Handle d'enregistrement |
| `PlayerConnectEvent` | `com.hypixel.hytale.server.core.event.events.player` | Connexion joueur |
| `PlayerReadyEvent` | `com.hypixel.hytale.server.core.event.events.player` | Joueur prêt (chargé) |
| `PlayerDisconnectEvent` | `com.hypixel.hytale.server.core.event.events.player` | Déconnexion joueur |
| `AddPlayerToWorldEvent` | `com.hypixel.hytale.server.core.event.events.player` | Ajout au monde (annulable) |
| `PlayerChatEvent` | `com.hypixel.hytale.server.core.event.events.player` | Message chat (async, annulable) |

**Méthodes clés :**
```java
// Enregistrement synchrone
getEventRegistry().registerGlobal(PlayerConnectEvent.class, this::onConnect);

// Enregistrement asynchrone (chat)
getEventRegistry().registerAsyncGlobal(PlayerChatEvent.class, future ->
    future.thenApply(event -> {
        event.setContent("Modifié: " + event.getContent());
        return event;
    })
);

// Propriétés événement
event.getPlayerRef()    // Joueur concerné
event.setCancelled(true) // Annuler
event.getContent()      // Contenu chat
event.setSender()       // Modifier expéditeur
```

---

### 12.3 Joueur & Entités (7 classes)

| Classe | Package | Description |
|--------|---------|-------------|
| `PlayerRef` | `com.hypixel.hytale.server.core.universe` | Référence vers joueur en ligne |
| `Player` | `com.hypixel.hytale.server.core.entity.entities` | Composant Player (ECS) |
| `Entity` | `com.hypixel.hytale.server.core.entity` | Entité de base |
| `UUIDComponent` | `com.hypixel.hytale.server.core.entity` | Composant UUID |
| `DamageDataComponent` | `com.hypixel.hytale.server.core.entity.damage` | Timers de combat (lastCombatAction, lastDamageTime) |
| `Ref<T>` | `com.hypixel.hytale.component` | Référence générique ECS |
| `Store<T>` | `com.hypixel.hytale.component` | Stockage de composants |
| `PlayerDeathPositionData` | `com.hypixel.hytale.server.core.entity.entities.player.data` | Position de mort |

**Méthodes clés :**
```java
// PlayerRef
playerRef.getUuid()       // UUID du joueur
playerRef.getUsername()   // Nom d'affichage
playerRef.isValid()       // Encore connecté ?
playerRef.sendMessage()   // Envoyer message
playerRef.getTransform()  // Position/rotation
playerRef.getWorldUuid()  // UUID du monde actuel
playerRef.getPacketHandler().disconnect() // Kick

// ECS - Récupérer composant
Player player = store.getComponent(ref, Player.getComponentType());

// Combat - temps de dernier hit
DamageDataComponent damageData = store.getComponent(ref, DamageDataComponent.getComponentType());
if (damageData != null && damageData.getLastCombatAction() != null) {
    long lastCombatMs = damageData.getLastCombatAction().toEpochMilli();
}
```

---

### 12.4 Monde & Univers (12 classes)

| Classe | Package | Description |
|--------|---------|-------------|
| `Universe` | `com.hypixel.hytale.server.core.universe` | Singleton univers global |
| `World` | `com.hypixel.hytale.server.core.universe.world` | Représentation d'un monde |
| `EntityStore` | `com.hypixel.hytale.server.core.universe.world.storage` | Stockage entités du monde |
| `WorldConfig` | `com.hypixel.hytale.server.core.universe.world` | Configuration monde |
| `GlobalSpawnProvider` | `com.hypixel.hytale.server.core.universe.world.spawn` | Fournisseur de spawn |
| `TransformComponent` | `com.hypixel.hytale.server.core.modules.entity.component` | Position/rotation |
| `HeadRotation` | `com.hypixel.hytale.server.core.modules.entity.component` | Rotation de la tête |
| `Teleport` | `com.hypixel.hytale.server.core.modules.entity.teleport` | Composant téléportation |
| `TeleportHistory` | `com.hypixel.hytale.builtin.teleport.components` | Historique téléportations |
| `Vector3d` | `com.hypixel.hytale.math.vector` | Vecteur 3D double (positions) |
| `Vector3f` | `com.hypixel.hytale.math.vector` | Vecteur 3D float (rotations) |
| `Transform` | `com.hypixel.hytale.math.vector` | Position + rotation combinées |

**Méthodes clés :**
```java
// Accès univers
Universe.get().getPlayers()           // Tous les joueurs
Universe.get().getPlayer(uuid)        // Joueur par UUID
Universe.get().getWorld(worldUuid)    // Monde par UUID

// Exécution thread-safe
world.execute(() -> {
    // Code exécuté sur le thread du monde
});

// Téléportation
Teleport tp = new Teleport(targetWorld, new Vector3d(x, y, z), new Vector3f(yaw, pitch, 0));
store.addComponent(ref, Teleport.getComponentType(), tp);

// EntityStore
EntityStore entityStore = world.getEntityStore();
Store<EntityStore> store = entityStore.getStore();
Ref<EntityStore> ref = entityStore.getRefFromUUID(playerUuid);
```

---

### 12.5 Interface Utilisateur (9 classes)

| Classe | Package | Description |
|--------|---------|-------------|
| `InteractiveCustomUIPage<T>` | `com.hypixel.hytale.server.core.entity.entities.player.pages` | Base pour pages UI |
| `Page` | `com.hypixel.hytale.protocol.packets.interface_` | Représentation page |
| `CustomPageLifetime` | `com.hypixel.hytale.protocol.packets.interface_` | Durée de vie (CanDismiss, etc.) |
| `CustomUIEventBindingType` | `com.hypixel.hytale.protocol.packets.interface_` | Types de bindings |
| `UICommandBuilder` | `com.hypixel.hytale.server.core.ui.builder` | Builder commandes UI |
| `UIEventBuilder` | `com.hypixel.hytale.server.core.ui.builder` | Builder événements UI |
| `EventData` | `com.hypixel.hytale.server.core.ui.builder` | Données d'événement |
| `LocalizableString` | `com.hypixel.hytale.server.core.ui` | Texte localisable |
| `DropdownEntryInfo` | `com.hypixel.hytale.server.core.ui` | Entrée dropdown |

**Méthodes clés :**
```java
// Builder UI
UICommandBuilder cmd;
cmd.page("Pages/MonMenu.ui");           // Charger fichier .ui
cmd.set("#elementId", "valeur");        // Modifier valeur
cmd.text("#titre", "Mon Titre");        // Modifier texte
cmd.visible("#element", true);          // Visibilité

UIEventBuilder event;
event.onClick("#bouton", "action:click"); // Événement clic
event.onInput("#input", "action:input");  // Événement input

// Ouvrir page
player.getPageManager().openCustomPage(ref, store, new MaPage(...));
```

---

### 12.6 Inventaire & Items (6 classes)

| Classe | Package | Description |
|--------|---------|-------------|
| `ItemStack` | `com.hypixel.hytale.server.core.inventory` | Pile d'items |
| `ItemContainer` | `com.hypixel.hytale.server.core.inventory.container` | Interface container |
| `SimpleItemContainer` | `com.hypixel.hytale.server.core.inventory.container` | Container simple |
| `ItemContainerChangeEvent` | `com.hypixel.hytale.server.core.inventory.container.ItemContainer` | Événement changement |
| `ItemStackTransaction` | `com.hypixel.hytale.server.core.inventory.transaction` | Transaction items |
| `ContainerWindow` | `com.hypixel.hytale.server.core.entity.entities.player.windows` | Fenêtre container |

**Méthodes clés :**
```java
// Accès inventaire
Player player = store.getComponent(ref, Player.getComponentType());
ItemContainer hotbar = player.getInventory().getHotbar();
ItemContainer storage = player.getInventory().getStorage();

// Créer item
ItemStack item = new ItemStack("hytale:iron_sword");

// Ajouter item
if (hotbar.canAddItemStack(item)) {
    hotbar.addItemStack(item);
}

// Sérialisation
String json = ItemStack.CODEC.encode(item);
ItemStack decoded = ItemStack.CODEC.decode(json);
```

---

### 12.7 Permissions (2 classes)

| Classe | Package | Description |
|--------|---------|-------------|
| `PermissionsModule` | `com.hypixel.hytale.server.core.permissions` | Module permissions singleton |
| `PermissionProvider` | `com.hypixel.hytale.server.core.permissions.provider` | Interface fournisseur |

**Méthodes clés :**
```java
// Vérifier permission
PermissionsModule.get().hasPermission(uuid, "monplugin.admin");

// Ajouter provider custom
PermissionsModule.get().addProvider(new MonPermissionProvider());
PermissionsModule.get().getProviders();
PermissionsModule.get().removeProvider(provider);

// Dans commande
requirePermission("monplugin.commande");
```

---

### 12.8 Utilitaires (12 classes)

| Classe | Package | Description |
|--------|---------|-------------|
| `Message` | `com.hypixel.hytale.server.core` | Messages formatés |
| `JavaPlugin` | `com.hypixel.hytale.server.core.plugin` | Classe de base plugin |
| `JavaPluginInit` | `com.hypixel.hytale.server.core.plugin` | Paramètre init plugin |
| `HytaleLogger` | `com.hypixel.hytale.logger` | Système de logging |
| `Codec<T>` | `com.hypixel.hytale.codec` | Interface codec |
| `KeyedCodec<T>` | `com.hypixel.hytale.codec` | Codec avec clé |
| `BuilderCodec<T>` | `com.hypixel.hytale.codec.builder` | Codec builder pattern |
| `EmptyExtraInfo` | `com.hypixel.hytale.codec` | Info extra vide |
| `ServerManager` | `com.hypixel.hytale.server.core.io` | Gestion serveur I/O |
| `NameMatching` | `com.hypixel.hytale.server.core` | Matching noms joueurs |
| `EventTitleUtil` | `com.hypixel.hytale.server.core.util` | Affichage titres |
| `ShutdownEvent` | `com.hypixel.hytale.server.core.event.events` | Événement arrêt serveur |

**Méthodes clés :**
```java
// Messages
Message msg = Message.raw("Texte");
msg.color("#FF0000");     // Couleur hex
msg.bold(true);           // Gras
msg.italic(true);         // Italique
Message.join(msg1, msg2); // Combiner

// Logger
private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
LOGGER.atInfo().log("Message info");
LOGGER.at(Level.SEVERE).withCause(e).log("Erreur");

// Titre événement
EventTitleUtil.showEventTitleToPlayer(
    playerRef,
    Message.raw("Titre"),
    Message.raw("Sous-titre"),
    true,
    EventTitleUtil.DEFAULT_ZONE,
    fadeIn, stay, fadeOut
);
```

---

## 13. Diagramme des Packages

```
com.hypixel.hytale
├── codec/                     # Sérialisation
│   └── builder/
├── component/                 # ECS (Ref, Store)
├── event/                     # EventRegistry
├── logger/                    # HytaleLogger
├── math/
│   └── vector/               # Vector3d, Vector3f, Transform
├── protocol/
│   └── packets/
│       └── interface_/       # UI packets
└── server/
    └── core/
        ├── command/
        │   └── system/
        │       ├── arguments/
        │       │   ├── system/   # RequiredArg, OptionalArg
        │       │   └── types/    # ArgTypes
        │       └── basecommands/ # CommandBase, AbstractPlayerCommand
        ├── entity/
        │   └── entities/
        │       ├── player/
        │       │   ├── data/
        │       │   ├── pages/    # InteractiveCustomUIPage
        │       │   └── windows/
        │       └── Player
        ├── event/
        │   └── events/
        │       └── player/       # PlayerConnectEvent, etc.
        ├── inventory/
        │   ├── container/
        │   └── transaction/
        ├── io/                   # ServerManager
        ├── modules/
        │   └── entity/
        │       ├── component/    # TransformComponent
        │       └── teleport/     # Teleport
        ├── permissions/
        │   └── provider/
        ├── plugin/               # JavaPlugin
        ├── ui/
        │   └── builder/         # UICommandBuilder, UIEventBuilder
        ├── universe/
        │   └── world/
        │       ├── spawn/
        │       └── storage/      # EntityStore
        └── util/                 # EventTitleUtil
```

---

## 14. Référence Complète des Événements

> **5218 classes** ont été découvertes dans HytaleServer.jar. Cette section détaille tous les événements disponibles.

### 14.1 Événements Joueur

| Classe | Package | Description |
|--------|---------|-------------|
| `PlayerConnectEvent` | `server.core.event.events.player` | Connexion au serveur |
| `PlayerDisconnectEvent` | `server.core.event.events.player` | Déconnexion du serveur |
| `PlayerReadyEvent` | `server.core.event.events.player` | Joueur chargé et prêt |
| `PlayerChatEvent` | `server.core.event.events.player` | Message chat (async) |
| `PlayerInteractEvent` | `server.core.event.events.player` | Interaction générale |
| `PlayerCraftEvent` | `server.core.event.events.player` | Craft par le joueur |
| `PlayerMouseButtonEvent` | `server.core.event.events.player` | Clic souris |
| `PlayerMouseMotionEvent` | `server.core.event.events.player` | Mouvement souris |
| `AddPlayerToWorldEvent` | `server.core.event.events.player` | Ajout au monde (annulable) |
| `DrainPlayerFromWorldEvent` | `server.core.event.events.player` | Retrait du monde |
| `PlayerSetupConnectEvent` | `server.core.event.events.player` | Setup connexion |
| `PlayerSetupDisconnectEvent` | `server.core.event.events.player` | Setup déconnexion |
| `PlayerRefEvent` | `server.core.event.events.player` | Événement avec PlayerRef |

### 14.2 Événements ECS (Gameplay)

| Classe | Package | Description |
|--------|---------|-------------|
| `BreakBlockEvent` | `server.core.event.events.ecs` | Casser un bloc |
| `PlaceBlockEvent` | `server.core.event.events.ecs` | Placer un bloc |
| `DamageBlockEvent` | `server.core.event.events.ecs` | Endommager un bloc |
| `UseBlockEvent` | `server.core.event.events.ecs` | Utiliser un bloc |
| `DiscoverZoneEvent` | `server.core.event.events.ecs` | **Découvrir une zone** |
| `DropItemEvent` | `server.core.event.events.ecs` | Lâcher un item |
| `InteractivelyPickupItemEvent` | `server.core.event.events.ecs` | Ramasser un item |
| `CraftRecipeEvent` | `server.core.event.events.ecs` | Craft d'une recette |
| `SwitchActiveSlotEvent` | `server.core.event.events.ecs` | Changer de slot actif |
| `ChangeGameModeEvent` | `server.core.event.events.ecs` | Changer de mode de jeu |

### 14.3 Événements Entités

| Classe | Package | Description |
|--------|---------|-------------|
| `EntityEvent` | `server.core.event.events.entity` | Base événement entité |
| `EntityRemoveEvent` | `server.core.event.events.entity` | Suppression entité |
| `LivingEntityInventoryChangeEvent` | `server.core.event.events.entity` | Changement inventaire |
| `LivingEntityUseBlockEvent` | `server.core.event.events.entity` | Entité utilise bloc |
| `KillFeedEvent` | `server.core.modules.entity.damage.event` | Kill feed (mort) |

#### KillFeedEvent details

Nested event classes (from `KillFeedEvent`):

- `KillFeedEvent.KillerMessage`
  - `getDamage()` -> `Damage`
  - `getTargetRef()` -> `Ref<EntityStore>` (target/decedent ref)
  - `getMessage()` / `setMessage(Message)`
- `KillFeedEvent.DecedentMessage`
  - `getDamage()` -> `Damage`
  - `getMessage()` / `setMessage(Message)`
- `KillFeedEvent.Display`
  - `getDamage()` -> `Damage`
  - `getIcon()` / `setIcon(String)`
  - `getBroadcastTargets()` -> `List<PlayerRef>`

Damage source access:

- `Damage.getSource()` -> `Damage.Source`
- `Damage.EntitySource.getRef()` -> `Ref<EntityStore>`

Example (killer + victim UUID from `KillerMessage`):
```java
getEventRegistry().registerGlobal(KillFeedEvent.KillerMessage.class, event -> {
    Damage damage = event.getDamage();
    Damage.Source source = damage.getSource();
    if (!(source instanceof Damage.EntitySource)) {
        return;
    }

    Ref<EntityStore> killerRef = ((Damage.EntitySource) source).getRef();
    Ref<EntityStore> victimRef = event.getTargetRef();
    if (killerRef == null || !killerRef.isValid() || victimRef == null || !victimRef.isValid()) {
        return;
    }

    Store<EntityStore> killerStore = killerRef.getStore();
    Store<EntityStore> victimStore = victimRef.getStore();
    PlayerRef killerPlayer = killerStore.getComponent(killerRef, PlayerRef.getComponentType());
    PlayerRef victimPlayer = victimStore.getComponent(victimRef, PlayerRef.getComponentType());
    if (killerPlayer == null || victimPlayer == null) {
        return;
    }

    UUID killerId = killerPlayer.getUuid();
    UUID victimId = victimPlayer.getUuid();
    // Use killerId / victimId
});
```

### 14.4 Événements Permissions

| Classe | Package | Description |
|--------|---------|-------------|
| `GroupPermissionChangeEvent` | `server.core.event.events.permissions` | Changement permission groupe |
| `PlayerPermissionChangeEvent` | `server.core.event.events.permissions` | Changement permission joueur |
| `PlayerGroupEvent` | `server.core.event.events.permissions` | Changement groupe joueur |

### 14.5 Événements Système

| Classe | Package | Description |
|--------|---------|-------------|
| `BootEvent` | `server.core.event.events` | Démarrage serveur |
| `ShutdownEvent` | `server.core.event.events` | Arrêt serveur |
| `PrepareUniverseEvent` | `server.core.event.events` | Préparation univers |

### 14.6 Événements Assets

| Classe | Package | Description |
|--------|---------|-------------|
| `AssetPackRegisterEvent` | `server.core.asset` | Enregistrement asset pack |
| `AssetPackUnregisterEvent` | `server.core.asset` | Désenregistrement asset pack |
| `LoadAssetEvent` | `server.core.asset` | Chargement asset |
| `GenerateSchemaEvent` | `server.core.asset` | Génération schéma |

**Exemple d'écoute d'événement ECS :**
```java
// Détecter quand un joueur découvre une zone
getEventRegistry().registerGlobal(DiscoverZoneEvent.class, event -> {
    PlayerRef player = event.getPlayerRef();
    // Zone zone = event.getZone();
    player.sendMessage(Message.raw("Vous avez découvert une nouvelle zone !"));
});

// Détecter quand un joueur casse un bloc
getEventRegistry().registerGlobal(BreakBlockEvent.class, event -> {
    // Annuler si dans zone protégée
    // event.setCancelled(true);
});
```

---

## 15. Système HUD & Overlays

Hytale possède un système de HUD avancé pour les overlays visuels persistants.

### 15.1 Classes HUD

| Classe | Package | Description |
|--------|---------|-------------|
| `HudManager` | `server.core.entity.entities.player.hud` | Gestionnaire HUD joueur |
| `CustomUIHud` | `server.core.entity.entities.player.hud` | HUD personnalisé |
| `CustomHud` | `protocol.packets.interface_` | Packet HUD custom |
| `HudComponent` | `protocol.packets.interface_` | Composant HUD |
| `UpdateVisibleHudComponents` | `protocol.packets.interface_` | Mise à jour visibilité |

### 15.2 Utilisation HUD

```java
// Récupérer le HudManager du joueur
Player player = store.getComponent(ref, Player.getComponentType());
HudManager hudManager = player.getHudManager();

// Afficher/masquer des composants HUD
// hudManager.setVisible("monOverlay", true);
// hudManager.update();
```

---

## 16. Système de Notifications

### 16.1 Classes Notification

| Classe | Package | Description |
|--------|---------|-------------|
| `Notification` | `protocol.packets.interface_` | Notification |
| `NotificationStyle` | `protocol.packets.interface_` | Style notification |
| `NotificationUtil` | `server.core.util` | Utilitaire notifications |
| `AssetNotifications` | `server.core.asset` | Notifications assets |

### 16.2 Utilisation

```java
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.util.NotificationUtil;

// Envoyer une notification au joueur
NotificationUtil.sendNotification(
    playerRef.getPacketHandler(),
    Message.raw("Titre"),
    Message.raw("Message"),
    NotificationStyle.Default
);
```

---

## 17. Système de Particules

### 17.1 Classes Particules

| Classe | Package | Description |
|--------|---------|-------------|
| `ParticleSystem` | `server.core.asset.type.particle.config` | Système de particules |
| `ParticleSpawner` | `server.core.asset.type.particle.config` | Spawner de particules |
| `ParticleSpawnerGroup` | `server.core.asset.type.particle.config` | Groupe de spawners |
| `Particle` | `server.core.asset.type.particle.config` | Configuration particule |
| `ParticleAttractor` | `server.core.asset.type.particle.config` | Attracteur particules |
| `ParticleCollision` | `server.core.asset.type.particle.config` | Collision particules |
| `ParticleAnimationFrame` | `server.core.asset.type.particle.config` | Frame animation |
| `WorldParticle` | `server.core.asset.type.particle.config` | Particule monde |
| `ParticleUtil` | `server.core.universe.world` | Utilitaire particules |
| `ParticleCommand` | `server.core.asset.type.particle.commands` | Commande particules |
| `SpawnParticleSystem` | `protocol.packets.world` | Packet spawn particules |

### 17.2 Utilisation Particules

```java
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;

// Spawner des particules à une position
world.execute(() -> {
    ParticleUtil.spawnParticles(world, "hytale:smoke", position, count);
});
```

---

## 18. Système d'Effets

### 18.1 Effets Entités

| Classe | Package | Description |
|--------|---------|-------------|
| `EntityEffect` | `server.core.asset.type.entityeffect.config` | Effet sur entité |
| `ActiveEntityEffect` | `server.core.entity.effect` | Effet actif |
| `EffectControllerComponent` | `server.core.entity.effect` | Contrôleur effets |
| `AbilityEffects` | `server.core.asset.type.entityeffect.config` | Effets capacités |
| `ApplicationEffects` | `server.core.asset.type.entityeffect.config` | Effets application |
| `DamageEffects` | `server.core.modules.interaction.interaction.config.server.combat` | Effets dégâts |

### 18.2 Effets Caméra

| Classe | Package | Description |
|--------|---------|-------------|
| `CameraEffect` | `server.core.asset.type.camera` | Effet caméra |
| `CameraShakeEffect` | `builtin.adventure.camera.asset.cameraeffect` | Tremblement caméra |
| `ShakeIntensity` | `builtin.adventure.camera.asset.cameraeffect` | Intensité tremblement |
| `CameraEffectSystem` | `builtin.adventure.camera.system` | Système effets caméra |
| `CameraEffectCommand` | `builtin.adventure.camera.command` | Commande effet caméra |

### 18.3 Effets Audio

| Classe | Package | Description |
|--------|---------|-------------|
| `SoundEvent` | `server.core.asset.type.soundevent.config` | Événement son |
| `ReverbEffect` | `server.core.asset.type.reverbeffect.config` | Effet réverbération |
| `EqualizerEffect` | `server.core.asset.type.equalizereffect.config` | Effet égaliseur |
| `AmbienceFXSoundEffect` | `server.core.asset.type.ambiencefx.config` | Effet ambiance |

### 18.4 Utilisation Sons (SoundUtil)

```java
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.protocol.SoundCategory;

// Résoudre l'ID d'un son à partir de son asset ID
int soundId = SoundEvent.getAssetMap().getIndexOrDefault("hytale:alarm_bell", SoundEvent.EMPTY_ID);
if (soundId != SoundEvent.EMPTY_ID) {
    SoundUtil.playSoundEvent2dToPlayer(playerRef, soundId, SoundCategory.SFX, 1.0f, 1.0f);
}
```

---

## 19. Système de Zones

### 19.1 Classes Zones

| Classe | Package | Description |
|--------|---------|-------------|
| `Zone` | `server.worldgen.zone` | Zone du monde |
| `ZoneDiscoveryConfig` | `server.worldgen.zone` | Config découverte zone |
| `ZoneColorMapping` | `server.worldgen.zone` | Mapping couleurs zone |
| `ZonePatternGenerator` | `server.worldgen.zone` | Générateur pattern zone |
| `ZonePatternProvider` | `server.worldgen.zone` | Fournisseur pattern |
| `DiscoverZoneEvent` | `server.core.event.events.ecs` | Événement découverte |
| `PlayerZoneCommand` | `server.core.command.commands.player` | Commande zone joueur |

### 19.2 Zones pour Claims (Custom)

Pour implémenter des zones de claims personnalisées :

```java
// 1. Écouter DiscoverZoneEvent pour les zones natives
getEventRegistry().registerGlobal(DiscoverZoneEvent.class, this::onZoneDiscover);

// 2. Ou scanner périodiquement les positions joueurs (comme dans Faction)
scheduler.scheduleAtFixedRate(() -> {
    for (PlayerRef player : Universe.get().getPlayers()) {
        Location loc = getLocation(player);
        String currentClaim = getClaimAt(loc);
        String lastClaim = lastClaimByPlayer.get(player.getUuid());

        if (!Objects.equals(currentClaim, lastClaim)) {
            // Joueur a changé de zone
            showClaimOverlay(player, currentClaim);
            lastClaimByPlayer.put(player.getUuid(), currentClaim);
        }
    }
}, 0, 1, TimeUnit.SECONDS);

// 3. Afficher l'overlay
private void showClaimOverlay(PlayerRef player, String claimName) {
    // Option A: Titre temporaire
    EventTitleUtil.showEventTitleToPlayer(
        player,
        Message.raw("Zone: " + claimName).color("#00FF00"),
        Message.raw("Bienvenue !"),
        true, EventTitleUtil.DEFAULT_ZONE,
        10, 40, 10
    );

    // Option B: Notification
    // NotificationUtil.sendNotification(player, "Zone", claimName, NotificationStyle.Default);

    // Option C: HUD persistant (via CustomUIHud)
    // Nécessite un fichier .ui et une page custom
}
```

---

## 20. Système Adventure Mode (Built-in)

Hytale inclut des systèmes de jeu pré-construits dans `com.hypixel.hytale.builtin.adventure.*`.

### 20.1 Farming

| Classe | Package | Description |
|--------|---------|-------------|
| `FarmingPlugin` | `builtin.adventure.farming` | Plugin farming |
| `FarmingBlock` | `builtin.adventure.farming.states` | Bloc farming |
| `TilledSoilBlock` | `builtin.adventure.farming.states` | Sol labouré |
| `HarvestCropInteraction` | `builtin.adventure.farming.interactions` | Récolter |
| `UseWateringCanInteraction` | `builtin.adventure.farming.interactions` | Arroser |

### 20.2 Objectives (Quêtes)

| Classe | Package | Description |
|--------|---------|-------------|
| `ObjectivePlugin` | `builtin.adventure.objectives` | Plugin objectifs |
| `Objective` | `builtin.adventure.objectives` | Objectif |
| `ObjectiveAsset` | `builtin.adventure.objectives.config` | Config objectif |
| `ObjectiveCompletion` | `builtin.adventure.objectives.completion` | Complétion |
| `GatherObjectiveTaskAsset` | `builtin.adventure.objectives.config.task` | Tâche récolte |
| `KillObjectiveTask` | `builtin.adventure.npcobjectives.task` | Tâche kill |

### 20.3 NPC Shop

| Classe | Package | Description |
|--------|---------|-------------|
| `NPCShopPlugin` | `builtin.adventure.npcshop` | Plugin shop NPC |
| `ActionOpenShop` | `builtin.adventure.npcshop.npc` | Ouvrir shop |
| `ActionOpenBarterShop` | `builtin.adventure.npcshop.npc` | Ouvrir troc |

### 20.4 Memories (Journal)

| Classe | Package | Description |
|--------|---------|-------------|
| `MemoriesPlugin` | `builtin.adventure.memories` | Plugin mémoires |
| `PlayerMemories` | `builtin.adventure.memories.component` | Composant mémoires |
| `Memory` | `builtin.adventure.memories.memories` | Mémoire |
| `MemoriesPage` | `builtin.adventure.memories.page` | Page mémoires |

---

## 21. Protocole Réseau

### 21.1 Packets Interface (UI)

| Classe | Package | Description |
|--------|---------|-------------|
| `Page` | `protocol.packets.interface_` | Page UI |
| `CustomPageEvent` | `protocol.packets.interface_` | Événement page |
| `ShowEventTitle` | `protocol.packets.interface_` | Afficher titre |
| `HideEventTitle` | `protocol.packets.interface_` | Masquer titre |
| `Notification` | `protocol.packets.interface_` | Notification |
| `CustomHud` | `protocol.packets.interface_` | HUD custom |

### 21.2 Packets Monde

| Classe | Package | Description |
|--------|---------|-------------|
| `SpawnParticleSystem` | `protocol.packets.world` | Spawn particules |
| `SpawnBlockParticleSystem` | `protocol.packets.world` | Spawn particules bloc |

### 21.3 Packets Caméra

| Classe | Package | Description |
|--------|---------|-------------|
| `CameraShakeEffect` | `protocol.packets.camera` | Tremblement caméra |

### 21.4 Packets World Map

| Classe | Package | Description |
|--------|---------|-------------|
| `MapMarker` | `protocol.packets.worldmap` | Marqueur de carte |
| `MapChunk` | `protocol.packets.worldmap` | Chunk de carte (coordonnées + image) |
| `MapImage` | `protocol.packets.worldmap` | Image RGBA (width/height/data) |
| `UpdateWorldMap` | `protocol.packets.worldmap` | Mise a jour de la carte (chunks + markers) |
| `UpdateWorldMapVisible` | `protocol.packets.worldmap` | Affichage/masquage de la carte |
| `ClearWorldMap` | `protocol.packets.worldmap` | Efface la carte |
| `Transform` | `protocol` | Transform (Position + Direction) |
| `Position` | `protocol` | Position (x, y, z) |
| `Direction` | `protocol` | Orientation (yaw, pitch, roll) |

### 21.5 World Map (serveur)

| Classe | Package | Description |
|--------|---------|-------------|
| `WorldMapTracker` | `server.core.universe.world` | Tracker par joueur (markers, settings) |
| `WorldMapManager` | `server.core.universe.world.worldmap` | Manager de world map + providers |
| `WorldMapManager.MarkerProvider` | `server.core.universe.world.worldmap` | Provider de markers |
| `IWorldMap` | `server.core.universe.world.worldmap` | Generateur de carte |
| `PacketHandler` | `server.core.io` | Envoi de packets (write) |
| `TriFunction` | `function.function` | Callback de creation de markers |
| `Predicate` | `java.util.function` | Filtre de markers joueurs |

### 21.6 Utilisation World Map

```java
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.Transform;
import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;

// Obtenir le WorldMapTracker d'un joueur
Player player = store.getComponent(ref, Player.getComponentType());
WorldMapTracker tracker = player.getWorldMapTracker();

// Creer un marker de carte (icone = asset id)
String markerId = "faction:example";
String markerName = "Faction Example";
String markerImage = "hytale:map_marker";
Transform markerTransform = new Transform(
    new Position(x, y, z),
    new Direction(yaw, pitch, 0f)
);
MapMarker marker = new MapMarker(markerId, markerName, markerImage, markerTransform, new com.hypixel.hytale.protocol.packets.worldmap.ContextMenuItem[0]);

// Envoi du marker (premier param = -1 pour bypass visibilite)
tracker.trySendMarker(-1, 0, 0, marker);

// Filtrer les players visibles sur la world map (true = cacher)
tracker.setPlayerMapFilter(playerRef -> !allowedPlayers.contains(playerRef.getUuid()));
```

**Chunks + overlay (UpdateWorldMap)** :
```java
import com.hypixel.hytale.protocol.packets.worldmap.MapChunk;
import com.hypixel.hytale.protocol.packets.worldmap.MapImage;
import com.hypixel.hytale.protocol.packets.worldmap.UpdateWorldMap;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;

WorldMapManager manager = world.getWorldMapManager();
MapImage base = manager.getImageIfInMemory(chunkX, chunkZ);
if (base != null) {
    MapImage tinted = base.clone(); // clone = copie profonde du tableau data
    // MapImage.data = RGBA (r<<24 | g<<16 | b<<8 | a)
    int[] data = tinted.data;
    for (int i = 0; i < data.length; i++) {
        data[i] = blend(data[i], overlayR, overlayG, overlayB, overlayAlpha);
    }
    MapChunk chunk = new MapChunk(chunkX, chunkZ, tinted);
    PacketHandler handler = playerRef.getPacketHandler();
    handler.writeNoCache(new UpdateWorldMap(new MapChunk[] { chunk }, null, null));
}
```

---

## 22. Statistiques Finales

| Catégorie | Classes Documentées |
|-----------|---------------------|
| **Plugin Base** | 3 |
| **Commandes** | 13+ |
| **Événements** | 50+ |
| **Joueur/Entités** | 15+ |
| **Monde/Univers** | 20+ |
| **UI/GUI** | 15+ |
| **Inventaire** | 10+ |
| **Permissions** | 5+ |
| **HUD/Overlays** | 10+ |
| **Particules** | 15+ |
| **Effets** | 20+ |
| **Zones** | 10+ |
| **Notifications** | 5+ |
| **Adventure Mode** | 30+ |
| **Protocole** | 20+ |
| **Utilitaires** | 15+ |
| **TOTAL** | **250+ classes** |

> **Note**: 5218 classes existent dans HytaleServer.jar. Ce guide couvre les plus utiles pour le développement de plugins.
