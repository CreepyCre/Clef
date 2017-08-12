package me.ichun.mods.clef.common.util.instrument;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import me.ichun.mods.clef.client.render.BakedModelInstrument;
import me.ichun.mods.clef.common.util.instrument.component.InstrumentInfo;
import me.ichun.mods.clef.common.util.instrument.component.InstrumentTuning;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ItemLayerModel;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;

public class Instrument
    implements Comparable<Instrument>
{
    public final InstrumentInfo info;
    public final BufferedImage iconImg;
    public final BufferedImage handImg;
    public InstrumentTuning tuning;

    @SideOnly(Side.CLIENT)
    public BakedModelInstrument iconModel;
    @SideOnly(Side.CLIENT)
    public BakedModelInstrument handModel;


    public Instrument(InstrumentInfo info, BufferedImage iconImg, BufferedImage handImg)
    {
        this.info = info;
        this.iconImg = iconImg;
        this.handImg = handImg;
    }

    public boolean hasAvailableKey(int key)
    {
        return tuning.keyToTuningMap.containsKey(key);
    }

    @Override
    public int compareTo(Instrument o)
    {
        return info.shortdescription.compareTo(o.info.shortdescription);
    }

    @SideOnly(Side.CLIENT)
    public void setupModels()
    {
        //TODO test reloading the resources and see what happens?
        if(iconModel == null && handModel == null)
        {
            Minecraft mc = Minecraft.getMinecraft();

            ResourceLocation iconRl = new ResourceLocation("clef", "instrument/" + info.itemName + "/icon.png");
            ResourceLocation handRl = new ResourceLocation("clef", "instrument/" + info.itemName + "/hand.png");

            InstrumentTexture iconTx = new InstrumentTexture(iconRl, iconImg);
            InstrumentTexture handTx = new InstrumentTexture(handRl, handImg);

            mc.getTextureManager().loadTexture(iconTx.rl, iconTx);
            mc.getTextureManager().loadTexture(handTx.rl, handTx);

            iconModel = new BakedModelInstrument(iconTx.quads, iconTx.tasi, ImmutableMap.copyOf(new HashMap<>()), this, iconRl);
            handModel = new BakedModelInstrument(handTx.quads, handTx.tasi, ImmutableMap.copyOf(new HashMap<>()), this, handRl);
        }
    }

    @SideOnly(Side.CLIENT)
    public class InstrumentTexture extends AbstractTexture
    {
        public final ResourceLocation rl;
        public final BufferedImage image;
        public ImmutableList<BakedQuad> quads;
        public TextureAtlasSpriteInstrument tasi;

        public InstrumentTexture(ResourceLocation rl, BufferedImage image)
        {
            this.rl = rl;
            if(image.getWidth() != image.getHeight())
            {
                int size = Math.max(image.getWidth(), image.getHeight());
                BufferedImage image1 = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                int halfX = (int)Math.floor((size - image.getWidth()) / 2D); //offsetX
                int halfY = (int)Math.floor((size - image.getHeight()) / 2D); //offsetY
                for(int x = 0; x < image.getWidth(); x++)
                {
                    for(int y = 0; y < image.getHeight(); y++)
                    {
                        image1.setRGB(halfX + x, halfY + y, image.getRGB(x, y));
                    }
                }
                this.image = image1;
            }
            else
            {
                this.image = image;
            }
        }

        @Override
        public void loadTexture(IResourceManager resourceManager) throws IOException
        {
            TextureUtil.uploadTextureImageAllocate(this.getGlTextureId(), image, false, false);

            ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
            tasi = new TextureAtlasSpriteInstrument(this.rl, this.image);
            tasi.load(Minecraft.getMinecraft().getResourceManager(), rl);
            builder.addAll(ItemLayerModel.getQuadsForSprite(0, tasi, DefaultVertexFormats.ITEM, Optional.absent()));
            quads = builder.build();
        }
    }

    @SideOnly(Side.CLIENT)
    public class TextureAtlasSpriteInstrument extends TextureAtlasSprite
    {
        public BufferedImage image;

        public TextureAtlasSpriteInstrument(ResourceLocation rl, BufferedImage image)
        {
            super(rl.toString());
            this.image = image;
        }

        public boolean hasCustomLoader(net.minecraft.client.resources.IResourceManager manager, net.minecraft.util.ResourceLocation location)
        {
            return true;
        }

        public boolean load(net.minecraft.client.resources.IResourceManager manager, net.minecraft.util.ResourceLocation location)
        {
            this.width = image.getWidth();
            this.height = image.getHeight();

            int[][] aint = new int[Minecraft.getMinecraft().getTextureMapBlocks().getMipmapLevels() + 1][];
            aint[0] = new int[image.getWidth() * image.getHeight()];
            image.getRGB(0, 0, image.getWidth(), image.getHeight(), aint[0], 0, image.getWidth());

            this.framesTextureData.add(aint);

            this.initSprite(width, height, 0, 0, false);

            return false;
        }
    }

}