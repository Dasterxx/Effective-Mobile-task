package nikita.com.bankcards.util;

public record ApiResponse<T>(String message, T data) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("Success", data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(message, null);
    }
}
