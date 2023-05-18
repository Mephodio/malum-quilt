package ca.rttv.malum.registry;

import ca.rttv.malum.util.spirit.SpiritType;
import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.LinkedHashMap;
import java.util.Map;

import static ca.rttv.malum.Malum.MODID;

public interface MalumStatusEffectRegistry {
    Map<Identifier, StatusEffect> STATUS_EFFECTS = new LinkedHashMap<>();

    StatusEffect AERIAL_AURA            = register("aerial_aura",            new StatusEffect(StatusEffectType.BENEFICIAL, SpiritType.AERIAL_SPIRIT.color.getRGB()).addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED, "E3F9C028-D6CC-4CF2-86A6-D5B5EFD86BE6", 0.2d, EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
    StatusEffect CORRUPTED_AERIAL_AURA  = register("corrupted_aerial_aura",  new StatusEffect(StatusEffectType.BENEFICIAL, SpiritType.AERIAL_SPIRIT.color.getRGB()));
    StatusEffect AQUEOUS_AURA           = register("aqueous_aura",           new StatusEffect(StatusEffectType.BENEFICIAL, SpiritType.AQUEOUS_SPIRIT.color.getRGB()).addAttributeModifier(ReachEntityAttributes.REACH, "a2a69bb7-468d-4c45-a004-eae2f4471e5e", 2.0d, EntityAttributeModifier.Operation.ADDITION).addAttributeModifier(ReachEntityAttributes.ATTACK_RANGE, "0669c96f-32b8-4e26-8385-64ca857ee687", 2.0d, EntityAttributeModifier.Operation.ADDITION));
    StatusEffect SACRED_AURA            = register("sacred_aura",            new StatusEffect(StatusEffectType.BENEFICIAL, SpiritType.SACRED_SPIRIT.color.getRGB()));
    StatusEffect EARTHEN_AURA           = register("earthen_aura",           new StatusEffect(StatusEffectType.BENEFICIAL, SpiritType.EARTHEN_SPIRIT.color.getRGB()).addAttributeModifier(EntityAttributes.GENERIC_ARMOR, "04448cbf-ee2c-4f36-b71f-e641a312834a", 3.0d, EntityAttributeModifier.Operation.MULTIPLY_TOTAL).addAttributeModifier(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, "dc5fc5d7-db54-403f-810d-a16de6293ffd", 1.5d, EntityAttributeModifier.Operation.ADDITION));
    StatusEffect CORRUPTED_EARTHEN_AURA = register("corrupted_earthen_aura", new StatusEffect(StatusEffectType.BENEFICIAL, SpiritType.EARTHEN_SPIRIT.color.getRGB()).addAttributeModifier(EntityAttributes.GENERIC_ATTACK_DAMAGE, "e2a25284-a8b1-41a5-9472-90cc83793d44", 2.0d, EntityAttributeModifier.Operation.ADDITION));
    StatusEffect INFERNAL_AURA          = register("infernal_aura",          new StatusEffect(StatusEffectType.BENEFICIAL, SpiritType.INFERNAL_SPIRIT.color.getRGB()).addAttributeModifier(EntityAttributes.GENERIC_ATTACK_SPEED, "0a74b987-a6ec-4b9f-815e-a589bf435b93", 0.2d, EntityAttributeModifier.Operation.MULTIPLY_TOTAL));

    static <T extends StatusEffect> StatusEffect register(String id, T effect) {
        STATUS_EFFECTS.put(new Identifier(MODID, id), effect);
        return effect;
    }

    static void init() {
        STATUS_EFFECTS.forEach((id, effect) -> Registry.register(Registry.STATUS_EFFECT, id, effect));
    }
}
