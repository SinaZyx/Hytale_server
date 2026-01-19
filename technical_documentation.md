# Documentation Technique - Fancy Core

Cette documentation détaille l'ensemble des fonctionnalités, commandes et points d'ancrage API disponibles dans le module **Fancy Core**.

## 1. Vue d'ensemble des Modules

Fancy Core est divisé en plusieurs modules fonctionnels :
- **Chat** : Gestion avancée du chat, canaux, et messagerie privée.
- **Économie** : Système multi-devises complet.
- **Inventaire** : Sacs à dos virtuels (Backpacks) et Kits.
- **Modération** : Outils de sanction et de rapport.
- **Permissions** : Gestion des droits d'accès.
- **Téléportation** : Système complet de voyage (Homes, Warps, TPA).

---

## 2. Commandes et Actions (Jeu)

### 💬 Module Chat
Commandes pour la gestion de la communication.

| Commande | Description |
| :--- | :--- |
| `/chatcolor` | Ouvre l'interface de sélection de couleur de chat. |
| `/chatcolor set <color>` | Définit une couleur de chat spécifique. |
| `/chatroom create <nom>` | Crée un nouveau canal de discussion. |
| `/chatroom list` | Liste les canaux disponibles. |
| `/chatroom join/switch <nom>` | Rejoint un canal. |
| `/chatroom mute/unmute <nom>` | Coupe/Active les messages d'un canal. |
| `/chatroom info <nom>` | Affiche les infos d'un canal. |
| `/msg <joueur> <message>` | Envoie un message privé. |
| `/reply <message>` | Répond au dernier message privé. |
| `/ignore <joueur>` | Ignore les messages d'un joueur. |
| `/togglemessages` | Active/Désactive la réception de messages privés. |

### 💰 Module Économie
Gestion des finances et des devises multiples.

| Commande | Description |
| :--- | :--- |
| `/balance` ou `/money` | Affiche le solde du joueur. |
| `/pay <joueur> <montant>` | Envoie de l'argent à un autre joueur. |
| `/economy add <joueur> <montant>` | (Admin) Ajoute de l'argent. |
| `/economy remove <joueur> <montant>` | (Admin) Retire de l'argent. |
| `/economy set <joueur> <montant>` | (Admin) Définit le solde. |
| `/currency create/remove` | Gestion des types de monnaies. |
| `/currency list` | Liste les monnaies du serveur. |

### 🎒 Module Inventaire
Utilitaires de stockage et d'équipement.

| Commande | Description |
| :--- | :--- |
| `/backpack` | Ouvre le sac à dos virtuel du joueur. |
| `/createbackpack` | Crée un nouveau type de sac à dos. |
| `/kit create <nom>` | Crée un kit à partir de l'inventaire actuel. |
| `/kit <nom>` | Obtient un kit. |
| `/clearinventory` | Vide l'inventaire. |
| `/openinv <joueur>` | Ouvre l'inventaire d'un autre joueur. |

### 🛡️ Module Modération
Outils pour maintenir l'ordre sur le serveur.

| Commande | Description |
| :--- | :--- |
| `/ban <joueur> [raison]` | Bannissement définitif. |
| `/tempban <joueur> <temps> [raison]` | Bannissement temporaire. |
| `/mute <joueur> [raison]` | Mute définitif. |
| `/tempmute <joueur> <temps> [raison]` | Mute temporaire. |
| `/kick <joueur> [raison]` | Expulse un joueur. |
| `/warn <joueur> [raison]` | Avertit un joueur. |
| `/unban <joueur>` | Révoque un bannissement. |
| `/unmute <joueur>` | Révoque un mute. |
| `/history <joueur>` | (Supposé) Voir l'historique des sanctions. |

### ✈️ Module Téléportation
Déplacements rapides et points de sauvegarde.

| Commande | Description |
| :--- | :--- |
| `/spawn` | Téléportation au spawn du serveur. |
| `/setspawn` | Définit le point de spawn. |
| `/sethome <nom>` | Définit un domicile. |
| `/home <nom>` | Téléporte à un domicile. |
| `/delhome <nom>` | Supprime un domicile. |
| `/listhomes` | Liste vos domiciles. |
| `/setwarp <nom>` | Crée un point de warp public. |
| `/warp <nom>` | Téléporte à un warp. |
| `/delwarp <nom>` | Supprime un warp. |
| `/tpa <joueur>` | Demande de téléportation vers un joueur. |
| `/tpaccept` / `/tpdeny` | Accepte/Refuse une demande. |
| `/tp <joueur>` | Téléportation immédiate (Admin). |
| `/tphere <joueur>` | Téléporte un joueur à soi. |
| `/tppos <x> <y> <z>` | Téléporte aux coordonnées. |
| `/back` | Retourne à la dernière position (ou lieu de mort). |
| `/teleportdeathback` | Retourne au lieu de mort. |

---

## 3. API & Développement (Code)

Le cœur offre une API riche via `fc-api` pour créer des addons ou interagir avec les systèmes.

### Services Principaux
L'accès aux fonctionnalités se fait via les services (Pattern Service/Manager).

- **`ChatService`** :
  - `createChatRoom(...)`, `getChatRoom(...)`
  - Gestion des canaux et de la diffusion des messages.
- **`CurrencyService`** :
  - `getCurrency(...)`, `createCurrency(...)`
  - Manipulation des soldes (`deposit`, `withdraw`).
- **`BackpacksService` / `KitsService`** :
  - Gestion des stockages persistants.
- **`PunishmentService`** :
  - Création et application de sanctions (`PunishmentType`).
- **`FancyPlayerService`** :
  - Accès aux données étendues des joueurs (`FancyPlayer`).
  - Gestion des métadonnées joueur.
- **`PermissionService`** :
  - Gestion de groupes et permissions.

### Événements (Events)
Abonnez-vous à ces événements pour réagir aux actions du jeu.

#### Chat
- `BroadcastMessageSentEvent`
- `PlayerSentMessageEvent`
- `PrivateMessageSentEvent`
- `ChatClearedEvent`
- `PlayerSwitchedChatRoomEvent`

#### Joueur
- `PlayerJoinedEvent` / `PlayerLeftEvent`
- `PlayerModifiedEvent` (Changement de données)

#### Modération
- `PlayerPunishedEvent`
- `PlayerReportedEvent`

#### Serveur
- `ServerStartedEvent`
- `ServerStoppedEvent`

### Exemples d'utilisation (Pseudo-code)

```java
// Écouter un message chat
@EventListener
public void onChat(PlayerSentMessageEvent event) {
    FancyPlayer player = event.getPlayer();
    ChatRoom room = event.getChatRoom();
    // Logique custom...
}

// Ajouter de l'argent
Currency currency = currencyService.getCurrency("Or");
currencyService.deposit(player, currency, 100.0);
```
