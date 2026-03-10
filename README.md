# AuthMeReloadedSHRIMPCRAFTVERSION
**"The best authentication plugin for SHRIMCRAFT BY MP3"**

<img src="wallpaper.png?raw=true" alt="AuthMeLogo"/>

| Type              | Badges                                                                                                                                                                                                                                                                                                                                                                              |
|-------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Code quality:** | S++ the best                                             |
| **Jenkins CI:**   | what |
| **Other CIs:**    | 100% working and aslo its shrimpcraft themed                                                                                                                                                                                                                                                           |

## Description

prevent stealing usernames<br>
use it to secure offline and even online mode thats kinda crazy
authmereloadedshrimpcraft versions executes any players tryna hack ur server<br>
also it can ban people from specific countires
configuration file exists

if u make a translationf ile pls share it with authme devs not me tho

## FEATURES THAT I ADDED

### I added shrimpbot

#### shrimpbot makes it so ingame you could do /contactadmin which connects u with an admin

### I added news and helptext

### teleports you to a server lobby

#### /lockdown /kickall /muteall /freeze <player> /unfreeze <player> accessible through both console and discord

#### the above commands can also be accessed in minercraft if they are trusted (someone gets trusted if mp3 in console or in discord runs /trust <player>)

#### Features:
<ul>
  <li><strong>E-Mail Recovery System!</strong></li>
  <li>Username spoofing protection.</li>
  <li>Countries Whitelist/Blacklist! <a href="https://dev.maxmind.com/geoip/legacy/codes/iso3166/">(country codes)</a></li>
  <li><strong>Built-in AntiBot System!</strong></li>
  <li><strong>ForceLogin Feature: Admins can login with all account via console command!</strong></li>
  <li><strong>Avoid the "Logged in from another location" message!</strong></li>
  <li>Two-factor (2FA) support!</li>
  <li>Session Login!</li>
  <li>Editable translations and messages!</li>
  <li><strong>MySQL and SQLite Backend support!</strong></li>
  <li>Supported password encryption algorithms: SHA256, ARGON2, BCRYPT, PBKDF2, <a href="https://github.com/CypherX/xAuth/wiki/Password-Hashing">xAuth</a></li>
  <li>Supported alternative registration methods:<br>
  <ul>
    <li>PHPBB, VBulletin: PHPBB - MD5VB</li>
    <li>Xenforo: XFBCRYPT</li>
    <li>MyBB: MYBB</li>
    <li>IPB3: IPB3</li>
    <li>IPB4: IPB4</li>
    <li>PhpFusion: PHPFUSION</li>
    <li>Joomla: JOOMLA</li>
    <li>WBB3: WBB3*</li>
    <li>SHA512: SALTEDSHA512</li>
    <li>DoubleSaltedMD5: SALTED2MD5</li>
    <li>WordPress: WORDPRESS</li>
    <li><a href="https://github.com/AuthMe/AuthMeReloaded/blob/master/docs/hash_algorithms.md">List of all supported hashes</a></li>
  </ul></li>
  <li>Custom MySQL tables/columns names (useful with forum databases)</li>
  <li><strong>Cached database queries!</strong></li>
  <li><strong>Fully compatible with Citizens2, CombatTag, CombatTagPlus!</strong></li>
  <li>Compatible with Minecraft mods like <strong>BuildCraft or RedstoneCraft</strong></li>
  <li>Restricted users (associate a username with an IP)</li>
  <li>Protect player's inventory until correct authentication (requires ProtocolLib)</li>
  <li>Saves the quit location of the player</li>
  <li>Automatic database backup</li>
  <li>Available languages: <a href="https://github.com/AuthMe/AuthMeReloaded/blob/master/docs/translations.md">translations</a></li>
  <li>Built-in Deprecated FlatFile (auths.db) to SQL (authme.sql) converter!</li>
  <li><strong>Import your old database from other plugins like Rakamak, xAuth, CrazyLogin, RoyalAuth and vAuth!</strong></li>
  <li><strong>milady</strong></li>
</ul>

#### Configuration
[How to configure AuthMe](https://github.com/AuthMe/AuthMeReloaded/blob/master/docs/config.md)
#### Commands
[Command list and usage](https://github.com/AuthMe/AuthMeReloaded/blob/master/docs/commands.md)
#### Permissions
- authme.player.* - for all user commands
- authme.admin.* - for all admin commands
- [List of all permission nodes](http://github.com/AuthMe/AuthMeReloaded/blob/master/docs/permission_nodes.md)

#### How To
- [How to use the converter](https://github.com/AuthMe/AuthMeReloaded/wiki/Converters)
- [How to import database from xAuth](https://dev.bukkit.org/projects/authme-reloaded/pages/how-to-import-database-from-xauth)
- [Website integration](https://github.com/AuthMe/AuthMeReloaded/tree/master/samples/website_integration)
- [How to convert from Rakamak](https://dev.bukkit.org/projects/authme-reloaded/pages/how-to-import-database-from-rakamak)
- Convert between database types (e.g. SQLite to MySQL): /authme converter


## Links and Contacts

 - **Support:**
   - [GitHub issue tracker](https://github.com/AuthMe/AuthMeReloaded/issues)
   - [Discord](https://discord.gg/Vn9eCyE)
   - [BukkitDev page](https://dev.bukkit.org/projects/authme-reloaded)
   - [Spigot page](https://www.spigotmc.org/resources/authmereloaded.6269/)

- **Dev resources:**
  - <a href="https://ci.codemc.org/job/AuthMe/job/AuthMeReloaded/javadoc/">JavaDocs</a>
  - <a href="http://repo.codemc.org/repository/maven-public/">Maven Repository</a>
  ```xml
    <repositories>
        <repository>
            <id>codemc-repo</id>
            <url>https://repo.codemc.org/repository/maven-public/</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>fr.xephi</groupId>
            <artifactId>authme</artifactId>
            <version>5.6.1-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
  ```

- **Statistics:**
    ![Graph](https://bstats.org/signatures/bukkit/AuthMe.svg)

## Requirements

##### Compiling requirements:
>- JDK 17+
>- Maven (3.8.8+)
>- Git/GitHub (Optional)

##### How to compile the project:
>- Clone the project with Git/GitHub
>- Execute command "mvn clean package -DskipTests"

##### Running requirements:
>- Java 17+
>- Paper or Spigot (1.16.5 and up)
>- ProtocolLib (optional, required by some features)

## Credits

#### the guy who forked authme and added the cool stuff
### thepoiedit

##### Contributors:
Team members: <a href="https://github.com/AuthMe/AuthMeReloaded/wiki/Development-team">developers</a>, <a href="https://github.com/AuthMe/AuthMeReloaded/wiki/Translators">translators</a>

Credits for the old version of the plugin: d4rkwarriors, fabe1337, Whoami2 and pomo4ka

Thanks also to: AS1LV3RN1NJA, Hoeze and eprimex

##### GeoIP License:
This product uses data from the GeoLite API created by MaxMind, available at https://www.maxmind.com
