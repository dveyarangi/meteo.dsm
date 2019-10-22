package meteo.dsm.vis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;

import lombok.extern.slf4j.Slf4j;
import meteo.common.util.geodesy.Datum;
import meteo.dsm.DSMCfg;
import meteo.dsm.DSMGrid;
import meteo.dsm.dem.DEMProvider;
import meteo.dsm.dem.alos.AlosCfg;
import meteo.dsm.dem.alos.AlosProvider;
import meteo.dsm.landuse.LanduseProvider;
import meteo.dsm.landuse.globcover.GlobCover;
import meteo.viewer.IOverlay;
import midas.core.spatial.AOI;

@Slf4j
public class DSMOverlay implements IOverlay
{

	// CONFIGURATION
	
	
	private DSMCfg cfg;
    private int res = 150;
    
    // DEPENDENCIES

    DEMProvider demProvider;
    
    LanduseProvider landuse;
    
    // STATE
    
	Environment environment;
	
    List <DSMMesh> meshes = new ArrayList <>();
    
    List <Renderable> renderables;


	public DSMOverlay(DSMCfg cfg)
	{
        this.cfg = cfg;
	}
	
	
	@Override
	public void init(Environment environment, AOI coverage, Datum datum)
	{
		if( this.environment != null )
			return;
		
        AlosCfg alosCfg = new AlosCfg();
        demProvider = new AlosProvider(cfg, alosCfg, coverage, datum, res);
        demProvider.init();
        
        try
		{
			landuse = new GlobCover(cfg);
		} catch( IOException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.environment = environment;
		
		this.renderables = new ArrayList <> ();
	      
		log.debug("Preparing DSM meshes... ");
	    demProvider.iterateWindows(res, res, (window)-> {
	     	
	     	Color [][] colors = landuse.getColors( window.getCoverage(), res);
	     	DSMGrid grid = new DSMGrid(window, colors);
	   		
	 		DSMMesh field = new DSMMesh(true, grid, true, Usage.Position | Usage.Normal | Usage.ColorUnpacked | Usage.TextureCoordinates);
	
	   		Renderable ground = new Renderable();
	 		ground.environment = environment;
	 		ground.meshPart.mesh = field.mesh;
	 		ground.meshPart.primitiveType = GL20.GL_TRIANGLES;
	 		ground.meshPart.offset = 0;
	 		ground.meshPart.size = field.mesh.getNumIndices();
	 		ground.meshPart.update();
	 		ground.material = new Material(/*TextureAttribute.createDiffuse(texture)*/);
	 		ground.material.set(new IntAttribute(IntAttribute.CullFace, 0));
	 		
	 		meshes.add(field);
	 		renderables.add(ground);
	
	    });					
	}

	
	@Override
	public void renderModel( ModelBatch modelBatch )
	{
		for(int idx = 0; idx < renderables.size(); idx ++)
			modelBatch.render(renderables.get(idx));
	}
	
	@Override
	public void dispose()
	{
		for(DSMMesh mesh : meshes)
			mesh.dispose();
	}


}
