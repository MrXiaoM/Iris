/*
 * Iris is a World Generator for Minecraft Bukkit Servers
 * Copyright (c) 2021 Arcane Arts (Volmit Software)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.volmit.iris.core.nms.v16_3;

import com.volmit.iris.Iris;
import com.volmit.iris.core.nms.INMSBinding;
import com.volmit.iris.engine.data.cache.AtomicCache;
import com.volmit.iris.util.collection.KMap;
import com.volmit.iris.util.nbt.io.NBTUtil;
import com.volmit.iris.util.nbt.mca.palette.BiomeContainer;
import com.volmit.iris.util.nbt.mca.palette.ChunkBiomeContainer;
import com.volmit.iris.util.nbt.mca.palette.IdMap;
import com.volmit.iris.util.nbt.mca.palette.PaletteAccess;
import com.volmit.iris.util.nbt.tag.CompoundTag;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

public class NMSBinding16_3 implements INMSBinding {
    private final KMap<Biome, Object> baseBiomeCache = new KMap<>();
    private final AtomicCache<IdMap<BiomeBase>> biomeMapCache = new AtomicCache<>();
    private Field biomeStorageCache = null;

    public boolean supportsDataPacks() {
        return true;
    }

    @Override
    public PaletteAccess createPalette() {
        return null;
    }

    private Object getBiomeStorage(ChunkGenerator.BiomeGrid g) {
        try {
            return getFieldForBiomeStorage(g).get(g);
        } catch (IllegalAccessException e) {
            Iris.reportError(e);
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean hasTile(Location l) {
        return ((CraftWorld) l.getWorld()).getHandle().getTileEntity(new BlockPosition(l.getBlockX(), l.getBlockY(), l.getBlockZ()), false) != null;
    }

    @Override
    public CompoundTag serializeTile(Location location) {
        TileEntity e = ((CraftWorld) location.getWorld()).getHandle().getTileEntity(new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ()), true);

        if (e == null) {
            return null;
        }

        NBTTagCompound tag = new NBTTagCompound();
        e.save(tag);
        return convert(tag);
    }

    @Override
    public void deserializeTile(CompoundTag s, Location newPosition) {
        NBTTagCompound c = convert(s);

        if (c != null) {
            int x = newPosition.getBlockX();
            int y = newPosition.getBlockY();
            int z = newPosition.getBlockZ();
            WorldServer w = ((CraftWorld) newPosition.getWorld()).getHandle();
            Chunk ch = w.getChunkAt(x >> 4, z >> 4);
            ChunkSection sect = ch.getSections()[y >> 4];
            IBlockData block = sect.getBlocks().a(x & 15, y & 15, z & 15);
            c.setInt("x", x);
            c.setInt("y", y);
            c.setInt("z", z);
            ch.a(TileEntity.create(block, c));
        }
    }

    private NBTTagCompound convert(CompoundTag tag) {
        try {
            ByteArrayOutputStream boas = new ByteArrayOutputStream();
            NBTUtil.write(tag, boas, false);
            DataInputStream din = new DataInputStream(new ByteArrayInputStream(boas.toByteArray()));
            NBTTagCompound c = NBTCompressedStreamTools.a((DataInput) din);
            din.close();
            return c;
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return null;
    }

    private CompoundTag convert(NBTTagCompound tag) {
        try {
            ByteArrayOutputStream boas = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(boas);
            NBTCompressedStreamTools.a(tag, (DataOutput) dos);
            dos.close();
            return (CompoundTag) NBTUtil.read(new ByteArrayInputStream(boas.toByteArray()), false).getTag();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        return null;
    }
    @Override
    public CompoundTag serializeEntity(Entity be) {
        net.minecraft.server.v1_16_R3.Entity entity = ((CraftEntity) be).getHandle();
        NBTTagCompound tag = new NBTTagCompound();
        entity.save(tag);
        CompoundTag t = convert(tag);
        t.putInt("btype", be.getType().ordinal());
        return t;
    }

    @Override
    public Entity deserializeEntity(CompoundTag s, Location newPosition) {
        EntityType type = EntityType.values()[s.getInt("btype")];
        s.remove("btype");
        NBTTagCompound tag = convert(s);
        NBTTagList pos = tag.getList("Pos", 6);
        pos.a(0, NBTTagDouble.a(newPosition.getX()));
        pos.a(1, NBTTagDouble.a(newPosition.getY()));
        pos.a(2, NBTTagDouble.a(newPosition.getZ()));
        tag.set("Pos", pos);
        org.bukkit.entity.Entity be = newPosition.getWorld().spawnEntity(newPosition, type);
        ((CraftEntity) be).getHandle().load(tag);

        return be;
    }

    @Override
    public boolean supportsCustomHeight() {
        return false;
    }

    @Override
    public int getMinHeight(World world) {
        return 0;
    }

    @Override
    public boolean supportsCustomBiomes() {
        return true;
    }

    private Field getFieldForBiomeStorage(Object storage) {
        Field f = biomeStorageCache;

        if (f != null) {
            return f;
        }
        try {

            f = storage.getClass().getDeclaredField("biome");
            f.setAccessible(true);
            return f;
        } catch (Throwable e) {
            Iris.reportError(e);
            e.printStackTrace();
            Iris.error(storage.getClass().getCanonicalName());
        }

        biomeStorageCache = f;
        return null;
    }

    private IRegistryWritable<BiomeBase> getCustomBiomeRegistry() {
        return ((CraftServer) Bukkit.getServer()).getHandle().getServer().getCustomRegistry().b(IRegistry.ay);
    }

    @Override
    public Object getBiomeBaseFromId(int id) {
        return getCustomBiomeRegistry().fromId(id);
    }

    @Override
    public int getTrueBiomeBaseId(Object biomeBase) {
        return getCustomBiomeRegistry().a((BiomeBase) biomeBase);
    }

    @Override
    public Object getTrueBiomeBase(Location location) {
        return ((CraftWorld) location.getWorld()).getHandle().getBiome(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    public String getTrueBiomeBaseKey(Location location) {
        return getKeyForBiomeBase(getTrueBiomeBase(location));
    }

    @Override
    public Object getCustomBiomeBaseFor(String mckey) {
        try {
            return getCustomBiomeRegistry().d(ResourceKey.a(IRegistry.ay, new MinecraftKey(mckey)));
        } catch (Throwable e) {
            Iris.reportError(e);
        }

        return null;
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public String getKeyForBiomeBase(Object biomeBase) {
        return getCustomBiomeRegistry().c((BiomeBase) biomeBase).get().a().toString();
    }

    @Override
    public Object getBiomeBase(World world, Biome biome) {
        return getBiomeBase(((CraftWorld) world).getHandle().r().b(IRegistry.ay), biome);
    }

    private Class<?>[] classify(Object... par) {
        Class<?>[] g = new Class<?>[par.length];
        for (int i = 0; i < g.length; i++) {
            g[i] = par[i].getClass();
        }

        return g;
    }

    private <T> T invoke(Object from, String name, Object... par) {
        try {
            Method f = from.getClass().getDeclaredMethod(name, classify(par));
            f.setAccessible(true);
            //noinspection unchecked
            return (T) f.invoke(from, par);
        } catch (Throwable e) {
            Iris.reportError(e);
            e.printStackTrace();
        }

        return null;
    }

    private <T> T invokeStatic(Class<?> from, String name, Object... par) {
        try {
            Method f = from.getDeclaredMethod(name, classify(par));
            f.setAccessible(true);
            //noinspection unchecked
            return (T) f.invoke(null, par);
        } catch (Throwable e) {
            Iris.reportError(e);
            e.printStackTrace();
        }

        return null;
    }

    private <T> T getField(Object from, String name) {
        try {
            Field f = from.getClass().getDeclaredField(name);
            f.setAccessible(true);
            //noinspection unchecked
            return (T) f.get(from);
        } catch (Throwable e) {
            Iris.reportError(e);
            e.printStackTrace();
        }

        return null;
    }

    private <T> T getStaticField(Class<?> t, String name) {
        try {
            Field f = t.getDeclaredField(name);
            f.setAccessible(true);
            //noinspection unchecked
            return (T) f.get(null);
        } catch (Throwable e) {
            Iris.reportError(e);
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Object getBiomeBase(Object registry, Biome biome) {
        Object v = baseBiomeCache.get(biome);

        if (v != null) {
            return v;
        }
        //noinspection unchecked
        v = org.bukkit.craftbukkit.v1_16_R3.block.CraftBlock.biomeToBiomeBase((IRegistry<BiomeBase>) registry, biome);
        if (v == null) {
            // Ok so there is this new biome name called "CUSTOM" in Paper's new releases.
            // But, this does NOT exist within CraftBukkit which makes it return an error.
            // So, we will just return the ID that the plains biome returns instead.
            //noinspection unchecked
            return org.bukkit.craftbukkit.v1_16_R3.block.CraftBlock.biomeToBiomeBase((IRegistry<BiomeBase>) registry, Biome.PLAINS);
        }
        baseBiomeCache.put(biome, v);
        return v;
    }

    @Override
    public int getBiomeId(Biome biome) {
        for (World i : Bukkit.getWorlds()) {
            if (i.getEnvironment().equals(World.Environment.NORMAL)) {
                IRegistry<BiomeBase> registry = ((CraftWorld) i).getHandle().r().b(IRegistry.ay);
                return registry.a((BiomeBase) getBiomeBase(registry, biome));
            }
        }

        return biome.ordinal();
    }

    private IdMap<BiomeBase> getBiomeMapping() {
        return biomeMapCache.aquire(() -> new IdMap<>() {
            @NotNull
            @Override
            public Iterator<BiomeBase> iterator() {
                return getCustomBiomeRegistry().iterator();
            }

            @Override
            public int getId(BiomeBase paramT) {
                return getCustomBiomeRegistry().a(paramT);
            }

            @Override
            public BiomeBase byId(int paramInt) {
                return getCustomBiomeRegistry().fromId(paramInt);
            }
        });
    }
    @Override
    public BiomeContainer newBiomeContainer(int min, int max) {
        ChunkBiomeContainer<BiomeBase> base = new ChunkBiomeContainer<>(getBiomeMapping(), min, max);
        return getBiomeContainerInterface(getBiomeMapping(), base);
    }

    @Override
    public BiomeContainer newBiomeContainer(int min, int max, int[] data) {
        ChunkBiomeContainer<BiomeBase> base = new ChunkBiomeContainer<>(getBiomeMapping(), min, max, data);
        return getBiomeContainerInterface(getBiomeMapping(), base);
    }

    @NotNull
    private BiomeContainer getBiomeContainerInterface(IdMap<BiomeBase> biomeMapping, ChunkBiomeContainer<BiomeBase> base) {
        return new BiomeContainer() {
            @Override
            public int[] getData() {
                return base.writeBiomes();
            }

            @Override
            public void setBiome(int x, int y, int z, int id) {
                base.setBiome(x, y, z, biomeMapping.byId(id));
            }

            @Override
            public int getBiome(int x, int y, int z) {
                return biomeMapping.getId(base.getBiome(x, y, z));
            }
        };
    }
    @Override
    public int countCustomBiomes() {
        AtomicInteger a = new AtomicInteger(0);
        getCustomBiomeRegistry().d().forEach((i) -> {
            MinecraftKey k = i.getKey().a();

            if (k.getNamespace().equals("minecraft")) {
                return;
            }

            a.incrementAndGet();
            Iris.debug("Custom Biome: " + k);
        });

        return a.get();
    }

    @Override
    public void forceBiomeInto(int x, int y, int z, Object somethingVeryDirty, ChunkGenerator.BiomeGrid chunk) {
        try {
            BiomeStorage s = (BiomeStorage) getFieldForBiomeStorage(chunk).get(chunk);
            s.setBiome(x, y, z, (BiomeBase) somethingVeryDirty);
        } catch (IllegalAccessException e) {
            Iris.reportError(e);
            e.printStackTrace();
        }
    }

    @Override
    public boolean isBukkit() {
        return true;
    }
}
