package com.sky.controller.admin;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Employee;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;

    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品：{}",dishDTO);
        Dish dish = BeanUtil.copyProperties(dishDTO, Dish.class);
        dishService.save(dish,dishDTO);
        return Result.success();
    }

    /**
     * 菜品分页查询
     * @param queryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO queryDTO){
        log.info("菜品分页查询：{}",queryDTO);
        //分页实现
        PageResult PageResult = dishService.pageQuery(queryDTO);
        return Result.success(PageResult);
    }

    /**
     * 删除菜品
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("删除菜品接口")
    public Result delete(@RequestParam List<Long> ids){
        log.info("删除菜品接口:{}",ids);
        dishService.removeByCndition(ids);
        return Result.success();
    }

    /**
     * 查询菜品接口
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("查询菜品接口")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("查询菜品接口:{}",id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }
    @PutMapping
    @ApiOperation("修改对象接口")
    public Result updateWithDishFlavor(@RequestBody DishDTO dishDTO){
        Dish dish = BeanUtil.copyProperties(dishDTO, Dish.class);
        dishService.updateWithFlavor(dish,dishDTO);
        return Result.success();
    }
}
