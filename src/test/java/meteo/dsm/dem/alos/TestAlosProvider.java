package meteo.dsm.dem.alos;

import org.junit.Test;

import meteo.dsm.DSMCfg;
import meteo.dsm.dem.DEMProvider;
import meteo.util.geodesy.Datum;
import midas.core.spatial.AOI;

public class TestAlosProvider
{
	@Test
	public void testProvider()
	{
		DSMCfg cfg = new DSMCfg();
		AlosCfg alosCfg = new AlosCfg();
		
		AOI coverage = new AOI(32,32,2,2);
		Datum datum = Datum.WGS_84;
		int resolution = 150;
		
		DEMProvider provider = new AlosProvider(cfg, alosCfg, coverage, datum, resolution);
		provider.init();
		
		provider.iterateWindows(resolution, resolution, (window) -> {
			System.out.println(window);
		});
		
		//System.out.println(output);
	}
}
