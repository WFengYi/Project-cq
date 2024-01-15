package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;

@Service
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper,Employee> implements EmployeeService     {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        //对前端传来的数据进行md5加密
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    /**
     * 员工分页
     *
     * @param pageQueryDTO
     * @return
     */
    @Override
    public IPage<Employee> pageQuery(EmployeePageQueryDTO pageQueryDTO) {
        //创建分页
        Page<Employee> page = Page.of(pageQueryDTO.getPage(), pageQueryDTO.getPageSize());
        //排序顺序
        page.addOrder(new OrderItem("create_time",true));//根据那个字段，是否升序
        //分页条件
        QueryWrapper<Employee> wrapper = new QueryWrapper<>();
        if (pageQueryDTO.getName() != null && !pageQueryDTO.getName().isEmpty()) {
            wrapper.like("name", "%" + pageQueryDTO.getName() + "%");
        }

        return employeeMapper.selectPage(page, wrapper);
    }

    /**
     * 修改员工状态
     * @param status
     * @param id
     */
    @Override
    public void updateStatus(Integer status, Long id) {
        Employee employee = Employee.builder()
                .status(status)
                .id(id)
                .build();
        updateById(employee);
    }

    @Override
    public void updateEmployee(Employee employee) {
        employee.setUpdateTime(LocalDateTime.now());
        employee.setUpdateUser(BaseContext.getCurrentId());
        employeeMapper.updateById(employee);
    }

    @Override
    public boolean save(Employee entity) {
        System.out.println("当前线程的ID为：" + Thread.currentThread().getId());
        entity.setStatus(StatusConstant.ENABLE);//设置状态
        //设置密码状态用mp5加密
        entity.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        entity.setCreateUser(BaseContext.getCurrentId());
        entity.setUpdateUser(BaseContext.getCurrentId());
        return super.save(entity);
    }

}
