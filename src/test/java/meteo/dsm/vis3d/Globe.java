package meteo.dsm.vis3d;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;

import meteo.util.geodesy.Datum;

public class Globe
{
	
	private CoordinateGrid coordinateGrid;

	public Globe(Datum datum, Environment environment)
	{
		this.coordinateGrid = new CoordinateGrid(datum, 36, 36, environment);
	}
	
	public void render(ModelBatch batch, Environment environment)
	{
		coordinateGrid.render(batch, environment);
	}
}
