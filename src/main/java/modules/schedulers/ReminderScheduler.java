package modules.schedulers;

import commands.runnables.utilitycategory.ReminderCommand;
import constants.Permission;
import core.PermissionCheckRuntime;
import core.schedule.MainScheduler;
import mysql.modules.reminders.DBReminders;
import mysql.modules.reminders.RemindersBean;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class ReminderScheduler {

    private final static Logger LOGGER = LoggerFactory.getLogger(ReminderScheduler.class);

    private static final ReminderScheduler ourInstance = new ReminderScheduler();

    public static ReminderScheduler getInstance() {
        return ourInstance;
    }

    private ReminderScheduler() {
    }

    private boolean started = false;

    public void start() {
        if (started) return;
        started = true;

        try {
            DBReminders.getInstance().getBean().values().forEach(this::loadReminderBean);
        } catch (Throwable e) {
            LOGGER.error("Could not start reminder", e);
        }
    }

    public void loadReminderBean(RemindersBean remindersBean) {
        MainScheduler.getInstance().schedule(remindersBean.getTime(), "reminder", () -> {
            onReminderDue(remindersBean);
        });
    }

    private void onReminderDue(RemindersBean remindersBean) {
        if (remindersBean.isActive()) {
            remindersBean.stop();

            long channelId = remindersBean.getChannelId();
            remindersBean.getServer()
                    .flatMap(server -> server.getTextChannelById(channelId))
                    .ifPresent(channel -> {
                        if (PermissionCheckRuntime.getInstance().botHasPermission(
                                remindersBean.getServerBean().getLocale(),
                                ReminderCommand.class,
                                channel,
                                Permission.READ_MESSAGES | Permission.SEND_MESSAGES
                        )) {
                            channel.sendMessage(remindersBean.getMessage()).exceptionally(ExceptionLogger.get());
                        }
                    });

            Optional.ofNullable(remindersBean.getCompletedRunnable())
                    .ifPresent(Runnable::run);
        }

        try {
            DBReminders.getInstance().getBean().remove(remindersBean.getId(), remindersBean);
        } catch (Throwable e) {
            LOGGER.error("Could not load reminders", e);
        }
    }

}
