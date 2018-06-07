package com.pinyougou.search.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.FilterQuery;
import org.springframework.data.solr.core.query.GroupOptions;
import org.springframework.data.solr.core.query.HighlightOptions;
import org.springframework.data.solr.core.query.HighlightQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.data.solr.core.query.SimpleHighlightQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.result.GroupEntry;
import org.springframework.data.solr.core.query.result.GroupPage;
import org.springframework.data.solr.core.query.result.GroupResult;
import org.springframework.data.solr.core.query.result.HighlightEntry;
import org.springframework.data.solr.core.query.result.HighlightPage;

import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.search.service.ItemSearchService;

@Service(timeout=5000)
public class ItemSearchServiceImpl implements ItemSearchService {

	@Autowired
	private SolrTemplate solrTemplate;
	
	@Override
	public Map<String,Object> search(Map searchMap) {
		//对关键字的空格处理
		String keyword = (String)searchMap.get("keywords");
		searchMap.put("keywords", keyword.replace(" ", ""));
		
		Map<String, Object> map=new HashMap();
		// 1. 按关键字查询 高亮显示
		map.putAll(searchList(searchMap));
		// 2. 按关键字查询商品分类
		List<String> categoryList = searchCategoryList(searchMap);
		map.put("categoryList", categoryList);
		// 3. 查询品牌和规格列表
		//3.查询品牌和规格列表
		String category= (String) searchMap.get("category");
		if(!category.equals("")){						
			map.putAll(searchBrandAndSpecList(category));
		}else{
			if(categoryList.size()>0){			
				map.putAll(searchBrandAndSpecList(categoryList.get(0)));
			}	
		}
		return map;
	}
	
	private Map searchList(Map searchMap) {
		Map map=new HashMap();
		HighlightQuery query = new SimpleHighlightQuery();
		//设置高亮域
		HighlightOptions options = new HighlightOptions().addField("item_title");
		options.setSimplePrefix("<em style='color:red'>");
		options.setSimplePostfix("</em>");
		query.setHighlightOptions(options);
		
		//1.1 关键字查询
		Criteria criteria=new Criteria("item_keywords").is(searchMap.get("keywords"));
		query.addCriteria(criteria);
		//1.2 按分类筛选
		if (!"".equals(searchMap.get("category"))) {
			Criteria filterCriteria = new Criteria("item_category").is(searchMap.get("category"));
			FilterQuery filterQuery = new SimpleFilterQuery(filterCriteria);
			query.addFilterQuery(filterQuery);
		}
		//1.3按品牌筛选
		if (!"".equals(searchMap.get("brand"))) {
			Criteria filterCriteria=new Criteria("item_brand").is(searchMap.get("brand"));
			FilterQuery filterQuery=new SimpleFilterQuery(filterCriteria);
			query.addFilterQuery(filterQuery);
		}
		//1.4过滤规格
		if (searchMap.get("spec") != null) {
			Map<String, String> specMap = (Map) searchMap.get("spec");
			for (String key : specMap.keySet()) {
				Criteria filterCriteria = new Criteria("item_spec_" + key).is(specMap.get(key));
				FilterQuery filterQuery = new SimpleFilterQuery(filterCriteria);
				query.addFilterQuery(filterQuery);
			}
		}
		//1.5 按价格过滤
		if (!"".equals(searchMap.get("price"))) {
			String[] price = ((String)searchMap.get("price")).split("-");
			if (!price[0].equals("0")) {
				Criteria fliterCriteria = new Criteria("item_price").greaterThanEqual(price[0]);
				FilterQuery filterQuery = new SimpleFilterQuery(fliterCriteria);
				query.addFilterQuery(filterQuery);
			}
			if (!price[1].equals("*")) {
				Criteria filterCriteria = new Criteria("item_price").lessThanEqual(price[1]);
				FilterQuery filterQuery = new SimpleFilterQuery(filterCriteria);
				query.addFilterQuery(filterQuery);
			}
		}
		//1.6 分页查询页码
		Integer pageNo = (Integer)searchMap.get("pageNo");//提取页码
		if (pageNo==null) {
			pageNo=1;
		}
		Integer pageSize = (Integer)searchMap.get("pageSize");//每页记录数
		if (pageSize==null) {
			pageSize=20;
		}
		query.setOffset((pageNo-1)*pageSize);
		query.setRows(pageSize);
		
		// 1.7 排序

		String sortValue = (String) searchMap.get("sort");// 升序ASC 降序DESC
		String sortField = (String) searchMap.get("sortField");// 排序字段

		if (sortValue != null && !sortValue.equals("")) {

			if (sortValue.equals("ASC")) {
				Sort sort = new Sort(Sort.Direction.ASC, "item_" + sortField);
				query.addSort(sort);
			}
			if (sortValue.equals("DESC")) {
				Sort sort = new Sort(Sort.Direction.DESC, "item_" + sortField);
				query.addSort(sort);
			}
		}
		
		// 高亮处理
		HighlightPage<TbItem> page = solrTemplate.queryForHighlightPage(query, TbItem.class);
		for (HighlightEntry<TbItem> highlightEntry : page.getHighlighted()) {
			TbItem item = highlightEntry.getEntity();
			if (highlightEntry.getHighlights().size()>0 && page.getHighlighted().size()>0){ //循环高亮入口
				item.setTitle(highlightEntry.getHighlights().get(0).getSnipplets().get(0));//设置高亮结果
			}
			
		}
		map.put("rows", page.getContent());
		map.put("totalPages", page.getTotalPages());
		map.put("total", page.getTotalElements());
		return map;
	}
	/**
	 * 查询商品分类列表
	 * @param searchMao
	 * @return
	 */
	private  List searchCategoryList(Map searchMap){
		List<String> list=new ArrayList();	
		Query query=new SimpleQuery();		
		//按照关键字查询
		Criteria criteria=new Criteria("item_keywords").is(searchMap.get("keywords"));
		query.addCriteria(criteria);
		//设置分组选项
		GroupOptions groupOptions=new GroupOptions().addGroupByField("item_category");
		query.setGroupOptions(groupOptions);
		//得到分组页
		GroupPage<TbItem> page = solrTemplate.queryForGroupPage(query, TbItem.class);
		//根据列得到分组结果集
		GroupResult<TbItem> groupResult = page.getGroupResult("item_category");
		//得到分组结果入口页
		Page<GroupEntry<TbItem>> groupEntries = groupResult.getGroupEntries();
		//得到分组入口集合
		List<GroupEntry<TbItem>> content = groupEntries.getContent();
		for(GroupEntry<TbItem> entry:content){
			list.add(entry.getGroupValue());//将分组结果的名称封装到返回值中	
		}
		return list;
	}
	
	@Autowired
	private RedisTemplate redisTemplate;
	/**
	 * 根据商品分类名称查询品牌和规格列表
	 * @param category 商品分类名称
	 * @return
	 */
	private Map searchBrandAndSpecList(String category){
		Map map=new HashMap();
		//1.根据商品分类名称得到模板ID
		Long templateId = (Long) redisTemplate.boundHashOps("itemCat").get(category);
		System.out.println(templateId);
		if(templateId!=null){
			//2.根据模板ID获取品牌列表
			List brandList = (List) redisTemplate.boundHashOps("brandList").get(templateId);
			map.put("brandList", brandList);	
			System.out.println("品牌列表条数："+brandList.size());
			
			//3.根据模板ID获取规格列表
			List specList = (List) redisTemplate.boundHashOps("specList").get(templateId);
			map.put("specList", specList);		
			System.out.println("规格列表条数："+specList.size());
		}	
		return map;
	}
	@Override
	public void importList(List list){
		solrTemplate.saveBeans(list);
		solrTemplate.commit();
	}
	@Override
	public void deleteByGoodsIds(List goodsIdList){
		System.out.println("删除商品ID"+goodsIdList);
		Query query=new SimpleQuery();		
		Criteria criteria=new Criteria("item_goodsid").in(goodsIdList);
		query.addCriteria(criteria);
		solrTemplate.delete(query);
		solrTemplate.commit();

	}

}
