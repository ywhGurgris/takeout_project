package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.result.PageResult;
import com.sky.service.OrderSercvice;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderSercvice {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //处理业务异常（地址簿为空，购物车为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> ShoppingCartlist = shoppingCartMapper.list(shoppingCart);
        if (ShoppingCartlist == null || ShoppingCartlist.size() == 0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //向订单表插入一条数据

        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setUserId(userId);
        orders.setConsignee(addressBook.getConsignee());

        orderMapper.insert(orders);


        //向订单明细表插入N条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart: ShoppingCartlist) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart,orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);

        //清空当前用户购物车数据
        shoppingCartMapper.deleteByUserId(userId);

        //封装VO返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
        return orderSubmitVO;
    }

    /**
     * 查询历史订单
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    public PageResult historyOrdersQuery(int pageNum, int pageSize, Integer status) {
        //设置分页
        PageHelper.startPage(pageNum,pageSize);
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);
        Page<Orders> page = orderMapper.historyOrdersQuery(ordersPageQueryDTO);
        List<OrderVO> list = new ArrayList<>();
        if (page!=null && page.size() >0){
            for (Orders orders : page){
                Long ordersId = orders.getId();
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrdersId(ordersId);
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);

                list.add(orderVO);

            }
        }
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    public OrderVO orderDetails(Long id) {
        Orders orders = orderMapper.getById(id);
        Long ordersId = orders.getId();
        List<OrderDetail> details = orderDetailMapper.getByOrdersId(ordersId);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders,orderVO);
        orderVO.setOrderDetailList(details);
        return orderVO;
    }
}
