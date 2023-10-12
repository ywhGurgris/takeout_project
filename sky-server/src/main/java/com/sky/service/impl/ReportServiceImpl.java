package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Dish;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;


    /**
     * 营业额数据统计
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {

        //存放日期
        List<LocalDate> localDates = new ArrayList<>();
        //将日期时间段存放到list集合中
        localDates.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            localDates.add(begin);
        }
        //存放营业额
        List<Double> moneyList = new ArrayList<>();
        for (LocalDate date : localDates){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumBymap(map);
            turnover = turnover == null ? 0.0 : turnover;
            moneyList.add(turnover);
        }
        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(localDates,","))
                .turnoverList(StringUtils.join(moneyList,","))
                .build();
    }
    /**
     * 用户数量统计
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //存放日期
        List<LocalDate> localDates = new ArrayList<>();
        //将日期时间段存放到list集合中
        localDates.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            localDates.add(begin);
        }
        //存放用户数量总量
        List<Integer> totalUserList = new ArrayList<>();
        //存放新增用户数量
        List<Integer> newUserList = new ArrayList<>();

        for (LocalDate date : localDates){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("end",endTime);
            //得到截止到目前总用户量
            Integer totaluser = userMapper.getByMap(map);
            totalUserList.add(totaluser);
            //得到某日新增用户量
            map.put("begin",beginTime);
            Integer newUser = userMapper.getByMap(map);
            newUserList.add(newUser);
        }
        return UserReportVO.builder()
                .dateList(StringUtils.join(localDates,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .build();
    }

    /**
     * 订单数量统计
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOdersStatistics(LocalDate begin, LocalDate end) {
        //存放begin-end 日期的list
        List<LocalDate> localDateList = new ArrayList<>();
        localDateList.add(begin);
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            localDateList.add(begin);
        }

        //orderCountList 每日订单数，以逗号分割 select count(id) from orders where order_time > ? and order_time < ?
        List<Integer> orderCountList = new ArrayList<>(); //存放每日订单数
        //validOrderCountList每日有效订单数，以逗号分隔， select count(id) from orders where order_time > ? and order_time < ? and status  = 5
        List<Integer>  validOrderCountList = new ArrayList<>(); //存放每日有效订单数

        for (LocalDate localDate : localDateList){
            LocalDateTime beginTime = LocalDateTime.of(localDate,LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate,LocalTime.MAX);
            Map map = new HashMap();
            map.put("begin",beginTime);
            map.put("end",endTime);
            Integer totalOrderCountDay = orderMapper.orderSumBymap(map);
            orderCountList.add(totalOrderCountDay);
            map.put("status",Orders.COMPLETED);
            Integer validOrderCountDay = orderMapper.orderSumBymap(map);
            validOrderCountList.add(validOrderCountDay);
        }
        Map map = new HashMap();
        //totalOrderCount;订单总数       select count(id) from orders
        Integer totalOrderCount = orderMapper.orderSumBymap(map);
        //validOrderCount有效订单数       select count(id) from orders where status  = 5
        map.put("status",Orders.COMPLETED);
        Integer validOrderCount = orderMapper.orderSumBymap(map);
        //orderCompletionRate订单完成率   validOrderCount/totalOrderCount * 100
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0){
            orderCompletionRate = validOrderCount.doubleValue()/totalOrderCount;   //转换成double封装类
        }
        return OrderReportVO.builder()
                .dateList(StringUtils.join(localDateList,","))
                .orderCountList(StringUtils.join(orderCountList,","))
                .validOrderCountList(StringUtils.join(validOrderCountList,","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 销量top10 统计
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {


        //select od.name,od.number from order_detail od JOIN orders o on od.order_id = o.id where o.status=5 and o.order_time < "2023-10-12" and o.order_time > "2023-09-01" group by od.name order by od.number desc  limit 0,10

        LocalDateTime beginTime = LocalDateTime.of(begin,LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end,LocalTime.MAX);
        List<GoodsSalesDTO> goodsSalesDTOS = orderMapper.selectSaleTop10(beginTime,endTime);
        List<String> names = goodsSalesDTOS.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String stringName = StringUtils.join(names, ",");
        List<Integer> numbers = goodsSalesDTOS.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String stringNumber = StringUtils.join(numbers, ",");

        return SalesTop10ReportVO.builder()
                .nameList(stringName)
                .numberList(stringNumber)
                .build();
    }


}
