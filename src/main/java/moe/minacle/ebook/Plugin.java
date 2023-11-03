package moe.minacle.ebook;

import io.papermc.paper.enchantments.EnchantmentRarity;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class Plugin extends JavaPlugin implements Listener {

    // MARK: JavaPlugin

    @Override
    public void onEnable() {
        super.onEnable();
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    // MARK: Listener

    @EventHandler
    void onPrepareAnvil(PrepareAnvilEvent event) {
        final AnvilInventory anvilInventory;
        final int anvilInventoryRepairCost;
        final ItemStack firstItem;
        final ItemMeta firstItemMeta;
        final ItemStack secondItem;
        final ItemMeta secondItemMeta;
        final Material secondItemType;
        final Map<Enchantment, Integer> secondItemEnchantments;
        final int secondItemRepairCost;
        ItemStack result;
        final ItemMeta resultMeta;
        int totalEnchantmentCost = 0;
        if (!this.isEnabled())
            return;
        anvilInventory = event.getInventory();
        if (
            (firstItem = anvilInventory.getFirstItem()) == null ||
            (secondItem = anvilInventory.getSecondItem()) == null ||
            firstItem.getType() != Material.BOOK
        )
            return;
        if (
            (result = anvilInventory.getResult()) != null &&
            result.getType() != Material.BOOK
        )
            return;
        firstItemMeta = firstItem.getItemMeta();
        if ((secondItemMeta = secondItem.getItemMeta()) == null)
            return;
        if (secondItemMeta instanceof final EnchantmentStorageMeta secondItemEnchantmentStorageMeta)
            secondItemEnchantments = secondItemEnchantmentStorageMeta.getStoredEnchants();
        else
            secondItemEnchantments = secondItem.getEnchantments();
        if (secondItemEnchantments.isEmpty())
            return;
        if (firstItem.getAmount() > 1) {
            event.setResult(null);
            return;
        }
        anvilInventoryRepairCost = anvilInventory.getRepairCost();
        if (secondItemMeta instanceof final Repairable secondItemRepairable)
            secondItemRepairCost = secondItemRepairable.getRepairCost();
        else
            secondItemRepairCost = anvilInventoryRepairCost;
        result = new ItemStack(Material.ENCHANTED_BOOK);
        if (firstItemMeta != null)
            resultMeta = Bukkit.getItemFactory().asMetaFor(firstItemMeta, result);
        else
            resultMeta = result.getItemMeta();
        if (resultMeta == null)
            return;
        secondItemType = secondItem.getType();
        for (final Map.Entry<Enchantment, Integer> entry : secondItemEnchantments.entrySet()) {
            final Enchantment enchantment = entry.getKey();
            final int enchantmentLevel = entry.getValue();
            final EnchantmentRarity enchantmentRarity = enchantment.getRarity();
            if (secondItemType != Material.ENCHANTED_BOOK) {
                int enchantmentCost;
                switch (enchantmentRarity) {
                case COMMON:
                case UNCOMMON:
                    enchantmentCost = 1;
                    break;
                case RARE:
                    enchantmentCost = 2;
                    break;
                case VERY_RARE:
                    enchantmentCost = 4;
                    break;
                default:
                    return;
                }
                enchantmentCost *= enchantmentLevel;
                totalEnchantmentCost += enchantmentCost;
            }
            ((EnchantmentStorageMeta)resultMeta).addStoredEnchant(enchantment, enchantmentLevel, true);
        }
        if (secondItemRepairCost > 0) {
            if (secondItemType != Material.ENCHANTED_BOOK)
                totalEnchantmentCost += (secondItemRepairCost + 1) / 2 - 1;
            ((Repairable)resultMeta).setRepairCost(secondItemRepairCost);
        }
        result.setItemMeta(resultMeta);
        anvilInventory.setRepairCost(totalEnchantmentCost);
        event.setResult(result);
    }
}
