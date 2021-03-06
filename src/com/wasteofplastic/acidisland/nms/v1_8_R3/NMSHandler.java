package com.wasteofplastic.acidisland.nms.v1_8_R3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.IBlockData;
//import net.minecraft.server.v1_8_R3.EnumSkyBlock;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;
import net.minecraft.server.v1_8_R3.NBTTagString;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jnbt.CompoundTag;
import org.jnbt.ListTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;

import com.wasteofplastic.acidisland.nms.NMSAbstraction;

public class NMSHandler implements NMSAbstraction {

    @Override
    public void setBlockSuperFast(Block b, int blockId, byte data, boolean applyPhysics) {
	net.minecraft.server.v1_8_R3.World w = ((CraftWorld) b.getWorld()).getHandle();
        net.minecraft.server.v1_8_R3.Chunk chunk = w.getChunkAt(b.getX() >> 4, b.getZ() >> 4);
        BlockPosition bp = new BlockPosition(b.getX(), b.getY(), b.getZ());
        int combined = blockId + (data << 12);
        IBlockData ibd = net.minecraft.server.v1_8_R3.Block.getByCombinedId(combined);
        /*
        if (applyPhysics) {
            w.setTypeAndData(bp, ibd, 3); 
        } else {
            w.setTypeAndData(bp, ibd, 2); 
        }
        */
        chunk.a(bp, ibd);
        if (applyPhysics) {
            net.minecraft.server.v1_8_R3.Block block = chunk.getType(bp);
            w.update(bp, block);
        } 
        
        // Recalculate lighting
        //w.c(bp,true);
        //w.c(EnumSkyBlock.SKY, bp);
        //w.c(bp);
    }

    @Override
    public ItemStack setBook(Tag item) {
	ItemStack chestItem = new ItemStack(Material.WRITTEN_BOOK);
	//Bukkit.getLogger().info("item data");
	//Bukkit.getLogger().info(item.toString());
	
	Map<String,Tag> contents = (Map<String,Tag>) ((CompoundTag) item).getValue().get("tag").getValue();
	//BookMeta bookMeta = (BookMeta) chestItem.getItemMeta();
	String author = ((StringTag)contents.get("author")).getValue();
	//Bukkit.getLogger().info("Author: " + author);
	//bookMeta.setAuthor(author);
	String title = ((StringTag)contents.get("title")).getValue();
	//Bukkit.getLogger().info("Title: " + title);
	//bookMeta.setTitle(title);
	
	Map<String,Tag> display = (Map<String, Tag>) (contents.get("display")).getValue();
	List<Tag> loreTag = ((ListTag)display.get("Lore")).getValue();
	List<String> lore = new ArrayList<String>();
	for (Tag s: loreTag) {
	    lore.add(((StringTag)s).getValue());
	}
	//Bukkit.getLogger().info("Lore: " + lore);
	net.minecraft.server.v1_8_R3.ItemStack stack = CraftItemStack.asNMSCopy(chestItem); 
	// Pages
	NBTTagCompound tag = new NBTTagCompound(); //Create the NMS Stack's NBT (item data)
	tag.setString("title", title); //Set the book's title
	tag.setString("author", author);
	NBTTagList pages = new NBTTagList();
	List<Tag> pagesTag = ((ListTag)contents.get("pages")).getValue();
	for (Tag s: pagesTag) {
	    pages.add(new NBTTagString(((StringTag)s).getValue()));
	}
	tag.set("pages", pages); //Add the pages to the tag
	stack.setTag(tag); //Apply the tag to the item
	chestItem = CraftItemStack.asCraftMirror(stack); 
	ItemMeta bookMeta = (ItemMeta) chestItem.getItemMeta();
	bookMeta.setLore(lore);
	chestItem.setItemMeta(bookMeta);
	return chestItem;
	
    }

}
