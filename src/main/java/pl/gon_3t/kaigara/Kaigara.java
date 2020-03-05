package pl.gon_3t.kaigara;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class Kaigara extends JavaPlugin implements Listener {
    static final String version = "1.6";
    Map<Player, PlayerData> users = new HashMap<Player, PlayerData>();

    @Override
    public void onEnable(){
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("[開殻 -Kaigara- v"+version+" by Gon_3t]");
        //プラグインを有効にした時点でログインしているプレイヤーを登録
        for (Player p : getServer().getOnlinePlayers()) {
            if(!users.containsKey(p))users.put(p, new PlayerData());
            //getLogger().info(p.getName()+" has been registered to map.");
        }
        //getLogger().info("Map has set for online players.");
    }

    @EventHandler
	public void onLogin(PlayerJoinEvent e){
        //ログイン時に登録
        Player p = e.getPlayer();
        users.put(p, new PlayerData());
        //getLogger().info(p.getName()+" has been registered to map.");
	}

	@EventHandler
	public void onLogoff(PlayerQuitEvent e){
        //ログアウト時に登録を解除
        Player p = e.getPlayer();
        users.remove(p);
        //getLogger().info(p.getName()+" has been removed from map.");
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
                Messanger.sendLocalizedMessage(getServer().getPlayer(sender.getName()), Messanger.MSG_HELP);
            }
            // それ以外
            else{
                Messanger.sendLocalizedMessage(getServer().getPlayer(sender.getName()), Messanger.MSG_UNKNOWN_COMMAND);
            }
        }
        //引数なし
        Messanger.sendLocalizedMessage(getServer().getPlayer(sender.getName()), Messanger.MSG_HELP);
        return true;
    }
   
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e){
        Player p = e.getPlayer();
        //ドア開けた後ターゲットが空気だとRIGHT_CLICK_BLOCKの直後にLEFT_CLICK_AIRが2回発動する
        //そのため、RIGHT_CLICK_BLOCKが発動した後の一瞬シェルカーオープンを無効にする
        if(e.getAction()==Action.RIGHT_CLICK_BLOCK){
            //プレイヤーのチェックドアをtrueに
            users.get(p).checkdoor=true;
            //10ﾃｨｯｸ後に解除(10ﾃｨｯｸが最適とは限らない)
            getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                public void run() {
                    users.get(p).checkdoor=false;
                }
            }, 10L);
        }
        //シェルカーボックスを手に持った状態で任意のブロックを左クリック
        else if(e.getAction()==Action.LEFT_CLICK_AIR||e.getAction()==Action.LEFT_CLICK_BLOCK){
            if(users.get(p).checkdoor){
                //プレイヤーのチェックドアフラグが有効な場合は何もしない
                return;
            }
            if(e.getItem()!=null){
                //持ってるものがnullでなく、ｼｭﾙｶｰﾎﾞｯｸｽならKaigaraOpen
                if(e.getItem().getItemMeta() instanceof BlockStateMeta){
                    if(((BlockStateMeta) e.getItem().getItemMeta()).getBlockState() instanceof ShulkerBox){
                        kaigaraOpen(p, e);
                    }
                }
            }    
        }
    }
        
    

    @EventHandler
    public void onGuiClose(InventoryCloseEvent e){
        Player p = getServer().getPlayer(e.getPlayer().getUniqueId());
        //ｼｭﾙｶｰをあけているプレイヤーがGUIを閉じた時
        if(users.get(p).opening){
            ItemStack box = e.getPlayer().getInventory().getItemInMainHand();
            users.get(p).opening=false;
            users.get(p).checkhand=false;
            if(box.getItemMeta() instanceof BlockStateMeta){
                BlockStateMeta blockMeta = (BlockStateMeta) box.getItemMeta();
                if(blockMeta.getBlockState() instanceof ShulkerBox){
                    ShulkerBox shulker = (ShulkerBox) blockMeta.getBlockState();
                    if(users.get(p).usingbox==null){
                        //usingboxがnull
                        getLogger().info("usingbox for "+p.getName()+" is null!!");
                        Messanger.sendLocalizedMessage(p, Messanger.MSG_NULL);
                        Messanger.sendLocalizedMessage(p, Messanger.MSG_SHULKER_NOTSAVED);
                        return;
                    }
                    //現在の手持ちのボックスとそれを開いた時点での中身が一致しているか
                    if(Arrays.equals(users.get(p).usingbox,shulker.getInventory().getContents())){
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
                        //少しクールダウンを入れる
                        users.get(p).checkdoor=true;
                        getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                            public void run() {
                                users.get(p).checkdoor=false;
                            }
                        }, 10L);
                        p.playSound(p.getLocation(), Sound.BLOCK_SHULKER_BOX_CLOSE, 0.5f, 0.9f+(float)Math.random()*0.1f);
                        Messanger.sendLocalizedMessage(p, Messanger.MSG_SHULKER_SAVED, dispname);
                    }else{
                        Messanger.sendLocalizedMessage(p, Messanger.MSG_SHULKER_NOTSAVED);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryMoved(InventoryClickEvent e){
        Player p = getServer().getPlayer(e.getWhoClicked().getUniqueId());
        ItemStack mainhand = e.getWhoClicked().getInventory().getItemInMainHand();
        //プレイヤーがKaigaraOpenでｼｭﾙｶｰﾎﾞｯｸｽを開けている
        if(users.get(p).opening){
            //まだ正当性が未確認(checkhand==false)であればここで確認
            if(!users.get(p).checkhand){
                if(!(e.getClick()==ClickType.LEFT||e.getClick()==ClickType.RIGHT)){
                    //アイテム増殖およびロスト防止の為最初は通常のクリック以外禁止
                    e.setCancelled(true);
                    return;
                }
                if(mainhand==null){
                    //手に何も持っていないのに開けている場合の例外処理
                    e.setCancelled(true);
                    e.getWhoClicked().closeInventory();
                    e.getWhoClicked().sendMessage(Messanger.prefix_pl+Messanger.prefix_error+"エラー：null。");
                    users.get(p).opening=false;
                    return;
                }else if(mainhand.getItemMeta() instanceof BlockStateMeta){
                    BlockStateMeta blockMeta = (BlockStateMeta) mainhand.getItemMeta();
                    if(blockMeta.getBlockState() instanceof ShulkerBox){
                        //正しいｼｭﾙｶｰﾎﾞｯｸｽか?
                        ShulkerBox shulker = (ShulkerBox) blockMeta.getBlockState();
                        if(!Arrays.equals(users.get(p).usingbox,shulker.getInventory().getContents())){
                            //開けた時とは中身が異なるｼｭﾙｶｰﾎﾞｯｸｽを持っている
                            //ｼｭﾙｶｰﾎﾞｯｸｽの名前とかでも判定したほうがいいかも
                            //アイテムにuuidとか…ないか
                            e.setCancelled(true);
                            e.getWhoClicked().closeInventory();
                            Messanger.sendLocalizedMessage(p, Messanger.MSG_SHULKER_ANOTHER_ONE);
                            users.get(p).opening=false;
                            return;
                        }
                    }else{
                        //持っているものがｼｭﾙｶｰﾎﾞｯｸｽでない
                        e.setCancelled(true);
                        e.getWhoClicked().closeInventory();
                        Messanger.sendLocalizedMessage(p, Messanger.MSG_CANTOPEN_IT);
                        users.get(p).opening=false;
                        return;
                    }
                }else{
                    //手に持っているアイテムはBlockStateMetaのインスタンスではない
                    e.setCancelled(true);
                    e.getWhoClicked().closeInventory();
                    Messanger.sendLocalizedMessage(p, Messanger.MSG_CANTOPEN_IT);
                    users.get(p).opening=false;
                    return;
                }
                //ここまでreturnに引っかからなければ正当と判断、フラグを真に
                //以降、インベントリを閉じるまでこのチェックは省く
                users.get(p).checkhand=true;
            }

            //ｼｭﾙｶｰﾎﾞｯｸｽの移動をはじく部分
            ItemStack item = null;
            //数字キー移動の対象がシュルカーボックスなら
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


    private boolean kaigaraOpen(Player p, PlayerInteractEvent e){
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
                    users.get(p).opening=true;
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
                    users.get(p).usingbox=shulker.getInventory().getContents();
                    users.get(p).usingboxname=dispname;
                    //シュルカーの中身を入れたインベントリを用意
                    inv.setContents(shulker.getInventory().getContents());
                    //プレイヤーにGUIを見せる
                    p.openInventory(inv);
                    p.playSound(p.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, 0.5f, 0.9f+(float)Math.random()*0.1f);
                    Messanger.sendLocalizedMessage(p, Messanger.MSG_SHULKER_OPEN, dispname);
                }else{
                    Messanger.sendLocalizedMessage(p, Messanger.MSG_CANTOPEN_IT);
                }
            }else{
                Messanger.sendLocalizedMessage(p, Messanger.MSG_CANTOPEN_IT);
            }
        }else{
            Messanger.sendLocalizedMessage(p, Messanger.MSG_NULL);
            return true;
        }
        return false;
    }
}

//解決すべき問題点
//ｼｭﾙｶｰを開くと同時に手に持ったアイテムを変更するとｼｭﾙｶｰの変更が正しく適用されない
//＞増殖やロストの元凶。開いた後の初回インベントリ操作で正当性を確認するように。
//上に関連して、シュルカーを開いた後Shiftクリックや数字キーで移動できてしまうアイテムの幻影をつかむと実体化する
//＞開いた直後はクリック以外のアイテム操作を無効にした。本当はインベントリを閉じるのを遅らせたほうがいいのかも。

//更新履歴
//v1.5 - HashMapのキーをUUIDからPlayerに変更、プレイヤー毎のデータやSendMessage用のString定数をクラスにまとめた
//       クラスを分けたついでにファイルも分割してみた
//v1.6 - usersをログイン/アウト時にputおよびremoveする仕様に変更、Mapの肥大化を阻止！
//       nullこわいのでonEnable時点で既にオンラインのプレイヤーを登録するように。
//       takatronixさんアドバイスありがとうございます！