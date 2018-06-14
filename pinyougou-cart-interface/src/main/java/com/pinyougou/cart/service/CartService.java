package com.pinyougou.cart.service;

import java.util.List;

import com.pinyougou.pojo.group.Cart;

public interface CartService {

	List<Cart> addGoodsToCartList(List<Cart> cartList, Long itemId, Integer num);

	List<Cart> findCartListFromRedis(String username);

	void saveCartListToRedis(String username, List<Cart> cartList);

	List<Cart> mergeCartList(List<Cart> cartList1, List<Cart> cartList2);

}
