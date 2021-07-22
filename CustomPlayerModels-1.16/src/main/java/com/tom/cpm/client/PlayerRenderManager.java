package com.tom.cpm.client;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.ElytraModel;
import net.minecraft.client.renderer.entity.model.HumanoidHeadModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.model.Model;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.util.ResourceLocation;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import com.tom.cpl.math.Vec4f;
import com.tom.cpl.render.VBuffers;
import com.tom.cpl.render.VBuffers.NativeRenderType;
import com.tom.cpm.client.MinecraftObject.DynTexture;
import com.tom.cpm.client.optifine.OptifineTexture;
import com.tom.cpm.client.optifine.RedirectRendererOF;
import com.tom.cpm.shared.definition.ModelDefinitionLoader;
import com.tom.cpm.shared.model.PlayerModelParts;
import com.tom.cpm.shared.model.RootModelType;
import com.tom.cpm.shared.model.TextureSheetType;
import com.tom.cpm.shared.model.render.ModelRenderManager;
import com.tom.cpm.shared.model.render.RenderMode;
import com.tom.cpm.shared.model.render.VanillaModelPart;
import com.tom.cpm.shared.skin.TextureProvider;

public class PlayerRenderManager extends ModelRenderManager<IRenderTypeBuffer, CallbackInfoReturnable<ResourceLocation>, ModelRenderer, Model> {

	public PlayerRenderManager(ModelDefinitionLoader loader) {
		super(loader);
		setFactory(new RedirectHolderFactory<IRenderTypeBuffer, CallbackInfoReturnable<ResourceLocation>, ModelRenderer>() {

			@SuppressWarnings("unchecked")
			@Override
			public <M> RedirectHolder<?, IRenderTypeBuffer, CallbackInfoReturnable<ResourceLocation>, ModelRenderer> create(
					M model, String arg) {
				if(model instanceof PlayerModel) {
					return new RedirectHolderPlayer(PlayerRenderManager.this, (PlayerModel<AbstractClientPlayerEntity>) model);
				} else if(model instanceof HumanoidHeadModel) {
					return new RedirectHolderSkull(PlayerRenderManager.this, (HumanoidHeadModel) model);
				} else if(model instanceof ElytraModel) {
					return new RedirectHolderElytra(PlayerRenderManager.this, (ElytraModel<AbstractClientPlayerEntity>) model);
				} else if(model instanceof BipedModel && "armor1".equals(arg)) {
					return new RedirectHolderArmor1(PlayerRenderManager.this, (BipedModel<AbstractClientPlayerEntity>) model);
				} else if(model instanceof BipedModel && "armor2".equals(arg)) {
					return new RedirectHolderArmor2(PlayerRenderManager.this, (BipedModel<AbstractClientPlayerEntity>) model);
				}
				return null;
			}
		});
		setRedirectFactory(new RedirectRendererFactory<Model, CallbackInfoReturnable<ResourceLocation>, ModelRenderer>() {

			@Override
			public RedirectRenderer<ModelRenderer> create(Model model,
					RedirectHolder<Model, ?, CallbackInfoReturnable<ResourceLocation>, ModelRenderer> access,
					Supplier<ModelRenderer> modelPart, VanillaModelPart part) {
				return ClientProxy.optifineLoaded ?
						new RedirectRendererOF((RDH) access, modelPart, part) :
							new RedirectModelRendererVanilla((RDH) access, modelPart, part);
			}
		});
		setVis(m -> m.visible, (m, v) -> m.visible = v);
		setModelPosGetters(m -> m.x, m -> m.y, m -> m.z);
		setModelRotGetters(m -> m.xRot, m -> m.yRot, m -> m.zRot);
		setModelSetters((m, x, y, z) -> {
			m.x = x;
			m.y = y;
			m.z = z;
		}, (m, x, y, z) -> {
			m.xRot = x;
			m.yRot = y;
			m.zRot = z;
		});
	}

	public static abstract class RDH extends ModelRenderManager.RedirectHolder<Model, IRenderTypeBuffer, CallbackInfoReturnable<ResourceLocation>, ModelRenderer> {
		public ResourceLocation boundSkin;

		public RDH(
				ModelRenderManager<IRenderTypeBuffer, CallbackInfoReturnable<ResourceLocation>, ModelRenderer, Model> mngr,
				Model model) {
			super(mngr, model);
		}

		@Override
		public void setupRenderSystem(CallbackInfoReturnable<ResourceLocation> cbi, TextureSheetType tex) {
			boundSkin = cbi.getReturnValue();
			renderTypes.put(RenderMode.NORMAL, new NativeRenderType(RenderType.entityTranslucent(boundSkin), 0));
			renderTypes.put(RenderMode.GLOW, new NativeRenderType(RenderType.eyes(boundSkin), 1));
			renderTypes.put(RenderMode.OUTLINE, new NativeRenderType(CustomRenderTypes.getLinesNoDepth(), 2));
			renderTypes.put(RenderMode.COLOR, new NativeRenderType(CustomRenderTypes.getEntityColorTranslucentCull(), 0));
		}

		@Override
		protected void bindTexture(CallbackInfoReturnable<ResourceLocation> cbi, TextureProvider skin) {
			skin.bind();
			OptifineTexture.applyOptifineTexture(cbi.getReturnValue(), skin);
			cbi.setReturnValue(DynTexture.getBoundLoc());
		}

		@Override
		public void swapOut0() {
			this.boundSkin = null;
		}

		@Override
		public void swapIn0() {}
	}

	private static class RedirectHolderPlayer extends RDH {
		private RedirectRenderer<ModelRenderer> head;
		private RedirectRenderer<ModelRenderer> leftArm;
		private RedirectRenderer<ModelRenderer> rightArm;
		private RedirectRenderer<ModelRenderer> leftArmwear;
		private RedirectRenderer<ModelRenderer> rightArmwear;

		public RedirectHolderPlayer(PlayerRenderManager mngr, PlayerModel<AbstractClientPlayerEntity> model) {
			super(mngr, model);
			head = register(new Field<>(    () -> model.head    , v -> model.head     = v, PlayerModelParts.HEAD), p -> !((PlayerProfile)p).hasPlayerHead);
			register(new Field<>(           () -> model.body    , v -> model.body     = v, PlayerModelParts.BODY));
			rightArm = register(new Field<>(() -> model.rightArm, v -> model.rightArm = v, PlayerModelParts.RIGHT_ARM));
			leftArm = register(new Field<>( () -> model.leftArm , v -> model.leftArm  = v, PlayerModelParts.LEFT_ARM));
			register(new Field<>(           () -> model.rightLeg, v -> model.rightLeg = v, PlayerModelParts.RIGHT_LEG));
			register(new Field<>(           () -> model.leftLeg , v -> model.leftLeg  = v, PlayerModelParts.LEFT_LEG));

			register(new Field<>(               () -> model.hat        , v -> model.hat         = v, null)).setCopyFrom(head);
			leftArmwear = register(new Field<>( () -> model.leftSleeve , v -> model.leftSleeve  = v, null));
			rightArmwear = register(new Field<>(() -> model.rightSleeve, v -> model.rightSleeve = v, null));
			register(new Field<>(               () -> model.leftPants  , v -> model.leftPants   = v, null));
			register(new Field<>(               () -> model.rightPants , v -> model.rightPants  = v, null));
			register(new Field<>(               () -> model.jacket     , v -> model.jacket      = v, null));

			register(new Field<>(() -> model.cloak        , v -> model.cloak     = v, RootModelType.CAPE));
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean skipTransform(RedirectRenderer<ModelRenderer> part) {
			PlayerModel<AbstractClientPlayerEntity> model = (PlayerModel<AbstractClientPlayerEntity>) this.model;
			boolean skipTransform = false;
			if(leftArm == part && model.leftArmPose.ordinal() > 2) {
				skipTransform = true;
			}
			if(rightArm == part && model.rightArmPose.ordinal() > 2) {
				skipTransform = true;
			}
			return skipTransform;
		}
	}

	private static class RedirectHolderSkull extends RDH {
		private RedirectRenderer<ModelRenderer> hat;

		public RedirectHolderSkull(PlayerRenderManager mngr, HumanoidHeadModel model) {
			super(mngr, model);

			register(new Field<>(      () -> model.head, v -> model.head = v, PlayerModelParts.HEAD));
			hat = register(new Field<>(() -> model.hat , v -> model.hat  = v, null));
		}

	}

	private static class RedirectHolderElytra extends RDH {

		public RedirectHolderElytra(PlayerRenderManager mngr, ElytraModel<AbstractClientPlayerEntity> model) {
			super(mngr, model);

			register(new Field<>(() -> model.rightWing, v -> model.rightWing = v, RootModelType.ELYTRA_RIGHT));
			register(new Field<>(() -> model.leftWing,  v -> model.leftWing  = v, RootModelType.ELYTRA_LEFT));
		}

	}

	private static class RedirectHolderArmor1 extends RDH {

		public RedirectHolderArmor1(PlayerRenderManager mngr, BipedModel<AbstractClientPlayerEntity> model) {
			super(mngr, model);

			register(new Field<>(() -> model.head,     v -> model.head     = v, RootModelType.ARMOR_HELMET));
			register(new Field<>(() -> model.body,     v -> model.body     = v, RootModelType.ARMOR_BODY));
			register(new Field<>(() -> model.rightArm, v -> model.rightArm = v, RootModelType.ARMOR_RIGHT_ARM));
			register(new Field<>(() -> model.leftArm,  v -> model.leftArm  = v, RootModelType.ARMOR_LEFT_ARM));
			register(new Field<>(() -> model.rightLeg, v -> model.rightLeg = v, RootModelType.ARMOR_RIGHT_FOOT));
			register(new Field<>(() -> model.leftLeg,  v -> model.leftLeg  = v, RootModelType.ARMOR_LEFT_FOOT));
		}

	}

	private static class RedirectHolderArmor2 extends RDH {

		public RedirectHolderArmor2(PlayerRenderManager mngr, BipedModel<AbstractClientPlayerEntity> model) {
			super(mngr, model);

			register(new Field<>(() -> model.body,     v -> model.body     = v, RootModelType.ARMOR_LEGGINGS_BODY));
			register(new Field<>(() -> model.rightLeg, v -> model.rightLeg = v, RootModelType.ARMOR_RIGHT_LEG));
			register(new Field<>(() -> model.leftLeg,  v -> model.leftLeg  = v, RootModelType.ARMOR_LEFT_LEG));
		}

	}

	public static abstract class RedirectModelRendererBase extends ModelRenderer implements RedirectRenderer<ModelRenderer> {
		protected final RDH holder;
		protected final VanillaModelPart part;
		protected final Supplier<ModelRenderer> parentProvider;
		protected ModelRenderer parent;
		protected VBuffers buffers;

		public RedirectModelRendererBase(RDH holder, Supplier<ModelRenderer> parent, VanillaModelPart part) {
			super(0, 0, 0, 0);
			this.part = part;
			this.holder = holder;
			this.parentProvider = parent;
		}

		@Override
		public VBuffers getVBuffers() {
			return buffers;
		}

		@Override
		public ModelRenderer swapIn() {
			if(parent != null)return this;
			parent = parentProvider.get();
			holder.copyModel(parent, this);
			return this;
		}

		@Override
		public ModelRenderer swapOut() {
			if(parent == null)return parentProvider.get();
			ModelRenderer p = parent;
			parent = null;
			return p;
		}

		@Override
		public RedirectHolder<?, ?, ?, ModelRenderer> getHolder() {
			return holder;
		}

		@Override
		public ModelRenderer getParent() {
			return parent;
		}

		@Override
		public VanillaModelPart getPart() {
			return part;
		}

		protected float red, green, blue, alpha;

		@Override
		public Vec4f getColor() {
			return new Vec4f(red, green, blue, alpha);
		}
	}

	private static class RedirectModelRendererVanilla extends RedirectModelRendererBase {

		public RedirectModelRendererVanilla(RDH holder, Supplier<ModelRenderer> parent, VanillaModelPart part) {
			super(holder, parent, part);
		}

		private MatrixStack matrixStackIn;
		private IVertexBuilder bufferIn;
		private int packedLightIn, packedOverlayIn;

		@Override
		public void render(MatrixStack matrixStackIn, IVertexBuilder bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {
			if(!holder.renderTypes.isInitialized()) {
				holder.copyModel(this, parent);
				parent.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
				holder.logWarning();
				return;
			}
			this.matrixStackIn   = matrixStackIn  ;
			this.bufferIn        = bufferIn       ;
			this.packedLightIn   = packedLightIn  ;
			this.packedOverlayIn = packedOverlayIn;
			this.red             = red            ;
			this.green           = green          ;
			this.blue            = blue           ;
			this.alpha           = alpha          ;
			this.buffers = new VBuffers(rt -> new VBuffer(holder.addDt.getBuffer(rt.getNativeType()), packedLightIn, packedOverlayIn, matrixStackIn));
			render();
			holder.addDt.getBuffer(holder.renderTypes.get(RenderMode.NORMAL).getNativeType());
			this.matrixStackIn = null;
			this.bufferIn = null;
		}

		@Override
		public void renderParent() {
			parent.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
		}
	}
}
