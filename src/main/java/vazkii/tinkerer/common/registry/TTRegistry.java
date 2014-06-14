package vazkii.tinkerer.common.registry;

import com.google.common.reflect.ClassPath;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import vazkii.tinkerer.client.lib.LibResources;
import vazkii.tinkerer.common.core.handler.ModCreativeTab;
import vazkii.tinkerer.common.research.IRegisterableResearch;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;

public class TTRegistry {

	private ArrayList<Class> itemClasses = new ArrayList<Class>();
	private HashMap<Class, ArrayList<Item>> itemRegistry = new HashMap<Class, ArrayList<Item>>();

	private ArrayList<Class> blockClasses = new ArrayList<Class>();
	private HashMap<Class, ArrayList<Block>> blockRegistry = new HashMap<Class, ArrayList<Block>>();

	public void registerClasses() {
		try {
			ClassPath classPath = ClassPath.from(this.getClass().getClassLoader());
			for (ClassPath.ClassInfo classInfo : classPath.getTopLevelClassesRecursive("vazkii.tinkerer.common.block")) {
				if (ITTinkererBlock.class.isAssignableFrom(classInfo.load()) && !Modifier.isAbstract(classInfo.load().getModifiers())) {
					blockClasses.add(classInfo.load());
				}
			}
			for (ClassPath.ClassInfo classInfo : classPath.getTopLevelClassesRecursive("vazkii.tinkerer.common.item")) {
				if (ITTinkererItem.class.isAssignableFrom(classInfo.load()) && !ItemBlock.class.isAssignableFrom(classInfo.load()) && !Modifier.isAbstract(classInfo.load().getModifiers())) {
					itemClasses.add(classInfo.load());
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void registerResearch(ITTinkererRegisterable nextItem) {
		IRegisterableResearch registerableResearch = nextItem.getResearchItem();
		if (registerableResearch != null) {
			registerableResearch.registerResearch();
		}
	}

	public void registerRecipe(ITTinkererRegisterable nextItem) {
		ThaumicTinkererRecipe thaumicTinkererRecipe = nextItem.getRecipeItem();
		if (thaumicTinkererRecipe != null) {
			thaumicTinkererRecipe.registerRecipe();
		}
	}

	public void preInit() {
		registerClasses();
		for (Class clazz : blockClasses) {
			try {
				Block newBlock = (Block) clazz.newInstance();
				if (((ITTinkererBlock) newBlock).shouldRegister()) {
					newBlock.setBlockName(((ITTinkererBlock) newBlock).getBlockName());
					ArrayList<Block> blockList = new ArrayList<Block>();
					blockList.add(newBlock);
					if (((ITTinkererBlock) newBlock).getItemBlock() != null) {
						Item newItem = ((ITTinkererBlock) newBlock).getItemBlock().getConstructor(Block.class).newInstance(newBlock);
						newItem.setUnlocalizedName(((ITTinkererItem) newItem).getItemName());
						ArrayList<Item> itemList = new ArrayList<Item>();
						itemList.add(newItem);
						itemRegistry.put(((ITTinkererBlock) newBlock).getItemBlock(), itemList);

					}
					if (newBlock == null) {
						System.out.println(clazz.getName() + " Returned a null block upon registration");
						continue;
					}

					if (((ITTinkererBlock) newBlock).getSpecialParameters() != null) {
						for (Object param : ((ITTinkererBlock) newBlock).getSpecialParameters()) {

							for (Constructor constructor : clazz.getConstructors()) {
								if (constructor.getParameterTypes().length > 0 && constructor.getParameterTypes()[0].isAssignableFrom(param.getClass())) {
									Block nextBlock = (Block) clazz.getConstructor(param.getClass()).newInstance(param);
									nextBlock.setBlockName(((ITTinkererBlock) nextBlock).getBlockName());
									blockList.add(nextBlock);
									break;
								}
							}
						}
					}
					blockRegistry.put(clazz, blockList);
				}
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		for (Class clazz : itemClasses) {
			try {
				Item newItem = (Item) clazz.newInstance();
				if (((ITTinkererItem) newItem).shouldRegister()) {
					newItem.setUnlocalizedName(((ITTinkererItem) newItem).getItemName());
					ArrayList<Item> itemList = new ArrayList<Item>();
					itemList.add(newItem);
					if (newItem == null) {
						System.out.println(clazz.getName() + " Returned a null item upon registration");
						continue;
					}
					if (((ITTinkererItem) newItem).getSpecialParameters() != null) {
						for (Object param : ((ITTinkererItem) newItem).getSpecialParameters()) {
							for (Constructor constructor : clazz.getConstructors()) {
								if (constructor.getParameterTypes().length > 0 && constructor.getParameterTypes()[0].isAssignableFrom(param.getClass())) {
									Item nextItem = (Item) constructor.newInstance(param);
									nextItem.setUnlocalizedName(((ITTinkererItem) nextItem).getItemName());
									itemList.add(nextItem);
									break;
								}
							}
						}
					}
					itemRegistry.put(clazz, itemList);
				}
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}

	}

	public ArrayList<Item> getItemFromClass(Class clazz) {
		return itemRegistry.get(clazz);
	}

	public Item getFirstItemFromClass(Class clazz) {
		return itemRegistry.get(clazz) != null ? itemRegistry.get(clazz).get(0) : null;
	}

	public ArrayList<Block> getBlockFromClass(Class clazz) {
		return blockRegistry.get(clazz);
	}

	public Block getFirstBlockFromClass(Class clazz) {
		return blockRegistry.get(clazz) != null ? blockRegistry.get(clazz).get(0) : null;
	}

	public void init() {

		for (ArrayList<Item> itemArrayList : itemRegistry.values()) {
			for (Item item : itemArrayList) {
				registerRecipe((ITTinkererRegisterable) item);
				ModCreativeTab.INSTANCE.addItem(item);
			}
		}

		for (ArrayList<Block> blockArrayList : blockRegistry.values()) {
			for (Block block : blockArrayList) {
				registerRecipe((ITTinkererRegisterable) block);

				if (((ITTinkererBlock) block).shouldDisplayInTab()) {
					ModCreativeTab.INSTANCE.addBlock(block);
				}
			}
		}
		for (ArrayList<Item> itemArrayList : itemRegistry.values()) {
			for (Item item : itemArrayList) {
				registerResearch((ITTinkererRegisterable) item);
			}
		}
		for (ArrayList<Block> blockArrayList : blockRegistry.values()) {
			for (Block block : blockArrayList) {
				registerResearch((ITTinkererRegisterable) block);
			}
		}
		for (ArrayList<Item> itemArrayList : itemRegistry.values()) {
			for (Item item : itemArrayList) {
				if (!(item instanceof ItemBlock)) {
					GameRegistry.registerItem(item, ((ITTinkererItem) item).getItemName());

					if (((ITTinkererItem) item).shouldDisplayInTab()) {
						ModCreativeTab.INSTANCE.addItem(item);
					}
				}
			}
		}
		for (ArrayList<Block> blockArrayList : blockRegistry.values()) {
			for (Block block : blockArrayList) {
				if (((ITTinkererBlock) block).getItemBlock() != null) {
					GameRegistry.registerBlock(block, ((ITTinkererBlock) block).getItemBlock(), ((ITTinkererBlock) block).getBlockName());
				} else {
					GameRegistry.registerBlock(block, ((ITTinkererBlock) block).getBlockName());
				}
				if (((ITTinkererBlock) block).getTileEntity() != null) {
					GameRegistry.registerTileEntity(((ITTinkererBlock) block).getTileEntity(), LibResources.PREFIX_MOD + ((ITTinkererBlock) block).getBlockName());
				}
				if (((ITTinkererBlock) block).shouldDisplayInTab()) {
					ModCreativeTab.INSTANCE.addBlock(block);
				}
			}
		}
	}

	public void postInit() {

	}

}