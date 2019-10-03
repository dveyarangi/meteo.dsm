package meteo.dsm.vis3d;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import lombok.extern.slf4j.Slf4j;
import meteo.dsm.DSMCfg;
import meteo.dsm.DSMGrid;
import meteo.dsm.dem.DEMProvider;
import meteo.dsm.dem.alos.AlosCfg;
import meteo.dsm.dem.alos.AlosProvider;
import meteo.dsm.landuse.LanduseProvider;
import meteo.dsm.landuse.globcover.GlobCover;
import meteo.util.geodesy.Datum;
import meteo.util.geodesy.GeoUtil;
import midas.core.spatial.AOI;

@Slf4j
public class DSM3DSetup implements ApplicationListener
{
	public Environment environment;
    public PerspectiveCamera cam;
    public SatelliteCameraController camController;
    
    public ModelBatch modelBatch;
    public Model model;
    public ModelInstance instance;

    List <DSMMesh> fields = new ArrayList <>();
    List <Renderable> groundTiles = new ArrayList <>();
 
    
    Datum datum = Datum.WGS_84;
    
    LanduseProvider landuse;
    
    Globe globe;
    
	@Override
	public void create()
	{
		
		DSMCfg cfg = new DSMCfg();
		
 		int centerLat = 32;
 		int centerLon = 35;
 		float latSpan = 5;
 		float lonSpan = 5;

        
        AOI coverage = new AOI(centerLat, centerLon, latSpan, lonSpan); 
        
       /* int minLon=32;
        int maxLon=34;
        int minLat=32;
        int maxLat=33;		*/
        
        try
		{
			landuse = new GlobCover(cfg);
		} catch( IOException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        float msl = (float) GeoUtil.getEarthRadiusKm(coverage.getLatCenter(), datum);
		meteo.util.Vector3 camPos = meteo.util.Vector3.GEODETIC(msl+500, coverage.getLatCenter(), coverage.getLonCenter());
		meteo.util.Vector3 centerPos = meteo.util.Vector3.GEODETIC(msl, coverage.getLatCenter(), coverage.getLonCenter());

        
		Vector3 camera = new Vector3(camPos.xf(),camPos.yf(),camPos.zf());
        Vector3 center = new Vector3(centerPos.xf(),centerPos.yf(),centerPos.zf());

		environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.4f, 0.4f, 0.4f, new Vector3(center).sub(camera)));
        

		cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.position.set(camera);
        cam.near = 1f;
        cam.far = 30000f;
        camController = new SatelliteCameraController(cam, coverage.getLatCenter(), coverage.getLonCenter(), msl+1000 );

        Gdx.input.setInputProcessor(camController);
        
        //GL14.glShadeModel(GL14.GL_SMOOTH);
        //Gdx.gl.glHint(Gdx.gl.GL_POLYGON_SMOOTH_HINT, Gdx.gl.GL_NICEST);
        
        modelBatch = new ModelBatch();
        
        ModelBuilder modelBuilder = new ModelBuilder();
        model = modelBuilder.createBox(5f, 5f, 5f, 
            new Material(ColorAttribute.createDiffuse(Color.GREEN)),
            Usage.Position | Usage.Normal);
        instance = new ModelInstance(model);
        
        int res = 150;
        
        AlosCfg alosCfg = new AlosCfg();
        DEMProvider demProvider = new AlosProvider(cfg, alosCfg, coverage, datum, res);
        demProvider.init();
        
        demProvider.iterateWindows(res, res, (window)-> {
        	
        	Color [][] colors = landuse.getColors( window.getCoverage(), res);
        	DSMGrid grid = new DSMGrid(window, colors);
      		log.debug("Loading mesh covering {}", window.getCoverage().toString());
      		
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
			
			fields.add(field);
			groundTiles.add(ground);
       });
        
        this.globe = new Globe(datum, environment);

    }


	@Override
	public void resize( int width, int height ) { }

	@Override
	public void render()
	{
		
		camController.update();
		
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | (Gdx.graphics.getBufferFormat().coverageSampling?GL20.GL_COVERAGE_BUFFER_BIT_NV:0));

        
        modelBatch.begin(cam);
        for(Renderable ground : groundTiles)
        	modelBatch.render(ground);
 //       modelBatch.render(instance, environment);
        modelBatch.end();
       
        Gdx.gl.glEnable(0x0B41);
        modelBatch.begin(cam);
        globe.render(modelBatch, environment);
        modelBatch.end();
        Gdx.gl.glDisable(0x0B41);
     

	}

	@Override
	public void pause() { }

	@Override
	public void resume() { }

	@Override
	public void dispose()
	{
		modelBatch.dispose();
		model.dispose();
		for(DSMMesh mesh : fields)
			mesh.dispose();
	}
	

	
	public static VertexAttributes createAttributes (long usage) {
		final Array<VertexAttribute> attrs = new Array<VertexAttribute>();
		if ((usage & Usage.Position) == Usage.Position)
			attrs.add(new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE));
		if ((usage & Usage.ColorUnpacked) == Usage.ColorUnpacked)
			attrs.add(new VertexAttribute(Usage.ColorUnpacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
		if ((usage & Usage.ColorPacked) == Usage.ColorPacked)
			attrs.add(new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
		if ((usage & Usage.Normal) == Usage.Normal)
			attrs.add(new VertexAttribute(Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE));
		if ((usage & Usage.TextureCoordinates) == Usage.TextureCoordinates)
			attrs.add(new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));
		final VertexAttribute attributes[] = new VertexAttribute[attrs.size];
		for (int i = 0; i < attributes.length; i++)
			attributes[i] = attrs.get(i);
		return new VertexAttributes(attributes);
	}

}
