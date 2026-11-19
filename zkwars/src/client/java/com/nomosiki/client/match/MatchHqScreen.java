package com.nomosiki.client.match;

import com.nomosiki.client.gui.SakuraButton;
import com.nomosiki.client.rts.TacticalCameraController;
import com.nomosiki.client.rts.TacticalHudScreen;
import com.nomosiki.entity.NomosikRace;
import com.nomosiki.match.MatchBuildingCatalog;
import com.nomosiki.match.MatchPhase;
import com.nomosiki.match.MatchRecruitment;
import com.nomosiki.network.match.payload.c2s.MatchRecruitPayload;
import com.nomosiki.network.match.payload.s2c.MatchHudPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MatchHqScreen extends Screen {
	private enum Tab { RECRUIT, BUILD }

	private Tab tab = Tab.RECRUIT;
	private final NomosikRace raceHint = NomosikRace.HUMAN;

	public MatchHqScreen() {
		super(Component.translatable("screen.nomosiki.match.hq.title"));
	}

	@Override
	protected void init() {
		int panelW = 220;
		int left = (this.width - panelW) / 2;
		int top = 40;
		this.addRenderableWidget(new SakuraButton(left, top, 100, 20,
			Component.translatable("screen.nomosiki.match.hq.tab_recruit"), b -> {
				this.tab = Tab.RECRUIT;
				this.rebuildTabs();
			}));
		this.addRenderableWidget(new SakuraButton(left + 110, top, 100, 20,
			Component.translatable("screen.nomosiki.match.hq.tab_build"), b -> {
				this.tab = Tab.BUILD;
				this.rebuildTabs();
			}));

		int y = top + 36;
		MatchHudPayload hud = MatchClient.last();
		if (this.tab == Tab.RECRUIT) {
			for (MatchRecruitment.Role role : MatchRecruitment.Role.values()) {
				int cost = role.cost(this.raceHint);
				Component label = Component.translatable("screen.nomosiki.match.recruit." + role.id(), cost);
				this.addRenderableWidget(new SakuraButton(left, y, panelW, 20, label, b -> {
					ClientPlayNetworking.send(new MatchRecruitPayload(role.id()));
				}));
				y += 22;
			}
		} else {
			for (MatchBuildingCatalog.Kind kind : MatchBuildingCatalog.Kind.all()) {
				int cost = kind.cost(this.raceHint);
				Component label = Component.translatable("screen.nomosiki.match.build." + kind.id(), cost);
				String buildingId = kind.id();
				this.addRenderableWidget(new SakuraButton(left, y, panelW, 20, label, b -> {
					MatchClientState.beginBuildPlace(buildingId);
					Minecraft mc = this.minecraft;
					this.onClose();
					if (mc != null && TacticalCameraController.isActive()) {
						mc.setScreen(new TacticalHudScreen());
					}
				}));
				y += 22;
			}
		}
	}

	private void rebuildTabs() {
		this.clearWidgets();
		this.init();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics, mouseX, mouseY, partialTick);
		super.render(graphics, mouseX, mouseY, partialTick);
		MatchHudPayload hud = MatchClient.last();
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 18, 0xFFFFFF);
		if (hud.phase() == MatchPhase.RUNNING.id()) {
			graphics.drawCenteredString(
				this.font,
				Component.translatable("hud.nomosiki.match.authority", hud.authority(), hud.income()),
				this.width / 2,
				this.height - 24,
				0xFFE082
			);
		}
		if (MatchClientState.buildPlaceMode()) {
			graphics.drawCenteredString(
				this.font,
				Component.translatable("screen.nomosiki.match.build.place_hint"),
				this.width / 2,
				this.height - 40,
				0x80CBC4
			);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
