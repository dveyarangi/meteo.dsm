package meteo.dsm.dem;

import com.badlogic.gdx.math.Vector3;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import meteo.dsm.DSMCfg;
import meteo.util.geodesy.Datum;
import meteo.util.geodesy.GeoUtil;
import meteo.util.geodesy.PolarCoord;
import meteo.util.sampling.MinMaxSampler;
import midas.core.spatial.AOI;


/**
 * TODO: not thread safe! due to usage of temp vectors {@link #dx} and {@link #dy}
 * @author Fima
 *
 */
@Slf4j
public abstract class DEMProvider
{
	
	public abstract void readTiles(AOI coverage, DEMTileConsumer consumer);
	
	protected DSMCfg cfg;
	
	private AOI coverage;
	private Datum datum;
	private int cacheRes;
	
	private int w, h;

	@Getter private DEMValue[][] values;
	
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
	protected DEMProvider(DSMCfg cfg, AOI cover, Datum datum, int cacheRes)
	{
		this.cfg = cfg;
		this.datum = datum;
		this.cacheRes = cacheRes;
		
		int minLat = (int) Math.floor(cover.getMinLat());
		int maxLat = (int) Math.ceil (cover.getMaxLat());
		int minLon = (int) Math.floor(cover.getMinLon());
		int maxLon = (int) Math.ceil (cover.getMaxLon());
		
		this.coverage = AOI.fromEdges(minLat, minLon, maxLat, maxLon);
		this.h = (int) ( (this.coverage.getMaxLat() - this.coverage.getMinLat()) * cacheRes );
		this.w = (int) ( (this.coverage.getMaxLon() - this.coverage.getMinLon()) * cacheRes );
	}
	
	public void init() {
		log.trace("Caching DEM data...");
		this.values = loadAbsolutePositionsCache();
	}
	
	public DEMValue at(float lat, float lon)
	{
		checkInitialized();
		int xidx = xidx(lon);
		int yidx = yidx(lat);
		return values[xidx][yidx];
		
	}
	
	public float interpolateEarthRadius( float lat, float lon, Datum datum )
	{
		float msl = getMeanSeaLevelM(lat, lon);
		double earthRadius = GeoUtil.getEarthRadiusKm(lat, datum);
		//meteo.common.util.Vector3 pos = meteo.common.util.Vector3.GEODETIC(earthRadius, lat, lon);
	
		
		return (float)(earthRadius + msl/1000f);
	}
	
	public float getMeanSeaLevelM( float lat, float lon )
	{
		int w = values.length-1;
		int h = values[0].length-1;
		int x = xidx(lon);
		int y = yidx(lat);
		
		DEMValue q00 = values[x][y];
		DEMValue q01 = values[x][y < h ? y+1 : y];
		DEMValue q10 = values[x < w ? x+1 : x][y];
		DEMValue q11 = values[x < w ? x+1 : x][y < h ? y+1 : y];
		
		PolarCoord p00 = q00.getPolarPos();
		PolarCoord p01 = q01.getPolarPos();
		PolarCoord p10 = q10.getPolarPos();
		PolarCoord p11 = q11.getPolarPos();

		// converting to lower closest index:
		double dx = p11.getLongitude() - p00.getLongitude(); 
		double dy = p11.getLatitude() - p00.getLatitude();
		
		double f1 = (p11.getLongitude() - lon) / dx * q00.aboveMSLheightM + (lon - p00.getLongitude()) / dx * q10.aboveMSLheightM;
		double f2 = (p11.getLongitude() - lon) / dx * q01.aboveMSLheightM + (lon - p00.getLongitude()) / dx * q11.aboveMSLheightM;
		
		double aa = (p11.getLatitude() - lat);
		double bb = (lat - p00.getLatitude());
		double value = (p11.getLatitude() - lat) / dy * f1 + (lat - p00.getLatitude()) / dy * f2;
		
		
		return (float)value;
	}
	
	Vector3 q00 = new Vector3();
	Vector3 q01 = new Vector3();
	Vector3 q10 = new Vector3();
	Vector3 q11 = new Vector3();
	
	
	public Vector3 interpolateNormal( float lat, float lon, Vector3 out )
	{
		int w = values.length-1;
		int h = values[0].length-1;
		int x = xidx(lon);
		int y = yidx(lat);
		q00 = normalAt(x, y, q00);
		q01 = normalAt(x, y < h ? y+1 : y, q01);
		q10 = normalAt(x < w ? x+1 : x, y, q10);
		q11 = normalAt(x < w ? x+1 : x, y < h ? y+1 : y, q11);
		DEMValue v00 = values[x][y];
		DEMValue v01 = values[x][y < h ? y+1 : y];
		DEMValue v10 = values[x < w ? x+1 : x][y];
		DEMValue v11 = values[x < w ? x+1 : x][y < h ? y+1 : y];
	
		PolarCoord p00 = v00.getPolarPos();
		PolarCoord p01 = v01.getPolarPos();
		PolarCoord p10 = v10.getPolarPos();
		PolarCoord p11 = v11.getPolarPos();

		// converting to lower closest index:
		double dx = p11.getLongitude() - p00.getLongitude(); 
		double dy = p11.getLatitude() - p00.getLatitude();
		
		double fx1 = (p11.getLongitude() - lon) / dx * q00.x + (lon - p00.getLongitude()) / dx * q10.x;
		double fx2 = (p11.getLongitude() - lon) / dx * q01.x + (lon - p00.getLongitude()) / dx * q11.x;
		double ix = (p11.getLatitude() - lat) / dy * fx1 + (lat - p00.getLatitude()) / dy * fx2;
		double fy1 = (p11.getLongitude() - lon) / dx * q00.y + (lon - p00.getLongitude()) / dx * q10.y;
		double fy2 = (p11.getLongitude() - lon) / dx * q01.y + (lon - p00.getLongitude()) / dx * q11.y;
		double iy = (p11.getLatitude() - lat) / dy * fy1 + (lat - p00.getLatitude()) / dy * fy2;
		double fz1 = (p11.getLongitude() - lon) / dx * q00.z + (lon - p00.getLongitude()) / dx * q10.z;
		double fz2 = (p11.getLongitude() - lon) / dx * q01.z + (lon - p00.getLongitude()) / dx * q11.z;
		double iz = (p11.getLatitude() - lat) / dy * fz1 + (lat - p00.getLatitude()) / dy * fz2;

		return out.set((float)ix, (float)iy, (float)iz);
	}

	
	// temp vectors
	Vector3 dx = new Vector3();
	Vector3 dy = new Vector3();

	public Vector3 normalAt(float lat, float lon)
	{
		return normalAt(lat, lon, new Vector3());
	}
	
	public Vector3 normalAt(float lat, float lon, Vector3 out)
	{
		return normalAt(xidx(lon), yidx(lat), out);
	}
	
	public Vector3 normalAt(int x, int y, Vector3 out)
	{
		
		int w = values.length-1;
		int h = values[0].length-1;
		Vector3 above = values[x][y > 0 ? y-1 : y].getCartesianPos();
		Vector3 below = values[x][y < h ? y+1 : y].getCartesianPos();
		Vector3 right = values[x < w ? x+1 : x][y].getCartesianPos();
		Vector3 leftt = values[x > 0 ? x-1 : x][y].getCartesianPos();
		
		dy.set(below).sub(above);
		dx.set(leftt).sub(right);
		
		out = out.set(dx).crs(dy).nor().scl(-1);
		
		return out;
	}
	public void iterateWindows(int dx, int dy, DEMWindowConsumer consumer)
	{
		for(int x = 0; x < w; x += dx)
			for(int y = 0; y < h; y += dy)
			{
				float latCenter = coverage.getMinLat() + y / cacheRes + 0.5f;
				float lonCenter = coverage.getMinLon() + x / cacheRes + 0.5f;
				
				consumer.consumeWindow(new DEMWindow(values, x, y, 
						Math.min(dx, w-x), Math.min(dy, h-y),
						new AOI(latCenter, lonCenter, 0.5f, 0.5f)));

			}
	}
	
	protected void appendTile(DEMValue [][] output, DEMTile tile, int res)
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
				
				if(sx+x < 0 || sx+x >= output.length) continue;
				if(sy+y < 0 || sy+y >= output[0].length) continue;
				
				float lat = minLat + y * sLat;
				float lon = minLon + x * sLon;
				double earthRadius = GeoUtil.getEarthRadiusKm(lat, datum);
				
				float aboveMSLHeightM = 0;
				if( heightmap != null ) // if no heightmap provided, assuming it is MSL
					aboveMSLHeightM = heightmap[x][y];
				
				
				earthRadius += aboveMSLHeightM/1000;
				
				meteo.util.Vector3 pos = meteo.util.Vector3.GEODETIC(earthRadius, lat, lon);
				
				mmx.put(pos.xf());
				mmy.put(pos.yf());
				mmz.put(pos.zf());
				
				if( pos.x() < 0 || pos.y() < 0 ||pos.z() < 0)
					System.out.println("hiuh");
				
				 Vector3 cartesianPos = new Vector3((float)pos.x(), (float)pos.y(), (float)pos.z());
				 output[sx+x][sy+y] = new DEMValue(aboveMSLHeightM, cartesianPos, new PolarCoord(earthRadius, lat, lon));
			}
	}
	
	/**
	 * @return
	 */
	protected DEMValue [][] loadAbsolutePositionsCache()
	{
		
		DEMValue [][] output = new DEMValue[w][h];
		
		readTiles(coverage, tile -> appendTile(output, tile, cacheRes));

		return output;
	}
	

	public int xidx(float lon) { return (int) Math.floor((lon - coverage.getMinLon()) * cacheRes); }
	public int yidx(float lat) { return (int) Math.floor((lat - coverage.getMinLat()) * cacheRes); }

	private void checkInitialized()
	{
		if(this.values == null)
			throw new IllegalStateException("DEM provider was not initialized");
	}
}
