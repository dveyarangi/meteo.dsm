package meteo.dsm.dem.alos;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.junit.Test;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;

import meteo.dsm.dem.alos.AlosCfg;
import meteo.dsm.dem.alos.AlosDSMIndex;

public class AlosDSMIndexTest
{
	@Test public void testReadGeotiff() throws IOException
	{
		AlosCfg cfg = new AlosCfg();
		
		File f = new File(cfg.getDataDir() + "/N025E030_N030E035/N025E030_AVE_DSM.tif");

		ParameterValue<OverviewPolicy> policy = AbstractGridFormat.OVERVIEW_POLICY.createValue();
		policy.setValue(OverviewPolicy.IGNORE);

		ParameterValue<String> gridsize = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue();
		ParameterValue<Boolean> useJaiRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
		useJaiRead.setValue(true);

		GridCoverage2D image = new GeoTiffReader(f).read(new GeneralParameterValue[]{policy, gridsize, useJaiRead});

		Rectangle2D bounds2D = image.getEnvelope2D().getBounds2D();     
		GridGeometry2D geometry = image.getGridGeometry();
	}

	
	
	@Test public void testIndex()
	{
		AlosCfg cfg = new AlosCfg();
		AlosDSMIndex index = new AlosDSMIndex( cfg );
	}
}
