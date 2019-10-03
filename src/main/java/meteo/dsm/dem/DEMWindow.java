package meteo.dsm.dem;

import com.badlogic.gdx.math.Vector3;

import lombok.Getter;
import meteo.util.sampling.ArrayWindow;
import meteo.util.sampling.MinMaxSampler;
import midas.core.spatial.AOI;

public class DEMWindow extends ArrayWindow <Vector3>
{
	
	@Getter private AOI coverage;
	
	MinMaxSampler mmx;
	MinMaxSampler mmy;
	MinMaxSampler mmz;

	public DEMWindow( Vector3[][] arr, int xoff, int yoff, int sx, int sy, AOI coverage )
	{
		super(arr, xoff, yoff, sx, sy);
		this.coverage = coverage;
		
		extractMinMax();
	}
	
	Vector3 dx = new Vector3();
	Vector3 dy = new Vector3();


	public Vector3 normalAt(int x, int y, Vector3 out)
	{
		int w = sx();
		int h = sy();
		x = xoff+x;
		y = yoff+y;
		Vector3 above = arr[x][y > 0 ? y-1 : y];
		Vector3 below = arr[x][y < h ? y+1 : y];
		Vector3 right = arr[x < w ? x+1 : x][y];
		Vector3 leftt = arr[x > 0 ? x-1 : x][y];
		
		dy.set(above).sub(below);
		dx.set(right).sub(leftt);
		
		out = out.set(dx).crs(dy).nor();
		
		return out;
	}
	

	
	private void extractMinMax()
	{
		if( mmx != null) return;
		mmx = new MinMaxSampler();
		mmy = new MinMaxSampler();
		mmz = new MinMaxSampler();
		for(int i = 0; i < arr.length; i ++)
			for(int j = 0; j < arr[0].length; j ++)
			{
				mmx.put(arr[i][j].x);
				mmy.put(arr[i][j].y);
				mmz.put(arr[i][j].z);
			}
	}
}
