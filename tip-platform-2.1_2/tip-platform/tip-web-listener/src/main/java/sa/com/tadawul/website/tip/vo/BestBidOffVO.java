package sa.com.tadawul.website.tip.vo;

import java.util.Arrays;


public class BestBidOffVO {
    public int level ;
    public double price;
    public long quantity;
    public long orders;

    public BestBidOffVO() {
        doInit(1,0.0,0,0);
    }

    public BestBidOffVO(double price, long quantity) {
        doInit(1,price,quantity,0);
    }


    public BestBidOffVO(int level, double price, long quantity, long orders) {
        doInit(level, price,quantity,orders);
    }
    private void doInit(int level, double price, long quantity, long orders){
        this.level = level;
        this.price = price;
        this.quantity = quantity;
        this.orders = orders;
    }
    /** Copy constructor. */
    public BestBidOffVO(BestBidOffVO other) {
        doInit(other.level, other.price, other.quantity, other.orders);
    }

    /**
     * Deep copy of the first {@code depth} levels. {@code Arrays.setAll} rather than a Stream:
     * same speed as a hand-written loop, while {@code stream().map().toArray()} measured ~60% slower
     * on these 10-element arrays, and this runs for every share/bond copy.
     */
    public static BestBidOffVO[] copyOf(BestBidOffVO[] levels, int depth) {
        BestBidOffVO[] copy = new BestBidOffVO[depth];
        Arrays.setAll(copy, i -> new BestBidOffVO(levels[i]));
        return copy;
    }

    @Override
    public String toString() {
        return "OrderLevel{level=" + level + ",price=" + price + ", quantity=" + quantity + ", orders=" + orders +"}";
    }

}
