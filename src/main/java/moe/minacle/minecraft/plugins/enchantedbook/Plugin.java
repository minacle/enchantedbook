package moe.minacle.minecraft.plugins.enchantedbook;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.enchantments.CraftEnchantment;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.plugin.java.JavaPlugin;

public final class Plugin extends JavaPlugin implements Listener {

    private static final int BSTATS_PLUGIN_ID = 20341;

    /**
     * Calculates the cost of an enchantment based on its anvil cost and level.
     *
     * @param enchantment
     *  the enchantment to calculate the cost for
     *
     * @param level
     *  the level of the enchantment
     *
     * @return
     *  the cost of the enchantment, or -1 if the enchantment is invalid
     */
    private static int getEnchantmentCost(final @NotNull Enchantment enchantment, int level) {
        final int anvilCost;
        final CraftEnchantment craftEnchantment;
        final net.minecraft.world.item.enchantment.Enchantment nmsEnchantment;
        if (level <= 0)
            return -1;
        if ((craftEnchantment = (CraftEnchantment)enchantment) == null)
            return -1;
        if ((nmsEnchantment = craftEnchantment.getHandle()) == null)
            return -1;
        if ((anvilCost = nmsEnchantment.getAnvilCost()) <= 0)
            return -1;
        return Math.max(anvilCost / 2 * level, 1);
    }

    @EventHandler
    private void onPrepareAnvil(final @NotNull PrepareAnvilEvent event) {
        final AnvilInventory anvilInventory = event.getInventory();
        final AnvilView anvilView = event.getView();
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
        else
            ((Repairable)resultMeta).setRepairCost(secondItemRepairCost);
        result.setItemMeta(resultMeta);
        anvilView.setRepairCost(totalEnchantmentCost);
        event.setResult(result);
    }

    // MARK: JavaPlugin

    @Override
    public void onEnable() {
        super.onEnable();
        new Metrics(this, BSTATS_PLUGIN_ID);
        getServer().getPluginManager().registerEvents(this, this);
    }
}
