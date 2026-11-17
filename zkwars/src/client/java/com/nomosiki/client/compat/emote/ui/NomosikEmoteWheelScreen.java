package com.nomosiki.client.compat.emote.ui;

import com.nomosiki.client.compat.emote.NomosikEmoteCatalog;
import com.nomosiki.client.compat.emote.NomosikEmoteClientCompat;
import com.nomosiki.client.gui.SakuraButton;
import com.nomosiki.client.gui.SakuraStyle;
import com.nomosiki.compat.emote.NomosikEmoteIds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Simple radial wheel of builtin / catalog emotes for selected squads. */
public class NomosikEmoteWheelScreen extends Screen {
	private static final int SLOT_COUNT = 6;
	private static final int RADIUS = 72;
	private static final int SLOT_W = 72;
	private static final int SLOT_H = 24;

	private final Screen parent;
	private final List<UUID> squadIds;
	private final List<NomosikEmoteCatalog.Entry> slots = new ArrayList<>();
	private int hovered = -1;

	public NomosikEmoteWheelScreen(Screen parent, List<UUID> squadIds) {
		super(Component.translatable("screen.nomosiki.emote_wheel"));
		this.parent = parent;
		this.squadIds = List.copyOf(squadIds);
	}

	@Override
	protected void init() {
		NomosikEmoteCatalog.ensureLoaded();
		this.slots.clear();
		List<NomosikEmoteCatalog.Entry> all = NomosikEmoteCatalog.wheelEntries();
		for (int i = 0; i < SLOT_COUNT; i++) {
			if (i < all.size()) {
				this.slots.add(all.get(i));
			}
		}
		int btnW = 100;
		int doneY = this.height - 28;
		this.addRenderableWidget(new SakuraButton(this.width / 2 - btnW - 4, doneY, btnW, 20,
			Component.translatable("screen.nomosiki.emote_wheel.stop"), b -> {
				NomosikEmoteClientCompat.sendStop(this.squadIds);
				this.onClose();
			}));
		this.addRenderableWidget(new SakuraButton(this.width / 2 + 4, doneY, btnW, 20,
			Component.translatable("gui.cancel"), b -> this.onClose()));
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		SakuraStyle.drawDim(graphics, this.width, this.height);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		SakuraStyle.drawCentered(graphics, this.font, this.title, this.width / 2, 18, SakuraStyle.TITLE);
		SakuraStyle.drawCentered(graphics, this.font,
			Component.translatable("screen.nomosiki.emote_wheel.hint", this.squadIds.size()),
			this.width / 2, 32, SakuraStyle.INK_MUTED);

		int cx = this.width / 2;
		int cy = this.height / 2 - 8;
		this.hovered = hitSlot(mouseX, mouseY, cx, cy);

		for (int i = 0; i < SLOT_COUNT; i++) {
			double angle = -Math.PI / 2.0 + (Math.PI * 2.0 * i) / SLOT_COUNT;
			int x = cx + (int) Math.round(Math.cos(angle) * RADIUS);
			int y = cy + (int) Math.round(Math.sin(angle) * RADIUS);
			boolean hot = i == this.hovered;
			SakuraStyle.drawButton(graphics, x - SLOT_W / 2, y - SLOT_H / 2, SLOT_W, SLOT_H, hot, true);
			String label = i < this.slots.size()
				? this.font.plainSubstrByWidth(this.slots.get(i).name(), SLOT_W - 8)
				: "—";
			SakuraStyle.drawCentered(graphics, this.font, Component.literal(label), x, y - 4,
				hot ? SakuraStyle.ACCENT : SakuraStyle.INK);
		}

		SakuraStyle.drawCentered(graphics, this.font, Component.translatable("screen.nomosiki.emote_wheel.center"),
			cx, cy - 4, SakuraStyle.INK_MUTED);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int cx = this.width / 2;
		int cy = this.height / 2 - 8;
		int slot = hitSlot(mouseX, mouseY, cx, cy);
		if (slot >= 0 && slot < this.slots.size()) {
			if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
				NomosikEmoteCatalog.Entry entry = this.slots.get(slot);
				NomosikEmoteClientCompat.sendPlay(
					entry.id(),
					NomosikEmoteIds.keyString(entry.key()),
					this.squadIds);
				this.onClose();
				return true;
			}
			if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
				NomosikEmoteClientCompat.sendStop(this.squadIds);
				this.onClose();
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private int hitSlot(double mouseX, double mouseY, int cx, int cy) {
		for (int i = 0; i < SLOT_COUNT; i++) {
			double angle = -Math.PI / 2.0 + (Math.PI * 2.0 * i) / SLOT_COUNT;
			int x = cx + (int) Math.round(Math.cos(angle) * RADIUS);
			int y = cy + (int) Math.round(Math.sin(angle) * RADIUS);
			if (Math.abs(mouseX - x) <= SLOT_W / 2 && Math.abs(mouseY - y) <= SLOT_H / 2) {
				return i;
			}
		}
		double dx = mouseX - cx;
		double dy = mouseY - cy;
		double dist = Math.sqrt(dx * dx + dy * dy);
		if (dist < 28 || dist > RADIUS + 40) {
			return -1;
		}
		double angle = Math.atan2(dy, dx);
		double normalized = angle + Math.PI / 2.0;
		if (normalized < 0) {
			normalized += Math.PI * 2.0;
		}
		int idx = (int) Math.floor((normalized / (Math.PI * 2.0)) * SLOT_COUNT);
		return Mth.clamp(idx, 0, SLOT_COUNT - 1);
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) {
			this.minecraft.setScreen(this.parent);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
