package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.listeners.OnTrackerRequestListener;
import commands.runnables.PornSearchAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "furry",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        withLoadingBar = true,
        aliases = { "furrybooru", "yiff" }
)
public class FurryCommand extends PornSearchAbstract implements OnTrackerRequestListener {

    public FurryCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    /*@Override WHILE furry.booru.org is down
    protected String getDomain() {
        return "furry.booru.org";
    }

    @Override
    protected String getImageTemplate() {
        return "https://furry.booru.org/images/%d/%f";
    }

    @Override
    public boolean isExplicit() { return true; }*/

    @Override
    protected String getDomain() {
        return "rule34.xxx";
    }

    @Override
    protected String getImageTemplate() {
        return "https://img.rule34.xxx/images/%d/%f";
    }

    @Override
    public boolean isExplicit() { return true; }

    @Override
    protected String getAdditionalSearchKey() {
        return " furry";
    }

}
