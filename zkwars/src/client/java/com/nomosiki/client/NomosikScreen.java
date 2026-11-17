package com.nomosiki.client;

import com.nomosiki.client.gui.SakuraButton;
import com.nomosiki.client.gui.SakuraEditBox;
import com.nomosiki.client.gui.SakuraStyle;
import com.nomosiki.config.NomosikiConfig;
import com.nomosiki.entity.SquadOrder;
import com.nomosiki.entity.NomosikCombatMode;
import com.nomosiki.entity.NomosikEntity;
import com.nomosiki.network.unit.payload.c2s.NomosikRecruitActionPayload;
import com.nomosiki.network.unit.payload.c2s.NomosikRenamePayload;
import com.nomosiki.screen.NomosikScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.core.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class NomosikScreen extends AbstractContainerScreen<NomosikScreenHandler> {
	private static final int TAB_EQUIPMENT = 0;
	private static final int TAB_BEHAVIOR = 1;

	private int tab = TAB_EQUIPMENT;
	private EditBox renameBox;
	private Button applyNameButton;
	private final List<Button> behaviorButtons = new ArrayList<>();

	public NomosikScreen(NomosikScreenHandler handler, Inventory inventory, Component title) {
		super(handler, inventory, title);
		this.imageWidth = 276;
		this.imageHeight = 238;
		this.titleLabelX = 8;
		this.titleLabelY = 8;
		this.inventoryLabelX = NomosikScreenHandler.PLAYER_LEFT;
		this.inventoryLabelY = NomosikScreenHandler.PLAYER_TOP - 10;
	}

	@Override
	protected void init() {
		super.init();
		int maxTop = Math.max(2, this.height - this.imageHeight - 4);
		if (this.topPos > maxTop) {
			this.topPos = maxTop;
		}
		if (this.topPos < 2) {
			this.topPos = 2;
		}

		int nomosikId = this.menu.getNomosik().getId();
		this.renameBox = new SakuraEditBox(this.font, this.leftPos + 8, this.topPos + 108, 128, 18,
			Component.translatable("screen.nomosiki.nomosik.rename_box"));
		this.renameBox.setMaxLength(32);
		this.renameBox.setHint(Component.translatable("screen.nomosiki.nomosik.rename_hint"));
		this.renameBox.setValue(this.menu.getNomosik().hasCustomName() ? this.menu.getNomosik().getCustomName().getString() : "");
		this.addRenderableWidget(this.renameBox);

		this.applyNameButton = this.addRenderableWidget(new SakuraButton(this.leftPos + 140, this.topPos + 108, 64, 18,
			fitButtonLabel("screen.nomosiki.nomosik.rename_apply", 60), b -> this.applyRename(nomosikId)));

		this.addRenderableWidget(new SakuraButton(this.leftPos + 128, this.topPos + 6, 70, 18,
			fitButtonLabel("screen.nomosiki.nomosik.tab_equipment", 66), b -> this.setTab(TAB_EQUIPMENT)));
		this.addRenderableWidget(new SakuraButton(this.leftPos + 202, this.topPos + 6, 66, 18,
			fitButtonLabel("screen.nomosiki.nomosik.tab_behavior", 62), b -> this.setTab(TAB_BEHAVIOR)));

		int bx0 = this.leftPos + 8;
		int colGap = 4;
		int btnW = (this.imageWidth - 16 - colGap) / 2;
		int bh = 16;
		int gap = 2;
		int by = this.topPos + 122;
		this.behaviorButtons.clear();
		this.behaviorButtons.add(orderButton(bx0, by, btnW, bh, "button.nomosiki.squad_follow",
			"tooltip.nomosiki.squad_follow",
			() -> SquadCommandClient.send(SquadOrder.FOLLOW, BlockPos.ZERO, radiusForOrders(), List.of(nomosikId), List.of())));
		this.behaviorButtons.add(orderButton(bx0 + btnW + colGap, by, btnW, bh, "button.nomosiki.squad_hold",
			"tooltip.nomosiki.squad_hold",
			() -> {
				BlockPos anchor = anchorOrWarn();
				if (anchor != null) {
					SquadCommandClient.send(SquadOrder.HOLD, anchor, radiusForOrders(), List.of(nomosikId), List.of());
				}
			}));
		by += bh + gap;
		this.behaviorButtons.add(orderButton(bx0, by, btnW, bh, "button.nomosiki.squad_move",
			"tooltip.nomosiki.squad_move",
			() -> {
				BlockPos anchor = anchorOrWarn();
				if (anchor != null) {
					SquadCommandClient.send(SquadOrder.MOVE_TO, anchor, radiusForOrders(), List.of(nomosikId), List.of());
				}
			}));
		this.behaviorButtons.add(orderButton(bx0 + btnW + colGap, by, btnW, bh, "button.nomosiki.squad_attack_zone",
			"tooltip.nomosiki.squad_attack_zone",
			() -> {
				BlockPos anchor = anchorOrWarn();
				if (anchor != null) {
					SquadCommandClient.send(SquadOrder.ATTACK_ZONE, anchor, radiusForOrders(), List.of(nomosikId), List.of());
				}
			}));
		by += bh + gap;
		int cmW = (this.imageWidth - 16 - 3 * colGap) / 4;
		this.behaviorButtons.add(sakuraButton(bx0, by, cmW, bh, "button.nomosiki.combat_mode.passive",
			"tooltip.nomosiki.combat_mode.passive",
			b -> SquadCommandClient.sendCombatMode(NomosikCombatMode.PASSIVE, List.of(nomosikId), Optional.empty())));
		this.behaviorButtons.add(sakuraButton(bx0 + cmW + colGap, by, cmW, bh, "button.nomosiki.combat_mode.neutral",
			"tooltip.nomosiki.combat_mode.neutral",
			b -> SquadCommandClient.sendCombatMode(NomosikCombatMode.NEUTRAL, List.of(nomosikId), Optional.empty())));
		this.behaviorButtons.add(sakuraButton(bx0 + 2 * (cmW + colGap), by, cmW, bh, "button.nomosiki.combat_mode.aggressive",
			"tooltip.nomosiki.combat_mode.aggressive",
			b -> SquadCommandClient.sendCombatMode(NomosikCombatMode.AGGRESSIVE, List.of(nomosikId), Optional.empty())));
		this.behaviorButtons.add(sakuraButton(bx0 + 3 * (cmW + colGap), by, cmW, bh, "button.nomosiki.combat_mode.raid",
			"tooltip.nomosiki.combat_mode.raid",
			b -> SquadCommandClient.sendCombatMode(NomosikCombatMode.RAID, List.of(nomosikId), Optional.empty())));
		by += bh + gap;
		this.behaviorButtons.add(sakuraButton(bx0, by, btnW, bh, "screen.nomosiki.nomosik.toggle_follow",
			"tooltip.nomosiki.nomosik.toggle_follow",
			b -> ClientPlayNetworking.send(new NomosikRecruitActionPayload(nomosikId, NomosikRecruitActionPayload.ACTION_TOGGLE_FOLLOW))));
		this.behaviorButtons.add(sakuraButton(bx0 + btnW + colGap, by, btnW, bh, "screen.nomosiki.nomosik.clear_target",
			"tooltip.nomosiki.nomosik.clear_target",
			b -> ClientPlayNetworking.send(new NomosikRecruitActionPayload(nomosikId, NomosikRecruitActionPayload.ACTION_CLEAR_TARGET))));
		by += bh + gap;
		this.behaviorButtons.add(sakuraButton(bx0, by, this.imageWidth - 16, bh, "screen.nomosiki.squad_formations.open_short",
			"tooltip.nomosiki.squad_formations.open",
			b -> {
				if (this.minecraft != null) {
					this.minecraft.setScreen(new SquadFormationScreen(this));
				}
			}));
		by += bh + gap;
		this.behaviorButtons.add(sakuraButton(bx0, by, btnW, bh, "screen.nomosiki.nomosik.change_squad",
			"tooltip.nomosiki.nomosik.change_squad",
			b -> {
				if (this.minecraft != null) {
					this.minecraft.setScreen(new SquadPickerScreen(this, nomosikId, this.menu.getNomosik().getSquadIdForGui()));
				}
			}));
		this.behaviorButtons.add(sakuraButton(bx0 + btnW + colGap, by, btnW, bh, "screen.nomosiki.nomosik.leave_squad",
			"tooltip.nomosiki.nomosik.leave_squad",
			b -> SquadCommandClient.joinSquad(nomosikId, null)));
		for (Button button : this.behaviorButtons) {
			this.addRenderableWidget(button);
		}

		this.setTab(this.tab);
	}

	private SakuraButton orderButton(int x, int y, int width, int height, String labelKey, String tipKey, Runnable action) {
		return sakuraButton(x, y, width, height, labelKey, tipKey, b -> action.run());
	}

	private SakuraButton sakuraButton(int x, int y, int width, int height, String labelKey, String tipKey, Button.OnPress onPress) {
		return new SakuraButton(x, y, width, height, fitButtonLabel(labelKey, width - 4), onPress)
			.withTooltip(Tooltip.create(Component.translatable(tipKey)));
	}

	private Component fitButtonLabel(String key, int maxWidth) {
		String full = Component.translatable(key).getString();
		return Component.literal(this.font.plainSubstrByWidth(full, Math.max(10, maxWidth)));
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.tab == TAB_BEHAVIOR) {
			if (super.mouseClicked(mouseX, mouseY, button)) {
				return true;
			}
			return this.isPointerOverSlot(mouseX, mouseY);
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.minecraft != null && this.minecraft.player != null) {
			this.minecraft.player.closeContainer();
		}
		if (this.renameBox != null
			&& (this.renameBox.keyPressed(keyCode, scanCode, modifiers) || this.renameBox.canConsumeInput())) {
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private boolean isPointerOverSlot(double mouseX, double mouseY) {
		for (Slot slot : this.menu.slots) {
			if (slot.isActive() && this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
				return true;
			}
		}
		return false;
	}

	private int radiusForOrders() {
		return Mth.clamp(NomosikiConfig.INSTANCE.defaultAttackZoneRadius, 4, 24);
	}

	private void applyRename(int entityId) {
		ClientPlayNetworking.send(new NomosikRenamePayload(entityId, NomosikRenamePayload.sanitize(this.renameBox.getValue())));
	}

	private void setTab(int newTab) {
		this.tab = newTab;
		boolean equipment = newTab == TAB_EQUIPMENT;
		this.menu.setShowPlayerInventory(equipment);
		this.renameBox.setEditable(equipment);
		this.renameBox.setVisible(equipment);
		this.renameBox.setFocused(equipment);
		this.applyNameButton.visible = equipment;
		for (Button button : this.behaviorButtons) {
			button.visible = !equipment;
		}
	}

	private BlockPos anchorOrWarn() {
		if (this.minecraft == null || this.minecraft.player == null || this.minecraft.level == null) {
			return null;
		}
		var player = this.minecraft.player;
		Vec3 eye = player.getEyePosition(1f);
		Vec3 reach = eye.add(player.getViewVector(1f).scale(64.0));
		BlockHitResult hit = this.minecraft.level.clip(new ClipContext(eye, reach, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		if (hit.getType() == HitResult.Type.MISS) {
			player.displayClientMessage(Component.translatable("message.nomosiki.squad_need_block"), true);
			return null;
		}
		return hit.getBlockPos();
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		int x = this.leftPos;
		int y = this.topPos;
		SakuraStyle.drawPaperPanel(guiGraphics, x, y, this.imageWidth, this.imageHeight);
		int px = x + NomosikScreenHandler.PORTRAIT_X;
		int py = y + NomosikScreenHandler.PORTRAIT_Y;
		SakuraStyle.drawPortraitFrame(guiGraphics, px, py, NomosikScreenHandler.PORTRAIT_W, NomosikScreenHandler.PORTRAIT_H);
		for (Slot slot : this.menu.slots) {
			if (slot.isActive()) {
				SakuraStyle.drawSlot(guiGraphics, x + slot.x - 1, y + slot.y - 1);
			}
		}
		NomosikEntity nomosik = this.menu.getNomosik();
		if (nomosik.isAlive()) {
			InventoryScreen.renderEntityInInventoryFollowsMouse(
				guiGraphics,
				px + 2, py + 2,
				px + NomosikScreenHandler.PORTRAIT_W - 2, py + NomosikScreenHandler.PORTRAIT_H - 2,
				30, 0.0625F, (float) mouseX, (float) mouseY, nomosik);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, SakuraStyle.TITLE, false);
		if (this.tab == TAB_EQUIPMENT) {
			guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, SakuraStyle.INK_MUTED, false);
		}
		NomosikEntity nomosik = this.menu.getNomosik();
		int sx = NomosikScreenHandler.STATS_X;
		int sy = 26;
		int line = 10;
		guiGraphics.drawString(this.font, Component.translatable("screen.nomosiki.nomosik.stat_hp",
			String.format("%.0f / %.0f", nomosik.getHealth(), nomosik.getMaxHealth())), sx, sy, SakuraStyle.INK, false);
		sy += line;
		guiGraphics.drawString(this.font, Component.translatable("screen.nomosiki.nomosik.stat_armor",
			String.valueOf(nomosik.getArmorValue())), sx, sy, SakuraStyle.INK, false);
		sy += line;
		guiGraphics.drawString(this.font, Component.translatable("screen.nomosiki.nomosik.stat_follow",
			nomosik.shouldFollowOwner() ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF), sx, sy, SakuraStyle.INK, false);
		sy += line;
		Component orderLabel = Component.translatable("screen.nomosiki.nomosik.squad_order." + switch (nomosik.getSquadOrderForGui()) {
			case FOLLOW -> "follow";
			case HOLD -> "hold";
			case MOVE_TO -> "move";
			case ATTACK_ZONE -> "attack_zone";
			case ATTACK_TARGET -> "attack_target";
		});
		guiGraphics.drawString(this.font, Component.translatable("screen.nomosiki.nomosik.stat_order", orderLabel), sx, sy, SakuraStyle.INK, false);
		sy += line;
		guiGraphics.drawString(this.font, Component.translatable("screen.nomosiki.nomosik.stat_zone", nomosik.getSquadZoneRadiusForGui()),
			sx, sy, SakuraStyle.INK, false);
		sy += line;
		Component formationLabel = nomosik.getSquadFormationForGui().nomosikStatLabel();
		guiGraphics.drawString(this.font, Component.translatable("screen.nomosiki.nomosik.stat_formation", formationLabel), sx, sy, SakuraStyle.INK, false);
		sy += line;
		Component combatLabel = Component.translatable("screen.nomosiki.nomosik.combat_mode." + switch (nomosik.getCombatMode()) {
			case PASSIVE -> "passive";
			case NEUTRAL -> "neutral";
			case AGGRESSIVE -> "aggressive";
			case RAID -> "raid";
		});
		guiGraphics.drawString(this.font, Component.translatable("screen.nomosiki.nomosik.stat_combat_mode", combatLabel), sx, sy, SakuraStyle.INK, false);
		sy += line;
		Optional<UUID> squadId = nomosik.getSquadIdForGui();
		if (squadId.isEmpty()) {
			guiGraphics.drawString(this.font, Component.translatable("screen.nomosiki.nomosik.stat_squad_none"), sx, sy, SakuraStyle.INK_MUTED, false);
		} else {
			UUID id = squadId.get();
			String squadDisp = SquadClientState.squadNameFor(id).orElseGet(() -> id.toString().substring(0, 8) + "…");
			guiGraphics.drawString(this.font, Component.translatable("screen.nomosiki.nomosik.stat_squad", squadDisp), sx, sy, SakuraStyle.INK, false);
		}
		if (this.tab == TAB_BEHAVIOR) {
			guiGraphics.drawString(this.font, Component.translatable("screen.nomosiki.nomosik.behavior_hint"), 8, 112, SakuraStyle.INK_MUTED, false);
		}
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}
}
