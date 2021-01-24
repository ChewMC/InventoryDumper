package pw.chew.inventorydumper.client;

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
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.LiteralText;

import java.time.Instant;
import java.util.List;

@Environment(EnvType.CLIENT)
public class InventoryDumperClient implements ClientModInitializer {
    private static Instant lastPressed = Instant.now();

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register((MinecraftClient client) -> {
            if (InputUtil.isKeyPressed(client.getWindow().getHandle(), 79) && getDelayInSeconds() >= 1) {
                if (client.currentScreen != null) {
                    lastPressed = Instant.now();
                    client.player.sendMessage(new LiteralText("Inventory Title: " + client.currentScreen.getTitle().getString()), false);
                    List<Slot> slots = client.player.currentScreenHandler.slots;

                    JsonArray info = new JsonArray();
                    for (Slot slot : slots) {
                        ItemStack item = slot.getStack();
                        if (item.getCount() == 0) {
                            continue;
                        }

                        info.add(itemToJSON(item));
                        client.player.sendMessage(new LiteralText("Item name: " + item.getName().getString()), false);
                    }
                    System.out.println(info.toString());
                }
            }
        });
    }

    public long getDelayInSeconds() {
        return Instant.now().getEpochSecond() - lastPressed.getEpochSecond();
    }

    public JsonObject itemToJSON(ItemStack item) {
        JsonObject json = new JsonObject();
        json.addProperty("item", item.getItem().getName().getString());
        json.addProperty("count", item.getCount());
        json.addProperty("name", item.getName().getString());
        CompoundTag tag = item.getTag();
        JsonArray tags = new JsonArray();
        if (tag == null) {
            json.add("tag", tags);
            return json;
        }
        for(String key : tag.getKeys()) {
            JsonObject info = new JsonObject();
            info.addProperty("key", key);
            Tag getTag = tag.get(key);
            if (getTag != null) {
                info.addProperty("info", getTag.toString());
                tags.add(info);
            }
        }
        json.add("tag", tags);
        return json;
    }
}
