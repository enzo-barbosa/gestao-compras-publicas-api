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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> handleCorpoMalformado(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ErroResposta.of(HttpStatus.BAD_REQUEST.value(), "Requisição inválida",
                        "Corpo da requisição malformado ou ausente."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> handleIntegridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErroResposta.of(HttpStatus.CONFLICT.value(), "Conflito de dados",
                        "O registro viola uma restrição de integridade (duplicidade ou referência inválida)."));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErroResposta> handleNaoEncontrado(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErroResposta.of(HttpStatus.NOT_FOUND.value(), "Não encontrado", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> handleGenerico(Exception ex) {
        log.error("Erro interno não tratado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErroResposta.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erro interno",
                        "Ocorreu um erro inesperado. Tente novamente mais tarde."));
    }
}
