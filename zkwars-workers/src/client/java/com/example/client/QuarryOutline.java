package com.example.client;

import com.example.block.WorkerKind;
import com.example.block.entity.WorkerBlockEntity;
import com.example.block.work.QuarryBounds;
import com.example.block.work.WorkArea;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class QuarryOutline {
	private static @Nullable BlockPos previewPos;
	private static @Nullable QuarryBounds.Box preview;

	private QuarryOutline() {
	}

	public static void show(BlockPos pos, QuarryBounds.Box box) {
		previewPos = pos;
		preview = box;
	}

	public static void clear() {
		previewPos = null;
		preview = null;
	}

	public static void render(WorldRenderContext context) {
		if (context.consumers() == null || context.matrixStack() == null || context.camera() == null) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.level == null) {
			return;
		}
		BlockPos origin;
		QuarryBounds.Box box;
		if (minecraft.screen instanceof WorkerScreen && previewPos != null && preview != null) {
			origin = previewPos;
			box = preview;
		} else {
			clear();
			if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
				return;
			}
			BlockEntity blockEntity = minecraft.level.getBlockEntity(hit.getBlockPos());
			if (!(blockEntity instanceof WorkerBlockEntity worker)) {
				return;
			}
			if (worker.kind() == WorkerKind.SMELTER) {
				renderFurnaces(context, worker.furnaces());
				return;
			}
			if (worker.kind() == WorkerKind.CRAFTER) {
				return;
			}
			origin = hit.getBlockPos();
			box = worker.kind() == WorkerKind.QUARRY
					? worker.quarryBounds()
					: WorkArea.box(worker.kind(), worker.workRadius());
		}
		renderBox(context, WorkArea.volume(origin, box).inflate(0.002));
	}

	private static void renderFurnaces(WorldRenderContext context, List<BlockPos> furnaces) {
		if (furnaces.isEmpty() || context.matrixStack() == null || context.camera() == null || context.consumers() == null) {
			return;
		}
		PoseStack pose = context.matrixStack();
		Vec3 camera = context.camera().getPosition();
		pose.pushPose();
		pose.translate(-camera.x, -camera.y, -camera.z);
		var lines = context.consumers().getBuffer(RenderType.lines());
		for (BlockPos pos : furnaces) {
			LevelRenderer.renderLineBox(pose, lines, new AABB(pos).inflate(0.002), 0.96F, 0.78F, 0.28F, 1.0F);
		}
		pose.popPose();
		if (context.consumers() instanceof MultiBufferSource.BufferSource bufferSource) {
			bufferSource.endBatch(RenderType.lines());
		}
	}

	private static void renderBox(WorldRenderContext context, AABB volume) {
		PoseStack pose = context.matrixStack();
		Vec3 camera = context.camera().getPosition();
		pose.pushPose();
		pose.translate(-camera.x, -camera.y, -camera.z);
		var lines = context.consumers().getBuffer(RenderType.lines());
		LevelRenderer.renderLineBox(pose, lines, volume, 0.96F, 0.78F, 0.28F, 1.0F);
		pose.popPose();
		if (context.consumers() instanceof MultiBufferSource.BufferSource bufferSource) {
			bufferSource.endBatch(RenderType.lines());
		}
	}
}
