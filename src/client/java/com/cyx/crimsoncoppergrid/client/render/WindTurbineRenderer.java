package com.cyx.crimsoncoppergrid.client.render;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.cyx.crimsoncoppergrid.blockentity.WindGeneratorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

/**
 * 风力发电机的三叶片叶轮。
 *
 * <p>方块模型只负责机壳，转起来的部分在这里 —— 一个由代码拼出来的 3 叶片叶轮，
 * 绕水平轴旋转。贴图是 {@code TechReborn} 的 {@code wind_mill_turbine.png}（MIT）。
 *
 * <h2>几何（照搬 TechReborn 的 TurbineRenderer）</h2>
 * <ul>
 *   <li>轮毂：两个小方块，一个 4×4×2 的盘 + 一个 2×2×1 的轴；</li>
 *   <li>叶片 3 片，每片 24×2×1（模型单位，24 单位 = 1.5 格），绕 Z 轴均分 120°，
 *       并且各自再绕 X 轴倾 -30° —— 这是真实风机的叶片「桨距角」，
 *       转起来才有立体感，否则就是三根平板。</li>
 * </ul>
 *
 * <p>摆放：先把原点挪到方块中心，按 {@code FACING} 偏航，再推到正面外侧
 * （z = -0.56，即贴着正面但不相交），最后用模型自带的 1.5 格抬升把轮毂
 * 放到方块腰线上。于是叶轮像挂在机器正面的墙扇，叶片扫过的圆直径约 3 格 ——
 * 与 TechReborn 里的观感一致。
 *
 * <p>转速由方块实体的 {@code bladeAngle} / {@code spinSpeed} 驱动，
 * 那两个字段只在客户端推进（见 {@code WindGeneratorBlockEntity#clientTick}）。
 */
public class WindTurbineRenderer
		implements BlockEntityRenderer<WindGeneratorBlockEntity, WindTurbineRenderer.TurbineRenderState> {

	private static final Set<Direction> ALL_DIRECTIONS = EnumSet.allOf(Direction.class);
	private static final TurbineModel MODEL = TurbineModel.create();

	public static final Identifier TEXTURE =
			Identifier.parse("crimsoncoppergrid:textures/block/wind_generator_turbine.png");

	public WindTurbineRenderer(BlockEntityRendererProvider.Context ctx) {
	}

	@Override
	public TurbineRenderState createRenderState() {
		return new TurbineRenderState();
	}

	@Override
	public void extractRenderState(
		WindGeneratorBlockEntity blockEntity,
		TurbineRenderState state,
		float tickDelta,
		Vec3 vec3,
		ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay
	) {
		BlockEntityRenderState.extractBase(blockEntity, state, crumblingOverlay);
		Direction facing = blockEntity.getFacing();
		state.layer = RenderTypes.entitySolid(TEXTURE);
		// 叶轮画在 north 面，靠这一句把它转到 FACING 指向的那一面
		state.rotate = -facing.getCounterClockWise().toYRot() + 90;
		// 叶片转的时候要在两帧之间插值，否则 20tps 的步进会一顿一顿
		state.spin = blockEntity.bladeAngle + tickDelta * blockEntity.spinSpeed;
		// 光要从叶轮前方那一格取：叶轮悬在方块外面
		state.light = LightCoordsUtil.getLightCoords(blockEntity.getLevel(), blockEntity.getBlockPos().relative(facing));
	}

	@Override
	public void submit(
		TurbineRenderState state,
		PoseStack matrixStack,
		SubmitNodeCollector submitNodeCollector,
		CameraRenderState cameraRenderState
	) {
		matrixStack.pushPose();
		matrixStack.translate(0.5, 0, 0.5);
		matrixStack.rotateDegrees(Axis.YP, state.rotate);
		matrixStack.translate(0, -1, -0.56);
		submitNodeCollector.submitModel(MODEL, state.spin, matrixStack, state.layer, state.light,
				OverlayTexture.NO_OVERLAY, 0);
		if (state.breakProgress != null) {
			// 挖掘破坏的裂纹也要跟着叶片转
			submitNodeCollector.order(1).submitCrumblingOverlay(
					MODEL, state.spin, matrixStack, state.layer, state.light, OverlayTexture.NO_OVERLAY, -1,
					state.breakProgress
			);
		}
		matrixStack.popPose();
	}

	/**
	 * 叶轮的模型。{@code spin} 是当前角度（弧度），直接写进根节点的 {@code zRot} ——
	 * 三片叶片都是它的子节点，于是整盘一起转。
	 */
	private static class TurbineModel extends Model<Float> {

		private static TurbineModel create() {
			ModelPart.Cube[] baseCuboids = {
					new ModelPart.Cube(0, 0, -2.0F, -2.0F, -1.0F, 4F, 4F, 2F, 0F, 0F, 0F, false, 64F, 64F, ALL_DIRECTIONS),
					new ModelPart.Cube(0, 6, -1.0F, -1.0F, -2.0F, 2F, 2F, 1F, 0F, 0F, 0F, false, 64F, 64F, ALL_DIRECTIONS)
			};

			ModelPart base = new ModelPart(Arrays.asList(baseCuboids), new HashMap<>() {
				{
					ModelPart.Cube[] blade1Cuboids = {
							new ModelPart.Cube(0, 9, -24.0F, -1.0F, -0.5F, 24F, 2F, 1F, 0F, 0F, 0F, false, 64F, 64F, ALL_DIRECTIONS)
					};
					ModelPart blade1 = new ModelPart(Arrays.asList(blade1Cuboids), Collections.emptyMap());
					blade1.setPos(0.0F, 0.0F, 0.0F);
					setRotation(blade1, -0.5236F, 0.0F, 0.0F);
					put("blade1", blade1);

					ModelPart.Cube[] blade2Cuboids = {
							new ModelPart.Cube(0, 9, -24.0F, -1.0F, -0.5F, 24F, 2F, 1F, 0F, 0F, 0F, false, 64F, 64F, ALL_DIRECTIONS)
					};
					ModelPart blade2 = new ModelPart(Arrays.asList(blade2Cuboids), Collections.emptyMap());
					blade2.setPos(0.0F, 0.0F, 0.0F);
					setRotation(blade2, -0.5236F, 0.0F, 2.0944F);
					put("blade2", blade2);

					ModelPart.Cube[] blade3Cuboids = {
							new ModelPart.Cube(0, 9, -24.0F, -2.0F, -1.075F, 24F, 2F, 1F, 0F, 0F, 0F, false, 64F, 64F, ALL_DIRECTIONS)
					};
					ModelPart blade3 = new ModelPart(Arrays.asList(blade3Cuboids), Collections.emptyMap());
					blade3.setPos(0.0F, 0.0F, 0.0F);
					setRotation(blade3, -0.5236F, 0.0F, -2.0944F);
					put("blade3", blade3);
				}
			});
			base.setPos(0.0F, 24.0F, 0.0F);

			return new TurbineModel(base, RenderTypes::entityCutout);
		}

		private static void setRotation(ModelPart model, float x, float y, float z) {
			model.xRot = x;
			model.yRot = y;
			model.zRot = z;
		}

		public TurbineModel(ModelPart root, Function<Identifier, RenderType> layerFactory) {
			super(root, layerFactory);
		}

		@Override
		public void setupAnim(Float spin) {
			root.zRot = spin;
		}
		// Model#renderToBuffer 已经是 final，它内部会调用 root.render(...)，
		// 这里不需要再覆写。
	}

	public static class TurbineRenderState extends BlockEntityRenderState {
		public RenderType layer;
		public float rotate;
		public float spin;
		public int light;
	}
}
