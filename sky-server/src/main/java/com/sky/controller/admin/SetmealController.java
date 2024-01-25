package com.sky.controller.admin;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sky.dto.DishDTO;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Employee;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/setmeal")
@Slf4j
@Api(tags = "套餐相关接口")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;

    /**
     * 分页查询员工接口
     * @param pageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("分页查询套餐")
    public Result<PageResult> pageQuery(SetmealPageQueryDTO pageQueryDTO){
        log.info("套餐分页查询：{}",pageQueryDTO);
        PageResult pageResult =setmealService.pageQuery(pageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 新增菜品接口
     * @param setmealDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody SetmealDTO setmealDTO){
        log.info("新增菜品：{}",setmealDTO);
        Setmeal setmeal = BeanUtil.copyProperties(setmealDTO, Setmeal.class);
        setmealService.save(setmeal,setmealDTO);
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("根据id查询套餐")
    public Result<Setmeal> selectByid(@PathVariable Long id){
        log.info("根据id查询套餐：{}",id);
        Setmeal setmeal = setmealService.getById(id);
        return Result.success(setmeal);
    }

    @PutMapping
    @ApiOperation("修改套餐")
    public Result<Setmeal> update(@RequestBody SetmealDTO setmealDTO){
        log.info("修改套餐：{}",setmealDTO);
        Setmeal setmeal = BeanUtil.copyProperties(setmealDTO, Setmeal.class);
        setmealService.update(setmeal,setmealDTO);
        return Result.success();
    }
//    @DeleteMapping
//    @ApiOperation("删除套餐")
//    public Result deleteMeal(@RequestParam List<Long> ids){
//        log.info("删除套餐：{}",ids);
//        setmealService.deleteBatchByIds(ids);
//        return Result.success();
//    }
    @PostMapping("/status/{status}")
    @ApiOperation("修改套餐状态")
    public Result startOrBan(@PathVariable("status") Integer status,Long id){
        log.info("修改套餐状态：{},{}",status,id);
        setmealService.updateStatus(status,id);
        return Result.success();
    }

    @DeleteMapping
    @ApiOperation("删除套餐接口")
    public Result delete(@RequestParam List<Long> ids){
        log.info("删除套餐接口:{}",ids);
        setmealService.removeByCndition(ids);
        return Result.success();
    }
}
