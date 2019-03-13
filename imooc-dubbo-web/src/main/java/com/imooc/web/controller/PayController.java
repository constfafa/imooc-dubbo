package com.imooc.web.controller;

import com.imooc.common.utils.IMoocJSONResult;
import com.imooc.curator.utils.ZKCurator;
import com.imooc.web.service.CulsterService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @Description: 订购商品controller
 */
@Controller
public class PayController {
	
	@Autowired
	private CulsterService buyService;

	@Autowired
	private ZKCurator zkCurator;

	@RequestMapping("/index")
	public String index() {
		return "index";
	}
	
	@GetMapping("/buy")
	@ResponseBody
	public IMoocJSONResult buy(String itemId) {
		boolean isSuccess = false;

		if (StringUtils.isNotBlank(itemId)) {
			isSuccess = buyService.displayBuy(itemId);
		} else {
			return IMoocJSONResult.errorMsg("商品id不能为空");
		}
		if (isSuccess) {
			return IMoocJSONResult.ok();
		}
		return IMoocJSONResult.ok("购买失败，请再试");
	}

	@GetMapping("/buy2")
	@ResponseBody
	public IMoocJSONResult buy2(String itemId) {

		if (StringUtils.isNotBlank(itemId)) {
			buyService.displayBuy(itemId);
		} else {
			return IMoocJSONResult.errorMsg("商品id不能为空");
		}

		return IMoocJSONResult.ok();
	}

	public IMoocJSONResult isZKAlive() {
		String status = zkCurator.isZKAlive();
		return IMoocJSONResult.ok(status);
	}
}
