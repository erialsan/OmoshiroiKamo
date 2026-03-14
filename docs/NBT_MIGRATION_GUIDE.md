# NBT System Migration Guide

## Overview

The NBT system has been unified across all Input/Output types. The legacy `BlockNbtInput` and `BlockNbtOutput` classes have been removed and their functionality has been integrated into the standard `BlockInput` and `BlockOutput` classes.

## Breaking Changes

### Removed Classes
- `BlockNbtInput` - Use `BlockInput` with `nbt` or `nbtlist` fields instead
- `BlockNbtOutput` - Use `BlockOutput` with `nbt` or `nbtlist` fields instead

### Removed Recipe Types
- Input type `"block_nbt"` is no longer supported
- Output type `"block_nbt"` is no longer supported

## Migration Steps

### For BlockNbtInput

**Old Format (NO LONGER WORKS):**
```json
{
  "type": "block_nbt",
  "block": "minecraft:chest",
  "nbt": "energy",
  "op": ">=",
  "amount": 1000
}
```

**New Format:**
```json
{
  "type": "block",
  "block": "minecraft:chest",
  "nbt": "nbt('energy') >= 1000"
}
```

### For BlockNbtOutput

**Old Format (NO LONGER WORKS):**
```json
{
  "type": "block_nbt",
  "block": "minecraft:chest",
  "nbt": "energy",
  "op": "set",
  "amount": 1000
}
```

**New Format:**
```json
{
  "type": "block",
  "block": "minecraft:chest",
  "nbt": "nbt('energy') = 1000"
}
```

## New NBT System Features

### 1. Expression-Based NBT

You can now use full expression syntax for NBT operations:

```json
{
  "type": "item",
  "item": "minecraft:diamond_sword",
  "nbt": [
    "nbt('Damage') = 100",
    "nbt('RepairCost') = 5"
  ]
}
```

**Supported Operations:**
- `=` - Assignment (set)
- `+=` - Addition
- `-=` - Subtraction
- `*=` - Multiplication
- `/=` - Division

**Expressions can include:**
- Constants: `nbt('energy') = 1000`
- Math: `nbt('power') = 50 + 25`
- Comparisons (for inputs): `nbt('durability') >= 100`
- Context variables: `nbt('stored') = context.tickCount * 10`

### 2. NBTList Operations

For complex NBT list manipulations (like enchantments), use the `nbtlist` field:

```json
{
  "type": "item",
  "item": "minecraft:diamond_sword",
  "nbtlist": {
    "path": "ench",
    "ops": [
      {"id": 16, "lvl": ">=5"},
      {"id": 20, "lvl": 0},
      {"id": 34, "lvl": "+1"}
    ]
  }
}
```

**Operation Auto-Inference:**
- `">=5"`, `">5"`, `"<5"`, `"<=5"`, `"==5"`, `"!=5"` → REQUIRE (condition check)
- `0` → REMOVE (delete matching item)
- `"+5"` → MODIFY (relative change)
- `5` → SET (absolute value)

**Path Examples:**
- `"ench"` - Enchantments list
- `"AttributeModifiers"` - Attribute modifiers
- `"Items"` - Inventory items (for chests, etc.)

### 3. Type Inference with Suffixes

NBT values support type suffixes for explicit type control:

```json
{
  "nbt": [
    "nbt('shortValue') = 16s",
    "nbt('intValue') = 100i",
    "nbt('longValue') = 1000000L",
    "nbt('floatValue') = 1.5f",
    "nbt('doubleValue') = 3.14159d",
    "nbt('byteValue') = 1b"
  ]
}
```

Without suffixes, the system automatically infers the appropriate type.

## Complete Examples

### Example 1: Item with Enchantments (Input)

```json
{
  "type": "item",
  "item": "minecraft:diamond_sword",
  "nbtlist": {
    "path": "ench",
    "ops": [
      {"id": 16, "lvl": ">=5"}
    ]
  }
}
```
*Requires a diamond sword with Sharpness V or higher*

### Example 2: Item with Enchantments (Output)

```json
{
  "type": "item",
  "item": "minecraft:diamond_sword",
  "nbtlist": {
    "path": "ench",
    "ops": [
      {"id": 16, "lvl": 5},
      {"id": 34, "lvl": 3}
    ]
  }
}
```
*Creates a diamond sword with Sharpness V and Unbreaking III*

### Example 3: Block with TileEntity NBT

```json
{
  "type": "block",
  "block": "minecraft:chest",
  "nbt": [
    "nbt('CustomName') = 'Powered Chest'",
    "nbt('energy') = 10000"
  ]
}
```

### Example 4: Complex Enchantment Manipulation

```json
{
  "type": "item",
  "item": "minecraft:diamond_sword",
  "nbtlist": {
    "path": "ench",
    "ops": [
      {"id": 16, "lvl": ">=3"},
      {"id": 20, "lvl": 0},
      {"id": 34, "lvl": "+1"}
    ]
  }
}
```
*Requires Sharpness III+, removes Fire Aspect, and adds +1 to Unbreaking*

## Legacy BlockOutput Compatibility

`BlockOutput` maintains backward compatibility with the old `dynamicNbt` system:

```json
{
  "type": "block",
  "block": "minecraft:chest",
  "dynamicNbt": {
    "energy": "1000 + context.tickCount"
  }
}
```

However, it's recommended to migrate to the new `nbt` field for consistency:

```json
{
  "type": "block",
  "block": "minecraft:chest",
  "nbt": "nbt('energy') = 1000 + context.tickCount"
}
```

## Migration Checklist

- [ ] Replace all `"type": "block_nbt"` with `"type": "block"`
- [ ] Convert old `nbt`/`op`/`amount` format to new expression format
- [ ] Update enchantment operations to use `nbtlist` format
- [ ] Test all affected recipes in-game
- [ ] Consider using new features like type suffixes and complex expressions

## Need Help?

If you encounter issues during migration, please check:
1. Expression syntax - ensure proper quotes around NBT keys: `nbt('key')`
2. Operation formats - use proper operators (=, +=, -=, >=, etc.)
3. nbtlist path - make sure the path matches your NBT structure
4. Type inference - use suffixes (s, i, L, f, d, b) if needed for specific types
