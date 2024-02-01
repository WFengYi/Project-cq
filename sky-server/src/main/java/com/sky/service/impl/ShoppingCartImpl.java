package com.sky.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart> implements ShoppingCartService {
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        //判断购物车是否已经存在
        //属性拷贝
        ShoppingCart shoppingCart = BeanUtil.copyProperties(shoppingCartDTO, ShoppingCart.class);
        //设置用户id
        shoppingCart.setUserId(BaseContext.getCurrentId());
        //创建查询器
        QueryWrapper<ShoppingCart> queryWrapper = new QueryWrapper<>();
        if (shoppingCart.getUserId() != null) {
            queryWrapper.eq("user_id", shoppingCart.getUserId());
        }
        if (shoppingCart.getSetmealId() != null) {
            queryWrapper.eq("setmeal_id", shoppingCart.getSetmealId());
        }
        if (shoppingCart.getDishId() != null) {
            queryWrapper.eq("dish_id", shoppingCart.getDishId());
        }
        if (shoppingCart.getDishFlavor() != null) {
            queryWrapper.eq("dish_flavor", shoppingCart.getDishFlavor());
        }
        List<ShoppingCart> cartList = shoppingCartMapper.selectList(queryWrapper);

        //List<ShoppingCart> cartList = shoppingCartMapper.list(shoppingCart);
        //如果已经存在则数量加一
        if (cartList.size() > 0) {

            ShoppingCart cart = cartList.get(0);
            cart.setNumber(cart.getNumber() + 1);
            UpdateWrapper<ShoppingCart> updateWrapper = new UpdateWrapper<>();
            updateWrapper.set("number", cart.getNumber())
                    .eq("id", cart.getId());

            shoppingCartMapper.update(null, updateWrapper);
        } else {
            //判断是套餐还是菜品
            Long dishId = shoppingCartDTO.getDishId();
            if (dishId != null) {
                //是菜品
                Dish dish = dishMapper.selectById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setAmount(dish.getPrice());
                shoppingCart.setImage(dish.getImage());
            }else {
                Long setmealId = shoppingCartDTO.getSetmealId();
                //是套餐
                Setmeal setmeal = setmealMapper.selectById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setAmount(setmeal.getPrice());
                shoppingCart.setImage(setmeal.getImage());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    @Override
    public List<ShoppingCart> showShoppingCart() {
        //获取当前微信用户id
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> cartList = shoppingCartMapper.list(shoppingCart);
        return cartList;
    }

    @Override
    public void clearShoppingCart() {
        QueryWrapper wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", BaseContext.getCurrentId());
        shoppingCartMapper.delete(wrapper);
    }

    @Override
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);
        //设置查询条件，查询当前登录用户的购物车数据
        shoppingCart.setUserId(BaseContext.getCurrentId());

        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        if(list != null && list.size() > 0){
            shoppingCart = list.get(0);

            Integer number = shoppingCart.getNumber();
            if(number == 1){
                //当前商品在购物车中的份数为1，直接删除当前记录
                shoppingCartMapper.deleteById(shoppingCart.getId());
            }else {
                //当前商品在购物车中的份数不为1，修改份数即可
                shoppingCart.setNumber(shoppingCart.getNumber() - 1);
                shoppingCartMapper.updateNumberById(shoppingCart);
            }
        }
    }
}
