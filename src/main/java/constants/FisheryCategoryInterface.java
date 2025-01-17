package constants;

public interface FisheryCategoryInterface {

    int PER_MESSAGE = 0, PER_DAY = 1, PER_VC = 2, PER_TREASURE = 3, ROLE = 4, PER_SURVEY = 5, MAX = 5;
    String[] PRODUCT_EMOJIS = { "\uD83C\uDFA3", "\uD83E\uDD16", "\uD83E\uDD45", "\uD83D\uDD0D","\uD83C\uDFF7️", "\uD83D\uDDF3️" };
    long[] START_PRICE = { 25000, 25000, 32000, 20000, 50000, 19000 };
    long[] EFFECT = { 1, 100, 1, 80000, 0, 60000 };

}