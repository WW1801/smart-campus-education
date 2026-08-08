package com.campus.education.common;

import com.campus.education.entity.Student;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 为尚未执行数据迁移的历史学生生成稳定的展示学号，不修改数据库记录。 */
public final class StudentNoResolver {

    private StudentNoResolver() {
    }

    public static boolean isStandard(Student student) {
        if (student == null) return false;
        String prefix = prefix(student);
        String studentNo = student.getStudentNo();
        return prefix != null && studentNo != null
                && studentNo.matches("\\d{13}") && studentNo.startsWith(prefix);
    }

    public static void fillDisplayStudentNos(List<Student> students) {
        if (students == null || students.isEmpty()) return;
        Map<String, List<Student>> groups = new HashMap<>();
        for (Student student : students) {
            String prefix = prefix(student);
            if (prefix != null) groups.computeIfAbsent(prefix, key -> new ArrayList<>()).add(student);
        }

        Comparator<Student> order = Comparator
                .comparing(Student::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(Student::getStudentId, Comparator.nullsFirst(String::compareTo));
        for (Map.Entry<String, List<Student>> entry : groups.entrySet()) {
            String prefix = entry.getKey();
            List<Student> group = entry.getValue();
            group.sort(order);
            Set<Integer> usedSequences = new HashSet<>();
            for (Student student : group) {
                if (isStandard(student)) {
                    usedSequences.add(Integer.parseInt(student.getStudentNo().substring(prefix.length())));
                }
            }
            int nextSequence = 1;
            for (Student student : group) {
                if (isStandard(student)) continue;
                while (usedSequences.contains(nextSequence) && nextSequence <= 999) nextSequence++;
                if (nextSequence > 999) continue;
                student.setStudentNo(prefix + String.format("%03d", nextSequence));
                usedSequences.add(nextSequence++);
            }
        }
    }

    private static String prefix(Student student) {
        if (student == null || student.getDepartmentId() == null || student.getMajorId() == null) return null;
        int year = student.getEnrollmentDate() == null
                ? LocalDate.now().getYear() : student.getEnrollmentDate().getYear();
        String departmentCode = businessCode(student.getDepartmentId());
        String majorCode = businessCode(student.getMajorId());
        return departmentCode == null || majorCode == null ? null : year + departmentCode + majorCode;
    }

    private static String businessCode(String value) {
        String digits = value.replaceAll("\\D", "");
        if (digits.isEmpty()) return null;
        return String.format("%3s", digits.substring(Math.max(0, digits.length() - 3))).replace(' ', '0');
    }
}
