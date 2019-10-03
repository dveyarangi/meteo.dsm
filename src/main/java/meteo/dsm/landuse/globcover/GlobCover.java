package meteo.dsm.landuse.globcover;

import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;

import com.badlogic.gdx.graphics.Color;

import meteo.dsm.DSMCfg;
import meteo.dsm.landuse.LanduseProvider;

public class GlobCover extends LanduseProvider
{
	
	public static final String LANDUSE_FILE = "E:/meteo/dem/GlobCover/GLOBCOVER_L4_200901_200912_V2.3.tif";
	public static final String LEGEND_FILE = "E:/meteo/dem/GlobCover/Globcover2009_Legend.csv";
	
	Map <Integer, Color> legend;
	GridCoverage2D coverage;
	Rectangle2D bounds;
	private int width;
	private int height;
	
	public GlobCover(DSMCfg cfg) throws IOException
	{
		super(cfg);
			
		File f = new File(LANDUSE_FILE);
		if( !f.exists() )
			throw new IOException("Cannot fine landuse file " + LANDUSE_FILE);
		
		ParameterValue<OverviewPolicy> policy = AbstractGridFormat.OVERVIEW_POLICY.createValue();
		policy.setValue(OverviewPolicy.IGNORE);

		ParameterValue<String> gridsize = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue();
		ParameterValue<Boolean> useJaiRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
		useJaiRead.setValue(true);

		coverage = new GeoTiffReader(f).read(new GeneralParameterValue[]{policy, gridsize, useJaiRead});

		width = coverage.getRenderedImage().getWidth();
		height = coverage.getRenderedImage().getHeight();

		bounds = coverage.getEnvelope2D().getBounds2D();
		legend = readLegend();
			
	}

	@Override
	public int getCode( float lat, float lon )
	{
		int y = (int) ( Math.round( (bounds.getMaxY() - lat) / bounds.getHeight() * height ) );
		int x = (int) ( Math.round( (lon - bounds.getMinX()) / bounds.getWidth() * width ) );
		int [] val = new int[1];
		coverage.evaluate(new GridCoordinates2D(x,y), val);
		return val[0];
	}
	
	@Override
	public Color getColor(int code)
	{
		return legend.get(code);
	}
	
	private Map <Integer, Color> readLegend()
	{
		Map <Integer, Color> legend = new HashMap <> ();
		try (BufferedReader csvReader = new BufferedReader(new FileReader(LEGEND_FILE)))
		{
			csvReader.readLine(); // skip titles
			String row;
			while ((row = csvReader.readLine()) != null) {
			    String[] data = row.split(",");
			    int code = Integer.parseInt(data[0]);
			    
			    float r = Integer.parseInt(data[2]);
			    float g = Integer.parseInt(data[3]);
			    float b = Integer.parseInt(data[4]);
			    
			    Color color = new Color(r/255f, g/255f, b/255f, 1);
			    
			    legend.put( code, color );
			    
			}
			
			//legend.put(0, new Color(0.5f, 0.5f, 0.5f, 1));
		} catch( IOException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return legend;
	}

	@Override
	public String getName() { return "globcover"; }

}
