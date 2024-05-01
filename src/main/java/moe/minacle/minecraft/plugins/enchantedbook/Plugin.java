package moe.minacle.minecraft.plugins.enchantedbook;

import java.lang.reflect.Method;
import java.util.Map;

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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Plugin extends JavaPlugin implements Listener {

    private static final Integer ENCHANTMENT_WEIGHT_COMMON = 10;
    private static final Integer ENCHANTMENT_WEIGHT_UNCOMMON = 5;
    private static final Integer ENCHANTMENT_WEIGHT_RARE = 2;
    private static final Integer ENCHANTMENT_WEIGHT_VERY_RARE = 1;

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
    private static int getEnchantmentCost(final @NotNull Enchantment enchantment, int level) {
        int enchantmentCost;
        final Object handle;
        handle = getHandle(enchantment);
        if (handle == null)
            return -1;
        if ((enchantmentCost = getEnchantmentCostByWeight(handle, level)) >= 0)
            return enchantmentCost;
        if ((enchantmentCost = getEnchantmentCostByAnvilCost(handle, level)) >= 0)
            return enchantmentCost;
        try {
            enchantmentCost = getEnchantmentCostByRarity(enchantment, level);
        }
        catch (final UnsupportedOperationException exception) {
        }
        if (enchantmentCost >= 0)
            return enchantmentCost;
        Bukkit.getLogger().severe("Enchantment cost calculation is not supported for this server version.");
        return -1;
    }

    @SuppressWarnings("unused")
    private static int getEnchantmentCostByAnvilCost(final @NotNull Enchantment enchantment, int level) {
        return getEnchantmentCostByAnvilCost(getHandle(enchantment), level);
    }

    private static int getEnchantmentCostByAnvilCost(final @Nullable Object handle, int level) {
        Method getAnvilCostMethod = null;
        final Object anvilCost;
        try {
            getAnvilCostMethod = handle.getClass().getMethod("getAnvilCost");
        }
        catch (final Exception exception) {
            return -1;
        }
        if (getAnvilCostMethod == null)
            return -1;
        try {
            anvilCost = getAnvilCostMethod.invoke(handle);
        }
        catch (final Exception exception) {
            return -1;
        }
        if (anvilCost instanceof Integer)
            return (int)anvilCost * level;
        return -1;
    }

    @SuppressWarnings({"deprecation", "removal"})
    private static int getEnchantmentCostByRarity(final @NotNull Enchantment enchantment, int level) {
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

    @SuppressWarnings("unused")
    private static int getEnchantmentCostByWeight(final @NotNull Enchantment enchantment, int level) {
        return getEnchantmentCostByWeight(getHandle(enchantment), level);
    }

    private static int getEnchantmentCostByWeight(final @Nullable Object handle, int level) {
        Method getWeightMethod = null;
        final Object weight;
        try {
            getWeightMethod = handle.getClass().getMethod("getWeight");
        }
        catch (final Exception exception) {
            return -1;
        }
        if (getWeightMethod == null)
            return -1;
        try {
            weight = getWeightMethod.invoke(handle);
        }
        catch (final Exception exception) {
            return -1;
        }
        if (
            ENCHANTMENT_WEIGHT_COMMON.equals(weight) ||
            ENCHANTMENT_WEIGHT_UNCOMMON.equals(weight)
        )
            return level;
        if (ENCHANTMENT_WEIGHT_RARE.equals(weight))
            return level * 2;
        if (ENCHANTMENT_WEIGHT_VERY_RARE.equals(weight))
            return level * 4;
        return -1;
    }

    private static @Nullable Object getHandle(final @NotNull Enchantment enchantment) {
        try {
            return enchantment.getClass().getMethod("getHandle").invoke(enchantment);
        }
        catch (final Exception exception) {
            return null;
        }
    }

    @EventHandler
    private void onPrepareAnvil(final @NotNull PrepareAnvilEvent event) {
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
        else
            ((Repairable)resultMeta).setRepairCost(secondItemRepairCost);
        result.setItemMeta(resultMeta);
        anvilInventory.setRepairCost(totalEnchantmentCost);
        event.setResult(result);
    }

    // MARK: JavaPlugin

    @Override
    public void onEnable() {
        super.onEnable();
        getServer().getPluginManager().registerEvents(this, this);
    }
}
