package pl.gon_3t.kaigara;

import org.bukkit.inventory.ItemStack;

public class PlayerData{
    Boolean opening = false;
    Boolean checkhand = false;
    Boolean checkdoor = false;
    String usingboxname = "";
    ItemStack[] usingbox;
    PlayerData(){
        opening=false;
        checkhand=false;
        checkdoor=false;
        usingboxname="";
        usingbox=null;
    }
}