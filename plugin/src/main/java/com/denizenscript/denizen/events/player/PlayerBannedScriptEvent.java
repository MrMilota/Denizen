package com.denizenscript.denizen.events.player;

import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.BanEntry;
import org.bukkit.profile.PlayerProfile;

public class PlayerBannedScriptEvent extends BukkitScriptEvent implements Listener {

    // <--[event]
    // @Events
    // player banned
    //
    // @Group Player
    //
    // @Cancellable true
    //
    // @Triggers when a player is banned from the server.
    //
    // @Context
    // <context.reason> returns an ElementTag of the ban reason.
    // <context.duration> returns an ElementTag of the ban duration (or 'permanent' if permanent).
    // <context.source> returns an ElementTag of the ban source (who banned the player).
    // <context.expiration> returns an ElementTag of the exact expiration date/time, if temporary.
    // <context.kick_message> returns an ElementTag of the kick message shown to the player.
    //
    // @Determine
    // "REASON:<ElementTag>" to change the ban reason.
    // "CANCELLED" to prevent the ban.
    // "MESSAGE:<ElementTag>" to change the kick message.
    //
    // @Player Always.
    //
    // -->

    public PlayerBannedScriptEvent() {
        registerCouldMatcher("player banned");
    }

    public PlayerTag player;
    public PlayerKickEvent event;
    private BanEntry<PlayerProfile> banEntry;
    private String banReason;
    private String banSource;

    @Override
    public boolean applyDetermination(ScriptPath path, ObjectTag determinationObj) {
        if (determinationObj instanceof ElementTag) {
            String determination = determinationObj.toString();
            String lower = CoreUtilities.toLowerCase(determination);
            if (lower.startsWith("reason:")) {
                banReason = determination.substring("reason:".length());
                if (banEntry != null) {
                    BanList<PlayerProfile> banList = Bukkit.getBanList(BanList.Type.PROFILE);
                    PlayerProfile profile = player.getPlayerEntity().getPlayerProfile();
                    banList.addBan(profile, banReason, banEntry.getExpiration(), banEntry.getSource());
                }
                return true;
            }
            else if (lower.startsWith("message:")) {
                event.setReason(determination.substring("message:".length()));
                return true;
            }
            else if (lower.equals("cancelled")) {
                cancelled = true;
                return true;
            }
        }
        return super.applyDetermination(path, determinationObj);
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        BanList<PlayerProfile> banList = Bukkit.getBanList(BanList.Type.PROFILE);
        PlayerProfile profile = player.getPlayerProfile();
        
        if (banList.isBanned(profile)) {
            BanEntry<PlayerProfile> entry = banList.getBanEntry(profile);
            if (entry != null && (System.currentTimeMillis() - entry.getCreated().getTime()) < 1000) {
                this.player = PlayerTag.mirrorBukkitPlayer(player);
                this.banEntry = entry;
                this.banReason = entry.getReason();
                this.banSource = entry.getSource();
                this.event = event;
                
                fire(event);
                
                if (cancelled) {
                    banList.pardon(profile);
                }
            }
        }
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(player, null);
    }

    @Override
    public ObjectTag getContext(String name) {
        return switch (name) {
            case "reason" -> new ElementTag(banReason);
            case "duration" -> banEntry != null && banEntry.getExpiration() != null ? 
                             new ElementTag("temporary") : new ElementTag("permanent");
            case "source" -> new ElementTag(banSource);
            case "expiration" -> banEntry != null && banEntry.getExpiration() != null ? 
                               new ElementTag(banEntry.getExpiration().toString()) : null;
            case "kick_message" -> new ElementTag(event.getReason());
            default -> super.getContext(name);
        };
    }
}