package com.example.coursematch.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * 敏感内容脱敏。观察事实在服务端保存原文的同时生成脱敏文本，
 * 讲师视图只能拿到脱敏结果——不是单纯在前端遮住文字。
 *
 * <p>规则（按标注的敏感类型）：
 * <ul>
 *   <li>STUDENT_ID 学员身份：手机号、身份证号、邮箱、“学员XXX”称谓统一替换</li>
 *   <li>COMPLAINT 投诉原文：整段以摘要占位，不向讲师下发原话</li>
 *   <li>INTERNAL_JUDGEMENT 内部判断：整段不下发（投影层直接置空），此处不做脱敏</li>
 * </ul>
 */
@Service
public class SensitiveMaskingService {

    private static final Pattern PHONE = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final Pattern ID_CARD = Pattern.compile("(?<!\\d)\\d{17}[0-9Xx](?!\\d)");
    private static final Pattern EMAIL = Pattern.compile("[\\w.\\-]+@[\\w.\\-]+\\.[A-Za-z]{2,}");
    private static final Pattern STUDENT_REF = Pattern.compile("学员(?:姓名)?[“\"']?[\\u4e00-\\u9fa5A-Za-z0-9·]{1,10}[”\"']?");

    /**
     * 按敏感类型生成脱敏文本。
     *
     * @param text          原始事实
     * @param sensitiveType STUDENT_ID / COMPLAINT / INTERNAL_JUDGEMENT（可为空，空则按身份类通用规则）
     * @return 脱敏文本；INTERNAL_JUDGEMENT 返回 null（讲师视图不下发）
     */
    public String mask(String text, String sensitiveType) {
        if (text == null) {
            return null;
        }
        if ("INTERNAL_JUDGEMENT".equals(sensitiveType)) {
            // 内部判断不进入讲师视图
            return null;
        }
        if ("COMPLAINT".equals(sensitiveType)) {
            // 投诉原文不向讲师逐字开放，仅保留存在性提示
            return "【投诉原文已脱敏：含学员投诉记录，详情仅督导/负责人可见】";
        }
        return maskStudentIdentity(text);
    }

    /** 学员身份类：去除可直接定位到具体学员的信息 */
    public String maskStudentIdentity(String text) {
        if (text == null) {
            return null;
        }
        String masked = ID_CARD.matcher(text).replaceAll("身份证号***");
        masked = PHONE.matcher(masked).replaceAll("1**********");
        masked = EMAIL.matcher(masked).replaceAll("***@***");
        masked = STUDENT_REF.matcher(masked).replaceAll("某学员");
        return masked;
    }
}
