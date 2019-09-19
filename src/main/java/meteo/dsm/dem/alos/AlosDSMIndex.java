package meteo.dsm.dem.alos;

import java.io.File;

public class AlosDSMIndex
{
	AlosTile [][] grid = new AlosTile[360][180];	
	
	public AlosDSMIndex(AlosCfg cfg)
	{
		initIndex( cfg );
	}

	private void initIndex(AlosCfg cfg)
	{
		File dataDir = new File(cfg.getDataDir());
		
		System.out.println(dataDir.getAbsolutePath());
		File [] files = dataDir.listFiles();
		for(File sectorDir : files)
		{
			System.out.println(sectorDir);
		}
	}

}
