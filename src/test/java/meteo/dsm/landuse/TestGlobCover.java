package meteo.dsm.landuse;

import java.io.IOException;

import org.junit.Test;

import meteo.dsm.DSMCfg;
import meteo.dsm.landuse.globcover.GlobCover;

public class TestGlobCover
{
	@Test
	public void testGlobCover() throws IOException
	{
		DSMCfg cfg = new DSMCfg();
		
		GlobCover cover = new GlobCover(cfg);
		
		int code = cover.getCode(32, 32);
		
		System.out.println(code);
	}
}
