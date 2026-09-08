package com.gestaocompras.exception;

import com.gestaocompras.dto.ErroResposta;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> handleValidacao(MethodArgumentNotValidException ex) {
        List<String> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(ErroResposta.of(HttpStatus.BAD_REQUEST.value(), "Requisição inválida",
                        "Alguns campos não passaram na validação.", erros));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroResposta> handleViolacaoConstraint(ConstraintViolationException ex) {
        List<String> erros = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .toList();
        return ResponseEntity.badRequest()
                .body(ErroResposta.of(HttpStatus.BAD_REQUEST.value(), "Requisição inválida",
                        "Alguns parâmetros não passaram na validação.", erros));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> handleCorpoMalformado(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ErroResposta.of(HttpStatus.BAD_REQUEST.value(), "Requisição inválida",
                        "Corpo da requisição malformado ou ausente."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResposta> handleTipoArgumentoInvalido(MethodArgumentTypeMismatchException ex) {
        String detalhe = "O parâmetro '" + ex.getName() + "' recebeu um valor inválido: " + ex.getValue();
        return ResponseEntity.badRequest()
                .body(ErroResposta.of(HttpStatus.BAD_REQUEST.value(), "Requisição inválida",
                        "Parâmetro com tipo incorreto.", List.of(detalhe)));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResposta> handleParametroAusente(MissingServletRequestParameterException ex) {
        String detalhe = "O parâmetro obrigatório '" + ex.getParameterName() + "' não foi informado.";
        return ResponseEntity.badRequest()
                .body(ErroResposta.of(HttpStatus.BAD_REQUEST.value(), "Requisição inválida",
                        "Parâmetro ausente na requisição.", List.of(detalhe)));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResposta> handleMetodoNaoSuportado(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErroResposta.of(HttpStatus.METHOD_NOT_ALLOWED.value(), "Método não permitido",
                        "O método HTTP utilizado não é suportado para este recurso."));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResposta> handleMediaTypeNaoSuportada(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ErroResposta.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), "Tipo de mídia não suportado",
                        "O Content-Type da requisição não é suportado."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResposta> handleRecursoNaoEncontrado(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErroResposta.of(HttpStatus.NOT_FOUND.value(), "Não encontrado",
                        "O recurso solicitado não existe."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> handleIntegridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErroResposta.of(HttpStatus.CONFLICT.value(), "Conflito de dados",
                        "O registro viola uma restrição de integridade (duplicidade ou referência inválida)."));
    }

    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErroResposta> handleConflitoConcorrencia(
            org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErroResposta.of(HttpStatus.CONFLICT.value(), "Conflito de concorrência",
                        "Outra operação alterou este registro ao mesmo tempo. Recarregue os dados e tente novamente."));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErroResposta> handleNaoEncontrado(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErroResposta.of(HttpStatus.NOT_FOUND.value(), "Não encontrado", ex.getMessage()));
    }

    @ExceptionHandler(NaoMembroException.class)
    public ResponseEntity<ErroResposta> handleNaoMembro(NaoMembroException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErroResposta.of(HttpStatus.FORBIDDEN.value(), "Acesso negado", ex.getMessage()));
    }

    @ExceptionHandler(SaldoInsuficienteException.class)
    public ResponseEntity<ErroResposta> handleSaldoInsuficiente(SaldoInsuficienteException ex) {
        return ResponseEntity.badRequest()
                .body(ErroResposta.of(HttpStatus.BAD_REQUEST.value(), "Regra de negócio violada",
                        ex.getMessage()));
    }

    @ExceptionHandler(RegistroDuplicadoException.class)
    public ResponseEntity<ErroResposta> handleRegistroDuplicado(RegistroDuplicadoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErroResposta.of(HttpStatus.CONFLICT.value(), "Registro duplicado", ex.getMessage()));
    }

    @ExceptionHandler(OperacaoNaoPermitidaException.class)
    public ResponseEntity<ErroResposta> handleOperacaoNaoPermitida(OperacaoNaoPermitidaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErroResposta.of(HttpStatus.CONFLICT.value(), "Operação não permitida", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResposta> handleArgumentoInvalido(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ErroResposta.of(HttpStatus.BAD_REQUEST.value(), "Requisição inválida", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<ErroResposta> handleCredenciaisInvalidas(
            org.springframework.security.authentication.BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErroResposta.of(HttpStatus.UNAUTHORIZED.value(), "Não autenticado",
                        ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> handleGenerico(Exception ex) {
        log.error("Erro interno não tratado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErroResposta.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erro interno",
                        "Ocorreu um erro inesperado. Tente novamente mais tarde."));
    }
}
