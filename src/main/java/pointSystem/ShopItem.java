package pointSystem;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShopItem {

    private String itemName;
    private int pointCost;
    private int maxQuantity;

    public ShopItem(String itemName, int pointCost, int maxQuantity) {
        this.itemName = itemName;
        this.pointCost = pointCost;
        this.maxQuantity = maxQuantity;
    }

}
