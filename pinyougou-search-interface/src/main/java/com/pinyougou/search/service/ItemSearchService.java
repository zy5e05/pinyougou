package com.pinyougou.search.service;

import java.util.List;
import java.util.Map;

public interface ItemSearchService {
	/**
	 * 搜索
	 * @param searchMap
	 * @return
	 */
	public Map<String, Object> search(Map searchMap);

	void importList(List list);

	void deleteByGoodsIds(List goodsIdList);
}
