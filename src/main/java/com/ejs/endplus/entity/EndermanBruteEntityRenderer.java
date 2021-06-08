package com.ejs.endplus.entity;

import java.util.Random;

import com.ejs.endplus.EndPlus;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.EndermanEyesFeatureRenderer;
import net.minecraft.client.render.entity.model.EndermanEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public class EndermanBruteEntityRenderer extends MobEntityRenderer<EndermanBruteEntity, EndermanEntityModel<EndermanBruteEntity>> {
   private static final Identifier TEXTURE = new Identifier(EndPlus.MOD_ID, "textures/entity/enderman_brute.png");
   private final Random random = new Random();

   @SuppressWarnings({ "unchecked", "rawtypes" })
public EndermanBruteEntityRenderer(EntityRenderDispatcher entityRenderDispatcher) {
      super(entityRenderDispatcher, new EndermanEntityModel(0.0F), 0.5F);
      this.addFeature(new EndermanEyesFeatureRenderer(this));
      //this.addFeature(new EndermanBlockFeatureRenderer(this));
   }

   public void render(EndermanBruteEntity endermanEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
      BlockState blockState = endermanEntity.getCarriedBlock();
      EndermanEntityModel<EndermanBruteEntity> endermanEntityModel = (EndermanEntityModel<EndermanBruteEntity>)this.getModel();
      endermanEntityModel.carryingBlock = blockState != null;
      endermanEntityModel.angry = endermanEntity.isAngry();
      super.render((EndermanBruteEntity)endermanEntity, f, g, matrixStack, vertexConsumerProvider, i);
   }

   public Vec3d getPositionOffset(EndermanBruteEntity endermanEntity, float f) {
      if (endermanEntity.isAngry()) {
       //  double d = 0.02D;
         return new Vec3d(this.random.nextGaussian() * 0.02D, 0.0D, this.random.nextGaussian() * 0.02D);
      } else {
         return super.getPositionOffset(endermanEntity, f);
      }
   }

   public Identifier getTexture(EndermanBruteEntity endermanEntity) {
      return TEXTURE;
   }

}
