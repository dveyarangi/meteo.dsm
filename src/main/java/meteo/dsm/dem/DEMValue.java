package meteo.dsm.dem;

import com.badlogic.gdx.math.Vector3;

import lombok.AllArgsConstructor;
import lombok.Getter;
import meteo.common.util.geodesy.PolarCoord;

@AllArgsConstructor
public class DEMValue
{
	@Getter float aboveMSLheightM;
	@Getter Vector3 cartesianPos;
	@Getter PolarCoord polarPos;
}
