package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
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
}
