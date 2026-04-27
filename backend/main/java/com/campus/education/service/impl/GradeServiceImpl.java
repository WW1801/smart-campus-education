package com.campus.education.service.impl;

/**
 * 成绩服务实现类，负责处理成绩相关业务逻辑。
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.education.common.BusinessException;
import com.campus.education.entity.Course;
import com.campus.education.entity.Grade;
import com.campus.education.entity.TeachingPlan;
import com.campus.education.mapper.CourseMapper;
import com.campus.education.mapper.GradeMapper;
import com.campus.education.mapper.TeachingPlanMapper;
import com.campus.education.service.GradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class GradeServiceImpl extends ServiceImpl<GradeMapper, Grade> implements GradeService {

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private TeachingPlanMapper teachingPlanMapper;

    private static final Map<String, double[]> SCORE_WEIGHTS = createWeights();

    // 创建权重
    private static Map<String, double[]> createWeights() {
        Map<String, double[]> map = new HashMap<>();
        map.put("compulsory", new double[]{0.3, 0.7});
        map.put("elective_major", new double[]{0.4, 0.6});
        map.put("elective_public", new double[]{0.5, 0.5});
        return map;
    }

    // 处理提交成绩
    @Override
    @Transactional
    public void submitGrade(Grade grade) {
        validateScore(grade);
        calculateTotalScore(grade);
        grade.setStatus("pending");
        grade.setIsPass(null);
        upsertGrade(grade);
    }

    // 批量提交成绩
    @Override
    @Transactional
    public void batchSubmitGrades(List<Grade> grades) {
        for (Grade grade : grades) {
            validateScore(grade);
            calculateTotalScore(grade);
            grade.setStatus("pending");
            grade.setIsPass(null);
            upsertGrade(grade);
        }
    }

    // 处理通过成绩
    @Override
    @Transactional
    public void approveGrade(String gradeId) {
        Grade grade = this.getById(gradeId);
        if (grade == null) {
            throw new BusinessException("成绩记录不存在");
        }
        if (!"pending".equals(grade.getStatus())) {
            throw new BusinessException("只能审核待审核状态的成绩");
        }
        grade.setStatus("approved");
        grade.setIsPass(grade.getTotalScore() >= 60);
        this.updateById(grade);
    }

    // 处理驳回成绩
    @Override
    @Transactional
    public void rejectGrade(String gradeId, String reason) {
        Grade grade = this.getById(gradeId);
        if (grade == null) {
            throw new BusinessException("成绩记录不存在");
        }
        if (!"pending".equals(grade.getStatus())) {
            throw new BusinessException("只能驳回待审核状态的成绩");
        }
        grade.setStatus("rejected");
        grade.setIsPass(null);
        this.updateById(grade);
    }

    // 计算绩点
    @Override
    public Map<String, Object> calculateGpa(String studentId, String semesterId) {
        LambdaQueryWrapper<Grade> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Grade::getStudentId, studentId);
        wrapper.eq(Grade::getStatus, "approved");
        if (semesterId != null && !semesterId.trim().isEmpty()) {
            wrapper.eq(Grade::getSemesterId, semesterId);
        }
        List<Grade> grades = this.list(wrapper);

        double totalWeightedPoints = 0.0;
        double totalCredits = 0.0;
        int totalCourses = grades.size();
        int passCourses = 0;
        double totalScore = 0.0;

        for (Grade grade : grades) {
            Course course = courseMapper.selectById(grade.getCourseId());
            if (course == null) {
                continue;
            }

            double credit = course.getCredits() == null ? 0 : course.getCredits();
            double gradePoint = grade.getTotalScore() >= 60 ? (grade.getTotalScore() - 50) / 10 : 0;

            totalWeightedPoints += gradePoint * credit;
            totalCredits += credit;
            totalScore += grade.getTotalScore();
            if (Boolean.TRUE.equals(grade.getIsPass())) {
                passCourses++;
            }
        }

        double gpa = totalCredits > 0 ? totalWeightedPoints / totalCredits : 0;
        double avgScore = totalCourses > 0 ? totalScore / totalCourses : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentId);
        result.put("totalCourses", totalCourses);
        result.put("passCourses", passCourses);
        result.put("totalCredits", totalCredits);
        result.put("gpa", Math.round(gpa * 100) / 100.0);
        result.put("averageScore", Math.round(avgScore * 100) / 100.0);
        return result;
    }

    // 获取统计
    @Override
    public Map<String, Object> getStatistics(String semesterId, String courseId, String classId) {
        LambdaQueryWrapper<Grade> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Grade::getStatus, "approved");
        if (semesterId != null) {
            wrapper.eq(Grade::getSemesterId, semesterId);
        }
        if (courseId != null) {
            wrapper.eq(Grade::getCourseId, courseId);
        }
        List<Grade> grades = this.list(wrapper);

        if (grades.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("total", 0);
            return empty;
        }

        DoubleSummaryStatistics stats = grades.stream()
                .map(Grade::getTotalScore)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();

        long passCount = grades.stream().filter(g -> Boolean.TRUE.equals(g.getIsPass())).count();
        long excellentCount = grades.stream().filter(g -> g.getTotalScore() >= 90).count();
        long goodCount = grades.stream().filter(g -> g.getTotalScore() >= 80 && g.getTotalScore() < 90).count();
        long mediumCount = grades.stream().filter(g -> g.getTotalScore() >= 70 && g.getTotalScore() < 80).count();
        long passMarkCount = grades.stream().filter(g -> g.getTotalScore() >= 60 && g.getTotalScore() < 70).count();
        long failCount = grades.stream().filter(g -> g.getTotalScore() < 60).count();

        int total = grades.size();

        Map<String, Object> distribution = new LinkedHashMap<>();
        distribution.put("excellent", excellentCount);
        distribution.put("good", goodCount);
        distribution.put("medium", mediumCount);
        distribution.put("pass", passMarkCount);
        distribution.put("fail", failCount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("average", Math.round(stats.getAverage() * 100) / 100.0);
        result.put("max", stats.getMax());
        result.put("min", stats.getMin());
        result.put("passRate", Math.round((double) passCount / total * 10000) / 100.0 + "%");
        result.put("excellentRate", Math.round((double) excellentCount / total * 10000) / 100.0 + "%");
        result.put("distribution", distribution);

        Map<String, Double> courseAvgMap = new LinkedHashMap<>();
        Map<String, Integer> courseCountMap = new LinkedHashMap<>();
        for (Grade grade : grades) {
            courseAvgMap.merge(grade.getCourseId(), grade.getTotalScore(), Double::sum);
            courseCountMap.merge(grade.getCourseId(), 1, Integer::sum);
        }

        List<Map<String, Object>> courseAvgList = new ArrayList<>();
        for (Map.Entry<String, Double> entry : courseAvgMap.entrySet()) {
            Course course = courseMapper.selectById(entry.getKey());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", course != null ? course.getName() : entry.getKey());
            item.put("avg", Math.round(entry.getValue() / courseCountMap.get(entry.getKey()) * 100) / 100.0);
            courseAvgList.add(item);
        }
        result.put("courseAvgList", courseAvgList);

        return result;
    }

    // 校验分数
    private void validateScore(Grade grade) {
        if (grade.getUsualScore() == null && grade.getExamScore() == null) {
            throw new BusinessException("平时成绩和考试成绩至少填写一项");
        }
        if (grade.getUsualScore() != null && (grade.getUsualScore() < 0 || grade.getUsualScore() > 100)) {
            throw new BusinessException("平时成绩必须在 0~100 之间");
        }
        if (grade.getExamScore() != null && (grade.getExamScore() < 0 || grade.getExamScore() > 100)) {
            throw new BusinessException("考试成绩必须在 0~100 之间");
        }
    }

    // 计算总分
    private void calculateTotalScore(Grade grade) {
        if (grade.getExamScore() == null) {
            grade.setTotalScore(0.0);
            return;
        }
        if (grade.getUsualScore() == null) {
            return;
        }

        String courseNature = getCourseNature(grade.getCourseId());
        double[] weights = SCORE_WEIGHTS.getOrDefault(courseNature, new double[]{0.3, 0.7});
        double total = grade.getUsualScore() * weights[0] + grade.getExamScore() * weights[1];
        grade.setTotalScore(Math.round(total * 100) / 100.0);
    }

    // 获取课程性质
    private String getCourseNature(String courseId) {
        LambdaQueryWrapper<TeachingPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeachingPlan::getCourseId, courseId);
        wrapper.last("LIMIT 1");
        TeachingPlan plan = teachingPlanMapper.selectOne(wrapper);
        return plan != null ? plan.getCourseNature() : "compulsory";
    }

    // 新增或更新成绩
    private void upsertGrade(Grade grade) {
        if (grade.getGradeId() != null && grade.getGradeId().trim().isEmpty()) {
            grade.setGradeId(null);
        }

        Grade existingGrade = findExistingGrade(grade);
        if (existingGrade != null) {
            if ("approved".equals(existingGrade.getStatus())) {
                throw new BusinessException("该学生该课程已存在已通过审核的成绩记录");
            }
            grade.setGradeId(existingGrade.getGradeId());
            this.updateById(grade);
            return;
        }

        this.save(grade);
    }

    // 查找现有成绩
    private Grade findExistingGrade(Grade grade) {
        if (grade == null || grade.getStudentId() == null || grade.getCourseId() == null
                || grade.getSemesterId() == null || grade.getTeacherId() == null) {
            return null;
        }

        LambdaQueryWrapper<Grade> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Grade::getStudentId, grade.getStudentId())
                .eq(Grade::getCourseId, grade.getCourseId())
                .eq(Grade::getSemesterId, grade.getSemesterId())
                .eq(Grade::getTeacherId, grade.getTeacherId())
                .orderByDesc(Grade::getUpdatedAt, Grade::getCreatedAt)
                .last("LIMIT 1");
        return this.getOne(wrapper, false);
    }
}
