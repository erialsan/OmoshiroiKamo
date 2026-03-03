package ruiseki.omoshiroikamo.module.dml.recipe;

import java.util.ArrayList;
import java.util.List;

import ruiseki.omoshiroikamo.api.condition.ConditionContext;
import ruiseki.omoshiroikamo.api.condition.ICondition;
import ruiseki.omoshiroikamo.api.entity.dml.ModelRegistryItem;
import ruiseki.omoshiroikamo.api.modular.IModularPort;
import ruiseki.omoshiroikamo.api.modular.IPortType;
import ruiseki.omoshiroikamo.api.recipe.core.IModularRecipe;
import ruiseki.omoshiroikamo.api.recipe.io.IRecipeInput;
import ruiseki.omoshiroikamo.api.recipe.io.IRecipeOutput;
import ruiseki.omoshiroikamo.api.recipe.io.ItemOutput;
import ruiseki.omoshiroikamo.api.recipe.visitor.IRecipeVisitor;

public class SimulationChamberRecipe implements IModularRecipe {

    private final ModelRegistryItem model;
    private final List<IRecipeInput> inputs = new ArrayList<>();
    private final List<IRecipeOutput> outputs = new ArrayList<>();

    public SimulationChamberRecipe(ModelRegistryItem model) {
        this.model = model;
        // DataModelInput added here
        DataModelInput input = new DataModelInput(model.getId());
        // The DataModelInput itself handles initialization if needed
        this.inputs.add((IRecipeInput) input);

        // Setup outputs
        if (model.getLivingMatter() != null) {
            this.outputs.add(new ItemOutput(model.getLivingMatter()));
        }
        if (model.getPristineMatter() != null) {
            this.outputs.add(new ItemOutput(model.getPristineMatter()));
        }
    }

    @Override
    public String getRegistryName() {
        return "dml:simulation_" + model.getDisplayName()
            .toLowerCase();
    }

    @Override
    public String getRecipeGroup() {
        return "simulation_chamber";
    }

    @Override
    public String getName() {
        return model.getDisplayName();
    }

    @Override
    public int getDuration() {
        return 300; // Default duration for DML simulation
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public List<IRecipeInput> getInputs() {
        return inputs;
    }

    @Override
    public List<IRecipeOutput> getOutputs() {
        return outputs;
    }

    @Override
    public List<ICondition> getConditions() {
        return new ArrayList<>();
    }

    @Override
    public boolean isConditionMet(ConditionContext context) {
        return true;
    }

    @Override
    public boolean processInputs(List<IModularPort> inputPorts, boolean simulate) {
        for (IRecipeInput input : inputs) {
            if (!input.process(inputPorts, simulate)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean processOutputs(List<IModularPort> outputPorts, boolean simulate) {
        // Implementation for outputs logic (probabilistic)
        return true;
    }

    @Override
    public boolean matchesInput(List<IModularPort> inputPorts) {
        return processInputs(inputPorts, true);
    }

    @Override
    public boolean canOutput(List<IModularPort> outputPorts) {
        return true;
    }

    @Override
    public IPortType.Type checkOutputCapacity(List<IModularPort> outputPorts) {
        return null;
    }

    @Override
    public void onTick(ConditionContext context) {
        // Handle RF consumption per tick if needed
    }

    @Override
    public void accept(IRecipeVisitor visitor) {
        visitor.visit((IModularRecipe) this);
    }

    @Override
    public int compareTo(IModularRecipe o) {
        return 0; // Simple comparison for now
    }
}
