package com.example.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

import java.util.OptionalDouble;

public final class QuarryRenderTypes extends RenderType {
	public static final RenderType LINES = create(
			"zk_workers_quarry_lines",
			DefaultVertexFormat.POSITION_COLOR_NORMAL,
			VertexFormat.Mode.LINES,
			8192,
			false,
			false,
			CompositeState.builder()
					.setShaderState(RENDERTYPE_LINES_SHADER)
					.setLineState(new LineStateShard(OptionalDouble.of(2.0)))
					.setLayeringState(VIEW_OFFSET_Z_LAYERING)
					.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
					.setOutputState(ITEM_ENTITY_TARGET)
					.setWriteMaskState(COLOR_WRITE)
					.setCullState(NO_CULL)
					.setDepthTestState(NO_DEPTH_TEST)
					.createCompositeState(false)
	);

	private QuarryRenderTypes(
			String name,
			VertexFormat format,
			VertexFormat.Mode mode,
			int bufferSize,
			boolean affectsCrumbling,
			boolean sortOnUpload,
			Runnable setupState,
			Runnable clearState
	) {
		super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
	}
}
