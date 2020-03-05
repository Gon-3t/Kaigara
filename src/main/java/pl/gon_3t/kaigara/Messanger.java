package pl.gon_3t.kaigara;

import java.util.IllegalFormatException;

import org.bukkit.entity.Player;

public class Messanger{
    static final String prefix_pl="§5[§d開殻§5]§f";
    static final String prefix_error="§4";

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

    static void sendLocalizedMessage(Player p, int id){
        if(p.getLocale().equals("ja_jp")){
            p.sendMessage(messages_jp[id]);
        }else{
            p.sendMessage(messages_en[id]);
        }
    }
    static void sendLocalizedMessage(Player p, int id ,String ... args){
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