package meteo.dsm.dem.alos;

import org.junit.Test;

import com.badlogic.gdx.math.Vector3;

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
		
		DEMProvider provider = new AlosProvider(cfg, alosCfg, coverage, datum);
		
		Vector3 [][] output = provider.getAbsolutePositions(150);
		
		System.out.println(output);
	}
}
