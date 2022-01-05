package meteo.dsm.dem;

import com.badlogic.gdx.math.Vector3;

import lombok.Getter;
import meteo.util.sampling.ArrayWindow;
import meteo.util.sampling.MinMaxSampler;
import midas.core.spatial.AOI;

public class DEMWindow extends ArrayWindow <DEMValue>
{
	
	@Getter private AOI coverage;
	
	MinMaxSampler mmx;
	MinMaxSampler mmy;
	MinMaxSampler mmz;

	public DEMWindow( DEMValue[][] arr, int xoff, int yoff, int sx, int sy, AOI coverage )
	{
		super(arr, xoff, yoff, sx, sy);
		this.coverage = coverage;
		
		//extractMinMax();
	}
	
	Vector3 dx = new Vector3();
	Vector3 dy = new Vector3();


	public Vector3 normalAt(int x, int y, Vector3 out)
	{
		int w = arr.si()-1;
		int h = arr.sj()-1;
		x = xoff+x;
		y = yoff+y;
		Vector3 above = arr.at(x, y > 0 ? y-1 : y).getCartesianPos();
		Vector3 below = arr.at(x, y < h ? y+1 : y).getCartesianPos();
		Vector3 right = arr.at(x < w ? x+1 : x, y).getCartesianPos();
		Vector3 leftt = arr.at(x > 0 ? x-1 : x, y).getCartesianPos();
		
		dy.set(below).sub(above);
		dx.set(leftt).sub(right);
		
		out = out.set(dx).crs(dy).nor();//.scl(-1);
		
		return out;
	}
	

	
/*	private void extractMinMax()
	{
		if( mmx != null) return;
		mmx = new MinMaxSampler();
		mmy = new MinMaxSampler();
		mmz = new MinMaxSampler();
		for(int i = 0; i < arr.length; i ++)
			for(int j = 0; j < arr[0].length; j ++)
			{
				mmx.put(arr[i][j].getCartesianPos().x);
				mmy.put(arr[i][j].getCartesianPos().y);
				mmz.put(arr[i][j].getCartesianPos().z);
			}
	}*/
}
