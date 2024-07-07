package org.example.mc.siteintegration.helpers;

import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.example.mc.siteintegration.utils.PlayerError;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class ItemSerializer {

    public static ItemStack deserializeItem(String item) throws Exception {
        byte[] serializedObject = Base64.getDecoder().decode(item);

        ByteArrayInputStream in = new ByteArrayInputStream(serializedObject);
        BukkitObjectInputStream is = new BukkitObjectInputStream(in);

        return (ItemStack) is.readObject();
    }

    public static String serializeItem(ItemStack item) throws Exception {
        ByteArrayOutputStream io = new ByteArrayOutputStream();
        BukkitObjectOutputStream os = new BukkitObjectOutputStream(io);
        os.writeObject(item);
        os.flush();

        byte[] serializedObject = io.toByteArray();

        return new String(Base64.getEncoder().encode(serializedObject));
    }
}
