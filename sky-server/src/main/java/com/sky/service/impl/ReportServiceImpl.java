package com.sky.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ReportServiceImpl implements ReportService{
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private WorkspaceService workspaceService;

    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //创建集合存放数据
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }


        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {

            //查询date日期对应的营业额数据，指状态为已完成的订单金额总计
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            //为空赋值为0
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //创建集合存放数据
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<Integer> tatalUserList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("endTime", endTime);
            //查询总用户数量
            Integer totalUser = userMapper.countByMap(map);

            //查询新增用户数量
            map.put("beginTime", beginTime);
            Integer newUser = userMapper.countByMap(map);

            tatalUserList.add(totalUser);
            newUserList.add(newUser);

        }
        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(tatalUserList, ","))
                .build();
    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        //创建集合存放数据
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //创建集合存放订单总数和订单有效数
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //查询订单总数
            Integer orderCount = getOrderCount(beginTime, endTime,null);
            orderCountList.add(orderCount);
            //查询订单有效数
            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);
            validOrderCountList.add(validOrderCount);

        }
        //订单总数 stream流将集合里数据累加到一起
        Integer totalOrderCount = orderCountList.stream().reduce(0,Integer::sum);
        Integer totalValidOrderCount = validOrderCountList.stream().reduce(0,Integer::sum);
        Double orderCompletionRate  = 0.0;
        if (totalOrderCount != 0.0){
            orderCompletionRate = (double) totalValidOrderCount / totalOrderCount;
        }

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))//日期
                .orderCountList(StringUtils.join(orderCountList, ","))//订单总数
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))//订单有效数
                .totalOrderCount(totalOrderCount)//订单总数
                .validOrderCount(totalValidOrderCount)//订单有效数
                .orderCompletionRate(orderCompletionRate)//订单完成率
                .build();
    }

    /**
     * 获取销量top10
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getTopStatistics(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<GoodsSalesDTO> salesTop = orderMapper.getSalesTop(beginTime, endTime);


        //通过stream流获取集合里name属性赋值给一个集合
        List<String> nameList = salesTop.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        //把集合构造成一个字符串
        String nameListStr = StringUtils.join(nameList, ",");

        //通过stream流获取集合里number属性赋值给一个集合
        List<Integer> numberList = salesTop.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        //把集合构造成一个字符串
        String numberListStr = StringUtils.join(numberList, ",");


        return SalesTop10ReportVO.builder()
                .nameList(nameListStr)
                .numberList(numberListStr)
                .build();
    }

    /**
     * 导出运营数据excel报表
     * @param response
     */
    @Override
    public void getBusinessData(HttpServletResponse response) {
        //1.查询数据库 获取营业额
        //获取当前前后三十天时间
        LocalDate begin = LocalDate.now().minusDays(30);

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.MAX);


        BusinessDataVO businessDataVO = workspaceService.getBusinessData(beginTime, endTime);
        //2.poi写入excel报表中
        //类加载器指向表所在位置
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            XSSFWorkbook excel = new XSSFWorkbook(in);
            XSSFSheet sheet = excel.getSheet("Sheet1");
            //填充数据--时间
            sheet.getRow(1).getCell(1).setCellValue("时间" + beginTime + " 至 " + endTime);

            //填充数据--营业额
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            //填充数据--订单完成率
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            //填充数据--新用户数
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());
            //换成第五行
            row = sheet.getRow(4);
            //填充数据--订单总数
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            //填充数据--订单平均单价
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = begin.plusDays(i);
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN),
                        LocalDateTime.of(date, LocalTime.MAX));

               //获得某一行
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());

            }

            //3.通过输出流响应给前端Excel文件
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            out.close();
            excel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end,Integer status) {
        Map map = new HashMap();
        map.put("beginTime", begin);
        map.put("endTime", end);
        map.put("status", status);
        return orderMapper.countByMap(map);
    }
}
