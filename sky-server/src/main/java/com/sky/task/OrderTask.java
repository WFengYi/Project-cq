package com.sky.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;

    //处理超时订单
    @Scheduled(cron = "0 * * * * ? ")
    public void handleTimeoutOrder() {
        log.info("处理超时订单,{}", LocalDateTime.now());

        //把当前的时间往前调15分钟，看是否有超时范围内的订单
        LocalDateTime checkTime = LocalDateTime.now().minusMinutes(15L);
        QueryWrapper<Orders> wrapper = new QueryWrapper<>();
        wrapper.eq("status", Orders.PENDING_PAYMENT)
                .lt("order_time", checkTime);
        List<Orders> ordersList = orderMapper.selectList(wrapper);
        if (ordersList != null && ordersList.size() > 0) {
            ordersList.forEach(orders -> {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时，取消订单");
                orders.setCancelTime(LocalDateTime.now());
                //订单超时，取消订单
                orderMapper.update(orders);
            });
        }
    }

    //处理一直派送订单
    @Scheduled(cron = "0 0 1 * * ? ")
    public void handleDeliveryOrder() {
        log.info("定时处理一直派送订单,{}", LocalDateTime.now());


        LocalDateTime checkTime = LocalDateTime.now().minusMinutes(60);

        QueryWrapper<Orders> wrapper = new QueryWrapper<>();
        wrapper.eq("status", Orders.DELIVERY_IN_PROGRESS)
                .lt("delivery_time", checkTime);
        List<Orders> ordersList = orderMapper.selectList(wrapper);
        if (ordersList != null && ordersList.size() > 0) {
            ordersList.forEach(orders -> {
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);
            });
        }
    }
}
