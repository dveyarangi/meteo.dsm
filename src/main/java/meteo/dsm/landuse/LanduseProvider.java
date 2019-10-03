package meteo.dsm.landuse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.badlogic.gdx.graphics.Color;

import lombok.extern.slf4j.Slf4j;
import meteo.dsm.DSMCfg;
import midas.core.spatial.AOI;

@Slf4j
public abstract class LanduseProvider
{
	private DSMCfg cfg;

	public LanduseProvider(DSMCfg cfg)
	{
		this.cfg = cfg;
	}
	

	public abstract String getName();
	public abstract int getCode(float lat, float lon);
	public abstract Color getColor(int code);
	
	
	public Color[][] getColors(AOI coverage, int res) 
	{
		int [][] landuseCodes = null;
		
		File landuseCacheFile = new File(getCacheFilePath(coverage, res));
		landuseCacheFile.getParentFile().mkdirs();
		float minLon = coverage.getMinLon();
		float maxLon = coverage.getMaxLon();
		float minLat = coverage.getMinLat();
		float maxLat = coverage.getMaxLat();
		
		float sLon = (maxLon - minLon) / res;
		float sLat = (maxLat - minLat) / res;
		
		if( !landuseCacheFile.exists()) 
		{
			landuseCodes = new int[res][res];
			
			for(int x = 0; x < res; x ++)
				for(int y = 0; y < res; y ++)
				{
					float lat = maxLat - y * sLat;
					float lon = minLon + x * sLon;
			
					landuseCodes[x][y] = getCode(lat, lon);
				}
			
			try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(landuseCacheFile)))
			{
				oos.writeObject(landuseCodes);
				log.debug("Wrote cache file " + landuseCacheFile.getAbsolutePath());
				
			} catch( IOException e ) { e.printStackTrace(); }

		}
		else {
			try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(landuseCacheFile)))
			{
				landuseCodes = (int[][]) ois.readObject();
			} 
			catch( IOException e ) { e.printStackTrace(); } 
			catch( ClassNotFoundException e ) { e.printStackTrace(); }
			
		}
		
		Color [][] colors = new Color[res][res];
	
		for(int x = 0; x < res; x ++)
			for(int y = 0; y < res; y ++)
			{
				
				colors[x][y] = getColor(landuseCodes[x][y]);
				
				if( colors[x][y] == null)
					colors[x][y] = Color.BLACK;
				//colors[x][y] = Color.GRAY;
			}
		
		return colors;
	}
	
	private String getCacheFilePath(AOI coverage, int res)
	{
		return new StringBuilder() 
				.append(cfg.getCacheDir()).append("/").append("landuse").append("/").append(getName()).append("/").append(res).append("/")
				.append("dsm_landuse_").append(getName()).append("_").append(res)
				.append("_").append((int)coverage.getMinLat())
				.append("_").append((int)coverage.getMinLon())
				
				.append(".dsmgrid")
				.toString();
	}
}
