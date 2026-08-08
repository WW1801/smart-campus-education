package com.campus.education.service.impl;

/**
 * 学期服务实现类，负责处理学期相关业务逻辑。
 */

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.entity.Semester;
import com.campus.education.mapper.SemesterMapper;
import com.campus.education.service.SemesterService;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class SemesterServiceImpl extends ServiceImpl<SemesterMapper, Semester> implements SemesterService {
    private Clock clock = Clock.system(ZoneId.of("Asia/Shanghai"));

    @Override
    public List<Semester> listWithRealtimeStatus() {
        LocalDate businessDate = LocalDate.now(clock);
        List<Semester> semesters = list();
        boolean hasDateCurrentSemester = semesters.stream()
                .anyMatch(semester -> "current".equals(resolveStatus(semester, businessDate)));
        Semester configuredCurrentSemester = semesters.stream()
                .filter(semester -> "current".equals(semester.getStatus()))
                .findFirst()
                .orElse(null);
        for (Semester semester : semesters) {
            // 状态只在响应时按日期计算，不依赖或回写数据库中的历史状态。
            semester.setStatus(resolveStatus(semester, businessDate));
        }
        if (!hasDateCurrentSemester && configuredCurrentSemester != null) {
            configuredCurrentSemester.setStatus("current");
        }
        return semesters;
    }

    static String resolveStatus(Semester semester, LocalDate businessDate) {
        if (semester == null || semester.getStartDate() == null || semester.getEndDate() == null
                || semester.getStartDate().isAfter(semester.getEndDate())) {
            return "invalid";
        }
        if (businessDate.isBefore(semester.getStartDate())) {
            return "upcoming";
        }
        if (businessDate.isAfter(semester.getEndDate())) {
            return "completed";
        }
        return "current";
    }
}
