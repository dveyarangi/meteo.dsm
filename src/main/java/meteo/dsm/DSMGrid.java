package meteo.dsm;

import java.io.Serializable;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class DSMGrid implements Serializable
{
	@Getter
	Vector3 [][] vectors;
	@Getter
	Color[][] colors;
}
