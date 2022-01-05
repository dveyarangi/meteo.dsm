package meteo.dsm.dem.alos;


import meteo.dsm.DSMCfg;
import meteo.dsm.dem.DEMProvider;
import meteo.dsm.dem.DEMTileConsumer;

import meteo.util.geodesy.Datum;
import midas.core.spatial.AOI;

public class AlosProvider extends DEMProvider
{
	
	private AlosCfg alosCfg;
	
	public AlosProvider( DSMCfg cfg, AlosCfg alosCfg, AOI coverage, Datum datum, int cacheRes )
	{
		super(cfg, coverage, datum, cacheRes);
		this.alosCfg = alosCfg;
	}

	@Override
	public void readTiles( AOI coverage, DEMTileConsumer consumer )
	{
		int minLat = (int)coverage.getMinLat()-10;
		int minLon = (int)coverage.getMinLon()-10;
		int maxLat = (int)coverage.getMaxLat()+10;
		int maxLon = (int)coverage.getMaxLon()+10;
		for(int lat = minLat; lat <= maxLat; lat ++ )
			for(int lon = minLon; lon <= maxLon; lon ++ )
				consumer.consume( new AlosTile(cfg, alosCfg, lat, lon) );
	}

}
