package meteo.dsm.dem.alos;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.media.jai.PlanarImage;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import meteo.dsm.DSMGrid;
import meteo.dsm.dem.DEMTile;
import meteo.dsm.landuse.LanduseProvider;
import meteo.util.sampling.ArraySubsampler;
import midas.core.spatial.AOI;
import midas.core.spatial.geo.GeoUtil;

@Slf4j
public class AlosTile implements DEMTile
{
	private AlosCfg cfg;
	
	private String filenamePreffix;
	
	@Getter private String tileId;
	
	private static final String TILE_FOLDER_FMT = "N%03dE%03d_N%03dE%03d";
	private static final String TILE_FMT = "N%03dE%03d_AVE_";
	
	@Getter private AOI coverage; 
	
	public AlosTile(AlosCfg cfg, int lat, int lon)
	{
		this.cfg = cfg;
		
		this.coverage = new AOI(lat+0.5f, lon+0.5f, 0.5f, 0.5f);
		
		this.tileId = createTileId(this.coverage);
		
		filenamePreffix = new StringBuilder()
				.append(cfg.getDataDir())
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

	
	public DSMGrid toVectorArray(int res, LanduseProvider landuse)
	{
		int [][] landuseCodes = null;
		
		File demCacheFile = null;//new File(getCacheFilePath(res, TYPE_DEM));
		File landuseCacheFile = null;//new File(getCacheFilePath(res, TYPE_LANDUSE));
		
		BufferedImage dsmImage = null;

		float minLon = coverage.getMinLon();
		float maxLon = coverage.getMaxLon();
		float minLat = coverage.getMinLat();
		float maxLat = coverage.getMaxLat();
		
		float sLon = (maxLon - minLon) / res;
		float sLat = (maxLat - minLat) / res;
		
		int [][] values = null;
		if( !demCacheFile.exists() )
		{
		
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
						origValues[x][y] = val[0];
					}

				values = ArraySubsampler.subsample(origValues, res, null);
				
			}
			
					
			demCacheFile.getParentFile().mkdirs();
			try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(demCacheFile)))
			{
				oos.writeObject(values);
				log.debug("Wrote cache file " + demCacheFile.getAbsolutePath());
				
			} catch( IOException e ) { e.printStackTrace(); }
		}
		else // has cache file
		{
			try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(demCacheFile)))
			{
				values = (int[][]) ois.readObject();
			} 
			catch( IOException e ) { e.printStackTrace(); } 
			catch( ClassNotFoundException e ) { e.printStackTrace(); }
			
		}
		
		
		
		if( !landuseCacheFile.exists()) 
		{
			landuseCodes = new int[res][res];
			
			for(int x = 0; x < res; x ++)
				for(int y = 0; y < res; y ++)
				{
					float lat = maxLat - y * sLat;
					float lon = minLon + x * sLon;
			
					landuseCodes[x][y] = landuse.getCode(lat, lon);
				}
			
			try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(landuseCacheFile)))
			{
				oos.writeObject(landuseCodes);
				log.debug("Wrote cache file " + landuseCacheFile.getAbsolutePath());
				
			} catch( IOException e ) { e.printStackTrace(); }

		}
		else {
			try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(landuseCacheFile)))
			{
				landuseCodes = (int[][]) ois.readObject();
			} 
			catch( IOException e ) { e.printStackTrace(); } 
			catch( ClassNotFoundException e ) { e.printStackTrace(); }
			
		}
			

		Vector3[][] vectors=  new Vector3[res][res];
		Color [][] colors = new Color[res][res];
		for(int x = 0; x < res; x ++)
			for(int y = 0; y < res; y ++)
			{
				float lat = maxLat - y * sLat;
				float lon = minLon + x * sLon;
				double z;
				if( values != null )
					z = GeoUtil.getEarthRadiusKm(lat) + values[x][y] / 100f;
				else
					z = GeoUtil.getEarthRadiusKm(lat);
				
				meteo.util.Vector3 pos = meteo.util.Vector3.GEODETIC(z, lat, lon);
				
				vectors[x][y] = new Vector3((float)pos.x(), (float)pos.y(), (float)pos.z());
				
				colors[x][y] = landuse.getColor(landuseCodes[x][y]);
				
				if( colors[x][y] == null)
					colors[x][y] = Color.BLACK;
				
			}
		
		return new DSMGrid(vectors, colors);

	}
	
	
	
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
					origValues[x][y] = val[0];
				}

			values = ArraySubsampler.subsample(origValues, res, null);
			
		}
		
	
		return values;
	}
}
