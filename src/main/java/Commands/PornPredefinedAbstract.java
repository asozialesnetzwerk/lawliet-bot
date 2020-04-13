package Commands;

import Modules.Porn.PornImage;
import Core.TextManager;

import java.util.ArrayList;
import java.util.Optional;

public abstract class PornPredefinedAbstract extends PornAbstract {

    private String notice = null;

    protected abstract String getSearchKey();
    protected abstract String getSearchExtra();
    protected abstract boolean isAnimatedOnly();
    protected abstract String getDomain();
    protected abstract String getImageTemplate();

    @Override
    public Optional<String> getNoticeOptional() {
        return Optional.ofNullable(notice);
    }

    @Override
    public ArrayList<PornImage> getPornImages(ArrayList<String> nsfwFilter, String search, int amount, ArrayList<String> usedResults) throws Throwable {
        if (!search.isEmpty()) notice = TextManager.getString(getLocale(), TextManager.COMMANDS, "porn_keyforbidden");

        search = getSearchKey();
        String searchAdd = getSearchExtra();
        boolean animatedOnly = isAnimatedOnly();
        String domain = getDomain();
        String imageTemplate = getImageTemplate();

        return downloadPorn(nsfwFilter, amount, domain, search, searchAdd, imageTemplate, animatedOnly, usedResults);
    }

}