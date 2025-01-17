package core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import commands.Command;
import constants.Permission;
import core.utils.PermissionUtil;
import core.utils.StringUtil;
import javafx.util.Pair;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class PermissionCheckRuntime {

    private static final PermissionCheckRuntime instance = new PermissionCheckRuntime();
    private static final int PERMISSION_ROLE_POS = -1;

    private final Cache<Pair<Long, Integer>, Boolean> errorCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofHours(6))
            .build();

    private PermissionCheckRuntime() {}
    public static PermissionCheckRuntime getInstance() {
        return instance;
    }

    public boolean botHasPermission(Locale locale, Class<? extends Command> c, Server server, int permissions) {
        return botHasPermission(locale, c, server, null, permissions);
    }

    public boolean botHasPermission(Locale locale, Class<? extends Command> c, ServerChannel channel, int permissions) {
        return botHasPermission(locale, c, channel.getServer(), channel, permissions);
    }

    private boolean botHasPermission(Locale locale, Class<? extends Command> c, Server server, ServerChannel channel, int permissions) {
        ArrayList<Integer> missingPermissions = PermissionUtil.getMissingPermissionListForUser(server, channel, DiscordApiManager.getInstance().getYourself(), permissions);

        if (missingPermissions.size() == 0) return true;

        if (canPostError(server, permissions) && canContactOwner(server)) {
            String permissionsList = new ListGen<Integer>().getList(missingPermissions, ListGen.SLOT_TYPE_BULLET, n -> "**"+TextManager.getString(locale, TextManager.PERMISSIONS, String.valueOf(n))+"**");
            EmbedBuilder eb = EmbedFactory.getEmbedError();
            eb.setTitle(TextManager.getString(locale, TextManager.GENERAL, "missing_permissions_title"));
            eb.setDescription(TextManager.getString(locale, TextManager.GENERAL, "permission_runtime", channel != null, Command.getClassProperties(c).trigger(), channel != null ? (channel.asServerTextChannel().isPresent() ? "#" : "") + StringUtil.escapeMarkdown(channel.getName()) : "", permissionsList));

            server.getOwner().get().sendMessage(eb);
            setErrorInstant(server, permissions);
        }

        return false;
    }

    public boolean botCanManageRoles(Locale locale, Class<? extends Command> c, List<Role> roles) {
        return botCanManageRoles(locale, c, roles.toArray(new Role[0]));
    }

    public boolean botCanManageRoles(Locale locale, Class<? extends Command> c, Role... roles) {
        ArrayList<Role> unreachableRoles = new ArrayList<>();

        for(Role role: roles) {
            if (!PermissionUtil.canYouManageRole(role)) unreachableRoles.add(role);
        }

        if (unreachableRoles.size() == 0) return true;

        Server server = roles[0].getServer();
        if (botHasPermission(locale, c, server, Permission.MANAGE_ROLES) && canPostError(server, PERMISSION_ROLE_POS) && canContactOwner(server)) {
            String rolesList = new ListGen<Role>().getList(unreachableRoles, ListGen.SLOT_TYPE_BULLET, role -> "**@" + StringUtil.escapeMarkdown(role.getName()) + "**");
            EmbedBuilder eb = EmbedFactory.getEmbedError();
            eb.setTitle(TextManager.getString(locale, TextManager.GENERAL, "missing_permissions_title"));
            eb.setDescription(TextManager.getString(locale, TextManager.GENERAL, "permission_runtime_rolespos", Command.getClassProperties(c).trigger(), rolesList));

            server.getOwner().get().sendMessage(eb);
            setErrorInstant(server, PERMISSION_ROLE_POS);
        }

        return false;
    }

    private boolean canContactOwner(Server server) {
        return canPostError(server, Permission.MANAGE_ROLES) && server.getOwner().isPresent();
    }

    private boolean canPostError(Server server, int permission) {
        return errorCache.asMap().containsKey(new Pair<>(server.getId(), permission));
    }

    private void setErrorInstant(Server server, int permission) {
        errorCache.put(new Pair<>(server.getId(), permission), true);
    }

}
