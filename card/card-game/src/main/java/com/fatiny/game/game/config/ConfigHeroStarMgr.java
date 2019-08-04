package com.fatiny.game.game.config;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.fatiny.game.base.base.AnnotationConfig;
import com.fatiny.game.game.config.pojo.ConfigHeroStar;
import com.fatiny.game.game.module.common.constant.Config;
import com.fatiny.game.util.FileUtilitys;
import com.google.common.collect.Maps;

@AnnotationConfig
public class ConfigHeroStarMgr {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigActivityControlMgr.class);

	private static String filename = Config.JsonConfigPath+"ConfigHeroStar.json";
	
	private static Map<Integer, ConfigHeroStar> maps = Maps.newHashMap();
	
	public static void load(){
		String content = FileUtilitys.ReadFile(filename);
		List<ConfigHeroStar> list = JSON.parseArray(content, ConfigHeroStar.class);
		
		Map<Integer, ConfigHeroStar> temps = Maps.newHashMap();
		for(ConfigHeroStar config : list)
		{
			config.parse();
			temps.put(config.getID(), config);
		}
		maps = temps;
	}

	public static ConfigHeroStar getConfig(int id) {
		ConfigHeroStar data = maps.get(id);
		if(data==null)
		{
			LOGGER.error("shit! ce hua config error in ConfigHeroStar! fuck him! id={}", id);
		}
		return data;
	}
	
	public static boolean exist(int id) {
		return maps.containsKey(id);
	}
	
	/////////UserDefine Begin///////////
	
	public static void analyse(){
	}
	
	public static void complete(){
	}
	
	/////////UserDefine End/////////////
	
}
