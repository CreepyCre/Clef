package me.ichun.mods.clef.common.util.abc.play;

import io.netty.util.internal.ThreadLocalRandom;
import me.ichun.mods.clef.client.sound.InstrumentSound;
import me.ichun.mods.clef.common.Clef;
import me.ichun.mods.clef.common.util.instrument.Instrument;
import me.ichun.mods.clef.common.util.instrument.component.InstrumentTuning;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Random;

import java.util.Map;
import java.util.List;
import com.google.common.collect.Multimap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.ITickableSound;
import paulscode.sound.SoundSystem;

@SideOnly(Side.CLIENT)
public class PlayedNote
{
    public final Instrument instrument;
    public final int key;
    public final int startTick;
    public final int duration;
    public final InstrumentSound instrumentSound;
    public Object noteLocation;

    public String uniqueId;
    public boolean played;

    //TODO handle corrupt sound files somehow.

    public PlayedNote(Instrument instrument, int startTick, int duration, int key, SoundCategory category, Object noteLocation)
    {
        this.instrument = instrument;
        this.key = key;
        this.startTick = startTick;
        this.duration = duration;
        this.noteLocation = noteLocation;

        uniqueId = MathHelper.getRandomUUID(ThreadLocalRandom.current()).toString();

        InstrumentTuning.TuningInfo tuning = instrument.tuning.keyToTuningMap.get(key);
        float pitch = (float)Math.pow(2.0D, (double)tuning.keyOffset / 12.0D);
        this.instrumentSound = new InstrumentSound(uniqueId, SoundEvents.BLOCK_NOTE_HARP, category, duration, (int)Math.ceil(instrument.tuning.fadeout * 20F), 0.7F * (Clef.config.instrumentVolume / 100F), pitch, noteLocation);
    }

    public PlayedNote start()
    {
		try {
			//        Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.ENTITY_PIG_AMBIENT, (float)Math.pow(2.0D, (double)((key) - 12 - 48) / 12.0D)));
			//        Minecraft.getMinecraft().getSoundHandler().playSound(sound);
			Minecraft mc = Minecraft.getMinecraft();
			Field fieldSoundManager = mc.getSoundHandler().getClass().getDeclaredField("field_147694_f");
			fieldSoundManager.setAccessible(true);
			SoundManager soundManager = (SoundManager)fieldSoundManager.get(mc.getSoundHandler());
			if (mc.gameSettings.getSoundLevel(SoundCategory.MASTER) > 0.0F && instrument.hasAvailableKey(key))
			{
			
				instrumentSound.createAccessor(mc.getSoundHandler());

				float f3 = instrumentSound.getVolume();
				float f = 16.0F;

				if (f3 > 1.0F)
				{
					f *= f3;
				}

				SoundCategory soundcategory = instrumentSound.getCategory();
            
				Method methodGetClampedVolume = soundManager.getClass().getMethod("func_188770_e", ISound.class);
				methodGetClampedVolume.setAccessible(true);
				float f1 = (float)methodGetClampedVolume.invoke(soundManager, instrumentSound);
	
				InstrumentTuning.TuningInfo tuning = instrument.tuning.keyToTuningMap.get(key);
				float f2 = (float)Math.pow(2.0D, (double)tuning.keyOffset / 12.0D);
				
				Field fieldSndSystem = soundManager.getClass().getDeclaredField("field_148620_e");
				fieldSndSystem.setAccessible(true);
				SoundSystem sndSystem = (SoundSystem)fieldSndSystem.get(soundManager);
				
				sndSystem.newSource(false, uniqueId, getURLForSoundResource(instrument, key - tuning.keyOffset), "clef:" + instrument.info.itemName + ":" + (key - tuning.keyOffset) + ".ogg", false, instrumentSound.getXPosF(), instrumentSound.getYPosF(), instrumentSound.getZPosF(), instrumentSound.getAttenuationType().getTypeInt(), f);
				net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.sound.PlaySoundSourceEvent(soundManager, instrumentSound, uniqueId));
	
				sndSystem.setPitch(uniqueId, f2);
				sndSystem.setVolume(uniqueId, f1);
				sndSystem.play(uniqueId);
				
				Field fieldPlayingSoundsStopTime = soundManager.getClass().getDeclaredField("field_148624_n");
				fieldPlayingSoundsStopTime.setAccessible(true);
				Map<String, Integer> playingSoundsStopTime = (Map<String, Integer>)fieldPlayingSoundsStopTime.get(soundManager);
				
				Field fieldPlayTime = soundManager.getClass().getDeclaredField("field_148618_g");
				fieldPlayTime.setAccessible(true);
				int playTime = (int)fieldPlayTime.get(soundManager);
				
				playingSoundsStopTime.put(uniqueId, playTime + duration + (int)(instrument.tuning.fadeout * 20F) + 20);
				
				Field fieldPlayingSounds = soundManager.getClass().getDeclaredField("field_148629_h");
				fieldPlayingSounds.setAccessible(true);
				Map<String, ISound> playingSounds = (Map<String, ISound>)fieldPlayingSounds.get(soundManager);
				
				playingSounds.put(uniqueId, instrumentSound);
	
				if (soundcategory != SoundCategory.MASTER)
				{
					Field fieldCategorySounds = soundManager.getClass().getDeclaredField("field_188776_k");
					fieldCategorySounds.setAccessible(true);
					Multimap<SoundCategory, String> categorySounds = (Multimap<SoundCategory, String>)fieldCategorySounds.get(soundManager);
					
					categorySounds.put(soundcategory, uniqueId);
				}
				Field fieldTickableSounds = soundManager.getClass().getDeclaredField("field_148625_l");
				fieldTickableSounds.setAccessible(true);
				List<ITickableSound> tickableSounds = (List<ITickableSound>)fieldTickableSounds.get(soundManager);
				
				tickableSounds.add(instrumentSound);
	
				played = true;
			}
		}
		catch(NoSuchFieldException e){
			Clef.LOGGER.error("NoSuchFieldException: " + e.getMessage());
		}
		catch(IllegalAccessException e){
			Clef.LOGGER.error("IllegalAccessException: " + e.getMessage());
		}
		catch(NoSuchMethodException e){
			Clef.LOGGER.error("NoSuchMethodException: " + e.getMessage());
		}
		catch(InvocationTargetException e){
			Clef.LOGGER.error("InvocationTargetException: " + e.getMessage());
		}
        return this;
    }

    private static URL getURLForSoundResource(final Instrument instrument, final int key)
    {
        int randKey = rand.nextInt(instrument.tuning.keyToTuningMap.get(key).streamsLength());
        String s = String.format("%s:%s:%s", "clef", instrument.info.itemName, key + ":" + randKey + ".ogg");
        URLStreamHandler urlstreamhandler = new URLStreamHandler()
        {
            protected URLConnection openConnection(final URL p_openConnection_1_)
            {
                return new URLConnection(p_openConnection_1_)
                {
                    public void connect() throws IOException
                    {
                    }
                    public InputStream getInputStream() throws IOException
                    {
                        return instrument.tuning.keyToTuningMap.get(key).get(randKey);
                    }
                };
            }
        };

        try
        {
            return new URL(null, s, urlstreamhandler);
        }
        catch (MalformedURLException var4)
        {
            throw new Error("Minecraft no has proper error throwing and handling.");
        }
    }

    private static Random rand = new Random();
}
