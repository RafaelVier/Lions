package org.lions.backend.presentation.handler;

import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;
import org.lions.backend.domain.exception.BusinessException;
import org.lions.backend.domain.exception.ConflictException;
import org.lions.backend.domain.exception.ResourceNotFoundException;
import org.lions.backend.domain.exception.UnauthorizedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tradução de exceções em {@link ProblemDetail} (RFC 7807) conforme plan §1.
 * Erros de validação trazem {@code errors} (campo → mensagem); {@link ConflictException} mescla {@code details}.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String ERRORS = "errors";
    static final String VALIDATION_TITLE = "Dados inválidos";
    static final String VALIDATION_DETAIL = "Um ou mais campos são inválidos.";

    // ---------- 400 ----------

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.merge(error.getField(), message(error), (a, b) -> a + "; " + b);
        }
        for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            errors.merge(error.getObjectName(), message(error), (a, b) -> a + "; " + b);
        }
        return badRequest(errors, headers);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            @NonNull HandlerMethodValidationException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getAllValidationResults().forEach(result -> {
            String name = result.getMethodParameter().getParameterName();
            String key = name != null ? name : "parametro" + result.getMethodParameter().getParameterIndex();
            result.getResolvableErrors().forEach(error ->
                    errors.merge(key, error.getDefaultMessage() != null ? error.getDefaultMessage() : "Valor inválido.",
                            (a, b) -> a + "; " + b));
        });
        return badRequest(errors, headers);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            @NonNull HttpMessageNotReadableException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (ex.getCause() instanceof JsonMappingException mapping && !mapping.getPath().isEmpty()) {
            String field = mapping.getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                    .collect(Collectors.joining("."));
            errors.put(field, "Valor inválido ou em formato incorreto.");
        } else {
            errors.put("body", "Corpo da requisição ausente ou malformado.");
        }
        return badRequest(errors, headers);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            @NonNull MissingServletRequestParameterException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        return badRequest(Map.of(ex.getParameterName(), "Parâmetro obrigatório."), headers);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            @NonNull TypeMismatchException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        String name = ex.getPropertyName() != null ? ex.getPropertyName() : "parametro";
        return badRequest(Map.of(name, "Valor inválido: " + ex.getValue()), headers);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errors.merge(leafName(violation.getPropertyPath()), violation.getMessage(), (a, b) -> a + "; " + b);
        }
        return badRequest(errors, new HttpHeaders());
    }

    // ---------- 401 / 403 ----------

    @ExceptionHandler({UnauthorizedException.class, AuthenticationException.class})
    public ResponseEntity<ProblemDetail> handleUnauthorized(RuntimeException ex) {
        String detail = ex instanceof UnauthorizedException
                ? ex.getMessage()
                : "Usuário ou senha inválidos, ou usuário inativo.";
        return problem(HttpStatus.UNAUTHORIZED, "Não autenticado", detail);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Acesso negado", "Seu perfil não tem permissão para esta operação.");
    }

    // ---------- 404 ----------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage());
    }

    // ---------- 409 ----------

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(ConflictException ex) {
        ResponseEntity<ProblemDetail> response = problem(HttpStatus.CONFLICT, "Conflito", ex.getMessage());
        ProblemDetail body = response.getBody();
        if (body != null) {
            ex.getDetails().forEach(body::setProperty);
        }
        return response;
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    public ResponseEntity<ProblemDetail> handleConcurrency(RuntimeException ex) {
        log.info("Conflito de concorrência: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "Conflito",
                "O registro foi alterado por outra operação. Atualize os dados e tente novamente.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "Conflito",
                "A operação viola uma restrição de integridade (registro duplicado ou referenciado).");
    }

    // ---------- 422 ----------

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusiness(BusinessException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio violada", ex.getMessage());
    }

    // ---------- 500 ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Erro inesperado", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente ou contate o administrador.");
    }

    // ---------- helpers ----------

    private static ResponseEntity<Object> badRequest(Map<String, String> errors, HttpHeaders headers) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, VALIDATION_DETAIL);
        problem.setTitle(VALIDATION_TITLE);
        problem.setProperty(ERRORS, errors);
        return ResponseEntity.badRequest().headers(headers).body(problem);
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return ResponseEntity.status(status).body(problem);
    }

    private static String message(ObjectError error) {
        return error.getDefaultMessage() != null ? error.getDefaultMessage() : "Valor inválido.";
    }

    private static String leafName(Path path) {
        String name = null;
        for (Path.Node node : path) {
            name = node.getName();
        }
        return name != null ? name : path.toString();
    }
}
