package com.ewp.crm.controllers;

import com.ewp.crm.service.interfaces.MessageService;
import com.ewp.crm.service.interfaces.SMSInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/client")
public class MessageInfoController {

	private final MessageService messageService;
	private final SMSInfoService smsInfoService;

	@Autowired
	public MessageInfoController(SMSInfoService smsInfoService, MessageService messageService) {
		this.smsInfoService = smsInfoService;
		this.messageService = messageService;
	}

	@GetMapping("message/info/{messageId}")
	public ModelAndView showMessageInfo(@PathVariable("messageId") long id) {
		ModelAndView modelAndView = new ModelAndView("blank_message-info");
		modelAndView.addObject("message", messageService.getById(id));
		return modelAndView;
	}

	@GetMapping("/sms/info/{smsId}")
	public ModelAndView showSMSInfo(@PathVariable("smsId") long id) {
		ModelAndView modelAndView = new ModelAndView("sms-info");
		modelAndView.addObject("sms", smsInfoService.getById(id));
		return modelAndView;
	}
}