package meteo.dsm.dem.alos;

import java.io.IOException;

import org.junit.Test;

import meteo.dsm.dem.alos.AlosCfg;
import meteo.dsm.dem.alos.AlosTile;
import meteo.dsm.landuse.LanduseProvider;
import meteo.dsm.landuse.globcover.GlobCover;
import midas.visual.gis.GISResources;
import midas.visual.gis.MapRenderer;

public class TestAlosDSMToVector
{
	
	
	GISResources gis = new GISResources();
	
	MapRenderer mr = new MapRenderer(gis.getBordersFeatures());
	

	@Test public void testTileRead() throws IOException
	{
		AlosCfg cfg = new AlosCfg();
		
		AlosTile tile = new AlosTile(cfg, 29, 27);
		
//		GridCoverage2D grid = tile.readDSMGrid();
		LanduseProvider landuse = new GlobCover();
		
		tile.toVectorArray(256, landuse);
		
	}
}
