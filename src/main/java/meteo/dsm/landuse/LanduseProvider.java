package meteo.dsm.landuse;

import com.badlogic.gdx.graphics.Color;

public interface LanduseProvider
{
	int getCode(float lat, float lon);
	Color getColor(int code);
}
