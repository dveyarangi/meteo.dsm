package meteo.dsm.dem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import lombok.extern.slf4j.Slf4j;
import meteo.dsm.DSMCfg;
import midas.core.spatial.AOI;

@Slf4j
public abstract class DEMTile
{
	DSMCfg cfg;
	
	public DEMTile(DSMCfg cfg)
	{
		this.cfg = cfg;
	}
	
	public abstract AOI getCoverage();

	public abstract int[][] readHeightmap(int res);
	
	public abstract String getName();
	
	public int [][] getHeightmap(int res)
	{
		File demCacheFile = new File(getCacheFilePath(res));
		demCacheFile.getParentFile().mkdirs();
		
		float minLon = getCoverage().getMinLon();
		float maxLon = getCoverage().getMaxLon();
		float minLat = getCoverage().getMinLat();
		float maxLat = getCoverage().getMaxLat();
		
		float sLon = (maxLon - minLon) / res;
		float sLat = (maxLat - minLat) / res;
		
		int [][] heightmap = null;
		if( !demCacheFile.exists() )
		{
		
			heightmap = readHeightmap( res );

					
			demCacheFile.getParentFile().mkdirs();
			try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(demCacheFile)))
			{
				oos.writeObject(heightmap);
				log.debug("Wrote cache file " + demCacheFile.getAbsolutePath());
				
			} catch( IOException e ) { e.printStackTrace(); }
		}
		else // has cache file
		{
			try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(demCacheFile)))
			{
				heightmap = (int[][]) ois.readObject();
			} 
			catch( IOException e ) { e.printStackTrace(); } 
			catch( ClassNotFoundException e ) { e.printStackTrace(); }
			
		}

		return heightmap;
	}
	

	

	private String getCacheFilePath(int res)
	{
		return new StringBuilder() 
				.append(cfg.getCacheDir()).append("/").append("dem").append("/").append(getName()).append("/").append(res).append("/")
				.append("dsm_dem_").append(getName()).append("_").append(res)
				.append("_").append((int)getCoverage().getMinLat())
				.append("_").append((int)getCoverage().getMinLon())
				
				.append(".dsmgrid")
				.toString();
	}
}
