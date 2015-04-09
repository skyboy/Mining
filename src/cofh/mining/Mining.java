package cofh.mining;

import cofh.core.CoFHProps;
import cofh.core.world.FeatureParser;
import cofh.lib.util.RegistryUtils;
import cofh.lib.util.WeightedRandomItemStack;
import cofh.mod.BaseMod;
import cofh.mod.updater.UpdateManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.CustomProperty;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.init.Blocks;

@Mod(modid = Mining.modId, name = Mining.modName, version = Mining.version, dependencies = Mining.dependencies,
		customProperties = @CustomProperty(k = "cofhversion", v = "true"))
public class Mining extends BaseMod {

	public static final String modId = "CoFHMining";
	public static final String modName = "Mining";
	public static final String version = "1.7.10R0.0.1B1";
	public static final String dependencies = CoFHProps.DEPENDENCIES +
			"required-after:CoFHCore@[" + CoFHProps.VERSION + ",);before:NetherOres";

	@EventHandler
	public void preinit(FMLPreInitializationEvent evt) {

		setConfigFolderBase(evt.getModConfigurationDirectory());
		if (!_configFolder.exists())
			_configFolder.mkdirs();
	}

	private static void addFiles(ArrayList<File> list, File folder) {

		File[] fList = folder.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File file, String name) {

				if (name == null) {
					return false;
				}
				return name.toLowerCase().endsWith(".json") || new File(file, name).isDirectory();
			}
		});

		list.addAll(Arrays.asList(fList));
	}

	@EventHandler
	public void loadComplete(FMLLoadCompleteEvent evt) {

		JsonParser parser = new JsonParser();

		ArrayList<File> worldGenList = new ArrayList<File>(5);
		addFiles(worldGenList, _configFolder);

		for (int q = 0; q < worldGenList.size(); ++q) {

			File genFile = worldGenList.get(q);
			if (genFile.isDirectory()) {
				addFiles(worldGenList, genFile);
				continue;
			}

			JsonObject genList;
			try {
				genList = (JsonObject) parser.parse(new FileReader(genFile));
			} catch (Throwable t) {
				_log.error("Critical error reading from a file: " + genFile + " > Please be sure the file is correct!", t);
				continue;
			}

			_log.info("Reading mining info from: " + genFile.getName());
			for (Entry<String, JsonElement> genEntry : genList.entrySet()) {
				try {
					JsonObject obj = genEntry.getValue().getAsJsonObject();
					String name = genEntry.getKey();
					Block ore = Block.getBlockFromName(name);
					if (ore == null) {
						_log.debug(name + " is not a block.");
						continue;
					}
					if (isBlockInvalid(ore)) {
						_log.error(name + " cannot be replaced.");
						continue;
					}
					_log.debug("Parsing " + name);
					if (obj.has("enabled") && !obj.get("enabled").getAsBoolean())
						continue;

					WeightedRandomItemStack[][] drops = new WeightedRandomItemStack[16][];
					if (obj.has("drops")) {
						JsonObject data = obj.get("drops").getAsJsonObject();
						for (int i = 0; i < 16; ++i) {
							if (!data.has(String.valueOf(i)))
								continue;
							ArrayList<WeightedRandomItemStack> list = new ArrayList<WeightedRandomItemStack>();
							if (!FeatureParser.parseWeightedItemList(data.get(String.valueOf(i)), list)) {
								_log.error("Failed to parse drops for " + name + "#" + i);
							}
							if (list.size() > 0)
								drops[i] = list.toArray(new WeightedRandomItemStack[list.size()]);
						}
					}

					int[] u = new int[16], v = new int[16];
					if (obj.has("uses")) {
						JsonElement ele = obj.get("uses");
						if (ele.isJsonPrimitive()) {
							for (int i = u.length, d = ele.getAsInt(); i-- > 0;) {
								u[i] = d;
							}
						} else {
							JsonObject d = ele.getAsJsonObject();
							for (int i = u.length; i-- > 0;) {
								if (d.has(String.valueOf(i)))
									u[i] = d.get(String.valueOf(i)).getAsInt();
							}
						}
					}
					if (obj.has("variability")) {
						JsonElement ele = obj.get("variability");
						if (ele.isJsonPrimitive()) {
							for (int i = v.length, d = ele.getAsInt(); i-- > 0;) {
								v[i] = d;
							}
						} else {
							JsonObject d = ele.getAsJsonObject();
							for (int i = v.length; i-- > 0;) {
								if (d.has(String.valueOf(i)))
									v[i] = d.get(String.valueOf(i)).getAsInt();
							}
						}
					}

					RegistryUtils.overwriteEntry(Block.blockRegistry, name, new BlockOverrideOre(ore, drops, u, v));

				} catch (Throwable t) {
					_log.fatal("There was a severe error parsing '" + genEntry.getKey() + "'!", t);
				}
			}
		}

		_log.info("Load Complete.");
		UpdateManager.registerUpdater(new UpdateManager(this, "https://raw.github.com/skyboy/Mining/master/VERSION",
				CoFHProps.DOWNLOAD_URL));
	}

	private boolean isBlockInvalid(Block block) {

		if (block == Blocks.air || block instanceof ITileEntityProvider)
			return true;
		for (int i = 16; i-- > 0;) {
			try {
				if (block.hasTileEntity(i)) {
					return true;
				}
			} catch (Throwable _) {
			}
		}
		return false;
	}

	@Override
	public String getModId() {

		return modId;
	}

	@Override
	public String getModName() {

		return modName;
	}

	@Override
	public String getModVersion() {

		return version;
	}

}
