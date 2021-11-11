package pointSystem;

import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;

import java.util.HashMap;

public class ShopProfile {

    public HashMap<ShopItem, ItemType> shopItems = new HashMap<>();
    private Guild guild;

    public ShopProfile() {
        shopItems.put(new ShopItem("Early Location", 30, 1), ItemType.LOCATION);

        shopItems.put(new ShopItem("Buzz Role", 1000, 1), ItemType.ROLE);

        shopItems.put(new ShopItem("1.5x Raid Boost", 100, 1), ItemType.BOOST);
        shopItems.put(new ShopItem("2x Raid Boost", 250, 1), ItemType.BOOST);
    }

    @AllArgsConstructor
    public enum ItemType {
        LOCATION("Early Location"),
        ROLE("Roles"),
        BOOST("Raid Boosts");

        private String name;

    }

}

