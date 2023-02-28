package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 套餐管理
 */

@RestController
@RequestMapping("/setmeal")
@Slf4j
@Api(tags = "套餐相关接口")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SetmealDishService setmealDishService;
    @Autowired
    private CacheManager cacheManager;

    /**
     * 新增套餐
     * @param setmealDto
     * @return
     */
    @PostMapping
    @CacheEvict(value = "deleteCache",allEntries = true)
    @ApiOperation(value = "新增套餐接口")
    public R<String> save(@RequestBody SetmealDto setmealDto){
        log.info("套餐信息：{}",setmealDto);

        setmealService.saveWithDish(setmealDto);

        return R.success("新增套餐成功");
    }
    /**
     * 套餐分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
  @GetMapping("/page")
  @ApiOperation(value = "套餐分页查询接口")
  @ApiImplicitParams({
          @ApiImplicitParam(name = "page",value = "页码",required = true),
          @ApiImplicitParam(name = "pageSize",value = "每页记录数",required = true),
          @ApiImplicitParam(name = "name",value = "套餐名称",required = false)
  })
    public R<Page> page(int page,int pageSize,String name){
        //分页构造器
      Page<Setmeal> pageInfo=new Page<>(page,pageSize);
      Page<SetmealDto> dtoPage = new Page<>();

      LambdaQueryWrapper<Setmeal> queryWrapper =new LambdaQueryWrapper<>();
      //添加查询条件，根据name来查询
      queryWrapper.like(name!=null,Setmeal::getName,name);
      //添加排序条件 降序
      queryWrapper.orderByDesc(Setmeal::getUpdateTime);

      setmealService.page(pageInfo,queryWrapper);

      //对象拷贝
      BeanUtils.copyProperties(pageInfo,dtoPage,"records");
      List<Setmeal> records = pageInfo.getRecords();

      List<SetmealDto> list= records.stream().map((item)->{
          SetmealDto setmealDto = new SetmealDto();
          BeanUtils.copyProperties(item,setmealDto);
          //分类id
          Long categoryId = item.getCategoryId();
          Category category = categoryService.getById(categoryId);

          if (category!=null){
              String categoryName = category.getName();
              setmealDto.setCategoryName(categoryName);
          }
            return setmealDto;
      }).collect(Collectors.toList());
      dtoPage.setRecords(list);
      return R.success(dtoPage);
  }
    /**
     * 删除套餐
     * @param id
     * @return
     */
    @DeleteMapping
    @CacheEvict(value = "setmealCache",allEntries = true)
    @ApiOperation(value = "套餐删除接口")
    public R<String> delete(@RequestParam List<Long> id){
        log.info("ids:{}",id);

        setmealService.removeWithDish(id);

        return R.success("套餐数据删除成功");
    }
    /**
     * 根据条件查询套餐数据
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    @ApiOperation(value = "套餐查询接口")
    @Cacheable(value = "setmealCache",key ="#setmeal.categoryId+'_'+#setmeal.status")//通过缓存保存套餐信息
    public R<List<Setmeal>> list(Setmeal setmeal){
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(setmeal.getCategoryId() != null,Setmeal::getCategoryId,setmeal.getCategoryId());
        queryWrapper.eq(setmeal.getStatus() != null,Setmeal::getStatus,setmeal.getStatus());
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        List<Setmeal> list = setmealService.list(queryWrapper);

        return R.success(list);
    }
    /*修改套餐
    * */
    @PutMapping
    @ApiOperation(value = "套餐修改接口")
    @CacheEvict(value = "setmealCache",allEntries = true)
    public R<String> update(@RequestBody SetmealDto setmealDto){
      setmealService.updateWithDish(setmealDto);
      return R.success("修改套餐成功");
    }
    //根据id查询套餐
  @Cacheable(value = "setmealCache",key = "#id")
  @GetMapping("/{id}")
  @ApiOperation(value = "套餐根据id查询接口")
  public R<SetmealDto> get(@PathVariable Long id){

    SetmealDto setmealDto=setmealService.getByIdWithDish(id);

    return R.success(setmealDto);
  }
  //套餐停售起售
  @PostMapping("/status/{status}")
  @ApiOperation(value = "套餐状态调整接口")
  @CacheEvict(value = "setmealCache",allEntries = true)
  public R<String> sale(@PathVariable int status,
                        String[] ids){
    for(String id: ids){
      Setmeal setmeal = setmealService.getById(id);
      setmeal.setStatus(status);
      setmealService.updateById(setmeal);
    }
    return R.success("修改成功");
  }

}
