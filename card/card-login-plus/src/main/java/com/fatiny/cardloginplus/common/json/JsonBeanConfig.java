package com.fatiny.cardloginplus.common.json;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;

import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.fatiny.cardloginplus.util.DateUtils;

/**
 * springboot 默认使用的是jackson作为json解析工具, 因为不擅长使用jackson,
 * 所以增加这个配置把json解析工具替换成fastjson
 * 
 * @auth Jeremy
 * @date 2018年9月21日下午2:39:44
 */
//@Configuration
@Deprecated
public class JsonBeanConfig {

	/* 注入Bean HttpMessageConverters, 以支持fastjson */
	//@Bean
	public HttpMessageConverters fastJsonHttpMessageConverters() {
		FastJsonHttpMessageConverter fastConvert = new FastJsonHttpMessageConverter();
		FastJsonConfig fastJsonConfig = new FastJsonConfig();
		fastJsonConfig.setSerializerFeatures(//
		// SerializerFeature.PrettyFormat //
		// , SerializerFeature.WriteNullStringAsEmpty //
		// , SerializerFeature.WriteNullNumberAsZero //
		// , SerializerFeature.WriteNullListAsEmpty //
		// , SerializerFeature.WriteMapNullValue//
		);
		fastJsonConfig.setDateFormat(DateUtils.FORMAT_FULL);
		// 处理中文乱码问题--无法解决json乱码	
		List<MediaType> fastMediaTypes = new ArrayList<>();
		fastMediaTypes.add(MediaType.APPLICATION_JSON);
		fastConvert.setSupportedMediaTypes(fastMediaTypes);
		fastConvert.setDefaultCharset(Charset.defaultCharset());
		fastConvert.setFastJsonConfig(fastJsonConfig);
		return new HttpMessageConverters((HttpMessageConverter<?>) fastConvert);
	}

}
