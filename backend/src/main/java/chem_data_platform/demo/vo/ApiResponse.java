package chem_data_platform.demo.vo;

/**
 * 统一响应格式
 */
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    // 无参构造器
    public ApiResponse() {
    }

    // 全参构造器
    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // Getter/Setter
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<T>(200, "Success", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<T>(200, message, data);
    }

    /**
     * 失败响应
     */
    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<T>(code, message, null);
    }

    public static <T> ApiResponse<T> fail(int code, String message, T data) {
        return new ApiResponse<T>(code, message, data);
    }

    // 常用状态码方法
    public static <T> ApiResponse<T> badRequest(String message) {
        return fail(400, message);
    }

    public static <T> ApiResponse<T> unauthorized(String message) {
        return fail(401, message);
    }

    public static <T> ApiResponse<T> forbidden(String message) {
        return fail(403, message);
    }

    public static <T> ApiResponse<T> notFound(String message) {
        return fail(404, message);
    }

    public static <T> ApiResponse<T> conflict(String message) {
        return fail(409, message);
    }

    public static <T> ApiResponse<T> serverError(String message) {
        return fail(500, message);
    }
}
