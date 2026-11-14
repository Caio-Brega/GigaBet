package br.ufmg.dcc603.apostas.aplicacao;

import br.ufmg.dcc603.apostas.dominio.Aposta;
import br.ufmg.dcc603.apostas.dominio.Apostador;
import br.ufmg.dcc603.apostas.dominio.Partida;
import br.ufmg.dcc603.apostas.dominio.StatusAposta;
import br.ufmg.dcc603.apostas.dominio.StatusPartida;
import br.ufmg.dcc603.apostas.persistencia.ApostaRepository;
import br.ufmg.dcc603.apostas.persistencia.UsuarioRepository;

public final class ApuracaoService {
    private final ApostaRepository apostas;
    private final UsuarioRepository usuarios;

    public ApuracaoService(ApostaRepository apostas, UsuarioRepository usuarios) {
        this.apostas = apostas;
        this.usuarios = usuarios;
    }

    public ResumoApuracao apurar(Partida partida) {
        if (partida.status() != StatusPartida.ENCERRADA || partida.resultado() == null) {
            throw new IllegalStateException("Partida encerrada com resultado e obrigatoria para apuracao");
        }
        int vencedoras = 0;
        int perdedoras = 0;
        for (Aposta aposta : apostas.listarPorPartida(partida.id())) {
            if (aposta.status() != StatusAposta.ABERTA) {
                continue;
            }
            if (aposta.avaliar(partida.resultado())) {
                aposta.marcarVencedora();
                Apostador apostador = (Apostador) usuarios.buscarPorId(aposta.apostadorId())
                        .orElseThrow(() -> new IllegalStateException("Apostador da aposta nao encontrado"));
                apostador.creditar(aposta.premioPotencial());
                usuarios.salvar(apostador);
                vencedoras++;
            } else {
                aposta.marcarPerdedora();
                perdedoras++;
            }
            apostas.salvar(aposta);
        }
        return new ResumoApuracao(vencedoras + perdedoras, vencedoras, perdedoras);
    }
}
