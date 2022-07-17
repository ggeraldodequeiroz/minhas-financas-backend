package com.guilherme.minhasfinancas.api.resources;

import com.guilherme.minhasfinancas.api.dto.AtualizaStatusDTO;
import com.guilherme.minhasfinancas.api.dto.LancamentoDTO;
import com.guilherme.minhasfinancas.exception.RegraNegocioException;
import com.guilherme.minhasfinancas.model.entity.Lancamento;
import com.guilherme.minhasfinancas.model.entity.Usuario;
import com.guilherme.minhasfinancas.model.enums.StatusLancamento;
import com.guilherme.minhasfinancas.model.enums.TipoLancamento;
import com.guilherme.minhasfinancas.service.LancamentoService;
import com.guilherme.minhasfinancas.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/lancamentos")
@RequiredArgsConstructor
public class LancamentoResource {

    private final LancamentoService service;
    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity buscar(
            @RequestParam(value="descricao", required = false) String descricao,
            @RequestParam(value="mes", required = false) Integer mes,
            @RequestParam(value="ano", required = false) Integer ano,
            @RequestParam(value="usuario") Long idUsuario) {

        Lancamento lancamentoFiltro = new Lancamento();
        lancamentoFiltro.setDescricao(descricao);
        lancamentoFiltro.setMes(mes);
        lancamentoFiltro.setAno(ano);

        Optional<Usuario> usuario = usuarioService.obterPorId(idUsuario);
        if(!usuario.isPresent()) {
            return ResponseEntity.badRequest().body("Não foi possível realizar a consulta. Usuário não encontrado para o Id informado.");
        } else {
            lancamentoFiltro.setUsuario(usuario.get());
        }

        List<Lancamento> lancamentos = service.buscar(lancamentoFiltro);
        return ResponseEntity.ok(lancamentos);
    }

    @GetMapping("{id}")
    public ResponseEntity obterLancamento( @PathVariable("id") Long id ) {
        return service.obterPorId(id)
                .map( lancamento -> new ResponseEntity(converter(lancamento), HttpStatus.OK ) )
                .orElseGet( () -> new ResponseEntity(HttpStatus.NOT_FOUND) );

    }

    @PostMapping
    public ResponseEntity salvar( @RequestBody LancamentoDTO dto ) {
        try {
            Lancamento entidade = converter(dto);
            entidade = service.salvar(entidade);
            return new ResponseEntity(entidade, HttpStatus.CREATED);
        } catch (RegraNegocioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("{id}")
    public ResponseEntity atualizar( @PathVariable("id") Long id, @RequestBody LancamentoDTO dto ) {
        return service.obterPorId(id).map( entidade -> {
            try {
                Lancamento lancamento = converter(dto);
                lancamento.setId(entidade.getId());
                service.atualizar(lancamento);
                return ResponseEntity.ok(lancamento);
            } catch(RegraNegocioException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }).orElseGet( () -> new ResponseEntity("Lançamento não encontrado na base de dados.", HttpStatus.BAD_REQUEST) );
    }

    @PutMapping("{id}/atualiza-status")
    public ResponseEntity atualizarStatus( @PathVariable Long id, @RequestBody AtualizaStatusDTO dto  ) {
        return service.obterPorId(id).map( entidade -> {
            StatusLancamento statusLancamento = StatusLancamento.valueOf(dto.getStatus());

            if(statusLancamento == null) {
                return ResponseEntity.badRequest().body("Não foi possível atualizar o status do lançamento, envie um status válido.");
            }

            try {
                entidade.setStatus(statusLancamento);
                service.atualizar(entidade);
                return ResponseEntity.ok(entidade);
            } catch (RegraNegocioException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }).orElseGet( () -> new ResponseEntity("Lancamento não encontrado na base de Dados.", HttpStatus.BAD_REQUEST) );
    }


    @DeleteMapping("{id}")
    public ResponseEntity deletar ( @PathVariable("id") Long id ) {
        return service.obterPorId(id).map( entidade -> {
            service.deletar(entidade);
            return new ResponseEntity( HttpStatus.NO_CONTENT );
        }).orElseGet( () -> new ResponseEntity("Lançamento não encontrado na base de dados.", HttpStatus.BAD_REQUEST));
    }

    private LancamentoDTO converter(Lancamento lancamento) {
        return LancamentoDTO.builder()
                .id(lancamento.getId())
                .descricao(lancamento.getDescricao())
                .valor(lancamento.getValor())
                .mes(lancamento.getMes())
                .ano(lancamento.getAno())
                .status(lancamento.getStatus().name())
                .tipo(lancamento.getTipo().name())
                .usuario(lancamento.getUsuario().getId())
                .build();
    }

    private Lancamento converter( LancamentoDTO dto ) {
        Lancamento lancamento = new Lancamento();
        lancamento.setId(dto.getId());
        lancamento.setDescricao(dto.getDescricao());
        lancamento.setAno(dto.getAno());
        lancamento.setMes(dto.getMes());
        lancamento.setValor(dto.getValor());

        Usuario usuario = usuarioService
                .obterPorId(dto.getUsuario())
                .orElseThrow( () -> new RegraNegocioException("Usuário não encontrado para o Id informado.") );

        lancamento.setUsuario(usuario);

        if(dto.getTipo() != null) {
            lancamento.setTipo(TipoLancamento.valueOf(dto.getTipo()));
        }

        if(dto.getStatus() != null) {
            lancamento.setStatus(StatusLancamento.valueOf(dto.getStatus()));
        }

        return lancamento;
    }

}
