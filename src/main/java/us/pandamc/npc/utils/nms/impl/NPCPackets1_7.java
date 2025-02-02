package us.pandamc.npc.utils.nms.impl;

import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import net.minecraft.server.v1_7_R4.*;
import net.minecraft.util.com.mojang.authlib.GameProfile;
import net.minecraft.util.com.mojang.authlib.properties.Property;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_7_R4.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import us.pandamc.npc.PandaNPC;
import us.pandamc.npc.npc.NPC;
import us.pandamc.npc.utils.CC;
import us.pandamc.npc.utils.NPCUtils;
import us.pandamc.npc.utils.nms.NPCPackets;

import java.util.List;
import java.util.UUID;

public class NPCPackets1_7 implements NPCPackets {

    @Override
    public void spawn(NPC npc, Player player) {
        CraftPlayer playerCp = (CraftPlayer)player;
        EntityPlayer playerEp = playerCp.getHandle();

        WrappedGameProfile profile = WrappedGameProfile.fromPlayer(player);

        WrappedSignedProperty current = Iterables.getFirst(profile.getProperties().get("textures"), null);

        String texture = "";
        String signature = "";
        if(current  != null){
            texture = current.getValue();
            signature = current.getSignature();
        }

        if(npc.getSignature() != null && npc.getTexture() != null){
            texture = npc.getTexture();
            signature = npc.getSignature();
        }

        if(npc.getDisplayName() == null || npc.getLocation() == null ) return;

        GameProfile gameProfile = new GameProfile(UUID.randomUUID(), CC.translate(npc.getDisplayName()));

        gameProfile.getProperties().put("textures", new Property("textures", texture, signature));

        EntityPlayer entityPlayer = new EntityPlayer(MinecraftServer.getServer(),
            ((CraftWorld)npc.getLocation().getWorld()).getHandle(),
            gameProfile,
            new PlayerInteractManager(((CraftWorld)npc.getLocation().getWorld()).getHandle()));

        entityPlayer.setPosition(npc.getLocation().getX(), npc.getLocation().getY(), npc.getLocation().getZ());

        PacketPlayOutPlayerInfo packetPlayOutPlayerInfo = PacketPlayOutPlayerInfo.addPlayer(entityPlayer);
        playerEp.playerConnection.sendPacket(packetPlayOutPlayerInfo);

        PacketPlayOutNamedEntitySpawn namedEntitySpawn = new PacketPlayOutNamedEntitySpawn(entityPlayer);
        DataWatcher data = entityPlayer.getDataWatcher();
        byte overlays = 0x01 | 0x02 | 0x04 | 0x08 | 0x10 | 0x20 | 0x40;
        data.watch(10, overlays);
        PacketPlayOutEntityMetadata metadata = new PacketPlayOutEntityMetadata(entityPlayer.getId(), data, true);
        playerEp.playerConnection.sendPacket(metadata);
        playerEp.playerConnection.sendPacket(namedEntitySpawn);

        List<PacketPlayOutEntityEquipment> entityEquipments = Lists.newArrayList();

        if(npc.getHand() != null) entityEquipments.add(new PacketPlayOutEntityEquipment(entityPlayer.getId(), 0, CraftItemStack.asNMSCopy(npc.getHand())));
        if(npc.getHelmet() != null) entityEquipments.add(new PacketPlayOutEntityEquipment(entityPlayer.getId(), 4, CraftItemStack.asNMSCopy(npc.getHelmet())));
        if(npc.getChest() != null) entityEquipments.add(new PacketPlayOutEntityEquipment(entityPlayer.getId(), 3, CraftItemStack.asNMSCopy(npc.getChest())));
        if(npc.getLegs() != null) entityEquipments.add(new PacketPlayOutEntityEquipment(entityPlayer.getId(), 2, CraftItemStack.asNMSCopy(npc.getLegs())));
        if(npc.getBoots() != null) entityEquipments.add(new PacketPlayOutEntityEquipment(entityPlayer.getId(), 1, CraftItemStack.asNMSCopy(npc.getBoots())));

        entityEquipments.forEach(packet -> playerEp.playerConnection.sendPacket(packet));

        new BukkitRunnable(){
            @Override
            public void run(){
                PacketPlayOutPlayerInfo packetPlayOutPlayerInfo = PacketPlayOutPlayerInfo.removePlayer(entityPlayer);
                playerEp.playerConnection.sendPacket(packetPlayOutPlayerInfo);
            }
        }.runTaskLaterAsynchronously(PandaNPC.get(), 20L * 5);

        npc.getEntitys().put(playerEp.getUniqueID(),entityPlayer.getId());

        PacketPlayOutEntityLook look = new PacketPlayOutEntityLook(entityPlayer.getId(),
            (byte) NPCUtils.getCompressedAngle(npc.getYaw()),
            (byte) NPCUtils.getCompressedAngle(npc.getPitch()),
            true);
        playerEp.playerConnection.sendPacket(look);

        PacketPlayOutEntityHeadRotation headRotation = new PacketPlayOutEntityHeadRotation(entityPlayer,
            (byte) NPCUtils.getCompressedAngle(npc.getHeadYaw()));
        playerEp.playerConnection.sendPacket(headRotation);
    }

    @Override
    public void destroy(NPC npc, Player player) {
        if (npc.getEntitys().containsKey(player.getUniqueId())) {
            CraftPlayer playerCp = (CraftPlayer)player;
            EntityPlayer playerEp = playerCp.getHandle();

            PacketPlayOutEntityDestroy destroy = new PacketPlayOutEntityDestroy(npc.getEntitys().get(player.getUniqueId()));

            playerEp.playerConnection.sendPacket(destroy);
        }
    }
}