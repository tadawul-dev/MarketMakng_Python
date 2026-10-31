package sa.com.tadawul.website.tip.vo;


public class BestBidOffVO {
    public volatile int level ;
    public volatile double price;
    public volatile long quantity;
    public volatile long orders;

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
    @Override
    public String toString() {
        return "OrderLevel{level=" + level + ",price=" + price + ", quantity=" + quantity + ", orders=" + orders +"}";
    }

}
