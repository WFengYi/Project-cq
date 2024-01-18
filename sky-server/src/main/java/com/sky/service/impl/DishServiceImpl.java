package com.sky.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.anno.AutoFill;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper,Dish> implements DishService {

    @Autowired
    private DishFlavorMapper flavorMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;



    @Override
    public void saveDish(DishDTO dishDTO) {

    }

    /**
     * 插入
     * @param dish
     * @param dishDTO
     */
    @Override
    @Transactional
    @AutoFill(value = OperationType.INSERT)
    //这里要注意@AutoFill第一个参数必须是实体，否则注解方法不其作用！！！！！！！！！！！！！！！！！
    public void save(Dish dish,DishDTO dishDTO) {
        BeanUtils.copyProperties(dishDTO, dish);
        // 插入Dish数据到Dish表
        save(dish);
        Long dishId = dish.getId();//获取insertBatch生成的主键值
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors!=null&&flavors.size()>0){
            flavors.forEach(dishFlavor->{
                dishFlavor.setDishId(dishId);
            });
            // 插入DishFlavor数据到另一张表
            flavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 分页查询
     * @param queryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO queryDTO) {
        // 创建分页对象并设置当前页和每页显示记录数
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getPageSize());

        // 构建查询条件
        Page<DishVO> page1 = dishMapper.pageQuery(queryDTO);

        // 执行分页查询，并返回结果
        return new PageResult(page1.getTotal(),page1.getResult());
    }

    @Override
    @Transactional
    public void removeByCndition(List<Long> ids) {
        //判断商品是否起售
        for (Long id : ids) {
            Dish dish = getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //判断是否关联套餐--如果数据为空则没有关联
        QueryWrapper<SetmealDish> wrapper = new QueryWrapper<>();
        wrapper.select("setmeal_id")
                .in("dish_id", 1, 2, 3, 4);
        List<Object> setmealId = setmealDishMapper.selectObjs(wrapper);
        if (setmealId != null && setmealId.size() > 0){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        for (Long id : ids) {
            dishMapper.deleteById(id);
            //删除菜品关联的口味数据
            dishMapper.deleteById(id);
        }
        dishMapper.deleteBatchIds(ids);
        dishMapper.deleteBatchIds(ids);
    }

    /**
     * 根据id查询菜品对应的口味数据
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        Dish dish = getById(id);
        QueryWrapper<DishFlavor> wrapper = new QueryWrapper<>();
        wrapper.eq("dish_id", id);
        List<DishFlavor> dishFlavors = flavorMapper.selectList(wrapper);
        DishVO dishVO = BeanUtil.copyProperties(dish, DishVO.class);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    @Override
    @Transactional

    public void updateWithFlavor(Dish dish,DishDTO dishDTO) {

//        UpdateWrapper<Dish> wrapper = new UpdateWrapper<>();
//        wrapper.set("name", dish.getName())
//                .set("category_id", dish.getCategoryId())
//                .set("price", dish.getPrice())
//                .set("image", dish.getImage())
//                .set("description", dish.getDescription())
//                .set("status", dish.getStatus())
//                .eq("id", dish.getId());
//        update(null,wrapper);

        dishMapper.updateDish(dish);
        //删除原有的口味数据

        flavorMapper.deleteFlavorById(dishDTO.getId());
        //重新插入口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors!=null&&flavors.size()>0){
            flavors.forEach(dishFlavor->{
                dishFlavor.setDishId(dish.getId());
            });
            // 插入DishFlavor数据到另一张表
            flavorMapper.insertBatch(flavors);
        }
    }
}
