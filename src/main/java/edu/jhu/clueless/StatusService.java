package edu.jhu.clueless;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Chris
 * @date Feb 18, 2016
 */
@Controller()
@RequestMapping("/home")
public class StatusService {
	private String version_ = "0.1.0";

	@RequestMapping("/version")
	@ResponseBody
	public String getVersion() {
		return version_;
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public String getPage(){
		return "index";
	}
	
	@RequestMapping(value = "/socket", method = RequestMethod.GET)
	public String getWebSocket(){
		return "webSock";
	}
}
