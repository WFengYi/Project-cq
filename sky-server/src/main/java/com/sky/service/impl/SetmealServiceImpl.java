package com.sky.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.anno.AutoFill;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;

import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishServiceImpl dishService;
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO pageQueryDTO) {
        // 创建分页对象并设置当前页和每页显示记录数
        PageHelper.startPage(pageQueryDTO.getPage(), pageQueryDTO.getPageSize());

        // 构建查询条件
        Page<SetmealVO> page1 = setmealMapper.pageQuery(pageQueryDTO);

        // 执行分页查询，并返回结果
        return new PageResult(page1.getTotal(),page1.getResult());
    }

    @Override
    @Transactional
    @AutoFill(OperationType.INSERT)
    public void save(Setmeal setmeal, SetmealDTO setmealDTO) {

        save(setmeal);
        Long setmealId = setmeal.getId();//获取insertBatch生成的主键值

        setmealDTO.setSetmealDishes(setmealDTO.getSetmealDishes());
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes!=null&&setmealDishes.size()>0){
            setmealDishes.forEach(setmealFlavor->{
                setmealFlavor.setDishId(setmealId);
            });
            // 插入DishFlavor数据到另一张表
            setmealDishMapper.insertDish(setmealDishes);
        }
    }

    @Override
    @Transactional
    @AutoFill(OperationType.UPDATE)
    public void update(Setmeal setmeal, SetmealDTO setmealDTO) {
        QueryWrapper<Setmeal> wrapper = new QueryWrapper<>();
        wrapper.eq("id",setmeal.getId());
        update(setmeal,wrapper);
        //套餐id
        Long id = setmealDTO.getId();
        //2、删除套餐和菜品的关联关系，操作setmeal_dish表，执行delete
        setmealDishMapper.deleteById(id);

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();

        setmealDishes.forEach( setmealDish -> {
            setmealDish.setSetmealId(id);
        });
        //3、重新插入套餐和菜品的关联关系，操作setmeal_dish表，执行insert
        // 插入DishFlavor数据到另一张表
        setmealDishMapper.insertDish(setmealDishes);
    }

    @Override
    public void deleteBatchByIds(List<Long> ids) {
        for (Long id : ids) {
            Setmeal setmeal = getById(id);
            if (StatusConstant.ENABLE == setmeal.getStatus()){
                //起售中的套餐不能删除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        for (Long id : ids) {
            removeById(id);//删除dish表中数据
            setmealDishMapper.deleteById(id);//删除套餐菜品关系表中数据
        }
    }

    @Override
    public void updateStatus(Integer status, Long id) {
        Setmeal setmeal = Setmeal.builder()
                .status(status)
                .id(id)
                .updateTime(LocalDateTime.now())
                .updateUser(BaseContext.getCurrentId())
                .build();
        QueryWrapper wrapper = new QueryWrapper<>();
        wrapper.eq("id",id);
        update(setmeal,wrapper);
    }

    @Override
    public void removeByCndition(List<Long> ids) {
        ids.forEach(id -> {
            Setmeal setmeal = setmealMapper.selectById(id);
            if(StatusConstant.ENABLE == setmeal.getStatus()){
                //起售中的套餐不能删除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        });

        ids.forEach(setmealId -> {
            //删除套餐表中的数据
            setmealMapper.deleteById(setmealId);
            //删除套餐菜品关系表中的数据
            setmealDishMapper.deleteById(setmealId);
        });
    }


    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        List<DishItemVO> dishItemBySetmealId = setmealMapper.getDishItemBySetmealId(id);
        return dishItemBySetmealId;
    }
}
