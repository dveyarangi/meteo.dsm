package meteo.dsm.vis3d;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.math.Vector3;

import meteo.util.geodesy.Datum;
import meteo.util.geodesy.GeoUtil;

public class CoordinateGrid
{
	
	Renderable grid;
	
	Mesh mesh;

	private int posPos, norPos, colPos, texPos;
	
	public CoordinateGrid(Datum datum, int lats, int lons, Environment environment)
	{
		
		VertexAttributes attributes = MeshBuilder.createAttributes(
				Usage.Position | Usage.Normal | Usage.ColorUnpacked | Usage.TextureCoordinates );
		
		int dlat = 180 / lats;
		int dlon = 360 / lons;
		
		int latVectors = 0;
		List <List <Vector3>> latLines = new ArrayList <> ();
		List <List <Vector3>> lonLines = new ArrayList <> ();
		
		for(int lat = -90; lat <= 90; lat += dlat )
		{
			double earthRadius = GeoUtil.getEarthRadiusKm(lat, datum);

			List <Vector3> latLine = new ArrayList <> ();
			for(int lon = 0; lon <= 360; lon += dlon )
			{
				meteo.util.Vector3 pos = meteo.util.Vector3.GEODETIC(earthRadius, lat+dlat/100, lon);
				latLine.add(new Vector3(pos.xf(), pos.yf(), pos.zf()));
				
				pos = meteo.util.Vector3.GEODETIC(earthRadius, lat-dlat/30f, lon);
				latLine.add(new Vector3(pos.xf(), pos.yf(), pos.zf()));
				
			}
			latVectors += latLine.size();
			latLines.add(latLine);
		}
		
		int numVertices = latVectors;
		int numIndices = latVectors * 3;
		int stride = attributes.vertexSize / 4;		
		
		float [] vx = new float[numVertices * stride];
		short [] ix = new short[numIndices];

		
		int v=0;
		int i=0;
		this.posPos = attributes.getOffset(Usage.Position, -1);
		this.norPos = attributes.getOffset(Usage.Normal, -1);
		this.texPos = attributes.getOffset(Usage.TextureCoordinates, -1);
		this.colPos = attributes.getOffset(Usage.ColorUnpacked, -1);
	
		short lineStartIdx = 0;
		Color color = Color.GREEN;
		short vertexIdx = 0;
		for(List <Vector3> latLine : latLines)
		{
			lineStartIdx = vertexIdx;
			for(int idx = 0; idx < latLine.size(); idx += 2)
			{
				Vector3 v1 = latLine.get(idx+0);
				
				setVertex(vx, v, v1, color, (float)idx/latLine.size(), 0);
				v += stride;
				
				Vector3 v2 = latLine.get(idx+1);
				setVertex(vx, v, v2, color, (float)idx/latLine.size(), 1);
				v += stride;
				
				short i0 = (short) ( vertexIdx+0 );
				short i1 = (short) ( vertexIdx+1 );
				short i2 = (short) ( idx <latLine.size()-2 ? vertexIdx+2 : lineStartIdx+0 );
				short i3 = (short) ( idx <latLine.size()-2 ? vertexIdx+3 : lineStartIdx+1 );
				ix[i++] = i0;
				ix[i++] = i1;
				ix[i++] = i2;
				ix[i++] = i1;
				ix[i++] = i3;
				ix[i++] = i2;
						
						
				vertexIdx+=2;
			}
			
		}
		
		
		mesh = new Mesh(true, numVertices, numIndices, attributes);
		mesh.setVertices(vx);
		mesh.setIndices(ix);
		
		grid = new Renderable();
		grid.environment = environment;
		grid.meshPart.mesh = mesh;
		grid.meshPart.primitiveType = GL20.GL_TRIANGLES;
		grid.meshPart.offset = 0;
		grid.meshPart.size = numIndices;
		grid.material = new Material(/*TextureAttribute.createDiffuse(texture)*/);
		
		grid.meshPart.update();
		//grid.material.set(new IntAttribute(IntAttribute.CullFace, 0));
		
	}
	
	private void setVertex( float[] vx, int vi, Vector3 vec, Color color, float u, float v )
	{
		if(posPos >= 0 ) {
			vx[posPos+vi+0] = vec.x;
			vx[posPos+vi+1] = vec.y;
			vx[posPos+vi+2] = vec.z;
		} 
		if( colPos >= 0) {
			vx[colPos+vi+0] = color.r;
			vx[colPos+vi+1] = color.g;
			vx[colPos+vi+2] = color.b;
			vx[colPos+vi+3] = color.a;
		}
		if( norPos >= 0) {
			vec.nor();
			vx[norPos+vi+0] = vec.x;
			vx[norPos+vi+1] = vec.y;
			vx[norPos+vi+2] = vec.z;
		}
		if( texPos >= 0) {
			vx[texPos+vi+0] = u;
			vx[texPos+vi+1] = v;
		}
	}

	public void render(ModelBatch batch, Environment environment)
	{
		
		batch.render(grid);
	}
}
