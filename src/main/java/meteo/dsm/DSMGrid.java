package meteo.dsm;

import java.io.Serializable;

import com.badlogic.gdx.graphics.Color;

import lombok.AllArgsConstructor;
import lombok.Getter;
import meteo.dsm.dem.DEMWindow;

@AllArgsConstructor
public class DSMGrid implements Serializable
{
	@Getter
	DEMWindow vectors;
	@Getter
	Color[][] colors;
	
	private static final long serialVersionUID = -5447996427902563826L;
}
