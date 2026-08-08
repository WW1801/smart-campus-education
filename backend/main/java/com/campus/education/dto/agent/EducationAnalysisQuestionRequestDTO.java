package com.campus.education.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 教务分析问答请求对象，用于接收用户问题和限定指标查询范围。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EducationAnalysisQuestionRequestDTO {

    /** 用户输入的自然语言问题。 */
    private String question;

    /** 查询所属学期主键；为空时表示不限制学期。 */
    private String semesterId;

    /** 查询所属院系主键；为空时表示不限制院系。 */
    private String departmentId;

    /** 查询所属专业主键；为空时表示不限制专业。 */
    private String majorId;

    /** 查询所属班级主键；为空时表示不限制班级。 */
    private String classId;

    /** Target student for student-scoped fixed analysis tools. */
    private String studentId;

    /** The second student, required only by the student_comparison tool. */
    private String compareStudentId;
}
