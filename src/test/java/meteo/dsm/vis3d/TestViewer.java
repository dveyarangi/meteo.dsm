package meteo.dsm.vis3d;

import meteo.dsm.DSMCfg;
import meteo.dsm.vis.DSMOverlay;
import meteo.util.geodesy.Datum;
import meteo.viewer.ViewerEngine;
import midas.core.spatial.AOI;

public class TestViewer
{
	public static void main(String ... args)
	{
		
		
 		int centerLat = 32;
 		int centerLon = 35;
 		float latSpan = 5;
 		float lonSpan = 5;
        
 		AOI coverage = new AOI(centerLat, centerLon, latSpan, lonSpan);
        Datum datum = Datum.WGS_84;
		
		ViewerEngine engine = new ViewerEngine(coverage, datum);

		
        DSMCfg cfg = new DSMCfg();
        
		engine.addOverlay(new DSMOverlay(cfg));
		
		engine.go();
	}
	
}
