package com.staffboard.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    USER_NOT_FOUND(1001, "用户不存在"),
    PASSWORD_INCORRECT(1002, "密码错误"),
    TOKEN_EXPIRED(1003, "Token已过期"),
    TOKEN_INVALID(1004, "Token无效"),
    USER_DISABLED(1005, "用户已禁用"),
    EMPLOYEE_NOT_FOUND(2001, "员工不存在"),
    EMPLOYEE_NO_DUPLICATE(2002, "工号已存在"),
    IMPORT_FILE_EMPTY(2003, "导入文件为空"),
    IMPORT_FILE_TOO_LARGE(2004, "导入文件超过大小限制"),
    IMPORT_FORMAT_ERROR(2005, "导入文件格式错误"),
    IMPORT_DATA_VALIDATION_FAIL(2006, "导入数据校验失败"),
    BUDGET_NOT_FOUND(3001, "预算记录不存在"),
    BUDGET_DUPLICATE(3002, "该部门该季度预算已存在");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public HttpStatus httpStatus() {
        return switch (this) {
            case BAD_REQUEST, IMPORT_DATA_VALIDATION_FAIL -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED, TOKEN_EXPIRED, TOKEN_INVALID -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN, USER_DISABLED -> HttpStatus.FORBIDDEN;
            case NOT_FOUND, USER_NOT_FOUND, EMPLOYEE_NOT_FOUND, BUDGET_NOT_FOUND -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
