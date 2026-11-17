package com.nomosiki.client;

import com.nomosiki.client.gui.SakuraButton;
import com.nomosiki.client.gui.SakuraEditBox;
import com.nomosiki.client.gui.SakuraStyle;
import com.nomosiki.network.squad.payload.s2c.SquadStatePayload;
import com.nomosiki.squad.SquadIcons;
import com.nomosiki.squad.SquadType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * Create or edit a squad name, icon, and branch (branch only when creating).
 * Merge / split squads: TODO (plan step 9 optional).
 */
public class SquadEditScreen extends Screen {
	private static final int PANEL_W = 200;
	private static final int PANEL_H = 170;
	private static final int ICON_COUNT = SquadIcons.IMAGES.size();

	private final Screen parent;
	private final @Nullable SquadStatePayload.Brief existing;
	private int guiLeft;
	private int guiTop;
	private EditBox nameBox;
	private byte iconIndex;
	private int typeOrdinal;

	public SquadEditScreen(Screen parent, @Nullable SquadStatePayload.Brief existing) {
		super(Component.translatable(existing == null ? "screen.nomosiki.squad_edit.title_add" : "screen.nomosiki.squad_edit.title_edit"));
		this.parent = parent;
		this.existing = existing;
		if (existing != null) {
			this.iconIndex = SquadIcons.clampIcon(existing.iconId());
			this.typeOrdinal = playableTypeIndex(SquadType.fromId(existing.typeId()));
		} else {
			this.iconIndex = 0;
			this.typeOrdinal = 0;
		}
	}

	@Override
	protected void init() {
		this.clearWidgets();
		this.guiLeft = (this.width - PANEL_W) / 2;
		this.guiTop = (this.height - PANEL_H) / 2;

		int x = this.guiLeft;
		int y = this.guiTop + 10;
		this.nameBox = new SakuraEditBox(this.font, x, y, PANEL_W, 18, Component.translatable("screen.nomosiki.squad_edit.name_label"));
		this.nameBox.setMaxLength(32);
		this.nameBox.setHint(Component.translatable("screen.nomosiki.squad_edit.name_placeholder"));
		if (this.existing != null && !this.existing.name().isEmpty()) {
			this.nameBox.setValue(SquadLabels.text(this.existing.name()));
		}
		this.addRenderableWidget(this.nameBox);
		y += 22;

		this.addRenderableWidget(new SakuraButton(x, y, 22, 20, Component.literal("◀"), b -> {
			this.iconIndex = (byte) Mth.positiveModulo(this.iconIndex - 1, ICON_COUNT);
		}).withTooltip(Tooltip.create(Component.translatable("screen.nomosiki.squad_edit.icon_prev"))));
		this.addRenderableWidget(new SakuraButton(x + PANEL_W - 22, y, 22, 20, Component.literal("▶"), b -> {
			this.iconIndex = (byte) Mth.positiveModulo(this.iconIndex + 1, ICON_COUNT);
		}).withTooltip(Tooltip.create(Component.translatable("screen.nomosiki.squad_edit.icon_next"))));
		y += 24;

		if (this.existing == null) {
			this.addRenderableWidget(new SakuraButton(x, y, PANEL_W, 20,
				Component.translatable("screen.nomosiki.squad_edit.type_next", Component.translatable(nextType().translationKey())),
				b -> {
					this.typeOrdinal = (this.typeOrdinal + 1) % SquadType.playableValues().size();
					b.setMessage(Component.translatable("screen.nomosiki.squad_edit.type_next", Component.translatable(nextType().translationKey())));
				}));
		} else {
			SakuraButton typeRo = new SakuraButton(x, y, PANEL_W, 20,
				Component.translatable("screen.nomosiki.squad_edit.type_locked", Component.translatable(nextType().translationKey())),
				b -> {
				});
			typeRo.active = false;
			this.addRenderableWidget(typeRo);
		}
		y += 24;

		int half = (PANEL_W - 4) / 2;
		this.addRenderableWidget(new SakuraButton(x, y, half, 20,
			Component.translatable("screen.nomosiki.squad_edit.button_save"), b -> this.save()));
		this.addRenderableWidget(new SakuraButton(x + half + 4, y, half, 20,
			Component.translatable("screen.nomosiki.squad_edit.button_cancel"), b -> this.returnToParent()));
		y += 24;

		if (this.existing != null) {
			this.addRenderableWidget(new SakuraButton(x, y, PANEL_W, 20,
				Component.translatable("screen.nomosiki.squad_edit.button_disband"), b -> this.openDisbandConfirm())
				.withTooltip(Tooltip.create(Component.translatable("tooltip.nomosiki.squad_edit.disband"))));
			y += 22;
			this.addRenderableWidget(new SakuraButton(x, y, PANEL_W, 20,
				Component.translatable("screen.nomosiki.squad_edit.button_assign_nearby"), b -> {
					SquadCommandClient.assignNearby(this.existing.squadId(), 24);
				}).withTooltip(Tooltip.create(Component.translatable("tooltip.nomosiki.squad_edit.assign_nearby"))));
		}
	}

	private void returnToParent() {
		if (this.minecraft != null) {
			this.minecraft.setScreen(this.parent);
		}
	}

	private void openDisbandConfirm() {
		if (this.minecraft == null || this.existing == null) {
			return;
		}
		this.minecraft.setScreen(new ConfirmScreen(
			accepted -> {
				if (accepted) {
					SquadCommandClient.disbandSquad(this.existing.squadId());
					this.minecraft.setScreen(this.parent);
				} else {
					this.minecraft.setScreen(new SquadEditScreen(this.parent, this.existing));
				}
			},
			Component.translatable("screen.nomosiki.squad_edit.confirm_disband_title"),
			Component.translatable("screen.nomosiki.squad_edit.confirm_disband_text"),
			Component.translatable("screen.nomosiki.squad_edit.confirm_disband_yes"),
			Component.translatable("screen.nomosiki.squad_edit.confirm_disband_no")
		));
	}

	private static int playableTypeIndex(SquadType type) {
		int idx = SquadType.playableValues().indexOf(type);
		return idx >= 0 ? idx : 0;
	}

	private SquadType nextType() {
		var playable = SquadType.playableValues();
		return playable.get(Mth.clamp(this.typeOrdinal, 0, playable.size() - 1));
	}

	private void save() {
		String name = this.nameBox.getValue();
		if (this.existing == null) {
			SquadCommandClient.createSquad(name, this.iconIndex, nextType());
		} else {
			SquadCommandClient.editSquad(this.existing.squadId(), name, this.iconIndex);
		}
		this.returnToParent();
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		SakuraStyle.drawDim(graphics, this.width, this.height);
		SakuraStyle.drawPaperPanel(graphics, this.guiLeft - 8, this.guiTop - 24, PANEL_W + 16, PANEL_H + 32);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		SakuraStyle.drawCentered(graphics, this.font, this.title, this.width / 2, this.guiTop - 18, SakuraStyle.TITLE);
		int nameY = this.guiTop + 2;
		SakuraStyle.drawLabel(graphics, this.font, Component.translatable("screen.nomosiki.squad_edit.name_label"),
			this.guiLeft, nameY, SakuraStyle.INK_MUTED);
		int iconRowY = this.guiTop + 32;
		SakuraStyle.drawLabel(graphics, this.font, Component.translatable("screen.nomosiki.squad_edit.icon_label"),
			this.guiLeft, iconRowY - 11, SakuraStyle.INK_MUTED);
		int cx = this.guiLeft + PANEL_W / 2;
		graphics.blit(SquadIcons.byId(this.iconIndex), cx - 8, iconRowY, 0, 0, 16, 16, 16, 16);
		int typeRowY = this.guiTop + 56;
		SakuraStyle.drawLabel(graphics, this.font, Component.translatable("screen.nomosiki.squad_edit.type_label"),
			this.guiLeft, typeRowY - 11, SakuraStyle.INK_MUTED);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
