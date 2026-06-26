package club.muimi.backend.exception;

import club.muimi.backend.common.api.ApiResponse;
import club.muimi.backend.common.api.ErrorCode;
import club.muimi.backend.common.api.FieldValidationError;
import club.muimi.backend.common.api.ValidationErrorBody;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(errorCode, exception.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ValidationErrorBody>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception
    ) {
        List<FieldValidationError> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldValidationError)
                .toList();
        ValidationErrorBody body = new ValidationErrorBody(fieldErrors);
        return ResponseEntity.status(ErrorCode.UNPROCESSABLE_ENTITY.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.UNPROCESSABLE_ENTITY, ErrorCode.UNPROCESSABLE_ENTITY.getDefaultMessage(), body));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException exception) {
        return ResponseEntity.status(ErrorCode.UNPROCESSABLE_ENTITY.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.UNPROCESSABLE_ENTITY, exception.getMessage(), null));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResourceFound(NoResourceFoundException exception) {
        return ResponseEntity.status(ErrorCode.NOT_FOUND.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.getDefaultMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpectedException(Exception exception) {
        log.error("发生未处理异常", exception);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(ApiResponse.failure(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage(), null));
    }

    private FieldValidationError toFieldValidationError(FieldError fieldError) {
        return new FieldValidationError(fieldError.getField(), fieldError.getDefaultMessage());
    }
}
