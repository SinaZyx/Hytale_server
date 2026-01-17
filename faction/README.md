Hytale Factions (PvP)

Ce plugin ajoute des factions avec claims, invites, alliances, UI et protections PvP.
Il est base sur le template officiel Hytale et cible Java 25.

Commandes
- /f (ouvre le menu UI)
- /f menu
- /f create <name>
- /f disband
- /f list
- /f info [faction]
- /f who <player>
- /f map
- /f desc [texte]
- /f rename <name>
- /f invite <player>
- /f accept <faction>
- /f deny <faction>
- /f leave
- /f kick <player>
- /f promote <player>
- /f demote <player>
- /f leader <player>
- /f ally <faction>
- /f unally <faction>
- /f sethome
- /f home
- /f claim
- /f unclaim

Configuration (config.json)
- minNameLength, maxNameLength
- maxMembers
- maxClaims (plafond dur)
- baseClaimLimit, claimLimitPerMember
- basePower, powerPerMember, maxPower
- claimCooldownSeconds, unclaimCooldownSeconds
- claimWorldAllowList, claimWorldDenyList
- notifyOnEnter, notifyOnLeave
- notifyUseTitle, notifyUseChat
- notifyCooldownSeconds
- claimEnterTitle, claimEnterSubtitle
- claimLeaveTitle, claimLeaveSubtitle
- wildernessLabel
- placeholders: {faction}, {wilderness}, {world}
- claimTitleFadeIn, claimTitleStay, claimTitleFadeOut
- mapRadius
- maxDescriptionLength
- chunkSize, allowFriendlyFire, allowAllyBuild, inviteExpiryMinutes

Installation
1) Installer Java 25.
2) Ajuster les valeurs dans gradle.properties si besoin.
3) Mettre a jour src/main/resources/manifest.json avec tes infos.
4) Ouvrir le projet dans IntelliJ IDEA et laisser Gradle synchroniser.
5) Lancer la configuration HytaleServer generee par le template.

Reprendre le projet
1) Ouvre le projet dans IntelliJ IDEA.
2) Verifie JAVA_HOME (Java 25) et que Gradle est bien synchronise.
3) Regarde gradle.properties (includes_pack=true, load_user_mods=true si tu lances depuis l'IDE).
4) Build local: .\gradlew.bat build

Tester en local avant serveur
Option A (via IntelliJ)
1) Dans gradle.properties: load_user_mods=true.
2) Lance la configuration "HytaleServer" generee par le template.
3) Connecte-toi en local et teste /f.

Option B (jar dans Mods)
1) Build: .\gradlew.bat build
2) Copie build\libs\HytaleFactions-0.2.0.jar vers %appdata%\Hytale\UserData\Mods
3) Demarre le serveur puis le client, et teste /f.

Notes
- Les donnees sont sauvegardees dans le dossier du plugin (config.json et factions.json).
- Le menu UI permet d'utiliser les commandes sans taper de texte.
- Affichage du territoire a l'ecran quand tu entres/sors d'un claim (title ou chat selon config).
- /f map affiche une mini carte ASCII des claims autour du joueur.
