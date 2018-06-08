package com.pinyougou.search.service.impl;

import java.util.List;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.search.service.ItemSearchService;

@Component
public class ItemSearchListener implements MessageListener {
	
	@Autowired
	private ItemSearchService itemSearchService;

	@Override
	public void onMessage(Message message) {
		TextMessage textMessage = (TextMessage) message;
		try {
			List<TbItem> list = JSON.parseArray(textMessage.getText(), TbItem.class);
			System.out.println("监听到消息了"+list);
			itemSearchService.importList(list);
			System.out.println("监听类导入到solr索引库");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
