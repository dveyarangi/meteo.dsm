package meteo.dsm.dem.alos;


import meteo.dsm.DSMCfg;
import meteo.dsm.dem.DEMProvider;
import meteo.dsm.dem.DEMTileConsumer;
import meteo.util.geodesy.Datum;
import midas.core.spatial.AOI;

public class AlosProvider extends DEMProvider
{
	
	public static final String NAME = "alos";
	private AlosCfg alosCfg;
	
	protected AlosProvider( DSMCfg cfg, AlosCfg alosCfg, AOI coverage, Datum datum )
	{
		super(NAME, cfg, coverage, datum);
		this.alosCfg = alosCfg;
	}

	@Override
	public void readTiles( AOI coverage, DEMTileConsumer consumer )
	{
		int minLat = (int) Math.floor(coverage.getMinLat());
		int maxLat = (int) Math.ceil (coverage.getMaxLat());
		int minLon = (int) Math.floor(coverage.getMinLon());
		int maxLon = (int) Math.ceil (coverage.getMaxLon());
		for(int lat = minLat; lat < maxLat; lat ++ )
			for(int lon = minLon; lon < maxLon; lon ++ )
				consumer.consume( new AlosTile(alosCfg, lat, lon) );
	}

}
