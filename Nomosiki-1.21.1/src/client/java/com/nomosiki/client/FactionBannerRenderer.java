package com.nomosiki.client;

import com.mojang.blaze3d.platform.Lighting;
import com.nomosiki.faction.FactionBannerHelper;
import com.nomosiki.faction.FactionColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.jetbrains.annotations.Nullable;

public final class FactionBannerRenderer {
	private ItemStack bannerStack = ItemStack.EMPTY;
	private BannerPatternLayers patterns = BannerPatternLayers.EMPTY;
	private DyeColor baseColor = DyeColor.WHITE;
	@Nullable
	private ModelPart flag;

	public void setBannerStack(ItemStack stack) {
		this.setBannerStack(stack, "");
	}

	public void setBannerStack(ItemStack stack, String colorId) {
		if (stack != null && !stack.isEmpty() && stack.getItem() instanceof BannerItem && FactionBannerHelper.hasPatterns(stack)) {
			this.applyStack(stack);
			return;
		}
		if (colorId != null && !colorId.isBlank()) {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.level != null) {
				this.applyStack(FactionBannerHelper.createFallbackBannerStack(
					FactionColor.fromId(colorId),
					minecraft.level.registryAccess()
				));
				return;
			}
		}
		this.applyStack(ItemStack.EMPTY);
	}

	private void applyStack(ItemStack stack) {
		this.bannerStack = stack.copy();
		if (this.bannerStack.isEmpty() || !(this.bannerStack.getItem() instanceof BannerItem bannerItem)) {
			this.patterns = BannerPatternLayers.EMPTY;
			this.baseColor = DyeColor.WHITE;
			return;
		}
		this.baseColor = bannerItem.getColor();
		this.patterns = this.bannerStack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
	}

	public boolean hasBanner() {
		return !this.bannerStack.isEmpty() && this.bannerStack.getItem() instanceof BannerItem;
	}

	public void render(GuiGraphics graphics, int left, int top, float scale) {
		if (!this.hasBanner()) {
			return;
		}
		ModelPart flagPart = this.flagPart();
		if (flagPart == null) {
			return;
		}
		graphics.flush();
		Lighting.setupForFlatItems();
		graphics.pose().pushPose();
		try {
			graphics.pose().translate(left + 8.0F, top + 18.0F, 200.0F);
			graphics.pose().scale(scale * 10.0F, -scale * 10.0F, 1.0F);
			float modelScale = 0.6666667F;
			graphics.pose().scale(modelScale, -modelScale, -modelScale);
			flagPart.xRot = 0.0F;
			flagPart.y = -32.0F;
			MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
			net.minecraft.client.renderer.blockentity.BannerRenderer.renderPatterns(
				graphics.pose(),
				bufferSource,
				15728880,
				OverlayTexture.NO_OVERLAY,
				flagPart,
				ModelBakery.BANNER_BASE,
				true,
				this.baseColor,
				this.patterns
			);
			bufferSource.endBatch();
		} finally {
			graphics.pose().popPose();
			Lighting.setupFor3DItems();
		}
	}

	@Nullable
	private ModelPart flagPart() {
		if (this.flag == null) {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.getEntityModels() == null) {
				return null;
			}
			this.flag = minecraft.getEntityModels()
				.bakeLayer(ModelLayers.BANNER)
				.getChild("flag");
		}
		return this.flag;
	}
}
