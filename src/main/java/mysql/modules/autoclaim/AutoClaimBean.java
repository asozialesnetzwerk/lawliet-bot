package mysql.modules.autoclaim;

import core.CustomObservableList;
import core.cache.PatreonCache;
import org.checkerframework.checker.nullness.qual.NonNull;
import java.util.ArrayList;

public class AutoClaimBean {

    private CustomObservableList<Long> userList;

    public AutoClaimBean(@NonNull ArrayList<Long> userList) {
        this.userList = new CustomObservableList<>(userList);
    }


    /* Getters */

    public CustomObservableList<Long> getUserList() {
        return userList;
    }

    public boolean isActive(long userId) {
        return userList.contains(userId) && PatreonCache.getInstance().getUserTier(userId) >= 2;
    }

    public void setActive(long userId, boolean active) {
        if (active && !userList.contains(userId))
            userList.add(userId);
        else if (!active)
            userList.remove(userId);
    }

}
