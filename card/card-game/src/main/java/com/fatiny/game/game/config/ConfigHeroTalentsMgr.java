package com.fatiny.game.game.config;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.fatiny.game.base.base.AnnotationConfig;
import com.fatiny.game.game.config.pojo.ConfigHeroTalents;
import com.fatiny.game.game.module.common.constant.Config;
import com.fatiny.game.util.FileUtilitys;
import com.google.common.collect.Maps;

@AnnotationConfig
public class ConfigHeroTalentsMgr {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigActivityControlMgr.class);

	private static String filename = Config.JsonConfigPath+"ConfigHeroTalents.json";
	
	private static Map<Integer, ConfigHeroTalents> maps = Maps.newHashMap();
	
	public static void load(){
		String content = FileUtilitys.ReadFile(filename);
		List<ConfigHeroTalents> list = JSON.parseArray(content, ConfigHeroTalents.class);
		
		Map<Integer, ConfigHeroTalents> temps = Maps.newHashMap();
		for(ConfigHeroTalents config : list)
		{
			config.parse();
			temps.put(config.getID(), config);
		}
		maps = temps;
	}

	public static ConfigHeroTalents getConfig(int id) {
		ConfigHeroTalents data = maps.get(id);
		if(data==null)
		{
			LOGGER.error("shit! ce hua config error in ConfigHeroTalents! fuck him! id={}", id);
		}
		return data;
	}
	
	public static boolean exist(int id) {
		return maps.containsKey(id);
	}
	
	/////////UserDefine Begin///////////
	
	public static final int Quality_Green = 0;
	public static final int Quality_Blue = 1;
	public static final int Quality_Purple = 2;
	public static final int Quality_Golden = 3;
	
	public static void analyse(){
	}
	
	public static void complete(){
	}
	
	/**
	 * 获取武将星格品质
	 */
	public static int getQuality(int starLevel) {
		int quality = starLevel/3;
		if(quality<Quality_Green)
			return Quality_Green;
		else if(quality>Quality_Golden)
			return Quality_Golden;
		else
			return quality;
	}
	
	/**
	 * 获取武将星格数
	 */
	public static int getStar(int starLevel) {
		int lvl = starLevel%3;
		return lvl;
	}
	
	/////////UserDefine End/////////////
	
}
