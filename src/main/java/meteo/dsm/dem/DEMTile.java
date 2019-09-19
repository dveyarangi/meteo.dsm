package meteo.dsm.dem;

import midas.core.spatial.AOI;

public interface DEMTile
{
	public AOI getCoverage();

	public int[][] readHeightmap(int res);
}
