package meteo.dsm.dem.alos;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;

import javax.media.jai.PlanarImage;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.imgscalr.Scalr;
import org.junit.Test;

import meteo.dsm.dem.alos.AlosCfg;
import meteo.dsm.dem.alos.AlosTile;
import midas.visual.gis.GISResources;
import midas.visual.gis.MapRenderer;
import midas.visual.gis.PoliticalMapLayerConf;
import midas.visual.util.BufferedCanvas;

public class TestAlosDSMTile
{
	
	
	GISResources gis = new GISResources();
	
	MapRenderer mr = new MapRenderer(gis.getBordersFeatures());
	

	@Test public void testTileRead() throws IOException
	{
		AlosCfg cfg = new AlosCfg();
		
		AlosTile tile = new AlosTile(cfg, 32, 34);
		
		GridCoverage2D grid = tile.readDSMGrid();
		
		Rectangle2D bounds2D = grid.getEnvelope2D().getBounds2D();
		
		
		
		GridGeometry2D geometry = grid.getGridGeometry();
		
		PlanarImage image = (PlanarImage) grid.getRenderedImage();
	
		System.out.println(image.getWidth() + "x" + image.getHeight());
		
		Raster dsmRaster = image.getData();
		
		BufferedImage dsmImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		
		int [] val = new int[1];
		int min = 0;
		int max = 0;
		for(int x = 0; x < image.getWidth(); x ++)
			for(int y = 0; y < image.getHeight(); y ++)
			{
				dsmRaster.getPixel(x, y, val);
				if( val[0] > max )
					max = val[0];
			}
		for(int x = 0; x < image.getWidth(); x ++)
			for(int y = 0; y < image.getHeight(); y ++)
			{
				dsmRaster.getPixel(x, y, val);
				int normVal = (int)( 255f * val[0] / max );
				if( normVal >= 0)
					dsmImage.setRGB(x, y, new Color(normVal,normVal,normVal).getRGB());
			}

		int outputWidth = 1024;
		int outputHeight = 1024;
		
		BufferedImage scaledDSM = Scalr.resize(dsmImage, outputWidth, outputHeight);
	
		BufferedCanvas canvas = new BufferedCanvas(outputWidth, outputHeight, BufferedImage.TYPE_INT_ARGB);
		
		
		canvas.init(Color.BLACK);
		
		canvas.g2d.drawImage(scaledDSM, 0,0, outputWidth, outputHeight, 0, 0,outputWidth, outputHeight, null);
		PoliticalMapLayerConf config = new PoliticalMapLayerConf();
		
		mr.renderMap(canvas.g2d(), config, tile.getCoverage(), outputWidth, outputHeight);
		canvas.writeImage(new File(tile.getTileId() + ".png"), "png");
		
	}
}
