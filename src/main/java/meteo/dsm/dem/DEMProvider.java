package meteo.dsm.dem;

import com.badlogic.gdx.math.Vector3;

import lombok.extern.slf4j.Slf4j;
import meteo.dsm.DSMCfg;
import meteo.util.geodesy.Datum;
import meteo.util.geodesy.GeoUtil;
import meteo.util.sampling.MinMaxSampler;
import midas.core.spatial.AOI;


@Slf4j
public abstract class DEMProvider
{
	
	public abstract void readTiles(AOI coverage, DEMTileConsumer consumer);
	
	protected DSMCfg cfg;
	
	private AOI coverage;
	private Datum datum;
	private int cacheRes;
	
	private int w, h;

	private Vector3[][] vectors;
	
	MinMaxSampler mmx = new MinMaxSampler();
	MinMaxSampler mmy = new MinMaxSampler();
	MinMaxSampler mmz = new MinMaxSampler();
	
	/**
	 * Creates a new DEM data provider for specified coverage area 
	 * @param name
	 * @param cfg
	 * @param coverage
	 * @param datum
	 * @param res - grid samples per lat/lon unit
	 */
	protected DEMProvider(DSMCfg cfg, AOI coverage, Datum datum, int cacheRes)
	{
		this.cfg = cfg;
		this.coverage = coverage;
		this.datum = datum;
		this.cacheRes = cacheRes;
		this.h = (int) ( (coverage.getMaxLat() - coverage.getMinLat()) * cacheRes );
		this.w = (int) ( (coverage.getMaxLon() - coverage.getMinLon()) * cacheRes );
	}
	
	public void init() {
		this.vectors = loadAbsolutePositionsCache();
	}
	
	/**
	 * @return
	 */
	private Vector3 [][] loadAbsolutePositionsCache()
	{
		
		Vector3 [][] output = new Vector3[w][h];
		
		readTiles(coverage, tile -> appendTile(output, tile, cacheRes));

		return output;
	}
	
	public void iterateWindows(int dx, int dy, DEMWindowConsumer consumer)
	{
		for(int x = 0; x < w; x += dx)
			for(int y = 0; y < h; y += dy)
			{
				float latCenter = coverage.getMinLat() + y / cacheRes + 0.5f;
				float lonCenter = coverage.getMinLon() + x / cacheRes + 0.5f;
				
				consumer.consumeWindow(new DEMWindow(vectors, x, y, 
						Math.min(dx, w-x), Math.min(dy, h-y),
						new AOI(latCenter, lonCenter, 0.5f, 0.5f)));

			}
	}
	
	protected void appendTile(Vector3 [][] output, DEMTile tile, int res)
	{
		
		int [][] heightmap = tile.getHeightmap(res);
		
		// convert heightmap to earth cartesian coordinates:

		float minLon = tile.getCoverage().getMinLon();
		float maxLon = tile.getCoverage().getMaxLon();
		float minLat = tile.getCoverage().getMinLat();
		float maxLat = tile.getCoverage().getMaxLat();
		
		float sLon = (maxLon - minLon) / res;
		float sLat = (maxLat - minLat) / res;
		
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
				
				mmx.put(pos.xf());
				mmy.put(pos.yf());
				mmz.put(pos.zf());
				
				if( pos.x() < 0 || pos.y() < 0 ||pos.z() < 0)
					System.out.println("hiuh");
				
				output[sx+x][sy+y] = new Vector3((float)pos.x(), (float)pos.y(), (float)pos.z());
				
			}
	}

}
