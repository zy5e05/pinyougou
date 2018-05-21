package com.pinyougou.sellergoods.service;

import java.util.List;

import com.pinyougou.pojo.TbBrand;

import entity.PageResult;

/**
 * 品牌接口
 * @author Administrator
 *
 */
public interface BrandService {

	public List<TbBrand> findAll();
	
	/**
	 * 返回分页列表
	 * @return
	 */
	public PageResult findPage(int pageNum,int pageSize);
	/**
	 * 新增
	 * @param tbBrand
	 */
	void add(TbBrand tbBrand);

	void update(TbBrand tbBrand);

	TbBrand findOne(Long id);

	
}
