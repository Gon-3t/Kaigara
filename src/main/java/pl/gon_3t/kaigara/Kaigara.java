package pl.gon_3t.kaigara;

import java.util.Arrays;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class Kaigara extends JavaPlugin implements Listener {
    static final String prefix_pl="§5[§d開殻§5]§f";
    static final String prefix_error="§4";
    static final String version = "1.4";
    static final int MSG_HELP=0;
    static final int MSG_UNKNOWN_COMMAND=1;
    static final int MSG_SHULKER_OPEN=2;
    static final int MSG_SHULKER_SAVED=3;
    static final int MSG_SHULKER_NOTSAVED=4;
    static final int MSG_SHULKER_ANOTHER_ONE=5;
    static final int MSG_CANTOPEN_IT=6;
    static final int MSG_NULL=7;
    static final String[] messages_jp={
        prefix_pl+"使い方：開けたいｼｭﾙｶｰﾎﾞｯｸｽを手に持ち、クリックもしくは\"/kaigara open\"を実行します。",
        prefix_pl+prefix_error+"エラー：不明なコマンドです。\"/kaigara help\"で利用方法を確認できます。",
        prefix_pl+"%sを開きます。",
        prefix_pl+"%sに内容を保存しました。",
        prefix_pl+prefix_error+"エラー：シュルカーボックスを保存できませんでした。",
        prefix_pl+prefix_error+"エラー：持っているｼｭﾙｶｰﾎﾞｯｸｽが異なります。内容は保障されません。",
        prefix_pl+prefix_error+"エラー：それを開くことはできません。",
        prefix_pl+prefix_error+"エラー：null。"
    };
    static final String[] messages_en={
        prefix_pl+"How to use: Hold the shulker box that you want to open, then punch it or type command \"/kaigara open\".",
        prefix_pl+prefix_error+"Error: Unknown command. type \"/kaigara help\" to show help.",
        prefix_pl+"Opening %s.",
        prefix_pl+"Contents has been saved to %s.",
        prefix_pl+prefix_error+"Error: Couldn't save contents to the shulker box.",
        prefix_pl+prefix_error+"Error: You have another box in your hand. The contents won't be guaranteed.",
        prefix_pl+prefix_error+"Error: You can't open it.",
        prefix_pl+prefix_error+"Error: null"
    };
    //プレイヤーごとのマップ多すぎ問題。クラス作ろうかな...
    //プレイヤーごとにシェルカーを開けているかどうか保持
    Map<UUID, Boolean> opening = new HashMap<UUID, Boolean>();
    //プレイヤーごとにドア開扉時の暴発防止用フラグを保持
    Map<UUID, Boolean> checkhand = new HashMap<UUID, Boolean>();
    //プレイヤーごとに手持ちの正当性の検証用フラグを保持
    Map<UUID, Boolean> checkdoor = new HashMap<UUID, Boolean>();
    //プレイヤーごとに手持ちの正当性の検証用フラグを保持
    Map<UUID, String> usingboxname = new HashMap<UUID, String>();
    //プレイヤーごとに開いた段階のシュルカー内容物を保持
    Map<UUID, ItemStack[]> usingbox = new HashMap<UUID, ItemStack[]>();

    @Override
    public void onEnable(){
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("[開殻 -Kaigara- v"+version+" by Gon_3t]");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 0) {
            // 開くコマンド(o,open)
            if(args[0].equalsIgnoreCase("o")||args[0].equalsIgnoreCase("open")){
                if(sender.getName().equals("CONSOLE")){
                    sender.sendMessage("[Kaigara] CONSOLE can't open shulker box...");
                    return true;
                }
                kaigaraOpen(getServer().getPlayer(sender.getName()),null);
                return true;
            }
            // ヘルプ(h,help)
            else if(args[0].equalsIgnoreCase("h")||args[0].equalsIgnoreCase("help")){
                sendLocalizedMessage(getServer().getPlayer(sender.getName()), MSG_HELP);
            }
            // それ以外
            else{
                sendLocalizedMessage(getServer().getPlayer(sender.getName()), MSG_UNKNOWN_COMMAND);
            }
        }
        //引数なし
        sendLocalizedMessage(getServer().getPlayer(sender.getName()), MSG_HELP);
        return true;
    }
   
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e){
        //プレイヤーのUUID取得
        UUID uuid=e.getPlayer().getUniqueId();
        //ドア開けた後ターゲットが空気だとRIGHT_CLICK_BLOCKの直後にLEFT_CLICK_AIRが2回発動する
        //そのため、RIGHT_CLICK_BLOCKが発動した後の一瞬シェルカーオープンを無効にする
        if(e.getAction()==Action.RIGHT_CLICK_BLOCK){
            //プレイヤーのチェックドアをtrueに
            checkdoor.put(e.getPlayer().getUniqueId(),true);
            //2ﾃｨｯｸ後に解除(2ﾃｨｯｸが最適とは限らない)
            getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                public void run() {
                    checkdoor.put(e.getPlayer().getUniqueId(),false);
                }
            }, 15L);
        }
        //シェルカーボックスを手に持った状態で任意のブロックを左クリック
        else if(e.getAction()==Action.LEFT_CLICK_AIR||e.getAction()==Action.LEFT_CLICK_BLOCK){
            if(checkdoor.containsKey(uuid)){
                if(checkdoor.get(uuid)){
                    //プレイヤーのチェックドアフラグが有効な場合は何もしない
                    return;
                }
            }
            if(e.getItem()!=null){
                if(e.getItem().getItemMeta() instanceof BlockStateMeta){
                    if(((BlockStateMeta) e.getItem().getItemMeta()).getBlockState() instanceof ShulkerBox){
                        //kaigaraOpenを入れる
                        kaigaraOpen(e.getPlayer(),e);
                    }
                }
            }    
        }
    }
        
    

    @EventHandler
    public void onGuiClose(InventoryCloseEvent e){
        UUID uuid=e.getPlayer().getUniqueId();
        //ｼｭﾙｶｰをあけているプレイヤーがGUIを閉じた時
        if(opening.containsKey(uuid)){
            if(opening.get(uuid)){
                ItemStack box = e.getPlayer().getInventory().getItemInMainHand();
                opening.put(uuid, false);
                checkhand.put(uuid, false);
                if(box.getItemMeta() instanceof BlockStateMeta){
                    BlockStateMeta blockMeta = (BlockStateMeta) box.getItemMeta();
                    if(blockMeta.getBlockState() instanceof ShulkerBox){
                        ShulkerBox shulker = (ShulkerBox) blockMeta.getBlockState();
                        if(Arrays.equals(usingbox.get(uuid),shulker.getInventory().getContents())){
                            Inventory inv = e.getInventory();
                            shulker.getInventory().setContents(inv.getContents());
                            blockMeta.setBlockState(shulker);
                            box.setItemMeta(blockMeta);
                            e.getPlayer().getInventory().setItemInMainHand(box);
                            String dispname = "Shulker Box";
                            if(box.getItemMeta().hasDisplayName()){
                                //名前付きのｼｭﾙｶｰﾎﾞｯｸｽならその名前に
                                dispname = box.getItemMeta().getDisplayName();
                            }else if(box.getItemMeta().hasLocalizedName()){
                                //名前がなければローカライズされた名前に
                                dispname = box.getItemMeta().getLocalizedName();
                                //どちらでもなければdispnameの初期値をそのまま使用
                            }
                            getServer().getPlayer(uuid).playSound(getServer().getPlayer(uuid).getLocation(), Sound.BLOCK_SHULKER_BOX_CLOSE, 0.5f, 0.9f+(float)Math.random()*0.1f);
                            sendLocalizedMessage(getServer().getPlayer(uuid), MSG_SHULKER_SAVED, dispname);
                        }else{
                            sendLocalizedMessage(getServer().getPlayer(uuid), MSG_SHULKER_NOTSAVED);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryMoved(InventoryClickEvent e){
        UUID uuid=e.getWhoClicked().getUniqueId();
        ItemStack mainhand = e.getWhoClicked().getInventory().getItemInMainHand();
        if(opening.containsKey(uuid)){
            if(opening.get(uuid)){
                if(!checkhand.containsKey(uuid)){
                    checkhand.put(uuid,false);
                }
                //まだ正当性が未確認であれば
                if(!checkhand.get(uuid)){
                    if(!(e.getClick()==ClickType.LEFT||e.getClick()==ClickType.RIGHT)){
                        //アイテム増殖およびロスト防止の為最初は通常のクリック以外禁止
                        //ほんとは他のクリックイベントハンドルした方がいいかも。
                        e.setCancelled(true);
                        return;
                    }
                    if(mainhand==null){
                        //手に何も持っていないのに開けている場合の例外処理
                        e.setCancelled(true);
                        e.getWhoClicked().closeInventory();
                        e.getWhoClicked().sendMessage(prefix_pl+prefix_error+"エラー：null。");
                        opening.put(uuid, false);
                        return;
                    }else if(mainhand.getItemMeta() instanceof BlockStateMeta){
                        BlockStateMeta blockMeta = (BlockStateMeta) mainhand.getItemMeta();
                        if(blockMeta.getBlockState() instanceof ShulkerBox){
                            //正しいｼｭﾙｶｰﾎﾞｯｸｽか?
                            ShulkerBox shulker = (ShulkerBox) blockMeta.getBlockState();
                            if(!Arrays.equals(usingbox.get(uuid),shulker.getInventory().getContents())){
                                //開けた時とは中身が異なるｼｭﾙｶｰﾎﾞｯｸｽを持っている
                                //ｼｭﾙｶｰﾎﾞｯｸｽの名前とかでも判定したほうがいいかも
                                //アイテムにuuidとか…ないか
                                e.setCancelled(true);
                                e.getWhoClicked().closeInventory();
                                sendLocalizedMessage(getServer().getPlayer(uuid), MSG_SHULKER_ANOTHER_ONE);
                                opening.put(uuid, false);
                                return;
                            }
                        }else{
                            //持っているものがｼｭﾙｶｰﾎﾞｯｸｽでない
                            e.setCancelled(true);
                            e.getWhoClicked().closeInventory();
                            sendLocalizedMessage(getServer().getPlayer(uuid), MSG_CANTOPEN_IT);
                            opening.put(uuid, false);
                            return;
                        }
                    }else{
                        //手に持っているアイテムはBlockStateMetaのインスタンスではない
                        e.setCancelled(true);
                        e.getWhoClicked().closeInventory();
                        sendLocalizedMessage(getServer().getPlayer(uuid), MSG_CANTOPEN_IT);
                        opening.put(uuid, false);
                        return;
                    }
                    //ここまで引っかからなければ正当と判断、フラグを真に
                    //以降、インベントリを閉じるまでこのチェックは省く
                    checkhand.put(uuid,true);
                }
                ItemStack item = null;
                if(e.getClick()==ClickType.NUMBER_KEY){
                    item = e.getWhoClicked().getInventory().getContents()[e.getHotbarButton()];
                    if(item!=null){
                        if(item.getItemMeta() instanceof BlockStateMeta){
                            BlockStateMeta blockMeta = (BlockStateMeta) item.getItemMeta();
                            if(blockMeta.getBlockState() instanceof ShulkerBox){
                                e.setCancelled(true);
                                return;
                            }
                        }
                    }
                }
                item = e.getCurrentItem();
                if(item!=null){
                    if(item.getItemMeta() instanceof BlockStateMeta){
                        BlockStateMeta blockMeta = (BlockStateMeta) item.getItemMeta();
                        if(blockMeta.getBlockState() instanceof ShulkerBox){
                            e.setCancelled(true);
                        }
                    }
                }
            }
        }
    }


    private boolean kaigaraOpen(Player p, PlayerInteractEvent e){
        //プレイヤーのUUID取得
        UUID uuid=p.getUniqueId();
        //手持ちのアイテムを取得
        ItemStack box = p.getInventory().getItemInMainHand();
        //空でなければ
        if(box!=null){
            if(box.getItemMeta() instanceof BlockStateMeta){
                BlockStateMeta blockMeta = (BlockStateMeta) box.getItemMeta();
                if(blockMeta.getBlockState() instanceof ShulkerBox){
                    //クリックイベントがあればキャンセル
                    if(e!=null)e.setCancelled(true);
                    
                    //プレイヤーのｼｭﾙｶｰ開封フラグをオン
                    opening.put(uuid, true);
                    //GUIのタイトルについて
                    String dispname = "Shulker Box";
                    if(blockMeta.hasDisplayName()){
                        //名前付きのｼｭﾙｶｰﾎﾞｯｸｽならその名前に
                        dispname = blockMeta.getDisplayName();
                    }else if(blockMeta.hasLocalizedName()){
                        //名前がなければローカライズされた名前に
                        dispname = blockMeta.getLocalizedName();
                        //どちらでもなければdispnameの初期値をそのまま使用
                    }
                    //インベントリを作成し、ｼｭﾙｶｰの内容を格納する
                    Inventory inv = Bukkit.createInventory(p, 27, dispname);
                    ShulkerBox shulker = (ShulkerBox) blockMeta.getBlockState();
                    //正当性検査用のハッシュマップを保管
                    usingbox.put(uuid,shulker.getInventory().getContents());
                    usingboxname.put(uuid,dispname);
                    //シュルカーの中身を入れたインベントリを用意
                    inv.setContents(shulker.getInventory().getContents());
                    //プレイヤーにGUIを見せる
                    p.openInventory(inv);
                    p.playSound(p.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, 0.5f, 0.9f+(float)Math.random()*0.1f);
                    sendLocalizedMessage(getServer().getPlayer(uuid), MSG_SHULKER_OPEN, dispname);
                }else{
                    sendLocalizedMessage(getServer().getPlayer(uuid), MSG_CANTOPEN_IT);
                }
            }else{
                sendLocalizedMessage(getServer().getPlayer(uuid), MSG_CANTOPEN_IT);
            }
        }else{
            sendLocalizedMessage(getServer().getPlayer(uuid), MSG_NULL);
            return true;
        }
        return false;
    }
    private void sendLocalizedMessage(Player p, int id){
        if(p.getLocale().equals("ja_jp")){
            p.sendMessage(messages_jp[id]);
        }else{
            p.sendMessage(messages_en[id]);
        }
    }
    private void sendLocalizedMessage(Player p, int id ,String ... args){
        try{
            if(p.getLocale().equals("ja_jp")){
                p.sendMessage(String.format(messages_jp[id],(Object[])args));
            }else{
                p.sendMessage(String.format(messages_en[id],(Object[])args));
            }
        }catch(IllegalFormatException e){
            sendLocalizedMessage(p, id);
        }
    }
}

//解決すべき問題点
//ｼｭﾙｶｰを開くと同時に手に持ったアイテムを変更するとｼｭﾙｶｰの変更が正しく適用されない
//＞増殖やロストの元凶。開いた後の初回インベントリ操作で正当性を確認するように。
//上に関連して、シュルカーを開いた後Shiftクリックや数字キーで移動できてしまうアイテムの幻影をつかむと実体化する
//＞開いた直後はクリック以外のアイテム操作を無効にした。本当はインベントリを閉じるのを遅らせたほうがいいのかも。
//シュルカーもってドア開けた後ターゲットブロックが空気になると右クリックイベントにつづいて左クリックイベントが2回発生する
//右クリック後一定時間左クリックイベントに反応しないようにすることで回避ただしラグによっては発動してしまう。