package com.tom.cpm.client;

import java.io.File;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.multiplayer.NetClientHandler;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.ResourceManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import com.tom.cpl.block.BiomeHandler;
import com.tom.cpl.gui.Frame;
import com.tom.cpl.gui.IGui;
import com.tom.cpl.gui.IKeybind;
import com.tom.cpl.render.RenderTypeBuilder;
import com.tom.cpl.tag.AllTagManagers;
import com.tom.cpl.util.AWTImageIO;
import com.tom.cpl.util.DynamicTexture.ITexture;
import com.tom.cpl.util.Image;
import com.tom.cpl.util.ImageIO.IImageIO;
import com.tom.cpm.common.BiomeHandlerImpl;
import com.tom.cpm.retro.GameProfile;
import com.tom.cpm.retro.GameProfileManager;
import com.tom.cpm.retro.MCExecutor;
import com.tom.cpm.shared.MinecraftClientAccess;
import com.tom.cpm.shared.MinecraftObjectHolder;
import com.tom.cpm.shared.definition.ModelDefinitionLoader;
import com.tom.cpm.shared.model.SkinType;
import com.tom.cpm.shared.network.NetHandler;
import com.tom.cpm.shared.retro.RetroGLAccess.RetroLayer;
import com.tom.cpm.shared.util.Log;
import com.tom.cpm.shared.util.MojangAPI;

public class MinecraftObject implements MinecraftClientAccess {
	private final Minecraft mc;
	private final PlayerRenderManager prm;
	private final AllTagManagers tags;
	private final ModelDefinitionLoader<GameProfile> loader;
	private final RenderTypeBuilder<ResourceLocation, RetroLayer> renderBuilder = RenderTypeBuilder.setupRetro(new RetroGL());

	public MinecraftObject(Minecraft mc) {
		this.mc = mc;
		MinecraftObjectHolder.setClientObject(this);
		loader = new ModelDefinitionLoader<>(PlayerProfile::new, GameProfile::getId, GameProfile::getName);
		prm = new PlayerRenderManager();
		tags = new AllTagManagers(mc.getResourceManager(), CPMTagLoader::new);
	}

	@Override
	public PlayerRenderManager getPlayerRenderManager() {
		return prm;
	}

	@Override
	public ITexture createTexture() {
		return new DynTexture(mc);
	}

	public static class DynTexture extends DynamicTexture implements ITexture {
		private final ResourceLocation loc;
		private final Minecraft mc;

		public DynTexture(Minecraft mc) {
			super(1, 1);
			loc = mc.getTextureManager().getDynamicTextureLocation("cpm", this);
			this.mc = mc;
		}

		private static ResourceLocation bound_loc;

		@Override
		public void bind() {
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, getGlTextureId());
			bound_loc = loc;
			if(mc.getTextureManager().getTexture(loc) == null)
				mc.getTextureManager().loadTexture(loc, this);
		}

		@Override
		public void load(Image image) {
			this.deleteGlTexture();
			TextureUtil.uploadTextureImageAllocate(this.getGlTextureId(), AWTImageIO.toBufferedImage(image), false, false);
		}

		@Override
		public void free() {
			this.deleteGlTexture();
		}

		@Override
		public void loadTexture(ResourceManager resourceManager) {}

		public static ResourceLocation getBoundLoc() {
			return bound_loc;
		}

		public void deleteGlTexture() {
			if (this.glTextureId != -1) {
				GL11.glDeleteTextures(this.glTextureId);
				this.glTextureId = -1;
			}
		}
	}

	@Override
	public void executeOnGameThread(Runnable r) {
		MCExecutor.addScheduledTask(r);
	}

	@Override
	public void executeNextFrame(Runnable r) {
		MCExecutor.tell(r);
	}

	@Override
	public ModelDefinitionLoader<GameProfile> getDefinitionLoader() {
		return loader;
	}

	@Override
	public SkinType getSkinType() {
		return SkinType.DEFAULT;
	}

	@Override
	public void setEncodedGesture(int value) {
		ClientProxy.INSTANCE.netHandler.sendLayer(value);
	}

	@Override
	public boolean isInGame() {
		return mc.thePlayer != null;
	}

	@Override
	public Object getPlayerIDObject() {
		return GameProfileManager.getProfile(mc.getSession().getUsername());
	}

	@Override
	public Object getCurrentPlayerIDObject() {
		return mc.thePlayer != null ? GameProfileManager.getProfile(mc.thePlayer.username) : null;
	}

	@Override
	public List<IKeybind> getKeybinds() {
		return KeyBindings.kbs;
	}

	@Override
	public ServerStatus getServerSideStatus() {
		return isInGame() ? getNetHandler().hasModClient() ? ServerStatus.INSTALLED : ServerStatus.UNAVAILABLE : ServerStatus.OFFLINE;
	}

	@Override
	public File getGameDir() {
		return mc.mcDataDir;
	}

	@Override
	public void openGui(Function<IGui, Frame> creator) {
		mc.displayGuiScreen(new GuiImpl(creator, mc.currentScreen));
	}

	@Override
	public Runnable openSingleplayer() {
		return () -> mc.displayGuiScreen(new GuiSelectWorld(mc.currentScreen));
	}

	@Override
	public NetHandler<?, ?, ?> getNetHandler() {
		return ClientProxy.INSTANCE.netHandler;
	}

	@Override
	public IImageIO getImageIO() {
		return new AWTImageIO();
	}

	@Override
	public MojangAPI getMojangAPI() {
		GameProfile gp = GameProfileManager.getProfile(mc.getSession().getUsername());
		return new MojangAPI(gp.getName(), gp.getId(), mc.getSession().getSessionID());
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void clearSkinCache() {
		GameProfileManager.clear();

		try {
			// Clear Ears cache
			Class<?> earsLegacy = Class.forName("com.unascribed.ears.legacy.LegacyHelper");
			Field f = earsLegacy.getDeclaredField("skinUrls");
			f.setAccessible(true);
			((Map) f.get(null)).clear();
			f = earsLegacy.getDeclaredField("cache");
			f.setAccessible(true);
			((Map) f.get(null)).clear();
			Log.info("Cleared Ears Cache");
		} catch (Throwable e) {
		}
	}

	@Override
	public String getConnectedServer() {
		if(mc.getNetHandler() == null)return null;
		SocketAddress sa = mc.getNetHandler().getNetManager().getSocketAddress();
		if(sa instanceof InetSocketAddress)
			return ((InetSocketAddress)sa).getHostString();
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Object> getPlayers() {
		if(mc.thePlayer == null)return Collections.emptyList();
		NetClientHandler net = this.mc.thePlayer.sendQueue;
		List<GuiPlayerInfo> plInfo = net.playerInfoList;
		List<EntityPlayer> players = mc.theWorld.playerEntities;
		return plInfo.stream().map(i -> players.stream().filter(e -> e.getCommandSenderName().equalsIgnoreCase(i.name)).
				findFirst().orElse(null)).filter(e -> e != null).map(e -> GameProfileManager.getProfile(e.username)).collect(Collectors.toList());
	}

	@Override
	public Proxy getProxy() {
		return mc.getProxy();
	}

	@Override
	public RenderTypeBuilder<?, ?> getRenderBuilder() {
		return renderBuilder;
	}

	@Override
	public AllTagManagers getBuiltinTags() {
		return tags;
	}

	@Override
	public BiomeHandler<?> getBiomeHandler() {
		return BiomeHandlerImpl.impl;
	}
}
