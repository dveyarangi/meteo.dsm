package meteo.dsm.dem.alos;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;

import javax.media.jai.PlanarImage;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import meteo.dsm.DSMCfg;
import meteo.dsm.dem.DEMTile;

import meteo.util.sampling.ArraySubsampler;
import midas.core.spatial.AOI;

@Slf4j
public class AlosTile extends DEMTile
{
	private AlosCfg alosCfg;
	
	private String filenamePreffix;
	
	@Getter private String tileId;
	
	private static final String TILE_FOLDER_FMT = "N%03dE%03d_N%03dE%03d";
	private static final String TILE_FMT = "N%03dE%03d_AVE_";
	
	@Getter private AOI coverage; 
	
	public AlosTile(DSMCfg cfg, AlosCfg alosCfg, int lat, int lon)
	{
		super(cfg);
		this.alosCfg = alosCfg;
		
		this.coverage = new AOI(lat+0.5f, lon+0.5f, 0.5f, 0.5f);
		
		this.tileId = createTileId(this.coverage);
		
		filenamePreffix = new StringBuilder()
				.append(alosCfg.getDataDir())
				.append("/")
				.append(createFolderName(lat, lon))
				.append("/")
				.append(tileId)
				.toString();
	}
	
	private String createFolderName( int lat, int lon )
	{
		int latWidth = 5;
		int lonWidth = 5;
		
		int folderLat = lat / latWidth * latWidth;
		int folderLon = lon / lonWidth * lonWidth;		
		
		String folderName = new StringBuilder()
			.append(String.format(TILE_FOLDER_FMT, folderLat, folderLon, folderLat + latWidth, folderLon+lonWidth))
			.toString();
		
		return folderName;
	}

	private String createTileId(AOI aoi)
	{
		return String.format(TILE_FMT, (int)aoi.getMinLat(), (int)aoi.getMinLon());
	}
	
	public GridCoverage2D readDSMGrid() throws IOException
	{
		return readGeotiff(filenamePreffix+"DSM.tif");
	}
	
	private GridCoverage2D readGeotiff(String filename) throws IOException
	{
		AlosCfg cfg = new AlosCfg();
		
		File f = new File(filename);
		if( !f.exists() )
			return null;
		ParameterValue<OverviewPolicy> policy = AbstractGridFormat.OVERVIEW_POLICY.createValue();
		policy.setValue(OverviewPolicy.IGNORE);

		ParameterValue<String> gridsize = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue();
		ParameterValue<Boolean> useJaiRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
		useJaiRead.setValue(true);

		GridCoverage2D image = new GeoTiffReader(f).read(new GeneralParameterValue[]{policy, gridsize, useJaiRead});

		Rectangle2D bounds2D = image.getEnvelope2D().getBounds2D();     
		GridGeometry2D geometry = image.getGridGeometry();
		
		return image;
	}
	

	
	public static final String TYPE_DEM = "dem";
	public static final String TYPE_LANDUSE = "landuse";


	
    private static BufferedImage resize(BufferedImage img, int width, int height) {
    	BufferedImage resizedImage = new BufferedImage(width, height, img.getType());
    	Graphics2D g = resizedImage.createGraphics();
    	g.drawImage(img, 0, 0, width, height, null);
    	g.dispose();
    	//g.setComposite(AlphaComposite.Src);
    	g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    	g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
    	g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
    	return resizedImage;
    }

	@Override
	public int[][] readHeightmap(int res)
	{
		
		int [][] values = null;
		GridCoverage2D grid;
		try
		{
			grid = readDSMGrid();
		} catch( IOException e1 )
		{
			e1.printStackTrace();
			return null;
		}
		
		if( grid != null) {
		
			PlanarImage image = (PlanarImage) grid.getRenderedImage();
			
			Raster dsmRaster = image.getData();
			
			int ow = image.getWidth(), oh = image.getHeight();
			if( ow % res != 0)
				throw new IllegalArgumentException("Requested resolution must be a divisor of " + image.getWidth());
			int [] val = new int[1];
			int [][] origValues = new int [image.getWidth()][image.getHeight()];
		
			for(int x = 0; x < ow; x ++)
				for(int y = 0; y < oh; y ++)
				{
					dsmRaster.getPixel(x, y, val);
					origValues[x][oh-y-1] = val[0];
				}

			values = ArraySubsampler.subsample(origValues, res, null);
			
		}
		
	
		return values;
	}

	public static final String NAME = "alos";
	@Override
	public String getName() { return NAME; }


}
