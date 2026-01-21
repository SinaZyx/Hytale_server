## Project Overview

This is a **Hytale Server Plugin Ecosystem** consisting of three Java 25 plugins built against the Hytale server API:

- **FancyCore** (`/FancyCore`) - All-in-one core plugin (permissions, economy, chat, moderation, teleportation)
- **HytaleFactions** (`/faction`) - PvP factions with claims, alliances, and territory management
- **HytaleDuels** (`/hytale-duels`) - Competitive 1v1/2v2 duels with ELO ranking system

## Build Commands

All plugins use Gradle with Java 25:

```bash
# Build any plugin
./gradlew build                    # Unix
.\gradlew.bat build                # Windows

# FancyCore specific
./gradlew :plugins:fancycore:shadowJar   # Fat JAR with dependencies
./gradlew runServer                       # Run development server

# Generate VSCode debug config
.\gradlew.bat generateVSCodeLaunch
```

Build output: `build/libs/*.jar` - copy to Hytale's `Mods` folder for testing.

## Architecture Pattern: Core/Adapter Split

All plugins follow a **clean architecture pattern** separating business logic from Hytale-specific code:

```
src/main/java/com/kingc/hytale/{plugin}/
├── api/              # Abstractions (CommandSource, PlayerRef, Location, ServerAdapter)
├── model/            # Domain entities
├── service/          # Business logic
├── storage/          # JSON persistence
├── command/          # Command dispatcher
└── hytale/           # Hytale-specific adapters only
    ├── Hytale{Plugin}Plugin.java   # Extends JavaPlugin
    ├── Hytale{Entity}.java         # Adapts API abstractions
    └── {Feature}Command.java       # Hytale command implementations
```

**Key principle**: Core logic (`{Plugin}Plugin.java`, services) has NO Hytale imports. Only the `hytale/` package contains Hytale dependencies.

## Hytale Plugin Lifecycle

```java
public class HytalePlugin extends JavaPlugin {
    public HytalePlugin(@Nonnull JavaPluginInit init) { super(init); }

    @Override protected void setup() { }    // Pre-world initialization
    @Override public void start() { }       // Register commands/listeners
    @Override protected void shutdown() { } // Cleanup
}
```

## Key Hytale API Patterns

### Commands
```java
CommandManager.get().register(new MyCommand());

public class MyCommand extends CommandBase {
    private final RequiredArg<String> nameArg = withRequiredArg("name", "desc", ArgTypes.STRING);

    @Override protected void executeSync(CommandContext ctx) {
        String value = nameArg.get(ctx);
        ctx.sendMessage(Message.raw("Hello"));
    }
}
```

### Events
```java
getEventRegistry().registerGlobal(PlayerConnectEvent.class, this::onConnect);

// Async events (chat)
getEventRegistry().registerAsyncGlobal(PlayerChatEvent.class, future ->
    future.thenApply(event -> { event.setCancelled(true); return event; })
);
```

### ECS (Entity Component System)
```java
Player player = store.getComponent(ref, Player.getComponentType());
Transform transform = store.getComponent(ref, Transform.getComponentType());

// Teleportation via component
world.execute(() -> {
    Teleport tp = new Teleport(world, new Vector3d(x, y, z), new Vector3f(yaw, pitch, 0));
    store.addComponent(ref, Teleport.getComponentType(), tp);
});
```

### Threading
- No built-in scheduler - use `ScheduledExecutorService` for async tasks
- Use `world.execute(() -> { })` for thread-safe world modifications

### UI Pages
```java
public class MenuPage extends InteractiveCustomUIPage<MenuData> {
    @Override public void build(Ref<EntityStore> ref, UICommandBuilder cmd, UIEventBuilder event, Store<EntityStore> store) {
        cmd.page("Pages/Menu.ui");
        cmd.text("#title", "My Menu");
        event.onClick("#button", "action:click");
    }
}
// Open: player.getPageManager().openCustomPage(ref, store, new MenuPage(...));
```

## Data Storage

All plugins use JSON file persistence with backup:
- `data/config.json` - Runtime settings (reloadable)
- `data/{entities}.json` - Persistent data (factions.json, kits.json, rankings.json)

## Plugin Manifest

Located at `src/main/resources/manifest.json`:
```json
{
  "Group": "KingC",
  "Name": "PluginName",
  "Version": "0.2.1",
  "Main": "com.kingc.hytale.plugin.hytale.HytalePlugin",
  "IncludesAssetPack": true
}
```

## Important Configuration Files

- `gradle.properties` - Version, Java version, asset pack flags
- `manifest.json` - Plugin metadata (name, version, main class)
- `build.gradle` / `build.gradle.kts` - Build configuration

## Reference Documentation

- `developer_guide.md` - Comprehensive reverse-engineered Hytale API documentation (1000+ lines)
- `hytale_classes.txt` - 5218 extracted API class names from HytaleServer.jar
- Individual plugin READMEs contain command lists and configuration options

---

# Règles de génération de code – Plugins Hytale (IA)

## 1. Objectif du document

Ce document définit **les règles impératives que toute IA doit respecter** lors de la génération de code pour des plugins Hytale.

Le périmètre est **strictement limité** aux deux sources suivantes :

* `developer_guide.md`
* `hytale_classes.txt`

Toute génération de code, d'architecture ou d'exemple **doit être traçable** vers ces documents.

Si une signature ou un usage manque dans `developer_guide.md`, il faut **d'abord** l'extraire depuis `HytaleServer.jar` (ex: `javap`), puis **mettre a jour** `developer_guide.md` avant d'utiliser l'API.

---

## 2. Sources autorisées (liste fermée)

L'IA **n'est autorisée à utiliser que** :

* Les classes explicitement listées dans `hytale_classes.txt`
* Les patterns, signatures et exemples présents dans `developer_guide.md`

👉 **Toute classe, méthode ou concept absent de ces fichiers est considéré comme inexistant.**

Exemple concret :

* ❌ `@EventHandler`, `Listener`, `BukkitScheduler` → **INTERDIT** (absents des sources)
* ✅ `EventRegistry.registerGlobal(...)` → **AUTORISÉ** (documenté)

---

## 3. Règles générales de génération

### 3.1 Interdiction d'extrapolation

L'IA ne doit **jamais** :

* Deviner une API
* S'inspirer de Bukkit / Spigot / Fabric / Forge
* Compléter une méthode « probable »

Exemple :

* ❌ `player.teleport(location)`
* ✅ Ajout d'un composant `Teleport` via `store.addComponent(...)`

---

### 3.2 Respect strict de l'ECS (Entity Component System)

Le modèle Hytale est **ECS-centric**.

Règles :

* Ne jamais manipuler directement un joueur comme un objet métier
* Toujours passer par `Ref<EntityStore>`, `Store<EntityStore>` et des composants

Exemple correct :

* Récupération du composant `Player` depuis le `Store`
* Ajout d'un composant (`Teleport`, `Inventory`, etc.) plutôt qu'un appel direct

---

## 4. Cycle de vie d'un plugin

Tout plugin doit :

1. Étendre `JavaPlugin`
2. Définir un constructeur `JavaPluginInit`
3. Implémenter correctement :

   * `setup()` → initialisation
   * `start()` → enregistrements
   * `shutdown()` → libération des ressources

Exemple attendu :

* Enregistrement des commandes dans `start()`
* Arrêt d'un `ExecutorService` dans `shutdown()`

---

## 5. Commandes

### 5.1 Règles impératives

* Toute commande **doit** étendre `CommandBase`
* Les arguments doivent utiliser `RequiredArg`, `OptionalArg` ou `DefaultArg`
* Les types doivent provenir de `ArgTypes`

Exemple valide :

* Commande `tp` avec `ArgTypes.PLAYER_REF`

Exemple invalide :

* Parsing manuel de `String[] args`

---

## 6. Événements

### 6.1 Enregistrement

Seul le système suivant est autorisé :

* `EventRegistry.registerGlobal(...)`
* `EventRegistry.registerAsyncGlobal(...)`

❌ Les annotations de type `@EventHandler` sont interdites.

### 6.2 Chat (asynchrone)

* Toute logique de chat **doit être thread-safe**
* Les modifications se font via `CompletableFuture.thenApply(...)`

Exemple correct :

* Modification du contenu via `event.setContent(...)`

---

## 7. Threading et sécurité

### 7.1 Asynchrone

Autorisé :

* `ScheduledExecutorService`
* `CompletableFuture`

Interdit :

* Toute API de scheduling externe

### 7.2 Accès au monde

Toute modification du monde **doit obligatoirement** être exécutée via :

```
world.execute(() -> { ... });
```

Exemple concret :

* Téléportation
* Modification d'entités
* Ajout de composants ECS

---

## 8. Interface Utilisateur (UI)

### 8.1 Principe

Les interfaces sont :

* Basées sur des fichiers `.ui`
* Contrôlées via `InteractiveCustomUIPage`

### 8.2 Règles

* Toujours étendre `InteractiveCustomUIPage<T>`
* Séparer clairement :

  * `build()` → rendu
  * `handleDataEvent()` → logique

Exemple correct :

* Bouton déclenchant une action via `data.getAction()`

---

## 9. Interdictions formelles

L'IA ne doit **jamais** générer :

* Des classes non listées dans `hytale_classes.txt`
* Des méthodes inexistantes
* Des imports non présents dans les sources
* Des comportements « inspirés » d'autres moteurs

Exemple interdit :

* `PlayerJoinEvent event` avec méthodes non documentées

---

## 9. Système de Traduction i18n (Internationalisation)

### 9.1 Architecture i18n

Tous les plugins doivent supporter **plusieurs langues** via un système de traduction centralisé.

**Composants requis** :

1. **Fichier JSON** : `messages.json` ou `{plugin}_messages.json`
2. **Service de traduction** : `TranslationService` ou `{Plugin}TranslationService`
3. **Classe Message** : Wrapper avec support placeholders

### 9.2 Structure du Fichier JSON

**Format obligatoire** : Clés hiérarchiques avec traductions par langue.

```json
{
  "error.not_found": {
    "en": "Item not found.",
    "fr": "Objet non trouvé."
  },
  "faction.create.success": {
    "en": "Faction {name} created!",
    "fr": "Faction {name} créée !"
  }
}
```

**Conventions de nommage** :
- `error.*` : Messages d'erreur
- `{feature}.{action}.{type}` : Organisation hiérarchique
- Placeholders : `{variable}` (remplacés dynamiquement)

### 9.3 Pattern Result<T> avec i18n

**OBLIGATOIRE** : Les services métier doivent retourner des **clés de traduction**, pas des messages hardcodés.

```java
public record Result<T>(
    boolean ok, 
    String messageKey,      // Clé de traduction (ex: "error.not_found")
    T value,
    Map<String, String> args // Arguments pour placeholders
)
```

**Service métier** :
```java
// ❌ INTERDIT
return Result.error("You are not in a faction.");

// ✅ AUTORISÉ
return Result.error("error.not_in_faction");

// ✅ AUTORISÉ avec placeholders
return Result.ok("faction.create.success", faction, Map.of("name", faction.name()));
```

### 9.4 Traduction dans les Commandes

**Pattern recommandé** : Helper `translateResult()` dans le dispatcher.

```java
private String translateResult(Result<?> result, String language) {
    Message msg = translator.getMessage(result.messageKey(), language);
    
    // Remplacer placeholders
    for (Map.Entry<String, String> entry : result.args().entrySet()) {
        msg = msg.replace(entry.getKey(), entry.getValue());
    }
    
    return msg.get(language);
}
```

**Utilisation** :
```java
@Override
protected void executeSync(CommandContext ctx) {
    String language = getPlayerLanguage(ctx.sender());
    Result<Faction> result = service.createFaction(playerId, name);
    
    String message = translateResult(result, language);
    ctx.sendMessage(Message.raw(message));
}
```

### 9.5 Règles i18n Impératives

**À FAIRE** :
- ✅ Toujours utiliser des clés de traduction
- ✅ Utiliser placeholders `{variable}` pour valeurs dynamiques
- ✅ Fournir traductions EN + FR minimum
- ✅ Nommer clés de manière hiérarchique

**À ÉVITER** :
- ❌ Messages hardcodés : `"Faction created!"`
- ❌ Concaténation : `"Hello " + name`
- ❌ Clés dupliquées dans JSON
- ❌ Traductions incomplètes

### 9.6 Checklist i18n

Avant de générer du code avec messages :

- [ ] Vérifier que `messages.json` existe
- [ ] Créer clés de traduction pour nouveaux messages
- [ ] Utiliser `Result<T>` avec messageKey
- [ ] Ajouter Map.of() pour placeholders si nécessaire
- [ ] Tester avec EN et FR

**Référence** : Voir FancyCore et HytaleFactions pour implémentation complète.

---

## 10. Vérification avant réponse (obligatoire)

Avant de produire une réponse, l'IA doit mentalement valider :

* [ ] Chaque classe est présente dans `hytale_classes.txt`
* [ ] Chaque pattern existe dans `developer_guide.md`
* [ ] Aucun concept externe n'est utilisé
