package meteo.dsm.dem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.badlogic.gdx.math.Vector3;

import lombok.extern.slf4j.Slf4j;
import meteo.dsm.DSMCfg;
import meteo.util.geodesy.Datum;
import meteo.util.geodesy.GeoUtil;
import meteo.util.sampling.ArrayWindow;
import midas.core.spatial.AOI;


@Slf4j
public abstract class DEMProvider
{
	
	public abstract void readTiles(AOI coverage, DEMTileConsumer consumer);
	
	
	private String name;
	private DSMCfg cfg;
	
	private AOI coverage;
	private Datum datum;
	private int res;
	private Object vec;
	private Vector3[][] vectors;
	
	/**
	 * Creates a new DEM data provider for specified coverage area 
	 * @param name
	 * @param cfg
	 * @param coverage
	 * @param datum
	 * @param res - grid samples per lat/lon unit
	 */
	protected DEMProvider(String name, DSMCfg cfg, AOI coverage, Datum datum, int res)
	{
		this.name = name;
		this.cfg = cfg;
		this.coverage = coverage;
		this.datum = datum;
		this.res = res;
		
		this.vectors = loadAbsolutePositionsCache();
	}
	
	/**
	 * @param aoi - the area of the extracted data
	 * @param datum - earth model
	 * @param res - samples per lat/lon
	 * @return
	 */
	private Vector3 [][] loadAbsolutePositionsCache()
	{
		int h = (int) ( (coverage.getMaxLat() - coverage.getMinLat()) * res );
		int w = (int) ( (coverage.getMaxLon() - coverage.getMinLon()) * res );
		
		Vector3 [][] output = new Vector3[w][h];
		
		readTiles(coverage, tile -> appendTile(output, tile, res));

		return output;
	}
	
	public void iterateWindows(int dx, int dy, DEMVectorsConsumer consumer)
	{
		for(int x = 0; x < vectors.length; x += dx)
			for(int y = 0; y < vectors[0].length; y += dy)
			{
				consumer.consumeWindow(new ArrayWindow<>(vectors, x, y, 
						Math.min(dx, vectors.length-x), Math.min(dy,  vectors[0].length-dy)));
			}
	}
	
	protected void appendTile(Vector3 [][] output, DEMTile tile, int res)
	{
		
		File demCacheFile = new File(getCacheFilePath(tile, res));

		float minLon = tile.getCoverage().getMinLon();
		float maxLon = tile.getCoverage().getMaxLon();
		float minLat = tile.getCoverage().getMinLat();
		float maxLat = tile.getCoverage().getMaxLat();
		
		float sLon = (maxLon - minLon) / res;
		float sLat = (maxLat - minLat) / res;
		
		int [][] heightmap = null;
		if( !demCacheFile.exists() )
		{
		
			heightmap = tile.readHeightmap( res );

					
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
		
		int sy = (int) ( (tile.getCoverage().getMinLat() - coverage.getMinLat()) * res );
		int sx = (int) ( (tile.getCoverage().getMinLon() - coverage.getMinLon()) * res );
		
		for(int x = 0; x < res; x ++)
			for(int y = 0; y < res; y ++)
			{
				float lat = maxLat - y * sLat;
				float lon = minLon + x * sLon;
				double earthRadius = GeoUtil.getEarthRadiusKm(lat, datum);
				
				// if no heightmap provided, assuming it is MSL
				if( heightmap != null )
					earthRadius += heightmap[x][y] / 100f;
				
				meteo.util.Vector3 pos = meteo.util.Vector3.GEODETIC(earthRadius, lat, lon);
				
				output[sx+x][sy+y] = new Vector3((float)pos.x(), (float)pos.y(), (float)pos.z());
				
			}
		

	}

	private String getCacheFilePath(DEMTile tile, int res)
	{
		return new StringBuilder() 
				.append(cfg.getCacheDir()).append("/").append(res).append("/")
				.append("dsm_dem_").append(name).append("_").append(res)
				.append("_").append((int)tile.getCoverage().getMinLat())
				.append("_").append((int)tile.getCoverage().getMinLon())
				
				.append(".dsmgrid")
				.toString();
	}
}
