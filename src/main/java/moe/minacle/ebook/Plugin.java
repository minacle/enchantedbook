package moe.minacle.ebook;

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

    /**
     * Calculates the cost of an enchantment based on its rarity and level.
     *
     * @param enchantment
     *  the enchantment to calculate the cost for
     *
     * @param level
     *  the level of the enchantment
     *
     * @return
     *  the cost of the enchantment, or -1 if the rarity is invalid
     */
    private static int getEnchantmentCost(Enchantment enchantment, int level) {
        switch (enchantment.getRarity()) {
        case COMMON:
        case UNCOMMON:
            return level;
        case RARE:
            return level * 2;
        case VERY_RARE:
            return level * 4;
        default:
            return -1;
        }
    }

    // MARK: JavaPlugin

    @Override
    public void onEnable() {
        super.onEnable();
        getServer().getPluginManager().registerEvents(this, this);
    }

    // MARK: Listener

    @EventHandler
    void onPrepareAnvil(PrepareAnvilEvent event) {
        final AnvilInventory anvilInventory = event.getInventory();
        final ItemStack firstItem;
        final ItemMeta firstItemMeta;
        final Material firstItemType;
        final Map<Enchantment, Integer> firstItemEnchantments;
        final boolean isFirstItemEnchantmentsEmpty;
        final int firstItemRepairCost;
        final ItemStack secondItem;
        final ItemMeta secondItemMeta;
        final Material secondItemType;
        final Map<Enchantment, Integer> secondItemEnchantments;
        final int secondItemRepairCost;
        final ItemStack anvilInventoryResult;
        final Material anvilInventoryResultType;
        final ItemStack result;
        final ItemMeta resultMeta;
        int totalEnchantmentCost = 0;
        if (
            (firstItem = anvilInventory.getFirstItem()) == null ||
            (secondItem = anvilInventory.getSecondItem()) == null
        )
            return;
        firstItemType = firstItem.getType();
        if (
            firstItemType != Material.BOOK &&
            firstItemType != Material.ENCHANTED_BOOK
        )
            return;
        if ((anvilInventoryResult = anvilInventory.getResult()) != null)
            anvilInventoryResultType = anvilInventoryResult.getType();
        else
            anvilInventoryResultType = null;
        secondItemType = secondItem.getType();
        if (
            firstItemType == Material.ENCHANTED_BOOK &&
            secondItemType == Material.ENCHANTED_BOOK &&
            anvilInventoryResultType == Material.ENCHANTED_BOOK
        )
            return;
        if (
            anvilInventoryResult != null &&
            anvilInventoryResultType != Material.BOOK
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
        if (firstItemType == Material.ENCHANTED_BOOK) {
            firstItemEnchantments = ((EnchantmentStorageMeta)firstItemMeta).getStoredEnchants();
            result = firstItem.clone();
            resultMeta = result.getItemMeta();
        }
        else {
            firstItemEnchantments = firstItem.getEnchantments();
            result = new ItemStack(Material.ENCHANTED_BOOK);
            if (firstItemMeta != null)
                resultMeta = Bukkit.getItemFactory().asMetaFor(firstItemMeta, result);
            else
                resultMeta = result.getItemMeta();
        }
        if (resultMeta == null)
            return;
        isFirstItemEnchantmentsEmpty = firstItemEnchantments.isEmpty();
        if (isFirstItemEnchantmentsEmpty) {
            final EnchantmentStorageMeta resultEnchantmentStorageMeta = (EnchantmentStorageMeta)resultMeta;
            for (final Map.Entry<Enchantment, Integer> entry : secondItemEnchantments.entrySet()) {
                final Enchantment enchantment = entry.getKey();
                final int enchantmentLevel = entry.getValue();
                if (
                    firstItemType != Material.BOOK ||
                    secondItemType != Material.ENCHANTED_BOOK
                ) {
                    final int enchantmentCost = getEnchantmentCost(enchantment, enchantmentLevel);
                    if (enchantmentCost < 0)
                        return;
                    totalEnchantmentCost += enchantmentCost;
                }
                resultEnchantmentStorageMeta.addStoredEnchant(enchantment, enchantmentLevel, true);
            }
        }
        else {
            final EnchantmentStorageMeta resultEnchantmentStorageMeta = (EnchantmentStorageMeta)resultMeta;
            for (final Map.Entry<Enchantment, Integer> entry : secondItemEnchantments.entrySet()) {
                final Enchantment enchantment = entry.getKey();
                final int enchantmentLevel = entry.getValue();
                if (enchantmentLevel > firstItemEnchantments.getOrDefault(enchantment, 0)) {
                    final int enchantmentCost = getEnchantmentCost(enchantment, enchantmentLevel);
                    if (enchantmentCost < 0)
                        return;
                    totalEnchantmentCost += enchantmentCost;
                    resultEnchantmentStorageMeta.addStoredEnchant(enchantment, enchantmentLevel, true);
                }
            }
        }
        if (firstItemMeta instanceof final Repairable firstItemRepairable)
            firstItemRepairCost = firstItemRepairable.getRepairCost();
        else
            firstItemRepairCost = 0;
        if (secondItemMeta instanceof final Repairable secondItemRepairable)
            secondItemRepairCost = secondItemRepairable.getRepairCost();
        else
            secondItemRepairCost = 0;
        if (totalEnchantmentCost > 0) {
            if (
                firstItemType == Material.BOOK &&
                isFirstItemEnchantmentsEmpty
            ) {
                if (secondItemRepairCost > 0) {
                    totalEnchantmentCost += (secondItemRepairCost + 1) / 2 - 1;
                    ((Repairable)resultMeta).setRepairCost(secondItemRepairCost);
                }
            }
            else {
                totalEnchantmentCost += firstItemRepairCost + secondItemRepairCost;
                ((Repairable)resultMeta).setRepairCost((Math.max(firstItemRepairCost, secondItemRepairCost) + 1) * 2 - 1);
            }
        }
        else {
            ((Repairable)resultMeta).setRepairCost(secondItemRepairCost);
        }
        result.setItemMeta(resultMeta);
        anvilInventory.setRepairCost(totalEnchantmentCost);
        event.setResult(result);
    }
}
