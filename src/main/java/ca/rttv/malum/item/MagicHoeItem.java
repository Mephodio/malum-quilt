package ca.rttv.malum.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ToolMaterial;

import static ca.rttv.malum.registry.MalumAttributeRegistry.MAGIC_DAMAGE;
import static ca.rttv.malum.registry.MalumRegistry.MAGIC_DAMAGE_MODIFIER_ID;

public class MagicHoeItem extends HoeItem {
    private final Multimap<EntityAttribute, EntityAttributeModifier> magicAttributeModifiers;
    public MagicHoeItem(ToolMaterial toolMaterial, float damage, float speed, float magic, Settings settings) {
        super(toolMaterial, (int) damage, speed, settings);
        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(
                EntityAttributes.GENERIC_ATTACK_DAMAGE,
                new EntityAttributeModifier(ATTACK_DAMAGE_MODIFIER_ID, "Weapon modifier", this.getAttackDamage(), EntityAttributeModifier.Operation.ADDITION)
        );
        builder.put(
                EntityAttributes.GENERIC_ATTACK_SPEED,
                new EntityAttributeModifier(ATTACK_SPEED_MODIFIER_ID, "Weapon modifier", speed, EntityAttributeModifier.Operation.ADDITION)
        );
        builder.put(
                MAGIC_DAMAGE,
                new EntityAttributeModifier(MAGIC_DAMAGE_MODIFIER_ID, "Weapon modifier", this.getAttackDamage() - magic, EntityAttributeModifier.Operation.ADDITION)
        );
        magicAttributeModifiers = builder.build();
    }

    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.magicAttributeModifiers : super.getAttributeModifiers(slot);
    }
}
