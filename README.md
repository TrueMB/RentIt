# RentIt - Shop and Hotel renting - Includes NPC Shop


![grafik](https://user-images.githubusercontent.com/25579052/165747482-de9cc9a3-599b-4c57-85e4-7d3164cdfa7e.png)

Join my Discord to get the newest Versions and fast Support! Invite Links are down below.

# Description:
This Plugin is made for Server owners that want on their server a little bit RPG feeling. You can create with this Plugin Custom Shop and Hotelroom Areas, that your Players can rent! Also it gots an own Permission System for the Shop/Hotelroom owners to make them work together with friends. The Shops containing the Shop Owner NPC there players can buy from.

It works with a completly customzible GUI. You can change everything in the config, since I am saving nearly everything in it.

# Youtube Tutorial:

[![Tutorial](https://img.youtube.com/vi/gcuaus7DY0Q/0.jpg)](https://www.youtube.com/watch?v=gcuaus7DY0Q)


# Important to know:
Since this is a new Shop System and doesnt quite work like others, there is some stuff that you should know.

*Both:*
 - This Plugins works with Categories. So you can set values and easily take them for multiple Areas. 
   Also you can sort them in a category Menu. F.e. in the /freeshops or /freehotels command.

*Shop:*
 - You can remove Shop Buy/Sell Items in the Menu with Rightclick (Shopowner or Player with the buy / sell permission)
 - Players can place a chest in the Shop Area (or players with the build Permission). This Chest counts as a Storage. 
   So if a Player wants to sell multiple Items, they can add the item to the Shop and fill the storage with more of the type. 
   The Buyer will at first get the Items of the storage. If there is nothing left in the chest, it will then take the item out of the shop.
 - You also need a Chest Storage, if you want to buy Items from Sellers. Those Items will be stored in all chests in the Shop. 
   Until there is no space left or the player got no money. You can prevent buying to much, with filling other items in the chest, 
   that are not present in the shop. So there is no space manually.


# Commands:

*%time%* = 7D, 7h, 7m (this is case sensitiv!)


## RentIt Commands
**/rentit reload** - Reloads Config and SQL Connection.

## Shop Commands
**/shops** - Shows all your owned Shops.<br />
**/freeshops** - Shows all free Shops. <br />
**/shop help** - Opens a Help Book<br />
**/shop createCat {categoryID} {Price} {Size} {Time}** - Create a Category.<br />
**/shop deleteCat** - Delete a Category.<br />
**/shop listCat** - Lists all Categories.<br />
**/shop list** - Lists all Shops.<br />
**/shop setarea {categoryID}** - Set the selected Region to the Shop category.<br />
**/shop setnpc** - Set or Change the Position of the Shop NPC.<br />
**/shop setprice {Price}** - Change the Rent Price. (Changes the category Settings)<br />
**/shop setsize {Size}** - Change the Shop Size. (Changes the category Settings)<br />
**/shop settime {Time}** - Change the Rent Time. (Changes the category Settings)<br />
**/shop reset** - Reset the Shop, so it can get a new Owner.<br />
**/shop delete** - Delete the Shop completly.<br />
**/shop updateBackup** - Creates a new Backup of the current Shop Area<br />
**/shop buy {shopId}** - Rent a specific Shop.<br />
**/shop sellitem {Price}** - Sell the Item in your Hand in the Shop.<br />
**/shop buyitem {Price}** - Add the Item in your Hand to the Buy Shop.<br />
**/shop buyitem {Type} {Price}** - Add the named Item to the Buy Shop.<br />
**/shop buyitem {Type} {Item Amount} {Price}** - Add the named Item with the given amount to the Buy Shop.<br />
**/shop setpermission {Player} {Permission} {Value}** - Set Permissions for Players (Shop admins only).<br />
**/shop permissions** - All Shop Permissions.<br />
**/shop users** - List all Players with Shop Permissions.<br />
**/shop noInfo** - Get no Informations of Transactions.<br />
**/shop info** - Get informations from the Shop you are standing in.<br />
**/shop info {ID}** - Get informations of the Shop with the ID.<br />
**/shop door add {ID}** - Add Doors to a Shop.<br />
**/shop door remove** - Remove a door from a Shop.<br />
**/shop door open** - Open the doors in your own Shop.<br />
**/shop door close** - Close the doors in your own Shop.<br />
**/shop rollback** - Opens a menu with the forgotten items of the Shop.<br />
**/shop rollback %target%** - Admin Command to get the Targets lost Items or to check them.<br />
**/shop setAlias %alias%** - Set a different Name for the Shop.<br />
**/shop setAliasCat %catAlias%** - Set a different Name for the Category of the Shop you are standing in.<br />
**/shop resign** - Ends your running Contract.<br />

## Hotelroom Commands
**/hotels** - Shows all your owned Hotelrooms.<br />
**/freehotels** - Shows all free Hotelrooms.<br />
**/hotel help** - Opens a Help Book.<br />
**/hotel createCat {categoryID} {Price} {Time}** - Create a Category.<br />
**/hotel deleteCat** - Delete a Category.<br />
**/hotel listCat** - Lists all Categories.<br />
**/hotel list** - Lists all Hotels.<br />
**/hotel setarea {categoryID}** - Set the selected Region to the Hotelroom category.<br />
**/hotel setpermission {Player} {Permission} {Value}** - Set Permissions for Players (Hotelroom admins only).<br />
**/hotel permissions** - All Hotelroom Permissions.<br />
**/hotel users** - List all Players with Hotelroom Permissions.<br />
**/hotel setprice {Price}** - Change the Rent Price. (Changes the category Settings)<br />
**/hotel settime {Time}** - Change the Rent Time. (Changes the category Settings)<br />
**/hotel reset** - Reset the Hotelroom, so it can get a new Owner.<br />
**/hotel delete** - Delete the Hotelroom completly.<br />
**/hotel updateBackup** - Creates a new Backup of the current Hotel Area<br />
**/hotel buy {shopId}** - Rent a specific Hotelroom.<br />
**/hotel info** - Get informations from the Hotelroom you are standing in.<br />
**/hotel info {ID}** - Get informations of the Hotel with the ID.<br />
**/hotel door add {ID}** - Add Doors to a Hotelroom.<br />
**/hotel door remove** - Remove a door from a Hotelroom.<br />
**/hotel door open** - Open the doors in your own Hotelroom.<br />
**/hotel door close** - Close the doors in your own Hotelroom.<br />
**/hotel setAlias %alias%** - Set a different Name for the Hotelroom.<br />
**/hotel setAliasCat %catAlias%** - Set a different Name for the Category of the Hotelroom you are standing in.<br />
**/hotel resign** - Ends your running Contract.<br />

# Permissions:
All Permissions can be found in the config. You could also change the name of the permission.

**rentit.shop** - Permission for all Shop Commands<br />
**rentit.hotel** - Permission for all Hotel Commands<br />
**rentit.sign** - Creating a Shop or Hotel sign<br />
**rentit.build** - Build bypass in Shops and Hotels<br />

# PlaceholderAPI:
**%rentit_currentId%** - returns the ID of the Hotel/Shop, that the player is standing in.<br />
**%rentit_currentType%** - returns the Type (Hotel/Shop), that the player is standing in.<br />
**%currentOwner%** - returns the Owner of the Shop/Hotel, that the player is standing in.<br />

# Installation:

## Dependencies:
This Plugin needs to have Vault installed, so that I can support nearly any Economy System.
Worldedit for the calculations. 
Citizens is needed for PlayerNPCs! You can turn them off in the config and dont need to use Citizens (Options.useNPCs = false)

#### Soft-Dependencies:
WorldGuard is supported. There will be no build errors in hotel/shop areas, if it is f.e. in the Spawn Saved Area.

## TODO:
1. Put all the .jars (worldedit.jar, vault.jar, citizens.jar*, rentit.jar) and your Economy Plugin in your "/plugins/" directory of your server.
2. Then please restart the Server
3. It will create the config in the "RentIt" Folder. You need to edit the config and insert your database connection to make it work. **Please use the Fork MariaDB, since MySQL doesn't work**
4. (optional) Look through the Config entrys and customize the language and setting to your like!

* SoftDependency

## How To Use:

#### Shop Setup for Admins:
1. Create a category for the Shop. f.e. /shop createCat 1 100 9 7D
   This means if a player wants to rent a Shop, with the category ID 1, then he has to pay 100$ every 7 Days. The Shop Slot Size will be 9.
   IMPORTANT: (Date letters need to be  case sensitive)
2. Then you need to select a square. (f.e. the air in a room). Players will be able to build in that region. 
   So please watchout that you dont select Stuff, that players shouldnt break.
3. Run then the command /shop setArea 1 to link that region with the Category. Your standing Position will be the teleport place for players.
4. After that you need to set the NPC Location (/shop setNPC). That one is the Shop Interacter later on.
5. Create then a Sign for the Players. (Permission: **rentit.sign**)
    [Shop]
    %ShopID%

With Shift+Rightclick Admins can change the category settings.


#### Hotel Setup for Admins:
1. Create a category for the Hotel. f.e. /hotel createCat 1 100 7D
   This means if a player wants to rent a hotelroom, with the category ID 1, then he has to pay 100$ every 7 Days.
   IMPORTANT: (Date letters need to be  case sensitive)
2. Then you need to select a square. (f.e. the air in a room). Players will be able to build in that region. 
   So please watchout that you dont select Stuff, that players shouldnt break.
3. Run then the command /hotel setArea 1 to link that region with the Category. Your standing Position will be the teleport place for players.
4. Create then a Sign for the Players. (Permission: **rentit.sign**)
    [Hotel]
    %HotelID%

With Shift+Rightclick Admins can change settings. They will be effected on the category.

## Category Settings:
This are Settings in the Config for specific Categories for Shops and Hotelrooms:

**usePermission** -> Are Permissions needed, to buy such a Shop/Hotel<br />
**autoPaymentDefault** -> The Default Status of the auto Payment<br />
**autoPaymentDisabled** -> If auto Payment should be changeable<br />
**maxRentExtendAmount** -> The Max Amount a Player can extend the rent<br />
**disableDoorCommand** -> Disables the door command<br />
**ownerBypassLock** -> Should Owner or Players with Permission be allowed to open locked doors<br />
**doorsClosedUntilBuy** -> <true/false> Should the doors be closed until a player bought the Shop/hotelroom? True Allows player to look, until someone rents it and does /shop door close<br />
**doorsClosedAfterBuy** -> the default status, after a player bought a Shop or Hotelroom<br />
**allowUsersToMoveNPC** -> Allows Shop Owners to use the /shop setnpc command and to move their NPC.<br />
**reminderRentRunningOut** -> Reminds the owner the given time before the rent end, if no auto payment enabled, that the rent is going to end.<br />

```
     categorySettings:
         ShopCategory:
             1:
                 usePermission: false
                 autoPaymentDefault: true
                 autoPaymentDisabled: true
                 maxRentExtendAmount: 5
                 disableDoorCommand: false
                 ownerBypassLock: true
                 doorsClosedUntilBuy: false
                 doorsClosedAfterBuy: false
                 reminderRentRunningOut: '1m30s'
             2:
                 usePermission: true
                 autoPaymentDefault: true
                 autoPaymentDisabled: false
                 maxRentExtendAmount: 3
                 disableDoorCommand: false
                 ownerBypassLock: true
                 doorsClosedUntilBuy: false
                 doorsClosedAfterBuy: false
         HotelCategory:
             1:
                 usePermission: false
                 autoPaymentDefault: true
                 autoPaymentDisabled: false
                 maxRentExtendAmount: 5
                 disableDoorCommand: false
                 ownerBypassLock: true
                 doorsClosedUntilBuy: false
                 doorsClosedAfterBuy: false
                 reminderRentRunningOut: '1m'
             2:
                 usePermission: true
                 autoPaymentDefault: true
                 autoPaymentDisabled: false
                 maxRentExtendAmount: 3
                 disableDoorCommand: false
                 ownerBypassLock: true
                 doorsClosedUntilBuy: false
                 doorsClosedAfterBuy: false

```

## Shop Use for Players:
To rent a Shop, the player only needs to click on a Sign and accept the contract.

 - He then can give a friend f.e. the permission to "build", "sell", "buy", "fill" or all the permissions with "admin". (listet with /shop permissions) 
   See "Permissions" for more information.
 - To give a Player a Permission, the owner of the Shop needs to run: /shop setpermission <Player> <Permission> <true/false>
 - Selling Items works with /shop sellitem <price>. It will sell the Item with the exact amount in the players hand for the given price. 
   After that he can create Chests with more Items in it. The Shop will sell then first the Storage Item.
 - To buy an Item from players, the Player needs to use /shop buyitem <Price> to buy the item in the hand or 
   /shop buyitem <Material> <Amount> <Price> to buy another item with a custom amount. The items will be filled in a shop chest with free space.
 - If the players doesnt want to get an information, he can use /shop noinfo to disable it for a specific shop.


## Hotel Use for Players:
To rent a Hotelroom, the player only needs to click on a Sign and accept the contract.

 - He then can give a friend f.e. the permission to "build" or all the permissions with "admin". (listet with /hotel permissions) 
   See "Permissions" for more information.
 - To give a Player a Permission, the owner of the Hotelroom needs to run: /hotel setpermission <Player> <Permission> <true/false>


# Permissions:

    build - build permission
    fill - can interact with chest, to fill items in it or remove them
    sell - selling Items in the Shop with the command /shop sellitem...
    buy - buying Items in the Shop with the command /shop buyitem..
    door - allows to go through locked doors and be able to un/lock them.
    admin - all permissions


 # Time Format:

    Y – Years
    M – Months
    D – Days
    H – Hours
    m – Minutes


# Contact Informations:
        
#### Discord:
 - Channel #spigotmc-en: https://discord.gg/8BVftSkwV8
 - Channel #spigotmc-de: https://discord.gg/N3BBjeb3DC

#### Spigot: 
https://www.spigotmc.org/threads/rentit-shop-and-hotel-renting.495468/

If you like this plugin, please consider to rate it on Spigot. And if you like to [donate](https://paypal.me/truemb).<br />
New Bugs will be shortly fixed!

