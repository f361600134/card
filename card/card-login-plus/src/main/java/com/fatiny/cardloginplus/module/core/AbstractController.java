package com.fatiny.cardloginplus.module.core;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.fatiny.cardloginplus.common.result.IResult;
import com.fatiny.cardloginplus.domain.entity.Account;
import com.fatiny.cardloginplus.domain.entity.OrderInfo;
import com.fatiny.cardloginplus.domain.entity.OrderStatusEnum;
import com.fatiny.cardloginplus.domain.entity.ServerStatus;
import com.fatiny.cardloginplus.module.core.annotation.ParameterMapping;
import com.fatiny.cardloginplus.module.core.base.AbstractLoginParam;
import com.fatiny.cardloginplus.module.core.base.IExchargeParam;
import com.fatiny.cardloginplus.module.core.base.PreOrderParam;
import com.fatiny.cardloginplus.module.core.support.AuthResult;
import com.fatiny.cardloginplus.module.core.support.ErrorCodeEnum;
import com.fatiny.cardloginplus.module.core.support.ExchargeResult;
import com.fatiny.cardloginplus.module.core.support.OrderResult;
import com.fatiny.cardloginplus.module.core.support.ResultCode;
import com.fatiny.cardloginplus.service.AccountService;
import com.fatiny.cardloginplus.service.OrderService;
import com.fatiny.cardloginplus.service.ServerService;
import com.fatiny.cardloginplus.util.CommonUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractController implements IController {

	//public final Logger log = LoggerFactory.getLogger(this.getClass());

	@Resource
	protected AccountService accountService;
	
	@Resource
	protected ServerService serverService;
	
	@Resource
	protected OrderService orderService;

	/**
	 * 登录业务操作
	 * @param params
	 * @param response
	 * @return  
	 * @return IResult  
	 * @date 2019年4月28日下午5:21:15
	 */
	@Override
	public IResult login(HttpServletRequest request, HttpServletResponse response){
		ParameterMapping parameterMapping = this.getClass().getAnnotation(ParameterMapping.class);
		if (parameterMapping == null || parameterMapping.loginParam() == null) {
			log.info("登录参数配置为空, 请检查是否配置[@AuthChannel]注解");
			return AuthResult.build(ErrorCodeEnum.IllEGAL_PARAMS);
		}
		try {
			Map<String, String[]> paramMap = request.getParameterMap();
			Object loginParam = CommonUtil.convertMap(parameterMapping.loginParam(), paramMap);
			IResult result = checkParams(loginParam);
			if (!result.success()) {
				return AuthResult.build(ErrorCodeEnum.IllEGAL_PARAMS);
			}
			result = sdkLogin(loginParam);
			if (!result.success()) {
				log.info("sdk校验出错, loginParam:{}", request.getParameterMap());
				return result;
			}
			//校验参数成功, 获取角色信息
			AbstractLoginParam absloginParam = (AbstractLoginParam) loginParam;
			//为了支持所有平台,所以这里需要生成唯一账号
			String userName = accountService.createUserName(absloginParam);
			Account account = accountService.getAccount(userName);
			if(account == null){//不存在则创建
				account= accountService.createAccount(absloginParam, userName);
				if(account==null)
				{
					log.error("create account error! userName={}, sdkUserName={}, os={}, ch={}",
							userName, absloginParam.getChannelUname(), absloginParam.getOs(), absloginParam.getCh());
					return AuthResult.build(ErrorCodeEnum.ERROR_ACCOUNT);
				}
			}
			log.info("userName:{}, account:{}", userName, (account == null));
			//认证成功
			if(account.getIsBan()){
				return AuthResult.build(ErrorCodeEnum.ERROR_BAN);
			}
			//缓存session
			String sskey = accountService.createSessionKey(userName);
			//获取上次登录服务器或推荐服务器
			int lastServerId = account.getLastServer();
			ServerStatus server = this.serverService.getServer(lastServerId);
			if(server == null)
				server = this.serverService.getRecommandServer(absloginParam.getCh());
			//构建成功消息
			result = AuthResult.success(absloginParam.getInputUname(), userName, sskey, server);
			log.info("login result:{}", JSON.toJSON(result));
			return result;
		} catch (Exception e) {
			log.error("解析配置失败", e);
			return AuthResult.build(ErrorCodeEnum.ERROR_RUNNING);
		}
	}

	public IResult createOrder(HttpServletRequest request, HttpServletResponse response) {
		ParameterMapping parameterMapping = this.getClass().getAnnotation(ParameterMapping.class);
		if (parameterMapping == null) {
			log.info("登录参数配置为空, 请检查是否配置[@ParameterMapping]注解");
			return OrderResult.build(ErrorCodeEnum.ERROR_RUNNING);
		}
		try {
			PreOrderParam param = (PreOrderParam) CommonUtil.convertMap(parameterMapping.orderParam(), request.getParameterMap());
			IResult result = checkParams(param);
			if (result.success()) {
				OrderInfo orderInfo = orderService.createOrder(param);
				if (orderInfo != null)
					return sdkOrder(orderInfo);
				else {
					return OrderResult.build(ErrorCodeEnum.ERROR_ORDER_CREATE);
				}
			} else {
				return OrderResult.build(ErrorCodeEnum.IllEGAL_PARAMS);
			}
		} catch (Exception e) {
			log.error("创建订单出错:{}", e);
			return OrderResult.build(ErrorCodeEnum.ERROR_ORDER_CREATE);
		}
	}

	/**
	 * sdk校验
	 * @param object
	 * @return  
	 * @return boolean
	 * @date 2019年4月29日下午7:57:28
	 */
	public abstract IResult sdkLogin(Object object);

	/**
	 * sdk创建订单验证
	 * 
	 * @param param
	 * @return
	 * @return boolean
	 * @date 2019年1月23日下午7:26:26
	 */
	public IResult sdkOrder(OrderInfo order) {
		return OrderResult.build(ErrorCodeEnum.SUCCESS);
	}

	/**
	 * sdk充值验证
	 * @param object
	 * @param orderInfo
	 * @return  
	 * @return IResult  
	 * @date 2019年4月30日下午3:17:03
	 */
	public abstract IResult sdkExcharge(IExchargeParam object, OrderInfo orderInfo);
	

	/**
	 *
	 * @return 0：接收成功 1:签名错误 2：未知错误
	 */
	@Override
	public String excharge(HttpServletRequest request, HttpServletResponse response) {
		String resultCode = null;
		info("This is AbstractController's excharge method");
		Map<String, String[]> paramMap = request.getParameterMap();
		IExchargeParam param = null;
		try {
			ParameterMapping parameterMapping = this.getClass().getAnnotation(ParameterMapping.class);
			if (parameterMapping == null || parameterMapping.loginParam() == null) {
				info("充值参数配置为空, 请检查是否配置@ParameterMapping参数");
				return ResultCode.CODE_RINNING_ERROR;// 返回系统内部的错误信息
			}
			param = (IExchargeParam) CommonUtil.convertMap(parameterMapping.exchargeParam(), paramMap);
			IResult result = checkParams(param);
			if (result != null) {
				return ResultCode.CODE_RINNING_ERROR;// 返回系统内部的错误信息
			}
			// 获取到有效订单信息
			OrderInfo orderInfo = orderService.getPayOrder(param.getGameOrderId());
			if (orderInfo != null) {
				// 验证订单信息
				IResult businessResult = sdkExcharge(param, orderInfo);
				if (businessResult.success()) {
					// 兑换游戏币
					orderService.exchangeGameMoney(orderInfo);
				} else {
					info("充值回调数据校验错误, IExchargeParam:{}", param);
				}
				resultCode = businessResult.result();
				orderInfo.setState(OrderStatusEnum.Order_Paid_Exchanging);
				orderInfo.setChannelOrderId(param.getChOrderId());
			} else {
				resultCode = ResultCode.CODE_ORDER_ERROR;
				info("充值失败订单有误, IExchargeParam:{}", param);
			}
		} catch (Exception e) {
			resultCode = ResultCode.CODE_RINNING_ERROR;
			info("充值回调出现异常, IExchargeParam:{}", param);
			error("充值回调出现异常, e:{}", e);
		}
		return resultCode;
	}
	
	/**
	 * 判断参数对象
	 * 
	 * @param params
	 * @return
	 * @return IResult
	 * @date 2018年9月25日上午10:22:32
	 */
	public IResult checkParams(Object object) {
		Class<?> clazz = object.getClass();
		Set<Field> fieldSet = new HashSet<>();
		while (clazz != null) {
			fieldSet.addAll(Arrays.asList(clazz.getDeclaredFields()));
			clazz = clazz.getSuperclass();
		}
		boolean bool = false;
		try {
			StringBuilder sb = new StringBuilder();
			for (Field field : fieldSet) {
				field.setAccessible(true);
				String name = field.getName();
				Object obj = field.get(object);
				if (obj == null) {
					info("校验失败, 参数为空, name:{}", name);
					bool = true;
				}
				sb.append(name).append("=").append(obj).append(", ");
			}
			sb.deleteCharAt(sb.length() - 1);
			info("参数信息, param:{}", sb.toString());
		} catch (Exception e) {
			error("校验参数异常, param:{}, e:{}", object, e);
		}
		return bool ? ExchargeResult.build(ErrorCodeEnum.IllEGAL_PARAMS) : null;
	}

	/**
	 * 判断参数
	 * 
	 * @param params
	 * @return
	 * @return IResult
	 * @date 2018年9月25日上午10:22:32
	 */
	public IResult checkParams(String... params) {
		boolean bool = StringUtils.isAnyBlank(params);
		return bool ? ExchargeResult.build(ErrorCodeEnum.IllEGAL_PARAMS) : null;
	}

	public void info(String infoMsg, Object... params) {
		if (log.isInfoEnabled()) {
			log.info(infoMsg, params);
		}
	}

	public void error(String infoMsg, Object... params) {
		if (log.isErrorEnabled()) {
			log.error(infoMsg, params);
		}
	}

	public void debug(String infoMsg, Object... params) {
		if (log.isDebugEnabled()) {
			log.debug(infoMsg, params);
		}
	}

}
