package pw.chew.inventorydumper.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.LiteralText;

import java.time.Instant;
import java.util.List;

// The Main class where things happen. Everything is client-side.
@Environment(EnvType.CLIENT)
public class InventoryDumperClient implements ClientModInitializer {
    private static Instant lastPressed = Instant.now();

    @Override
    public void onInitializeClient() {
        // Runs this every tick (I think?)
        ClientTickEvents.END_CLIENT_TICK.register((MinecraftClient client) -> {
            if (client.currentScreen == null || client.player == null)
                return;

            if (getDelayInSeconds() < 1)
                return;

            // Ignore unhandled screens
            try {
                client.player.currentScreenHandler.getType();
            } catch (UnsupportedOperationException e) {
                return;
            }
            
            // Looks for "o" presses inside a screen. Only do it no more than once a second.
            if (InputUtil.isKeyPressed(client.getWindow().getHandle(), 79)) {
                // Reset Timer
                lastPressed = Instant.now();

                // Dump the inventory
                JsonObject data = dumpInventory(client.player.currentScreenHandler.slots, client.currentScreen.getTitle().getString());

                // Print to console
                // TODO: Print to file
                System.out.println(data.toString());

                client.player.sendMessage(new LiteralText("Inventory dumped! See console for JSON."), false);
            }
            // Looks for "p" presses, only dumps chest inventory, not player inventory
            if (InputUtil.isKeyPressed(client.getWindow().getHandle(), 80)) {
                // Reset Timer
                lastPressed = Instant.now();

                List<Slot> slots = client.player.currentScreenHandler.slots;
                if (slots.size() <= 36) {
                    client.player.sendMessage(new LiteralText("No inventory found."), false);
                    return;
                }

                List<Slot> useSlots = slots.subList(0, slots.size() - 36);

                // Dump the inventory
                JsonObject data = dumpInventory(useSlots, client.currentScreen.getTitle().getString());

                // Print to console
                // TODO: Print to file
                System.out.println(data.toString());

                client.player.sendMessage(new LiteralText("Inventory dumped! See console for JSON."), false);
            }

        });
    }

    /**
     * The method that does all the magic!
     * @param slots the inventory slots
     * @param name the name of the inventory
     */
    public JsonObject dumpInventory(List<Slot> slots, String name) {
        JsonObject data = new JsonObject();
        data.addProperty("name", name);
        data.addProperty("slot_count", slots.size());

        // Sort items into json array
        JsonArray info = new JsonArray();
        for (Slot slot : slots) {
            ItemStack item = slot.getStack();

            // Only add if there's an item
            if (item.getCount() == 0) {
                continue;
            }

            info.add(itemToJSON(item));

            // Old debug message to print out item names
            // client.player.sendMessage(new LiteralText("Item name: " + item.getName().getString()), false);
        }
        data.add("slots", info);

        return data;
    }

    /**
     * Very basic delay checker
     * @return time between now and last time key was pressed
     */
    public long getDelayInSeconds() {
        return (Instant.now().toEpochMilli() - lastPressed.toEpochMilli()) / 1000;
    }

    /**
     * Convert itemstack data to JSON
     * @param item the item to convert
     * @return item in json format
     */
    public JsonObject itemToJSON(ItemStack item) {
        JsonObject json = new JsonObject();
        // Type of item, e.g. Piston
        json.addProperty("item", item.getItem().getName().getString());
        // Amount of item
        json.addProperty("count", item.getCount());
        // Actual display name of item
        json.addProperty("name", item.getName().getString());
        // Tag = NBT
        CompoundTag tag = item.getTag();
        JsonArray tags = new JsonArray();
        // If no NBT, don't provide anything
        if (tag == null) {
            return json;
        }
        // Cycle through each NBT tag
        for(String key : tag.getKeys()) {
            JsonObject info = new JsonObject();
            Tag getTag = tag.get(key);

            // Only add if there's associated data
            if (getTag == null) {
                continue;
            }

            // Special cases we can handle
            switch (key) {
                case ("Enchantments"):
                    JsonArray enchantmentArray = new JsonArray();
                    info.remove("key");
                    JsonArray data = new Gson().fromJson(getTag.toString(), JsonArray.class);
                    for (int i = 0; i < data.size(); i++) {
                        JsonObject enchantment = data.get(i).getAsJsonObject();
                        short level = Short.parseShort(enchantment.get("lvl").getAsString().replace("s", ""));
                        String name = enchantment.get("id").getAsString();
                        JsonObject details = new JsonObject();
                        details.addProperty(name, level);
                        enchantmentArray.add(details);
                    }
                    info.add("Enchantments", enchantmentArray);
                    tags.add(info);
                    continue;
            }

            info.addProperty(key, getTag.toString());
            tags.add(info);
        }
        // Add final NBT to object
        json.add("nbt", tags);
        // Return finalized json
        return json;
    }
}
