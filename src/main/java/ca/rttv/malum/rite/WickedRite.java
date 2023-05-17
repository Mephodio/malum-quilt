package ca.rttv.malum.rite;

import ca.rttv.malum.network.packet.s2c.play.MalumParticleS2CPacket;
import ca.rttv.malum.registry.MalumDamageSourceRegistry;
import ca.rttv.malum.util.spirit.SpiritType;
import io.netty.buffer.Unpooled;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.random.RandomGenerator;
import org.quiltmc.qsl.networking.api.ServerPlayNetworking;

import static ca.rttv.malum.Malum.MODID;

public class WickedRite extends Rite {
    public WickedRite(Item... items) {
        super(items);
    }

    @Override
    public void onTick(BlockState state, ServerWorld world, BlockPos pos, RandomGenerator random, long tick) {
        if (tick % 60 != 0) {
            return;
        }

        world.getEntitiesByClass(LivingEntity.class, new Box(pos.add(-8, -8, -8), pos.add(8, 8, 8)), entity -> entity.getHealth() >= 2.5f && (!(entity instanceof PlayerEntity playerEntity) || !playerEntity.getAbilities().creativeMode)).forEach(entity -> entity.damage(MalumDamageSourceRegistry.VOODOO, 2.0f));
    }

    @Override
    public void onCorruptTick(BlockState state, ServerWorld world, BlockPos pos, RandomGenerator random, long tick) {
        if (tick % 60 != 0) {
            return;
        }

        world.getEntitiesByClass(LivingEntity.class, new Box(pos.add(-8, -8, -8), pos.add(8, 8, 8)), entity -> entity.getHealth() <= 2.5f && (!(entity instanceof PlayerEntity playerEntity) || !playerEntity.getAbilities().creativeMode)).forEach(entity -> {
            world.getPlayers(players -> players.getWorld().isChunkLoaded(entity.getChunkPos().x, entity.getChunkPos().z)).forEach(players -> {
                PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                new MalumParticleS2CPacket(SpiritType.WICKED_SPIRIT.color.getRGB(), entity.getX(), entity.getY(), entity.getZ()).write(buf);
                ServerPlayNetworking.send(players, new Identifier(MODID, "malumparticles2cpacket"), buf);
            });
            entity.damage(MalumDamageSourceRegistry.FORCED_SHATTER, 10.0f);
        });
    }
}
