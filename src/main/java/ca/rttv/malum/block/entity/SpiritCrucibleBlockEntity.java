package ca.rttv.malum.block.entity;

import ca.rttv.malum.block.SpiritCatalyzerBlock;
import ca.rttv.malum.client.init.MalumParticleRegistry;
import ca.rttv.malum.inventory.DefaultedInventory;
import ca.rttv.malum.item.SpiritItem;
import ca.rttv.malum.recipe.SpiritFocusingRecipe;
import ca.rttv.malum.recipe.SpiritRepairRecipe;
import ca.rttv.malum.util.block.entity.ICrucibleAccelerator;
import ca.rttv.malum.util.helper.DataHelper;
import ca.rttv.malum.util.helper.SpiritHelper;
import ca.rttv.malum.util.particle.ParticleBuilders;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.random.RandomGenerator;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.StreamSupport;

import static ca.rttv.malum.registry.MalumBlockEntityRegistry.SPIRIT_CRUCIBLE_BLOCK_ENTITY;

public class SpiritCrucibleBlockEntity extends BlockEntity implements DefaultedInventory {
    public final DefaultedList<ItemStack> heldItem = DefaultedList.ofSize(1, ItemStack.EMPTY);
    public final DefaultedList<ItemStack> spiritSlots = DefaultedList.ofSize(9, ItemStack.EMPTY);
    private float spiritSpin = 0.0f;
    @Nullable
    public SpiritFocusingRecipe focusingRecipe;
    @Nullable
    public SpiritRepairRecipe repairRecipe;
    @Nullable
    List<Pair<ICrucibleAccelerator, BlockPos>> accelerators = null;
    @Nullable
    private List<ItemStack> tabletStacks = null;
    private Vec3d tabletItemPos = new Vec3d(0, 0, 0); // todo
    private float progress = 0.0f;
    private float speed = 0.0f;
    private int queuedCracks = 0;

    public SpiritCrucibleBlockEntity(BlockPos pos, BlockState state) {
        this(SPIRIT_CRUCIBLE_BLOCK_ENTITY, pos, state);
    }

    public SpiritCrucibleBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    public void searchRecipes(boolean checkFocusing) {
        if (checkFocusing) {
            focusingRecipe = SpiritFocusingRecipe.getRecipe(world, this.getHeldItem(), spiritSlots);
        }

        if (focusingRecipe == null) {
            if (tabletStacks == null) {
                Map<Item, Integer> map = new LinkedHashMap<>();
                BlockPos.iterateOutwards(pos, 4, 2, 4).forEach(possiblePos -> {
                    if (world != null && world.getBlockEntity(possiblePos) instanceof TabletBlockEntity displayBlock && !displayBlock.getHeldItem().isEmpty()) {
                        Item key = displayBlock.getHeldItem().getItem();
                        Integer value = displayBlock.getHeldItem().getCount();
                        map.merge(key, value, Integer::sum);
                        tabletItemPos = displayBlock.itemOffset();
                    }
                });
                tabletStacks = new ArrayList<>();
                map.forEach((item, count) -> tabletStacks.add(new ItemStack(item, count)));
            }
            repairRecipe = SpiritRepairRecipe.getRecipe(world, this.getHeldItem(), spiritSlots, tabletStacks);
        }
    }

    public static Vec3d spiritOffset(SpiritCrucibleBlockEntity blockEntity, int slot, float tickDelta) {
        float distance = 0.75f + (float) Math.sin(blockEntity.spiritSpin / 20f) * 0.025f;
        float height = 0.75f;
        return DataHelper.rotatedCirclePosition(new Vec3d(0.5f, height, 0.5f), distance, slot, blockEntity.getSpiritCount(), (long) blockEntity.spiritSpin, 360.0f, tickDelta);
    }

    private float getSpiritCount() {
        return spiritSlots.stream()
                          .filter(stack -> stack != ItemStack.EMPTY)
                          .count();
    }

    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, RandomGenerator random) {

//        if (queuedCracks > 0) {
//            crackTimer++;
//            if (crackTimer % 7 == 0) {
//                float pitch = 0.95f + (crackTimer - 8) * 0.015f + world.random.nextFloat() * 0.05f;
//                world.playSound(null, this.pos, MalumSoundRegistry.IMPETUS_CRACK, SoundCategory.BLOCKS, 0.7f, pitch);
//                queuedCracks--;
//                if (queuedCracks == 0) {
//                    crackTimer = 0;
//                }
//            }
//        }

        if (focusingRecipe != null) {
            if (accelerators == null) {
                accelerators = new ArrayList<>();
                StreamSupport.stream(BlockPos.iterateOutwards(pos, 4, 2, 4).spliterator(), false).filter(possiblePos -> {
                    BlockState state2 = world.getBlockState(possiblePos);
                    if (state2.getBlock() instanceof ICrucibleAccelerator accelerator && (!state2.contains(SpiritCatalyzerBlock.HALF) || state2.get(SpiritCatalyzerBlock.HALF) == DoubleBlockHalf.UPPER)) {
                        accelerators.add(new Pair<>(accelerator, possiblePos.toImmutable()));
                    }
                    return accelerators.size() >= 8;
                }).findFirst();
            }

            int cnt = Math.min(accelerators.stream().filter(pair -> pair.getLeft().canAccelerate(pair.getRight(), world)).mapToInt(pair -> {
                pair.getLeft().tick(pair.getRight(), world);
                return 1;
            }).sum(), SpiritCatalyzerBlock.SPEED_INCREASE.length - 1);
            speed = SpiritCatalyzerBlock.SPEED_INCREASE[cnt];
            progress += speed + 1;

            if (progress >= focusingRecipe.time()) {
                focusingRecipe.craft(this);

                int durabilityCost = focusingRecipe.durabilityCost();
                if (world.random.nextFloat() <= SpiritCatalyzerBlock.DAMAGE_CHANCES[cnt]) {
                    durabilityCost += world.random.nextInt(SpiritCatalyzerBlock.DAMAGE_MAX_VALUE[cnt] + 1);
                }
                queuedCracks += durabilityCost;
                if (this.getHeldItem().damage(durabilityCost, world.random, null)) {
                    Identifier id = Registry.ITEM.getId(this.getHeldItem().getItem());
                    this.setStack(0, Registry.ITEM.get(new Identifier(id.getNamespace(), "cracked_" + id.getPath())).getDefaultStack());
                }
                progress = 0;
                this.notifyListeners();
                searchRecipes(true);
            }
        } else if (repairRecipe != null) {
            ItemStack damagedItem = this.getHeldItem();
            int time = 400 + damagedItem.getDamage() * 5;
            progress++;
            if (progress >= time) {
                repairRecipe.craft(this);
                progress = 0;
                tabletStacks = null;
                this.notifyListeners();
                searchRecipes(true);
            }
        }

        if (focusingRecipe == null && repairRecipe == null) {
            progress = 0;
        }
    }

    public void clientTick(World world, BlockPos pos, BlockState state) {
        spiritSpin += (1 + Math.cos(Math.sin(world.getTime() % 7100 * 0.1f))) * (1 + speed * 0.1f);
        passiveParticles();
    }

    private void passiveParticles() {
        if (world == null) {
            return;
        }

        Vec3d itemPos = new Vec3d(0.5d, 0.6d, 0.5d);
        //passive spirit particles
        if (!spiritSlots.isEmpty()) {
            for (int i = 0; i < spiritSlots.size(); i++) {
                ItemStack item = spiritSlots.get(i);
                if (item.getItem() instanceof SpiritItem spiritSplinterItem) {
                    Vec3d offset = spiritOffset(this, i, 0.5f);
                    Color color = spiritSplinterItem.type.color;
                    Color endColor = spiritSplinterItem.type.endColor;
                    double x = pos.getX() + offset.x;
                    double y = pos.getY() + offset.y;
                    double z = pos.getZ() + offset.z;
                    SpiritHelper.spawnSpiritParticles(world, x, y, z, color, endColor);
                }
            }
        }

        if (repairRecipe != null) {
            ArrayList<Color> colors = new ArrayList<>();
            ArrayList<Color> endColors = new ArrayList<>();
            if (tabletStacks != null && tabletStacks.get(0).getItem() instanceof SpiritItem spiritItem) {
                colors.add(spiritItem.type.color);
                endColors.add(spiritItem.type.endColor);
            } else if (!spiritSlots.isEmpty()) {
                for (int i = 0; i < spiritSlots.size(); i++) {
                    ItemStack item = spiritSlots.get(i);
                    if (item.getItem() instanceof SpiritItem spiritItem) {
                        colors.add(spiritItem.type.color);
                        endColors.add(spiritItem.type.endColor);
                    }
                }
            }
            for (int i = 0; i < colors.size(); i++) {
                Color color = colors.get(i);
                Color endColor = endColors.get(i);
                Vec3d velocity = tabletItemPos.subtract(itemPos).normalize().multiply(-0.1f);
                ParticleBuilders.create(MalumParticleRegistry.STAR_PARTICLE)
                        .setAlpha(0.24f / colors.size(), 0f)
                        .setLifetime(15)
                        .setScale(0.45f + world.random.nextFloat() * 0.15f, 0)
                        .randomOffset(0.05)
                        .setSpinOffset(world.getTime() % 710 * 0.075f)
                        .setColor(color, endColor)
                        .enableNoClip()
                        .repeat(world, itemPos.x, itemPos.y, itemPos.z, 1);

                ParticleBuilders.create(MalumParticleRegistry.STAR_PARTICLE)
                        .setAlpha(0.24f / colors.size(), 0f)
                        .setLifetime(15)
                        .setScale(0.45f + world.random.nextFloat() * 0.15f, 0)
                        .randomOffset(0.05)
                        .setSpinOffset(world.getTime() % 710 * -0.075f)
                        .setColor(color, endColor)
                        .enableNoClip()
                        .repeat(world, tabletItemPos.x, tabletItemPos.y, tabletItemPos.z, 1);

                ParticleBuilders.create(MalumParticleRegistry.WISP_PARTICLE)
                        .setAlpha(0.4f / colors.size(), 0f)
                        .setLifetime((int) (10 + world.random.nextInt(8) + Math.sin(world.getTime() % 710 * 0.5)))
                        .setScale(0.2f + world.random.nextFloat() * 0.15f, 0)
                        .randomOffset(0.05)
                        .setSpinOffset(world.getTime() % 710 * 0.075f)
                        .setSpin(0.1f + world.random.nextFloat() * 0.05f)
                        .setColor(color.brighter(), endColor)
                        .setAlphaCurveMultiplier(0.5f)
                        .setColorCurveMultiplier(0.75f)
                        .setMotion(velocity.x, velocity.y, velocity.z)
                        .enableNoClip()
                        .repeat(world, tabletItemPos.x, tabletItemPos.y, tabletItemPos.z, 1);
            }
            return;
        }
        if (focusingRecipe != null) {
            for (int i = 0; i < spiritSlots.size(); i++) {
                ItemStack item = spiritSlots.get(i);
                if (item.getItem() instanceof SpiritItem spiritSplinterItem) {
                    Vec3d offset = spiritOffset(this, i, 0.5f);
                    Color color = spiritSplinterItem.type.color;
                    Color endColor = spiritSplinterItem.type.endColor;
                    double x = pos.getX() + offset.x;
                    double y = pos.getY() + offset.y;
                    double z = pos.getZ() + offset.z;
                    Vec3d velocity = new Vec3d(x, y, z).subtract(itemPos).normalize().multiply(-0.03f);
                    ParticleBuilders.create(MalumParticleRegistry.WISP_PARTICLE)
                            .setAlpha(0.30f, 0f)
                            .setLifetime(40)
                            .setScale(0.2f, 0)
                            .randomOffset(0.02f)
                            .randomMotion(0.01f, 0.01f)
                            .setColor(color, endColor)
                            .setColorCurveMultiplier(0.75f)
                            .randomMotion(0.0025f, 0.0025f)
                            .addMotion(velocity.x, velocity.y, velocity.z)
                            .enableNoClip()
                            .repeat(world, x, y, z, 1);

                    ParticleBuilders.create(MalumParticleRegistry.WISP_PARTICLE)
                            .setAlpha(0.12f / this.getSpiritCount(), 0f)
                            .setLifetime(25)
                            .setScale(0.2f + world.random.nextFloat() * 0.1f, 0)
                            .randomOffset(0.05)
                            .setSpinOffset(world.getTime() % 710 * 0.075f)
                            .setColor(color, endColor)
                            .enableNoClip()
                            .repeat(world, itemPos.x, itemPos.y, itemPos.z, 1);

                    ParticleBuilders.create(MalumParticleRegistry.STAR_PARTICLE)
                            .setAlpha(0.16f / this.getSpiritCount(), 0f)
                            .setLifetime(25)
                            .setScale(0.45f + world.random.nextFloat() * 0.1f, 0)
                            .randomOffset(0.05)
                            .setSpinOffset(world.getTime() % 710 * 0.075f)
                            .setColor(color, endColor)
                            .enableNoClip()
                            .repeat(world, itemPos.x, itemPos.y, itemPos.z, 1);
                }
            }
        }
    }

    @Override
    public World getWorld() {
        return super.getWorld();
    }

    @Override
    public BlockPos getPos() {
        return super.getPos();
    }

    @Override
    public BlockState getCachedState() {
        return super.getCachedState();
    }

    public static void resetAccelerators(World world, BlockPos center) {
        BlockPos.iterateOutwards(center, 4, 2, 4).forEach(blockPos -> {
            if (world.getBlockEntity(blockPos) instanceof SpiritCrucibleBlockEntity spiritCrucibleBlockEntity) {
                spiritCrucibleBlockEntity.accelerators = null;
            }
        });
    }
    public static void resetTablets(World world, BlockPos center) {
        BlockPos.iterateOutwards(center, 4, 2, 4).forEach(blockPos -> {
            if (world.getBlockEntity(blockPos) instanceof SpiritCrucibleBlockEntity spiritCrucibleBlockEntity) {
                spiritCrucibleBlockEntity.tabletStacks = null;
                spiritCrucibleBlockEntity.searchRecipes(false);
            }
        });
    }

    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (player.getStackInHand(hand).getItem() instanceof SpiritItem) {
            this.addSpirits(state, world, pos, player, hand, hit);
        } else if (this.getHeldItem().isEmpty() && player.getStackInHand(hand).isEmpty()) {
            this.grabSpirit(state, world, pos, player, hand, hit);
        } else {
            this.swapSlots(state, world, pos, player, hand, hit);
        }

        return ActionResult.CONSUME;
    }

    private void addSpirits(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack handStack = player.getStackInHand(hand);
        if (handStack.isEmpty()) return;
        for (ItemStack stack : this.spiritSlots) {
            if (stack.getItem() == handStack.getItem()) {
                if (stack.getCount() + handStack.getCount() <= stack.getMaxCount()) {
                    stack.increment(handStack.getCount());
                    player.setStackInHand(hand, ItemStack.EMPTY);
                } else {
                    int maxAddition = Math.max(stack.getMaxCount() - stack.getCount(), 0);
                    stack.increment(maxAddition);
                    handStack.decrement(maxAddition);
                }
                searchRecipes(true);
                this.notifyListeners();
                return;
            }
        }
        int index = -1;
        for (int i = 0; i < this.spiritSlots.size(); i++) {
            if (this.spiritSlots.get(i).isEmpty()) {
                index = i;
                break;
            }
        }
        if (index == -1) return;
        this.spiritSlots.set(index, handStack);
        player.setStackInHand(hand, ItemStack.EMPTY);
        searchRecipes(true);
        this.notifyListeners();
    }

    private void swapSlots(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (player.getStackInHand(hand).isEmpty() && this.getHeldItem().isEmpty()) {
            return;
        }

        world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2f, ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);

        ItemStack prevItem = getHeldItem();
        this.setStack(0, player.getStackInHand(hand));
        player.setStackInHand(hand, prevItem);
        searchRecipes(true);
    }

    private void grabSpirit(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        for (int i = this.spiritSlots.size() - 1; i >= 0; i--) {
            if (!this.spiritSlots.get(i).isEmpty()) {
                player.setStackInHand(hand, this.spiritSlots.get(i));
                this.spiritSlots.set(i, ItemStack.EMPTY);
                this.notifyListeners();
                searchRecipes(true);
                return;
            }
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        Inventories.writeNbt(nbt, heldItem);
        DataHelper.writeNbt(nbt, spiritSlots, "Spirits");
        nbt.putFloat("Progress", progress);
        nbt.putFloat("Speed", speed);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        heldItem.clear();
        spiritSlots.clear();
        Inventories.readNbt(nbt, heldItem);
        DataHelper.readNbt(nbt, spiritSlots, "Spirits");
        progress = nbt.getFloat("Progress");
        speed = nbt.getFloat("Speed");
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        NbtCompound tag = super.toInitialChunkDataNbt();
        this.writeNbt(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.of(this);
    }

    public ItemStack getHeldItem() {
        return this.getStack(0);
    }

    @Override
    public DefaultedList<ItemStack> getInvStackList() {
        return heldItem;
    }
}
