package modules.porn;

public class PornImageMeta {

    private final String imageUrl;
    private final int index;
    private final long score;

    public PornImageMeta(String imageUrl, long score, int index) {
        this.imageUrl = imageUrl;
        this.score = score;
        this.index = index;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public long getWeight() {
        return (long) Math.pow(score + 1, 2.75) * (imageUrl.endsWith("gif") ? 3 : 1);
    }

    public int getIndex() {
        return index;
    }
}
