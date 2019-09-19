package meteo.dsm.dem;

import com.badlogic.gdx.math.Vector3;

import meteo.util.sampling.ArrayWindow;

public interface DEMVectorsConsumer
{
	public void consumeWindow(ArrayWindow <Vector3> window);
}
