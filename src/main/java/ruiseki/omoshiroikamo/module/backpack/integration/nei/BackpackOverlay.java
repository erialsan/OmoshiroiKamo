package ruiseki.omoshiroikamo.module.backpack.integration.nei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import codechicken.nei.FastTransferManager;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.IOverlayHandler;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.IRecipeHandler;
import ruiseki.omoshiroikamo.core.client.gui.slot.ModularCraftingMatrixSlot;

public class BackpackOverlay implements IOverlayHandler {

    @Override
    public void overlayRecipe(GuiContainer gui, IRecipeHandler recipe, int recipeIndex, boolean maxTransfer) {
        transferRecipe(gui, recipe, recipeIndex, maxTransfer ? Integer.MAX_VALUE : 1);
    }

    @Override
    public int transferRecipe(GuiContainer gui, IRecipeHandler recipe, int recipeIndex, int multiplier) {
        List<PositionedStack> ingredients = recipe.getIngredientStacks(recipeIndex);

        if (!clearGrid(gui)) return 0;

        Slot[][] slotMap = mapIngredSlots(gui, ingredients);

        if (multiplier == 0) {
            multiplier = calculateMultiplier(gui, ingredients);;
        }

        if (multiplier <= 0) {
            return 0;
        }

        HashMap<Integer, Integer> usedItems = new HashMap<>();
        for (int m = 0; m < multiplier; m++) {

            for (int i = 0; i < ingredients.size(); i++) {

                PositionedStack ps = ingredients.get(i);
                if (ps == null || ps.item == null) continue;

                Slot target = slotMap[i][0];

                int slot = findInventoryStack(gui, ps.item, usedItems);
                if (slot == -1) return m;

                int used = usedItems.getOrDefault(slot, 0);
                usedItems.put(slot, used + ps.item.stackSize);

                FastTransferManager.clickSlot(gui, slot);
                FastTransferManager.clickSlot(gui, target.slotNumber, 1);
                FastTransferManager.clickSlot(gui, slot);
            }
        }

        return multiplier;
    }

    private boolean clearGrid(GuiContainer gui) {

        for (Slot slot : gui.inventorySlots.inventorySlots) {

            if (!(slot instanceof ModularCraftingMatrixSlot matrix) || !matrix.isActive()) {
                continue;
            }

            if (!slot.getHasStack()) continue;

            FastTransferManager.clickSlot(gui, slot.slotNumber, 0, 1);

            if (slot.getHasStack()) return false;
        }

        return true;
    }

    private int findInventoryStack(GuiContainer gui, ItemStack stack, HashMap<Integer, Integer> usedItems) {

        for (Slot slot : gui.inventorySlots.inventorySlots) {

            if (slot instanceof ModularCraftingMatrixSlot) continue;
            if (!slot.getHasStack()) continue;

            ItemStack inv = slot.getStack();

            if (!NEIClientUtils.areStacksSameTypeCrafting(inv, stack)) continue;

            int used = usedItems.getOrDefault(slot.slotNumber, 0);

            if (inv.stackSize - used >= stack.stackSize) {
                return slot.slotNumber;
            }
        }

        return -1;
    }

    private Slot[][] mapIngredSlots(GuiContainer gui, List<PositionedStack> ingredients) {

        Slot[][] recipeSlotList = new Slot[ingredients.size()][];

        List<Slot> craftingSlots = new ArrayList<>();

        for (Slot slot : gui.inventorySlots.inventorySlots) {
            if (slot instanceof ModularCraftingMatrixSlot matrix && matrix.isActive()) {
                craftingSlots.add(slot);
            }
        }

        int startX = 25;
        int startY = 6;
        int slotSize = 18;

        for (int i = 0; i < ingredients.size(); i++) {

            PositionedStack ps = ingredients.get(i);

            int col = (ps.relx - startX) / slotSize;
            int row = (ps.rely - startY) / slotSize;

            int index = row * 3 + col;

            if (index >= 0 && index < craftingSlots.size()) {
                recipeSlotList[i] = new Slot[] { craftingSlots.get(index) };
            }
        }

        return recipeSlotList;
    }

    private int calculateMultiplier(GuiContainer gui, List<PositionedStack> ingredients) {

        int max = Integer.MAX_VALUE;

        for (PositionedStack ps : ingredients) {

            if (ps == null || ps.item == null) continue;

            int need = ps.item.stackSize;
            int available = countInventory(gui, ps.item);

            if (available <= 0) {
                return 0;
            }

            max = Math.min(max, available / need);
        }

        return max == Integer.MAX_VALUE ? 1 : max;
    }

    private int countInventory(GuiContainer gui, ItemStack stack) {

        int count = 0;

        for (Slot slot : gui.inventorySlots.inventorySlots) {

            if (slot instanceof ModularCraftingMatrixSlot) continue;

            if (!slot.getHasStack()) continue;

            ItemStack s = slot.getStack();

            if (NEIClientUtils.areStacksSameTypeCrafting(s, stack)) {
                count += s.stackSize;
            }
        }

        return count;
    }

    @Override
    public List<GuiOverlayButton.ItemOverlayState> presenceOverlay(GuiContainer gui, IRecipeHandler recipe,
        int recipeIndex) {

        List<GuiOverlayButton.ItemOverlayState> result = new ArrayList<>();

        for (PositionedStack ps : recipe.getIngredientStacks(recipeIndex)) {
            result.add(new GuiOverlayButton.ItemOverlayState(ps, true));
        }

        return result;
    }
}
